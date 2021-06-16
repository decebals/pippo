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
package ro.pippo.controller.extractor;

import org.kohsuke.MetaInfServices;

import ro.pippo.controller.MethodParameter;
import ro.pippo.core.route.RouteContext;

/**
 * @author Decebal Suiu
 * @author James Moger
 */
@MetaInfServices
public class BodyExtractor implements MethodParameterExtractor {

    @Override
    public boolean isApplicable(MethodParameter parameter) {
        return parameter.isAnnotationPresent(Body.class);
    }

    @Override
    public Object extract(MethodParameter parameter, RouteContext routeContext) {
        return routeContext.createEntityFromBody(parameter.toTypeReference());
    }

}
