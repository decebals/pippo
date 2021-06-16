/*
 * Copyright (C) 2015 the original author or authors.
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
package ro.pippo.core.route;

import ro.pippo.core.Application;
import ro.pippo.core.Messages;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.Request;
import ro.pippo.core.Response;
import ro.pippo.core.Session;
import ro.pippo.core.SimpleTypeReference;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * @author James Moger
 */
public interface RouteContext {

    <T extends Application> T getApplication();

    Request getRequest();

    Response getResponse();

    boolean hasSession();

    Session getSession();

    void recreateSession();

    void resetSession();

    void touchSession();

    void invalidateSession();

    <T> T setSession(String name, T value);

    <T> T getSession(String name);

    <T> T removeSession(String name);

    <T> T setLocal(String name, T value);

    void setLocals(Map<String, Object> locals);

    <T> T getLocal(String name);

    <T> T removeLocal(String name);

    ParameterValue getParameter(String name);

    String getHeader(String name);

    <T> T setHeader(String name, T value);

    Date setHeader(String name, Date date);

    void flashError(String message, Object... args);

    void flashWarning(String message, Object... args);

    void flashInfo(String message, Object... args);

    void flashSuccess(String message, Object... args);

    <T> T createEntityFromParameters(Class<T> classOfT);

    <T> T createEntityFromBody(SimpleTypeReference simpleTypeReference);

    String getRequestUri();

    String getRequestMethod();

    boolean isRequestMethod(String method);

    /**
     * Renders a template and writes the output directly to the response.
     * <p>This method commits the response.</p>
     *
     * @param templateName
     */
    void render(String templateName);

    /**
     * Renders a template and writes the output directly to the response.
     * <p>This method commits the response.</p>
     *
     * @param templateName
     * @param model
     */
    void render(String templateName, Map<String, Object> model);

    /**
     * Renders a template to a {@link String}.
     *
     * @param templateName
     */
    String renderToString(String templateName);

    /**
     * Renders a template to a {@link String}.
     *
     * @param templateName
     * @param model
     */
    String renderToString(String templateName, Map<String, Object> model);

    void send(CharSequence content);

    void send(File file);

    void send(Object object);

    void redirect(String path);

    void redirect(String nameOrUriPattern, Map<String, Object> parameters);

    void next();

    void runFinallyRoutes();

    RouteContext text();

    RouteContext xml();

    RouteContext json();

    RouteContext yaml();

    RouteContext html();

    RouteContext negotiateContentType();

    RouteContext status(int code);

    String uriFor(String nameOrUriPattern, Map<String, Object> parameters);

    Messages getMessages();

    String getMessage(String key, Object... args);

    String getMessage(String key, String language, Object... args);

    PippoSettings getSettings();

    Route getRoute();

}
