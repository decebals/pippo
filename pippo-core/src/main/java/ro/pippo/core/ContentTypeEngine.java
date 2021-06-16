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
package ro.pippo.core;

import java.lang.reflect.Type;

/**
 * @author James Moger
 */
public interface ContentTypeEngine {

    void init(Application application);

    String getContentType();

    String toString(Object object);

    <T> T fromString(String content, Class<T> classOfT);

    // TODO: Instead of the "Type typeOfT", is it better to receive SimpleTypeReference right away? Thus giving more flexibility.
    default <T> T fromString(String content, Type typeOfT) {
        // TODO: default to fromString(String, Class<T>) with Class.forName(typeOfT.getTypeName()) ? Ugly and fragile idea !!!
        // TODO: throw new PippoRuntimeException("Not implemented"); ?
        return null;
    }

}
