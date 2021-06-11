/*
 * Copyright (C) 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.pippo.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.kohsuke.MetaInfServices;
import ro.pippo.core.Application;
import ro.pippo.core.ContentTypeEngine;
import ro.pippo.core.HttpConstants;

import java.sql.Time;
import java.util.Date;

/**
 * A JsonEngine based on Gson.
 *
 * @author James Moger
 */
@MetaInfServices
public class GsonEngine implements ContentTypeEngine {

    private Gson gson;

    @Override
    public void init(Application application) {
        this.gson = buildGson(application);
    }

    @Override
    public String getContentType() {
        return HttpConstants.ContentType.APPLICATION_JSON;
    }

    @Override
    public String toString(Object object) {
        return gson.toJson(object);
    }

    @Override
    public <T> T fromString(String content, Class<T> classOfT) {
        return gson.fromJson(content, classOfT);
    }

    protected Gson buildGson(Application application) {
        return new GsonBuilder()
            .registerTypeAdapter(Date.class, new ISO8601DateTimeTypeAdapter())
            .registerTypeAdapter(Time.class, new ISO8601TimeTypeAdapter())
            .registerTypeAdapter(java.sql.Date.class, new ISO8601DateTypeAdapter())
            .create();
    }

}
