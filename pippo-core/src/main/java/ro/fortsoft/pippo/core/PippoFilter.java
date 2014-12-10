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
package ro.fortsoft.pippo.core;

import ro.fortsoft.pippo.core.route.DefaultRouteHandlerChain;
import ro.fortsoft.pippo.core.route.RouteMatch;
import ro.fortsoft.pippo.core.route.Router;
import ro.fortsoft.pippo.core.util.ClasspathUtils;
import ro.fortsoft.pippo.core.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Decebal Suiu
 */
public class PippoFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PippoFilter.class);

    /**
     * The name of the root path parameter that specifies the root dir of the app.
     */
    public static final String FILTER_MAPPING_PARAM = "filterMappingUrlPattern";

    /**
     * The name of the context parameter that specifies application class.
     */
    public static final String APPLICATION_CLASS_PARAM = "applicationClassName";

    /**
     * Name of parameter used to express a comma separated list of paths that should be ignored.
     */
    public static final String IGNORE_PATHS_PARAM = "ignorePaths";

    private static final String slash = "/";

    private final String PIPPO_LOGO = "\n"
    		+" ____  ____  ____  ____  _____\n"
            + "(  _ \\(_  _)(  _ \\(  _ \\(  _  )\n"
            + " ) __/ _)(_  ) __/ ) __/ )(_)(   https://github.com/decebals/pippo\n"
            + "(__)  (____)(__)  (__)  (_____)  {}\n";

    private Application application;
    private List<Initializer> initializers;
    private Set<String> ignorePaths;
    private String filterPath;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	log.info(PIPPO_LOGO, readPippoVersion());

        if (ignorePaths == null) {
            initIgnorePaths(filterConfig);
        }
        log.debug("Ignore paths '{}'", ignorePaths);

        if (filterPath == null) {
            initFilterPath(filterConfig);
        }
        log.debug("The filter path is '{}'", filterPath);

        if (application == null) {
            createApplication(filterConfig);
            log.debug("Created application '{}'", application);
        }

        ThreadContext.setApplication(application);

        try {
            String contextPath = StringUtils.addStart(filterConfig.getServletContext().getContextPath(), "/");
            application.setContextPath(contextPath);
            log.debug("Serving application on context path '{}'", contextPath);

            initializers = getInitializers();
            for (Initializer initializer : initializers) {
                initializer.init(application);
            }

            log.debug("Initializing application '{}'", application);
            application.init();

            String runtimeMode = application.getRuntimeMode().toString().toUpperCase();
            log.info("Pippo started ({})", runtimeMode);
        } catch (Exception e) {
            destroy();
            throw new ServletException(e);
        } finally {
            ThreadContext.detach();
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        final ThreadContext previousThreadContext = ThreadContext.detach();

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        // TODO test for redirect
        // no redirect; process the request
        ThreadContext.setApplication(application);

        String requestMethod = httpServletRequest.getMethod();
        String requestUri = httpServletRequest.getRequestURI();
        log.debug("Request '{} {}'", requestMethod, requestUri);

        // check for ignore path
        if (shouldIgnorePath(httpServletRequest)) {
            log.debug("Ignoring request '{}'", requestUri);
            if (chain != null) {
                chain.doFilter(servletRequest, servletResponse);
            }

            return;
        }

        String relativePath = getRelativePath(httpServletRequest);
        log.debug("The relative path for '{}' is '{}'", requestUri, relativePath);

        final Request request = new Request(httpServletRequest);
        final Response response = new Response(httpServletResponse, application);
        try {
            Router router = application.getRouter();
            List<RouteMatch> routeMatches = router.findRoutes(relativePath, requestMethod);
            if (!routeMatches.isEmpty()) {
                new DefaultRouteHandlerChain(request, response, routeMatches).next();
            }

            if (!response.isCommitted()) {
                log.warn("Cannot find a route for '{} {}'", requestMethod, requestUri);
                ErrorHandler errorHandler = application.getErrorHandler();
                if (errorHandler != null) {
                    errorHandler.handle(HttpConstants.StatusCode.NOT_FOUND, request, response);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorHandler errorHandler = application.getErrorHandler();
            if (errorHandler != null) {
                if (!response.isCommitted()) {
                    errorHandler.handle(e, request, response);
                } else {
                    log.debug("The response has already been committed. Cannot use the exception handler.");
                }
            }
        } finally {
            ThreadContext.restore(previousThreadContext);
        }
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Set<String> getIgnorePaths() {
        return ignorePaths;
    }

    public void setIgnorePaths(Set<String> ignorePaths) {
        this.ignorePaths = ignorePaths;
    }

    public String getFilterPath() {
        return filterPath;
    }

    public void setFilterPath(String filterPath) {
        this.filterPath = filterPath;
    }

    @Override
    public void destroy() {
        if (application != null) {
            ThreadContext.setApplication(application);

            try {
                for (Initializer initializer : initializers) {
                    initializer.destroy(application);
                }
                application.destroy();

                log.info("Pippo destroyed");
            } finally {
                ThreadContext.detach();
                application = null;
            }
        }
    }

    private boolean shouldIgnorePath(HttpServletRequest request) {
        if (ignorePaths.size() > 0) {
            String relativePath = getRelativePath(request);
            if (relativePath != null && !relativePath.isEmpty()) {
                for (String path : ignorePaths) {
                    if (relativePath.startsWith(path)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void initIgnorePaths(FilterConfig filterConfig) {
        ignorePaths = new HashSet<String>();
        String paths = filterConfig.getInitParameter(IGNORE_PATHS_PARAM);
        if (paths != null && !paths.isEmpty()) {
            String[] parts = paths.split(",");
            for (String path : parts) {
                path = path.trim();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                ignorePaths.add(path);
            }
        }
    }

    private void initFilterPath(FilterConfig filterConfig) {
        initFilterPathFromConfig(filterConfig);
        if (filterPath == null) {
            initFilterPathFromWebXml(filterConfig);
        }

        if (filterPath == null) {
            StringBuilder message = new StringBuilder();
            message.append("Unable to determine filter path from filter init-param, web.xml.");
            message.append("Assuming user will set filter path manually by calling setFilterPath(String)");
            log.warn(message.toString());
        }
    }

    private void initFilterPathFromConfig(FilterConfig filterConfig) {
        String filterMapping = filterConfig.getInitParameter(FILTER_MAPPING_PARAM);
        if (filterMapping != null) {
            if (filterMapping.equals("/*")) {
                filterMapping = "";
            } else if (!filterMapping.startsWith("/") || !filterMapping.endsWith("/*")) {
                throw new PippoRuntimeException("Your {} must start with \"/\" and end with \"/*\". It is: ",
                        FILTER_MAPPING_PARAM, filterMapping);
            } else {
                // remove leading "/" and trailing "*"
                filterMapping = filterMapping.substring(1, filterMapping.length() - 1);
            }
        }

        filterPath = filterMapping;
    }

    private void initFilterPathFromWebXml(FilterConfig filterConfig) {
        String filterName = filterConfig.getFilterName();
        FilterRegistration filterRegistration = filterConfig.getServletContext().getFilterRegistration(filterName);
        Collection<String> mappings = filterRegistration.getUrlPatternMappings();
        int size = mappings.size();

        if (size > 1) {
            throw new PippoRuntimeException("Expected one filter path for '{}' but found multiple", filterName);
        }

        if (size == 1) {
            String urlPattern = mappings.iterator().next();
            filterPath = urlPattern.substring(1, urlPattern.length() - 1);
        }
    }

    private void createApplication(FilterConfig filterConfig) throws ServletException {
        String applicationClassName = filterConfig.getInitParameter(APPLICATION_CLASS_PARAM);
        if (applicationClassName == null) {
            log.error("Filter init param '{}' is missing", APPLICATION_CLASS_PARAM);
            throw new ServletException("Cannot found application class name");
        }
        try {
            Class<?> applicationClass = Class.forName(applicationClassName);
            application = (Application) applicationClass.newInstance();
        } catch (Exception e) {
            log.error("Cannot create application with className '{}'", applicationClassName, e);
            throw new ServletException(e);
        }
    }

    private String getRelativePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        path = path.substring(contextPath.length());
        if (path.length() > 0) {
            path = path.substring(1);
        }
        if (!path.startsWith(filterPath) && filterPath.equals(path + slash)) {
            path += slash;
        }
        if (path.startsWith(filterPath)) {
            path = path.substring(filterPath.length());
        }
        if (!path.startsWith(slash)) {
            path = slash + path;
        }
        if (path.length() > 1 && path.endsWith(slash)) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private List<Initializer> getInitializers() throws Exception {
        List<Initializer> initializers = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        Enumeration<URL> urls = classLoader.getResources("pippo.properties");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            log.debug("Read '{}'", url.getFile());
            Reader reader = new InputStreamReader(url.openStream(), "UTF-8");
            Properties properties = new Properties();
            properties.load(reader);
            String initializerClassName = properties.getProperty("initializer");
            log.debug("Found initializer '{}'", initializerClassName);
            Class<Initializer> initializerClass = (Class<Initializer>) classLoader.loadClass(initializerClassName);
            initializers.add(initializerClass.newInstance());
        }

        return initializers;
    }

    /**
     * Simply reads a property resource file that contains the version of this
     * Pippo build. Helps to identify the Pippo version currently running.
     *
     * @return The version of Pippo. Eg. "1.6-SNAPSHOT" while developing of "1.6" when released.
     */
    private final String readPippoVersion() {
        // and the key inside the properties file.
        String PIPPO_VERSION_PROPERTY_KEY = "pippo.version";

        String pippoVersion;

        try {

            Properties prop = new Properties();
            URL url = ClasspathUtils.locateOnClasspath(PippoConstants.LOCATION_OF_PIPPO_BUILTIN_PROPERTIES);
            InputStream stream = url.openStream();
            prop.load(stream);

            pippoVersion = prop.getProperty(PIPPO_VERSION_PROPERTY_KEY);

        } catch (Exception e) {
            //this should not happen. Never.
            throw new PippoRuntimeException("Something is wrong with your build. Cannot find resource {}",
                    PippoConstants.LOCATION_OF_PIPPO_BUILTIN_PROPERTIES);
        }

        return pippoVersion;
    }

}
