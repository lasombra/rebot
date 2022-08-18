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
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import xyz.rebasing.rebot.api.conf.BotConfig;
import xyz.rebasing.rebot.api.domain.MessageUpdate;
import xyz.rebasing.rebot.api.i18n.I18nHelper;
import xyz.rebasing.rebot.api.shared.components.httpclient.IRebotOkHttpClient;
import xyz.rebasing.rebot.api.spi.CommandProvider;
import xyz.rebasing.rebot.service.persistence.repository.NotionRepository;


@ApplicationScoped
public class NotionCommand implements CommandProvider {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @ConfigProperty(name = "xyz.rebasing.rebot.plugin.notion.token")
    String token;

    @Inject
    BotConfig config;

    @Inject
    private NotionRepository notion;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    IRebotOkHttpClient okclient;

    Request request;
    @Override
    public void load() {
        // on startup set the locale to en
        log.debugv("Loading command {0}", this.name());
    }

    @Override
    public Object execute(Optional<String> key, MessageUpdate messageUpdate, String locale) {
//        var filter = new HashMap<>();
//        filter.put("value", "database");
//        filter.put("property", "object");
        var body = new HashMap<>();
        body.put("", "");
//        body.put("filter", filter);

        request = new Request.Builder()
                .url("https://api.notion.com/v1/search")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Notion-Version", "2022-06-28")
                .post(RequestBody.create("", okclient.mediaTypeJson()))
                .build();

        try (Response response = okclient.get().newCall(request).execute()) {
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
//        switch (key.get().toLowerCase()) {
//            case "add":
//                log.info("/notion add");
//                break;
//            case "info":
//                log.info("/notion info");
//                break;
//            default:
//                log.info("/notion no_command");
//                return null;
//        }
//
//        return null;
//        return response;
    }

    @Override
    public String name() {
        return "/notion";
    }

    @Override
    public String help(String locale) {
        return this.name() + " - " + String.format(I18nHelper.resource("NotionMessages",
                                                                       locale, "notion.help"));
    }

    @Override
    public String description(String locale) {
        return String.format(I18nHelper.resource("NotionMessages",
                                                 locale, "notion.description"));
    }

    @Override
    public boolean deleteMessage() {
        return config.deleteMessages();
    }

    @Override
    public long deleteMessageTimeout() {
        return config.deleteMessagesAfter();
    }
}
