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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Application;
import ro.pippo.core.Messages;
import ro.pippo.core.ParameterValue;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.Request;
import ro.pippo.core.Response;
import ro.pippo.core.Session;
import ro.pippo.core.SimpleTypeReference;
import ro.pippo.core.util.StringUtils;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author James Moger
 */
public class DefaultRouteContext implements RouteContext {

    private static final Logger log = LoggerFactory.getLogger(DefaultRouteContext.class);

    protected final Application application;
    protected final Request request;
    protected final Response response;
    protected final Iterator<RouteMatch> iterator;

    private Route route;

    public DefaultRouteContext(Application application, Request request, Response response, List<RouteMatch> routeMatches) {
        this.application = application;
        this.request = request;
        this.response = response;
        this.iterator = routeMatches.iterator();
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public boolean hasSession() {
        return request.getSession(false) != null;
    }

    @Override
    public Session getSession() {
        return request.getSession();
    }

    @Override
    public void recreateSession() {
        request.recreateSession();
    }

    @Override
    public void resetSession() {
        request.resetSession();
    }

    @Override
    public void touchSession() {
        request.getSession().touch();
    }

    @Override
    public void invalidateSession() {
        request.getSession().invalidate();
    }

    @Override
    public <T> T setSession(String name, T value) {
        getSession().put(name, value);

        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSession(String name) {
        return hasSession() ? (T) request.getSession().get(name) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeSession(String name) {
        return hasSession() ? (T) request.getSession().remove(name) : null;
    }

    @Override
    public <T> T setLocal(String name, T value) {
        response.getLocals().put(name, value);

        return value;
    }

    @Override
    public void setLocals(Map<String, Object> locals) {
        response.getLocals().putAll(locals);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getLocal(String name) {
        return (T) response.getLocals().get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeLocal(String name) {
        return (T) response.getLocals().remove(name);
    }

    @Override
    public ParameterValue getParameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public <T> T setHeader(String name, T value) {
        response.header(name, value.toString());

        return value;
    }

    @Override
    public Date setHeader(String name, Date date) {
        response.header(name, date);

        return date;
    }

    @Override
    public void flashError(String message, Object... args) {
        request.getSession().getFlash().error(message, args);
    }

    @Override
    public void flashWarning(String message, Object... args) {
        request.getSession().getFlash().warning(message, args);
    }

    @Override
    public void flashInfo(String message, Object... args) {
        request.getSession().getFlash().info(message, args);
    }

    @Override
    public void flashSuccess(String message, Object... args) {
        request.getSession().getFlash().success(message, args);
    }

    @Override
    public <T> T createEntityFromParameters(Class<T> classOfT) {
        return request.createEntityFromParameters(classOfT);
    }

    @Override
    public <T> T createEntityFromBody(SimpleTypeReference simpleTypeReference) {
        return request.createEntityFromBody(simpleTypeReference);
    }

    @Override
    public String getRequestUri() {
        return request.getApplicationUri();
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod();
    }

    @Override
    public boolean isRequestMethod(String method) {
        return request.getMethod().equalsIgnoreCase(method);
    }

    @Override
    public void render(String templateName) {
        response.render(templateName);
    }

    @Override
    public void render(String templateName, Map<String, Object> model) {
        response.render(templateName, model);
    }

    @Override
    public String renderToString(String templateName) {
        return response.renderToString(templateName);
    }

    @Override
    public String renderToString(String templateName, Map<String, Object> model) {
        return response.renderToString(templateName, model);
    }

    @Override
    public void send(CharSequence content) {
        response.send(content);
    }

    @Override
    public void send(File file) {
        response.file(file);
    }

    @Override
    public void send(Object object) {
        response.send(object);
    }

    @Override
    public void redirect(String path) {
        response.redirectToApplicationPath(path);
    }

    @Override
    public void redirect(String nameOrUriPattern, Map<String, Object> parameters) {
        String uri = uriFor(nameOrUriPattern, parameters);

        String applicationPath = application.getRouter().getApplicationPath();
        if (!applicationPath.isEmpty()) {
            // remove application path
            uri = StringUtils.removeStart(uri.substring(applicationPath.length()), "/");
        }

        redirect(uri);
    }

    @Override
    public void next() {
        if (iterator.hasNext()) {
            // retrieves the next route
            RouteMatch routeMatch = iterator.next();
            Route route = routeMatch.getRoute();
            log.trace("Found {}", route);

            // set the new path parameters in request
            Map<String, String> pathParameters = routeMatch.getPathParameters();
            if (pathParameters != null) {
                request.internalSetPathParameters(pathParameters);
                log.trace("Added path parameters to request");
            }

            // remove route from chain
            iterator.remove();

            handleRoute(route);
        }
    }

    /**
     * Execute all routes that are flagged to run as finally.
     */
    @Override
    public void runFinallyRoutes() {
        while (iterator.hasNext()) {
            Route route = iterator.next().getRoute();
            if (route.isRunAsFinally()) {
                try {
                    handleRoute(route);
                } catch (Exception e) {
                    log.error("Unexpected error in Finally Route", e);
                }
            } else if (log.isDebugEnabled()) {
                if (StringUtils.isNullOrEmpty(route.getName())) {
                    log.debug("context.next() not called, skipping handler for {} '{}'", route.getRequestMethod(),
                        route.getUriPattern());
                } else {
                    log.debug("context.next() not called, skipping '{}' for {} '{}'", route.getName(),
                        route.getRequestMethod(), route.getUriPattern());
                }
            }
        }
    }

    @Override
    public RouteContext text() {
        response.text();

        return this;
    }

    @Override
    public RouteContext xml() {
        response.xml();

        return this;
    }

    @Override
    public RouteContext json() {
        response.json();

        return this;
    }

    @Override
    public RouteContext yaml() {
        response.yaml();

        return this;
    }

    @Override
    public RouteContext html() {
        response.html();

        return this;
    }

    @Override
    public RouteContext negotiateContentType() {
        response.contentType(request);

        return this;
    }

    @Override
    public RouteContext status(int code) {
        response.status(code);

        return this;
    }

    @Override
    public String uriFor(String nameOrUriPattern, Map<String, Object> parameters) {
        return application.getRouter().uriFor(nameOrUriPattern, parameters);
    }

    @Override
    public Messages getMessages() {
        return application.getMessages();
    }

    @Override
    public String getMessage(String key, Object... args) {
        return application.getMessages().get(key, this, args);
    }

    @Override
    public String getMessage(String key, String language, Object... args) {
        return application.getMessages().get(key, language, args);
    }

    @Override
    public Route getRoute() {
        return route;
    }

    @Override
    public PippoSettings getSettings() {
        return application.getPippoSettings();
    }

    @SuppressWarnings("unchecked")
    protected void handleRoute(Route route) {
        if (StringUtils.isNullOrEmpty(route.getName())) {
            log.debug("Executing handler for {} '{}'", route.getRequestMethod(), route.getUriPattern());
        } else {
            log.debug("Executing '{}' for {} '{}'", route.getName(), route.getRequestMethod(), route.getUriPattern());
        }

        this.route = route;

        route.getRouteHandler().handle(this);
    }

}
