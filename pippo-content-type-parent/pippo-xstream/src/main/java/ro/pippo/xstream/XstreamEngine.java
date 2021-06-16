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
package ro.pippo.xstream;

import com.thoughtworks.xstream.security.NoTypePermission;
import org.kohsuke.MetaInfServices;
import ro.pippo.core.Application;
import ro.pippo.core.ContentTypeEngine;
import ro.pippo.core.HttpConstants;

import com.thoughtworks.xstream.XStream;
import ro.pippo.core.util.WhitelistObjectInputStream;

import java.util.regex.Pattern;

/**
 * An XmlEngine based on XStream.
 *
 * @author James Moger
 */
@MetaInfServices
public class XstreamEngine implements ContentTypeEngine {

    @Override
    public void init(Application application) {
        // XStream - it may be shared across multiple threads allowing objects to be serialized/deserialized concurrently,
        // unless you enable the auto-detection to process annotations on-the-fly - which is our case with `autodetectAnnotations(true)`
        // https://x-stream.github.io/faq.html#Scalability_Thread_safety
    }

    @Override
    public String getContentType() {
        return HttpConstants.ContentType.APPLICATION_XML;
    }

    private XStream xstream() {
        XStream xstream = new XStream();
        // allow annotations on models for maximum flexibility
        xstream.autodetectAnnotations(true);
        // prevent xstream from creating complex XML graphs
        xstream.setMode(XStream.NO_REFERENCES);

        // clear out existing permissions and set own ones
        xstream.addPermission(NoTypePermission.NONE);

        //setup security
        xstream.allowTypes((String[]) WhitelistObjectInputStream.getWhitelistedClassNames().toArray());
        xstream.allowTypesByRegExp((Pattern[]) WhitelistObjectInputStream.getWhitelistedRegExp().toArray());
        return xstream;
    }

    @Override
    public String toString(Object object) {
        return xstream().toXML(object);
    }

    @Override
    public <T> T fromString(String content, Class<T> classOfT) {
        return (T) xstream().fromXML(content);
    }

}
