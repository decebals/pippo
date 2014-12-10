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
package ro.fortsoft.pippo.pebble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import ro.fortsoft.pippo.core.PippoRuntimeException;
import ro.fortsoft.pippo.core.route.ClasspathResourceHandler;
import ro.fortsoft.pippo.core.route.Router;

import com.mitchellbosecke.pebble.extension.Function;

/**
 * Base class for handling classpath resource url generation from a Pebble template.
 *
 * @author James Moger
 * @param <X>
 */
abstract class ClasspathResourceFunction<X extends ClasspathResourceHandler> implements Function {

        final Router router;
        final Class<X> resourceHandlerClass;
        final AtomicReference<String> patternRef;

        protected ClasspathResourceFunction(Router router, Class<X> resourceHandlerClass) {
            this.router = router;
            this.resourceHandlerClass = resourceHandlerClass;
            this.patternRef = new AtomicReference<>();
        }

        @Override
        public List<String> getArgumentNames() {
            List<String> names = new ArrayList<>();
            names.add(ClasspathResourceHandler.PATH_PARAMETER);
            return names;
        }

        @Override
        public Object execute(Map<String, Object> args) {
            if (patternRef.get() == null) {
                String pattern = router.urlPatternFor(resourceHandlerClass);
                if (pattern == null) {
                    throw new PippoRuntimeException("You must register a route for {}",
                            resourceHandlerClass.getSimpleName());
                }
                patternRef.set(pattern);
            }

            String path = (String) args.get(ClasspathResourceHandler.PATH_PARAMETER);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(ClasspathResourceHandler.PATH_PARAMETER, path);
            String url = router.urlFor(patternRef.get(), parameters);
            return url;
        }

    }