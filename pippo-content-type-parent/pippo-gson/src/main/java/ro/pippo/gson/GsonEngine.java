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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import org.kohsuke.MetaInfServices;
import ro.pippo.core.Application;
import ro.pippo.core.ContentTypeEngine;
import ro.pippo.core.HttpConstants;

import java.lang.reflect.Type;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        this.gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new ISO8601DateTimeTypeAdapter())
            .registerTypeAdapter(Time.class, new ISO8601TimeTypeAdapter())
            .registerTypeAdapter(java.sql.Date.class, new ISO8601DateTypeAdapter())
            .create();
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

    @Override
    public <T> T fromString(String content, Type typeOfT) {
        return gson.fromJson(content, typeOfT);
    }

    public static class ISO8601DateTypeAdapter implements JsonSerializer<java.sql.Date>, JsonDeserializer<java.sql.Date> {
        private final DateFormat dateFormat;

        public ISO8601DateTypeAdapter() {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }

        @Override
        public synchronized JsonElement serialize(java.sql.Date date, Type type,
                                                  JsonSerializationContext jsonSerializationContext) {
            synchronized (dateFormat) {
                String dateFormatAsString = dateFormat.format(date);
                return new JsonPrimitive(dateFormatAsString);
            }
        }

        @Override
        public synchronized java.sql.Date deserialize(JsonElement jsonElement, Type type,
                                                      JsonDeserializationContext jsonDeserializationContext) {
            try {
                synchronized (dateFormat) {
                    Date date = dateFormat.parse(jsonElement.getAsString());
                    return new java.sql.Date((date.getTime() / 1000) * 1000);
                }
            } catch (ParseException e) {
                throw new JsonSyntaxException(jsonElement.getAsString(), e);
            }
        }
    }

    public static class ISO8601TimeTypeAdapter implements JsonSerializer<Time>, JsonDeserializer<Time> {
        private final DateFormat timeFormat;

        public ISO8601TimeTypeAdapter() {
            timeFormat = new SimpleDateFormat("HH:mm:ssZ", Locale.US);
        }

        @Override
        public synchronized JsonElement serialize(Time time, Type type,
                                                  JsonSerializationContext jsonSerializationContext) {
            synchronized (timeFormat) {
                String timeFormatAsString = timeFormat.format(time);
                return new JsonPrimitive(timeFormatAsString);
            }
        }

        @Override
        public synchronized Time deserialize(JsonElement jsonElement, Type type,
                                             JsonDeserializationContext jsonDeserializationContext) {
            try {
                synchronized (timeFormat) {
                    Date date = timeFormat.parse(jsonElement.getAsString());
                    return new Time((date.getTime() / 1000) * 1000);
                }
            } catch (ParseException e) {
                throw new JsonSyntaxException(jsonElement.getAsString(), e);
            }
        }
    }

    public static class ISO8601DateTimeTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
        private final DateFormat dateTimeFormat;

        public ISO8601DateTimeTypeAdapter() {
            dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        }

        @Override
        public synchronized JsonElement serialize(Date date, Type type,
                                                  JsonSerializationContext jsonSerializationContext) {
            synchronized (dateTimeFormat) {
                String dateFormatAsString = dateTimeFormat.format(date);
                return new JsonPrimitive(dateFormatAsString);
            }
        }

        @Override
        public synchronized Date deserialize(JsonElement jsonElement, Type type,
                                             JsonDeserializationContext jsonDeserializationContext) {
            try {
                synchronized (dateTimeFormat) {
                    Date date = dateTimeFormat.parse(jsonElement.getAsString());
                    return new Date((date.getTime() / 1000) * 1000);
                }
            } catch (ParseException e) {
                throw new JsonSyntaxException(jsonElement.getAsString(), e);
            }
        }
    }
}
