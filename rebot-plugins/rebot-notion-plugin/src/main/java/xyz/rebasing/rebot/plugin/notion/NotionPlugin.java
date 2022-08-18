/*
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2017 Rebasing.xyz ReBot
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of
 *   this software and associated documentation files (the "Software"), to deal in
 *   the Software without restriction, including without limitation the rights to
 *   use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *   the Software, and to permit persons to whom the Software is furnished to do so,
 *   subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package xyz.rebasing.rebot.plugin.notion;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import xyz.rebasing.rebot.api.conf.BotConfig;
import xyz.rebasing.rebot.api.domain.MessageUpdate;
import xyz.rebasing.rebot.api.emojis.Emoji;
import xyz.rebasing.rebot.api.i18n.I18nHelper;
import xyz.rebasing.rebot.api.shared.components.httpclient.IRebotOkHttpClient;
import xyz.rebasing.rebot.api.spi.PluginProvider;
import xyz.rebasing.rebot.service.persistence.domain.Notion;
import xyz.rebasing.rebot.service.persistence.repository.NotionRepository;

import static xyz.rebasing.rebot.api.utils.Formatter.normalize;

@ApplicationScoped
public class NotionPlugin implements PluginProvider {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final Pattern FULL_MSG_PATTERN = Pattern.compile("(\\w*)(\\+\\+|\\-\\-|\\—|\\–)(\\s|$)");
    private final Pattern NOTION_PATTERN = Pattern.compile("(^\\S+)(\\+\\+|\\-\\-|\\—|\\–)($)");

    @Inject
    BotConfig config;

    @ConfigProperty(name = "xyz.rebasing.rebot.plugin.notion.timeout", defaultValue = "30")
    Long timeout;

    @ConfigProperty(name = "xyz.rebasing.rebot.plugin.notion.token")
    String token;

    @Inject
    NotionRepository notion;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    IRebotOkHttpClient okclient;

    Request request;

    Cache<String, Integer> notionCache;

    @Override
    public void load() {
        new Thread(() -> {
            notionCache = Caffeine.newBuilder()
                    .removalListener((String key, Integer value, RemovalCause cause) ->
                                             log.debugv("entry {0}={1} removed from the cache, cause: {2}",
                                                        key,
                                                        value,
                                                        cause))
                    .evictionListener((String key, Integer value, RemovalCause cause) ->
                                              log.debugv("entry {0}={1} evicted from the cache, cause: {2}",
                                                         key,
                                                         value,
                                                         cause))
                    .expireAfterWrite(timeout, TimeUnit.SECONDS)
                    .build();
            log.debug("Plugin notion-plugin enabled.");
        }).start();
    }

    @Override
    public String name() {
        return "notion";
    }

    @Override
    public String process(MessageUpdate update, String locale) {
        var filter = new HashMap<>();
        filter.put("value", "database");
        filter.put("property", "object");
        var body = new HashMap<>();
        body.put("query", "");
        body.put("filter", filter);


        try (Response response = okclient.get().newCall(request).execute()) {
            request = new Request.Builder()
                    .url("https://api.notion.com/v1/search")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(RequestBody.create(objectMapper.writeValueAsString(body), okclient.mediaTypeJson()))
                    .build();

            if (!response.isSuccessful()) {
                log.warnv("Error received from Telegram API, status code is {0}", response.code());
            }

            return response.body().string();

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteMessage() {
        return config.deleteMessages();
    }

    @Override
    public long deleteMessageTimeout() {
        return config.deleteMessagesAfter();
    }

    /**
     * Process the notion, to trigger it is necessary to use ++ or -- at the end of any string.
     *
     * @param operator ++ or --
     * @param target   key that will have its notion changed
     * @param username user that requested the notion
     * @return the amount of notion + or - 1, or does nothing in case of excessive notion update for the same target
     */
    private String processNotion(String operator, String target, String username, String locale) {

        if (target.equals(username)) {
            return String.format(I18nHelper.resource("NotionMessages", locale, "own.notion"),
                                 Emoji.DIZZY_FACE);
        }

        final int currentNotion = notion.get(target);
        switch (operator) {
            case "++":
                if (notionCache.getIfPresent(target + ":" + username) == null) {
                    // update it when Quarkus cache supports event listeners
                    notionCache.put(target + ":" + username, currentNotion + 1);
                    notion.updateOrCreateNotion(new Notion(target, String.valueOf(currentNotion + 1)));
                }
                break;

            case "--":
                if (notionCache.getIfPresent(target + ":" + username) == null) {
                    // update it when Quarkus cache supports event listeners
                    notionCache.put(target + ":" + username, currentNotion - 1);
                    notion.updateOrCreateNotion(new Notion(target, String.valueOf(currentNotion - 1)));
                }
                break;

            default:
                //do nothing
                break;
        }
        return String.format(I18nHelper.resource("NotionMessages", locale, "notion.updated"),
                             normalize(target), notionCache.getIfPresent(target + ":" + username));
    }

    /**
     * Verifies if the received text can be processed by this plugin
     *
     * @param messageContent
     * @return true if the message matches the notion pattern, otherwise returns false
     */
    private boolean canProcess(String messageContent) {
        boolean canProcess = null != messageContent && FULL_MSG_PATTERN.matcher(messageContent).find();
        log.debugv("Notion plugin - can process [{0}] - {1}", messageContent, canProcess);
        return canProcess;
    }
}