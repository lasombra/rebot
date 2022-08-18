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

package xyz.rebasing.rebot.api.shared.components.httpclient;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Priority;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Alternative
@Priority(100)
@ApplicationScoped
@DefaultBean
public class RebotOkHttpClient implements IRebotOkHttpClient {

    @ConfigProperty(name = "xyz.rebasing.rebot.api.http.debug.level", defaultValue = "none")
    String debugLevel;

    HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();

    @Override
    public OkHttpClient get() {

        if ("headers".equals(debugLevel)) {
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        } else if ("body".equals(debugLevel)) {
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else if ("basic".equals(debugLevel)) {
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        httpLoggingInterceptor.redactHeader("Authorization");
        httpLoggingInterceptor.redactHeader("Cookie");

        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor)
                .build();
    }
}