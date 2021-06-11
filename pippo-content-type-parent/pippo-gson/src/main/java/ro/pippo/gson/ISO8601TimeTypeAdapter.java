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

import java.lang.reflect.Type;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

public class ISO8601TimeTypeAdapter implements JsonSerializer<Time>, JsonDeserializer<Time> {

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
                return new Time(date.getTime());
            }
        } catch (ParseException e) {
            throw new JsonSyntaxException(jsonElement.getAsString(), e);
        }
    }

}
