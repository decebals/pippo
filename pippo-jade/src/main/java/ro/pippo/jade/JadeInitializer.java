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
package ro.pippo.jade;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.pippo.core.Application;
import ro.pippo.core.Initializer;

/**
 * @author Decebal Suiu
 */
@MetaInfServices(Initializer.class)
public class JadeInitializer implements Initializer {

    private static final Logger log = LoggerFactory.getLogger(JadeInitializer.class);

    @Override
    public void init(Application application) {
        application.registerTemplateEngine(JadeTemplateEngine.class);
    }

    @Override
    public void destroy(Application application) {
    }

}
