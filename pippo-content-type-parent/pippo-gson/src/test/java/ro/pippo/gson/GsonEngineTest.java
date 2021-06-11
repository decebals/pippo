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

import java.sql.Time;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author James Moger
 * @author Dwouglas Mhagnum
 */
public class GsonEngineTest extends Assert {

    GsonEngine engine;

    @Before
    public void setUp() {
        engine = new GsonEngine();
        engine.init(null);

        // This test runs in Australia's time zone = GMT+10 (arbitrary)
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Sydney"));
    }

    @Test
    public void testEngine() {
        MyTest test = new MyTest();

        String json = engine.toString(test);

        MyTest result = engine.fromString(json, MyTest.class);
        assertEquals(test.message, result.message);
    }

    @Test
    public void testDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        Date now = cal.getTime();

        MyTest test = new MyTest();
        test.date = now;

        String json = engine.toString(test);

        MyTest result = engine.fromString(json, MyTest.class);
        assertEquals(test.message, result.message);

        assertTrue(test.date.equals(result.date));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDates_ISO8601Time() {
        // Given
        String messageExpected = "Hello!";
        String json = MyTest.jsonForTime(messageExpected, "21:33:44-0700");

        // When
        MyTest result = engine.fromString(json, MyTest.class);

        // Then
        assertEquals(messageExpected, result.message);
        // 14:33:44 will be the time from 21:33:44 GMT-7 to GMT+10 (see @Before)
        assertEquals(14, result.time.getHours());
        assertEquals(33, result.time.getMinutes());
        assertEquals(44, result.time.getSeconds());
    }

    @Test
    public void testDates_ISO8601DateTime_v1() {
        // Given
        String messageExpected = "Hello!";

        ZonedDateTime zonedDateTime = ZonedDateTime.of(2019, 12, 28, 8, 53, 38, 0, ZoneId.of("UTC-3"));
        Date dateExpected = toDate(zonedDateTime);

        String json = MyTest.jsonForDate(messageExpected, "2019-12-28T08:53:38-0300");

        // When
        MyTest result = engine.fromString(json, MyTest.class);

        // Then
        assertEquals(messageExpected, result.message);
        assertTrue(dateExpected.equals(result.date));
    }

    @Test
    public void testDates_ISO8601DateTime_v2() {
        // Given
        String messageExpected = "Hello!";

        ZonedDateTime zonedDateTime = ZonedDateTime.of(2019, 12, 27, 20, 00, 00, 0, ZoneId.of("UTC-0"));
        Date dateExpected = toDate(zonedDateTime);

        String json = MyTest.jsonForDate(messageExpected, "2019-12-27T20:00:00.000Z");

        // When
        MyTest result = engine.fromString(json, MyTest.class);

        // Then
        assertEquals(messageExpected, result.message);
        assertTrue(dateExpected.equals(result.date));
    }

    public static class MyTest {


        public String message = "Hooray!";

        public Date date = new Date();

        public Time time;

        static String jsonForDate(String message, String date) {
            final String TEMPLATE = "{\"message\":\":message\",\"date\":\":date\"}";
            return TEMPLATE.replace(":message", message).replace(":date", date);
        }

        static String jsonForTime(String message, String time) {
            final String TEMPLATE = "{\"message\":\":message\",\"time\":\":time\"}";
            return TEMPLATE.replace(":message", message).replace(":time", time);
        }

    }

    private Date toDate(ZonedDateTime zdt) {
        Instant instant = zdt.toInstant();
        Instant instantTruncatedToMilliseconds = Instant.ofEpochMilli( instant.toEpochMilli() );
        return Date.from( instantTruncatedToMilliseconds );
    }

}
