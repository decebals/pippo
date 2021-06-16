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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.pippo.core.converters.Converter;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteDispatcher;
import ro.pippo.core.util.ClassUtils;
import ro.pippo.core.util.CookieUtils;
import ro.pippo.core.util.IoUtils;
import ro.pippo.core.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a server-side HTTP request. An instance of this class is created
 * for each request.
 *
 * @author Decebal Suiu
 */
public final class Request {

    private static final Logger log = LoggerFactory.getLogger(Request.class);

    private HttpServletRequest httpServletRequest;
    private ContentTypeEngines contentTypeEngines;
    private Map<String, ParameterValue> parameters; // query&post parameters
    private Map<String, ParameterValue> pathParameters; // path parameters
    private Map<String, ParameterValue> allParameters; // parameters + pathParameters
    private Map<String, FileItem> files;
    private Session session;
    private String applicationPath;
    private String method;
    private String path;

    private String acceptType;
    private String contentType;
    private String body; // cache

    public Request(HttpServletRequest servletRequest, Application application) {
        this.httpServletRequest = servletRequest;
        this.contentTypeEngines = application.getContentTypeEngines();

        applicationPath = application.getRouter().getApplicationPath();

        // fill (query&post) parameters if any
        initParameters();

        // empty path parameters for now (see internalSetPathParameters method)
        pathParameters = Collections.unmodifiableMap(new HashMap<String, ParameterValue>());

        // init all parameters
        initAllParameters();
    }

    /**
     * Returns all parameters (query, post, path).
     */
    public Map<String, ParameterValue> getParameters() {
        return allParameters;
    }

    /**
     * Returns one parameter value.
     */
    public ParameterValue getParameter(String name) {
        if (!getParameters().containsKey(name)) {
            return buildParameterValue();
        }

        return getParameters().get(name);
    }

    /**
     * Returns all query&post parameters.
     */
    public Map<String, ParameterValue> getQueryParameters() {
        return parameters;
    }

    /**
     * Returns one query parameter value.
     */
    public ParameterValue getQueryParameter(String name) {
        if (!getQueryParameters().containsKey(name)) {
            return buildParameterValue();
        }

        return getQueryParameters().get(name);
    }

    /**
     * Returns all path parameters.
     */
    public Map<String, ParameterValue> getPathParameters() {
        return pathParameters;
    }

    /**
     * Returns one path parameter.
     */
    public ParameterValue getPathParameter(String name) {
        if (!getPathParameters().containsKey(name)) {
            return buildParameterValue();
        }

        return getPathParameters().get(name);
    }

    private void initParameters() {
        Map<String, Map<Integer, String>> arrays = new HashMap<>();
        Map<String, ParameterValue> tmp = new HashMap<>();
        Enumeration<String> names = httpServletRequest.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();

            if (name.matches("(.+)\\[(\\d+)\\]")) {
                // support indexed parameter arrays e.g. setting[0], setting[1], setting[2]
                int brk = name.indexOf('[');
                String base = name.substring(0, brk);

                // try-catch for requests in the form //server/?a[123123123123123123123123123123]
                try {
                    int idx = Integer.parseInt(name.substring(brk + 1, name.length() - 1));
                    if (!arrays.containsKey(base)) {
                        // use an ordered map because we can not rely on parameter
                        // order from the servlet container nor from the request
                        arrays.put(base, new TreeMap<>());
                    }

                    Map<Integer, String> map = arrays.get(base);
                    String value = httpServletRequest.getParameterValues(name)[0];
                    map.put(idx, value);
                } catch (NumberFormatException e) {
                    // add as a simple parameter
                    String[] values = httpServletRequest.getParameterValues(name);
                    tmp.put(name, buildParameterValue(values));
                }
            } else {
                String[] values = httpServletRequest.getParameterValues(name);
                tmp.put(name, buildParameterValue(values));
            }
        }

        for (Map.Entry<String, Map<Integer, String>> entry : arrays.entrySet()) {
            // identify maximum specified index
            int maxIndex = 0;
            for (int index : entry.getValue().keySet()) {
                if (index > maxIndex) {
                    maxIndex = index;
                }
            }

            // populate array & respect specified indexes
            // Note: this may not be linear but we must respect that design choice
            String[] values = new String[maxIndex + 1];
            for (Map.Entry<Integer, String> indexedValue : entry.getValue().entrySet()) {
                values[indexedValue.getKey()] = indexedValue.getValue();
            }
            tmp.put(entry.getKey(), buildParameterValue(values));
        }

        parameters = Collections.unmodifiableMap(tmp);
    }

    private void initPathParameters(Map<String, String> map) {
        Map<String, ParameterValue> tmp = new HashMap<>();
        if (map != null) {
            Set<String> names = map.keySet();
            for (String name : names) {
                tmp.put(name, buildParameterValue(map.get(name)));
            }
        }

        pathParameters = Collections.unmodifiableMap(tmp);
    }

    private void initAllParameters() {
        Map<String, ParameterValue> tmp = new HashMap<>();

        // add query parameters
        tmp.putAll(parameters);

        // add path parameters
        tmp.putAll(pathParameters);

        allParameters = Collections.unmodifiableMap(tmp);
    }

    /**
     * THIS METHOD IS NOT PART OF THE PIPPO PUBLIC API.
     */
    public void internalSetPathParameters(Map<String, String> pathParameters) {
        initPathParameters(pathParameters);
        initAllParameters();
    }

    public <T> T createEntityFromParameters(Class<T> entityClass) {
        T entity;
        try {
            entity = entityClass.newInstance();
        } catch (Exception e) {
            log.error("Cannot create new instance of class '{}'", entityClass.getName(), e);
            return null;
        }

        updateEntityFromParameters(entity);

        return entity;
    }

    @SuppressWarnings("unchecked")
    public <T, X> T updateEntityFromParameters(T entity) {
        for (Field field : ClassUtils.getAllFields(entity.getClass())) {
            String parameterName = field.getName();
            ParamField parameter = field.getAnnotation(ParamField.class);
            if (parameter != null) {
                parameterName = parameter.value();
            }

            if (getParameters().containsKey(parameterName)) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                String pattern = (parameter != null) ? parameter.pattern() : null;
                Class<? extends Converter> converterClass = (parameter != null && void.class != parameter.converterClass()) ? parameter.converterClass() : null;

                try {
                    Class<?> fieldClass = field.getType();
                    Object value;
                    if (converterClass == null) {
                        if (Collection.class.isAssignableFrom(fieldClass)) {
                            Type parameterType = field.getGenericType();
                            if (!ParameterizedType.class.isAssignableFrom(parameterType.getClass())) {
                                throw new PippoRuntimeException("Please specify a generic parameter type for field '{}' {}",
                                        field.getName(), fieldClass.getName());
                            }
                            ParameterizedType parameterizedType = (ParameterizedType) parameterType;
                            Class<X> genericClass;
                            try {
                                genericClass = (Class<X>) parameterizedType.getActualTypeArguments()[0];
                            } catch (ClassCastException e) {
                                throw new PippoRuntimeException("Please specify a generic parameter type for field '{}' {}",
                                        field.getName(), fieldClass.getName());
                            }

                            if (Set.class == fieldClass) {
                                value = getParameters().get(parameterName).toSet(genericClass, pattern);
                            } else if (List.class == fieldClass) {
                                value = getParameters().get(parameterName).toList(genericClass, pattern);
                            } else if (fieldClass.isInterface()) {
                                throw new PippoRuntimeException("Field '{}' collection '{}' is not a supported type!",
                                        field.getName(), fieldClass.getName());
                            } else {
                                Class<? extends Collection> collectionClass = (Class<? extends Collection>) fieldClass;
                                value = getParameters().get(parameterName).toCollection(collectionClass, genericClass, pattern);
                            }
                        } else {
                            value = getParameters().get(parameterName).to(fieldClass, pattern);
                        }
                    } else {
                        value = getParameters().get(parameterName).convert(converterClass, pattern);
                    }
                    field.set(entity, value);
                } catch (IllegalAccessException e) {
                    log.error("Cannot set value for field '{}' from parameter '{}'", field.getName(), parameterName, e);
                } catch (PippoRuntimeException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return entity;
    }

    public <T> T createEntityFromBody(SimpleTypeReference simpleTypeReference) {
        try {
            String body = getBody();
            if (StringUtils.isNullOrEmpty(body)) {
                log.warn("Can not create entity '{}' from null or empty request body!", simpleTypeReference.getTypeName());
                return null;
            }

            // try to determine the body content-type
            String contentType = getContentType();
            if (StringUtils.isNullOrEmpty(contentType)) {
                // sloppy client, try the accept header
                contentType = getAcceptType();
            }

            if (StringUtils.isNullOrEmpty(contentType)) {
                throw new PippoRuntimeException(
                    "Failed to create entity '{}' from request body because 'content-type' is not specified!",
                    simpleTypeReference.getTypeName());
            }

            ContentTypeEngine engine = contentTypeEngines.getContentTypeEngine(contentType);
            if (engine == null) {
                throw new PippoRuntimeException(
                    "Failed to create entity '{}' from request body because a content engine for '{}' could not be found!",
                    simpleTypeReference.getTypeName(), contentType);
            }

            return engine.fromString(body, simpleTypeReference.getParameterizedType());
        } catch (PippoRuntimeException e) {
            // pass-through PippoRuntimeExceptions
            throw e;
        } catch (Exception e) {
            // capture and re-throw all other exceptions
            throw new PippoRuntimeException(e, "Failed to create entity '{}' from request body!", simpleTypeReference.getTypeName());
        }
    }

    public String getHost() {
        return httpServletRequest.getHeader(HttpConstants.Header.HOST);
    }

    public String getUserAgent() {
        return httpServletRequest.getHeader(HttpConstants.Header.USER_AGENT);
    }

    public int getPort() {
        return httpServletRequest.getServerPort();
    }

    public String getClientIp() {
        return httpServletRequest.getRemoteAddr();
    }

    public int getContentLength() {
        return httpServletRequest.getContentLength();
    }

    public String getScheme() {
        return httpServletRequest.getScheme();
    }

    public String getAcceptType() {
        if (acceptType == null) {
            acceptType = httpServletRequest.getHeader(HttpConstants.Header.ACCEPT);
            // try to specify an AcceptType from an registered ContentType suffix
            String suffix = StringUtils.getFileExtension(getPath());
            if (!StringUtils.isNullOrEmpty(suffix)) {
                ContentTypeEngine engine = contentTypeEngines.getContentTypeEngine(suffix);
                if (engine != null) {
                    acceptType = engine.getContentType();
                }
            }
        }
        return acceptType;
    }

    public String getContentType() {
        if (contentType == null) {
            String httpServletRequestContentType = httpServletRequest.getHeader(HttpConstants.Header.CONTENT_TYPE);
            if (HttpConstants.Method.POST.equals(httpServletRequest.getMethod())
                && (HttpConstants.ContentType.APPLICATION_FORM_URLENCODED.equals(httpServletRequestContentType)
                || HttpConstants.ContentType.MULTIPART_FORM_DATA.equals(httpServletRequestContentType))) {
                // Allow forms to exercise RESTful API endpoints by POSTing content like 'application/json'.
                // This parameter is usually paired with '_method' and '_content' parameters.
                contentType = getParameter("_content_type").toString(httpServletRequestContentType);
            } else {
                contentType = httpServletRequestContentType;
            }
        }
        return contentType;
    }

    /**
     * It's wrapper over {@link HttpServletRequest#getLocale}.
     */
    public Locale getLocale() {
        return httpServletRequest.getLocale();
    }

    public String getBody() {
        if (body == null) {
            String httpServletRequestContentType = httpServletRequest.getHeader(HttpConstants.Header.CONTENT_TYPE);
            if (HttpConstants.Method.POST.equals(httpServletRequest.getMethod())
                && (HttpConstants.ContentType.APPLICATION_FORM_URLENCODED.equals(httpServletRequestContentType)
                || HttpConstants.ContentType.MULTIPART_FORM_DATA.equals(httpServletRequestContentType))) {
                // Allow forms to exercise RESTful API endpoints by POSTing content like 'application/json'.
                // This parameter is usually paired with '_method' and '_content_type' parameters.
                body = getParameter("_content").toString(null);
            } else {
                try {
                    body = IoUtils.toString(httpServletRequest.getInputStream());
                } catch (Exception e) {
                    throw new PippoRuntimeException(e, "Exception when reading the request body");
                }
            }
        }
        return body;
    }

    public String getHeader(String name) {
        return httpServletRequest.getHeader(name);
    }

    public Enumeration<String> getHeaders(String name) {
        return httpServletRequest.getHeaders(name);
    }

    /**
     * Wrapper function to get all request header names
     * @return the enumerator for request headers
     */
    public Enumeration<String> getHeaderNames() {
        return httpServletRequest.getHeaderNames();
    }

    public boolean isSecure() {
        return httpServletRequest.isSecure();
    }

    /**
     * Returns the url with the protocol, application path, & resource path. The
     * query string is omitted.
     *
     * @return the url
     */
    public String getUrl() {
        return httpServletRequest.getRequestURL().toString();
    }

    /**
     * Returns the container uri with the application path & resource path. The
     * protocol and query string are omitted.
     *
     * @return the container uri
     */
    public String getUri() {
        return httpServletRequest.getRequestURI();
    }

    /**
     * Returns the query string component of the request.
     *
     * @return the query string
     */
    public String getQuery() {
        return httpServletRequest.getQueryString();
    }

    /**
     * Returns the uri relative to the application path.
     *
     * @return the uri relative to the application path
     */
    public String getApplicationUri() {
        if ("".equals(applicationPath)) {
            return getUri();
        } else {
            return getUri().substring(applicationPath.length());
        }
    }

    /**
     * Returns the uri with the query string relative to the application root path.
     *
     * @return the application-relative uri with the query
     */
    public String getApplicationUriWithQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append(getApplicationUri());
        if (getQuery() != null) {
            sb.append('?').append(getQuery());
        }

        return sb.toString();
    }

    public String getContextPath() {
        return httpServletRequest.getContextPath();
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    /**
     * Returns a request path relative to the application path. The returned value always start with "/".
     */
    public String getPath() {
        if (path == null) {
            // create a URI to automatically decode the path
            URI uri = URI.create(httpServletRequest.getRequestURL().toString());
            String requestUri = uri.getPath();

            path = applicationPath.isEmpty() ? requestUri : requestUri.substring(applicationPath.length());
            if (StringUtils.isNullOrEmpty(path)) {
                path = "/";
            }
        }

        return path;
    }

    public String getMethod() {
        if (method == null) {
            String httpServletRequestMethod = httpServletRequest.getMethod();
            String httpServletRequestContentType = httpServletRequest.getHeader(HttpConstants.Header.CONTENT_TYPE);
            if (HttpConstants.Method.POST.equals(httpServletRequestMethod)
                && (HttpConstants.ContentType.APPLICATION_FORM_URLENCODED.equals(httpServletRequestContentType)
                || HttpConstants.ContentType.MULTIPART_FORM_DATA.equals(httpServletRequestContentType))) {
                // Allow forms to more discretely control the Pippo form handler and encourages RESTful design.
                // This parameter may be paired with the '_content_type' and '_content' parameters.
                method = getParameter("_method").toString(httpServletRequestMethod).toUpperCase();
            } else {
                method = httpServletRequestMethod;
            }
        }
        return method;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public Session getSession() {
        return getSession(true);
    }

    public Session getSession(boolean create) {
        if (session == null) {
            HttpSession httpSession = httpServletRequest.getSession(create);
            if (httpSession != null) {
                session = new Session(httpSession);
            }
        }

        return session;
    }

    public void resetSession() {
        if (session == null) {
            return;
        }
        session.invalidate();
        session = null;
        getSession();
    }

    public void recreateSession() {
        if (session == null) {
            return;
        }

        // preserve the session data
        Map<String, Object> values = new HashMap<>();
        Enumeration<String> names = getSession().getNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            values.put(name, getSession().get(name));
        }

        // preserve the flash data
        Flash flash = session.getFlash();

        // create a new session
        resetSession();

        // restore the session data
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            getSession().put(entry.getKey(), entry.getValue());
        }

        // restore the flash instance
        if (flash != null) {
            getSession().put("flash", flash);
        }
    }

    public Map<String, FileItem> getFiles() {
        if (files == null) {
            files = new HashMap<>();
            try {
                Collection<Part> parts = httpServletRequest.getParts();
                for (Part part : parts) {
                    files.put(part.getName(), new FileItem(part));
                }
            } catch (Exception e) {
                throw new PippoRuntimeException(e, "Cannot get files");
            }
        }

        return files;
    }

    public FileItem getFile(String name) {
        return getFiles().get(name);
    }

    public List<Cookie> getCookies() {
        return CookieUtils.getCookies(httpServletRequest);
    }

    public Cookie getCookie(String name) {
        return CookieUtils.getCookie(httpServletRequest, name);
    }

    public static Request get() {
        RouteContext routeContext = RouteDispatcher.getRouteContext();

        return (routeContext != null) ? routeContext.getRequest() : null;
    }

    private ParameterValue buildParameterValue(final String... values) {
        return new ParameterValue(getLocale(), values);
    }

    @Override
    public String toString() {
        return "Request{" +
            "requestMethod='" + getMethod() + '\'' +
            ", uriPattern='" + getApplicationUri() + '\'' +
            '}';
    }

}
