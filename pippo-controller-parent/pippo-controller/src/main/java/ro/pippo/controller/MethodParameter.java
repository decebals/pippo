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
package ro.pippo.controller;

import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.SimpleTypeReference;
import ro.pippo.core.util.LangUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A wrapper over {@link Parameter}.
 *
 * @author Decebal Suiu
 */
public class MethodParameter {

    private final Method method;
    private final int parameterIndex;

    private volatile Parameter parameter;
    private volatile String parameterName;
    private volatile Class<?> parameterType;
    private volatile Class<?> parameterGenericType;
    private volatile Type parameterizedType;

    public MethodParameter(Method method, int parameterIndex) {
        this.method = method;
        this.parameterIndex = parameterIndex;
    }

    public SimpleTypeReference toTypeReference() {
        return new SimpleTypeReference(getParameterType(), getParameterizedType());
    }

    public Method getMethod() {
        return method;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    /**
     * Try looking for the parameter name in the compiled .class file.
     *
     * @return
     */
    public String getParameterName() {
        if (parameterName == null) {
            Parameter parameter = getParameter();
            if (parameter.isNamePresent()) {
                parameterName = parameter.getName();
            }
        }

        return parameterName;
    }

    public Class<?> getParameterType() {
        if (parameterType == null) {
            parameterType = getParameter().getType();
        }

        return parameterType;
    }

    public Type getParameterizedType() {
        if (parameterizedType == null) {
            parameterizedType = getParameter().getParameterizedType();
        }

        return parameterizedType;
    }

    public Class<?> getParameterGenericType() {
        if (parameterGenericType == null) {
            Parameter parameter = getParameter();

            Type parameterType = parameter.getParameterizedType();
            if (!ParameterizedType.class.isAssignableFrom(parameterType.getClass())) {
                throw new PippoRuntimeException("Please specify a generic parameter type for '{}' of '{}'",
                    getParameterName(), LangUtils.toString(method));
            }

            ParameterizedType parameterizedType = (ParameterizedType) parameterType;
            try {
                parameterGenericType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            } catch (ClassCastException e) {
                throw new PippoRuntimeException("Please specify a generic parameter type for '{}' of '{}'",
                    getParameterName(), LangUtils.toString(method));
            }
        }

        return parameterGenericType;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getParameter().isAnnotationPresent(annotationClass);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return getParameter().getAnnotation(annotationClass);
    }

    private Parameter getParameter() {
        if (parameter == null) {
            parameter = method.getParameters()[parameterIndex];
        }

        return parameter;
    }

}
