/*
 * Copyright (C) 2016 the original author or authors.
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
package ro.pippo.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;

import static ro.pippo.core.util.StringUtils.isNullOrEmpty;

/**
 * @author idealzh
 */
public class FilteringObjectInputStream extends ObjectInputStream {
    private ArrayList whiteClassNames = new ArrayList<String>();

    public FilteringObjectInputStream(InputStream in) throws IOException {
        super(in);

        whiteClassNames.add("ro.pippo.session.DefaultSessionData");
        whiteClassNames.add("java.util.HashMap");
        whiteClassNames.add("ro.pippo.core.Flash");
        whiteClassNames.add("java.util.ArrayList");
    }

    protected Class<?> resolveClass(ObjectStreamClass descriptor) throws ClassNotFoundException, IOException {
        String className = descriptor.getName();
        if (isNullOrEmpty(className) && !isWhiteListed(className)) {
            throw new InvalidClassException("Unauthorized deserialization attempt", descriptor.getName());
        } else {
            return super.resolveClass(descriptor);
        }
    }

    private boolean isWhiteListed(String className) {
        if (className == null) return false;
        for (Object name : whiteClassNames) {
            if (name.equals(className)) return true;
        }
        return false;
    }
}
