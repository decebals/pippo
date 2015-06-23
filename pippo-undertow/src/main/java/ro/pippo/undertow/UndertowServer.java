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
package ro.pippo.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;

import ro.pippo.core.AbstractWebServer;
import ro.pippo.core.Application;
import ro.pippo.core.PippoFilter;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.util.StringUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An implementation of WebServer based on Undertow.
 *
 * @see <a href="http://undertow.io">Undertow</a>
 *
 * @author James Moger
 */
public class UndertowServer extends AbstractWebServer<UndertowSettings> {

    private static final Logger log = LoggerFactory.getLogger(UndertowServer.class);

    private Undertow server;
    private DeploymentManager pippoDeploymentManager;

    @Override
    public void start() {
        try {
            pippoDeploymentManager = createPippoDeploymentManager();
            HttpHandler pippoHandler = pippoDeploymentManager.start();

            HttpHandler contextHandler = createContextHandler(pippoHandler);
            GracefulShutdownHandler rootHandler = new GracefulShutdownHandler(contextHandler);
            server = createServer(rootHandler);

            String version = server.getClass().getPackage().getImplementationVersion();
            log.info("Starting Undertow Server {} on port {}", version, getSettings().getPort());

            server.start();
        } catch (Exception e) {
            throw new PippoRuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                String version = server.getClass().getPackage().getImplementationVersion();
                log.info("Stopping Undertow {} on port {}", version, getSettings().getPort());

                server.stop();

                pippoDeploymentManager.undeploy();
            } catch (Exception e) {
                throw new PippoRuntimeException("Cannot stop Undertow Server", e);
            }
        }
    }

    @Override
    protected UndertowSettings createDefaultSettings() {
        return new UndertowSettings(pippoSettings);
    }

    protected Undertow createServer(HttpHandler contextHandler) {
        Builder builder = Undertow.builder();
        // TODO is it a better option?
        if (getSettings().getBufferSize() > 0) {
            builder.setBufferSize(getSettings().getBufferSize());
            log.info("Undertow basic setting: {} --> {}", "bufferSize", getSettings().getBufferSize());
        }
        if (getSettings().getBuffersPerRegion() > 0) {
            builder.setBuffersPerRegion(getSettings().getBuffersPerRegion());
            log.info("Undertow basic setting: {} --> {}", "buffersPerRegion", getSettings().getBuffersPerRegion());
        }
        if (getSettings().getDirectBuffers() == true) {
            builder.setDirectBuffers(getSettings().getDirectBuffers());
            log.info("Undertow basic setting: {} --> {}", "directBuffers", getSettings().getDirectBuffers());
        }
        if (getSettings().getIoThreads() > 0) {
            builder.setIoThreads(getSettings().getIoThreads());
            log.info("Undertow basic setting: {} --> {}", "ioThreads", getSettings().getIoThreads());
        }
        if (getSettings().getWorkerThreads() > 0) {
            builder.setWorkerThreads(getSettings().getWorkerThreads());
            log.info("Undertow basic setting: {} --> {}", "workerThreads", getSettings().getWorkerThreads());
        }
        
        // ------ OPTIONS >
        Map<String,Object> options;
        Iterator<Entry<String, Object>> opts;
        
        // --- worker options
        options = getSettings().getWorkerOptions();
        opts = options.entrySet().iterator();
        while(opts.hasNext()) {
        	Entry<String, Object> option = opts.next();
        	if(!option.getKey().equals("") && option.getValue() != null) {
        		switch(option.getKey()) {
        		case "tcpNoDelay":
        			builder.setWorkerOption(Options.TCP_NODELAY, (Boolean)option.getValue()); break;
        		}
    			log.info("Undertow worker setting: {} --> {}", option.getKey().toString(), option.getValue());
        	}
        }
        // --- sockets options
        options = getSettings().getSocketOptions();
        opts = options.entrySet().iterator();
        while(opts.hasNext()) {
        	Entry<String, Object> option = opts.next();
        	if(!option.getKey().equals("") && option.getValue() != null) {
        		switch(option.getKey()) {
        		case "tcpNoDelay":
        			builder.setSocketOption(Options.TCP_NODELAY, (Boolean)option.getValue()); break;
        		case "reuseAddresses":
        			builder.setSocketOption(Options.REUSE_ADDRESSES, (Boolean)option.getValue()); break;
        		}
    			log.info("Undertow socket setting: {} --> {}", option.getKey().toString(), option.getValue());
        	}
        }
        // --- server options
        options = getSettings().getServerOptions();
        opts = options.entrySet().iterator();
        while(opts.hasNext()) {
        	Entry<String, Object> option = opts.next();
        	if(!option.getKey().equals("") && option.getValue() != null) {
        		switch(option.getKey()) {
        		case "maxHeaderSize":
        			builder.setServerOption(UndertowOptions.MAX_HEADER_SIZE, (Integer)option.getValue()); break;
        		case "maxEntitySize":
        			builder.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, (Long)option.getValue()); break;
        		case "maxParameters":
        			builder.setServerOption(UndertowOptions.MAX_PARAMETERS, (Integer)option.getValue()); break;
        		case "maxHeaders":
        			builder.setServerOption(UndertowOptions.MAX_HEADERS, (Integer)option.getValue()); break;
        		case "maxCookies":
        			builder.setServerOption(UndertowOptions.MAX_COOKIES, (Integer)option.getValue()); break;
        		case "urlCharset":
        			builder.setServerOption(UndertowOptions.URL_CHARSET, (String)option.getValue()); break;
        		case "decodeUrl":
        			builder.setServerOption(UndertowOptions.DECODE_URL, (Boolean)option.getValue()); break;
        		case "AllowEncodedSlash":
        			builder.setServerOption(UndertowOptions.ALLOW_ENCODED_SLASH, (Boolean)option.getValue()); break;
        		case "AllowEqualsInCookieValue":
        			builder.setServerOption(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, (Boolean)option.getValue()); break;
        		case "AlwaysSetDate":
        			builder.setServerOption(UndertowOptions.ALWAYS_SET_DATE, (Boolean)option.getValue()); break;
        		case "AlwaysSetKeepAlive":
        			builder.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, (Boolean)option.getValue()); break;
        		case "MaxBufferedRequestSize":
        			builder.setServerOption(UndertowOptions.MAX_BUFFERED_REQUEST_SIZE, (Integer)option.getValue()); break;
        		case "RecordRequestStartTime":
        			builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, (Boolean)option.getValue()); break;
        		case "IdleTimeout":
        			builder.setServerOption(UndertowOptions.IDLE_TIMEOUT, (Integer)option.getValue()); break;
        		case "RequestParseTimeout":
        			builder.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, (Integer)option.getValue()); break;
        		case "EnableConnectorStatistics":
        			builder.setServerOption(UndertowOptions.ENABLE_CONNECTOR_STATISTICS, (Boolean)option.getValue()); break;
        		}
    			log.info("Undertow server setting: {} --> {}", option.getKey().toString(), option.getValue());
        	}
        }
        // ------ OPTIONS />
        
        if (getSettings().getKeystoreFile() == null) {
            // HTTP
            builder.addHttpListener(getSettings().getPort(), getSettings().getHost());
        } else {
            // HTTPS
            builder.setServerOption(UndertowOptions.ENABLE_SPDY, true);
            try {
                KeyStore keyStore = loadKeyStore(getSettings().getKeystoreFile(), getSettings().getKeystorePassword());
                KeyStore trustStore = loadKeyStore(getSettings().getTruststoreFile(), getSettings().getTruststorePassword());
                SSLContext sslContext = createSSLContext(keyStore, trustStore);
                builder.addHttpsListener(getSettings().getPort(), getSettings().getHost(), sslContext);
            } catch (Exception e) {
                throw new PippoRuntimeException("Failed to setup an Undertow SSL listener!", e);
            }
        }

        builder.setHandler(contextHandler);

        return builder.build();
    }

    protected HttpHandler createContextHandler(HttpHandler pippoHandler) throws ServletException {
        String contextPath = getSettings().getContextPath();

        // create a handler than redirects non-contact requests to the context
        PathHandler contextHandler = Handlers.path(Handlers.redirect(contextPath));

        // add the handler with the context prefix
        contextHandler.addPrefixPath(contextPath, pippoHandler);

        return contextHandler;
    }

    protected DeploymentManager createPippoDeploymentManager() throws ServletException {
        DeploymentInfo info = Servlets.deployment();
        info.setDeploymentName("Pippo");
        info.setClassLoader(this.getClass().getClassLoader());
        info.setContextPath(getSettings().getContextPath());
        info.setIgnoreFlush(true);

        if (StringUtils.isNullOrEmpty(pippoFilterPath)) {
            pippoFilterPath = "/*"; // default value
        }

        info.addFilters(new FilterInfo("PippoFilter", PippoFilter.class, new ImmediateInstanceFactory<>(pippoFilter)));
        info.addFilterUrlMapping("PippoFilter", pippoFilterPath, DispatcherType.REQUEST);

        ServletInfo defaultServlet = new ServletInfo("DefaultServlet", DefaultServlet.class);
        defaultServlet.addMapping("/");

        Application application = pippoFilter.getApplication();
        String location = application.getUploadLocation();
        long maxFileSize = application.getMaximumUploadSize();
        MultipartConfigElement multipartConfig = new MultipartConfigElement(location, maxFileSize, -1L, 0);
        defaultServlet.setMultipartConfig(multipartConfig);
        info.addServlets(defaultServlet);

        DeploymentManager deploymentManager = Servlets.defaultContainer().addDeployment(info);
        deploymentManager.deploy();
        log.debug("Using pippo filter for path '{}'", pippoFilterPath);

        return deploymentManager;
    }

    private SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, getSettings().getKeystorePassword().toCharArray());
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    private KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore loadedKeystore = KeyStore.getInstance("JKS");
        File file = new File(filename);
        if (file.exists()) {
            try (InputStream stream = new FileInputStream(file)) {
                loadedKeystore.load(stream, password.toCharArray());
            }
        } else {
            log.error("Failed to find keystore '{}'!", filename);
        }

        return loadedKeystore;
    }

}
