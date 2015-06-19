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
package ro.pippo.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.pippo.core.AbstractWebServer;
import ro.pippo.core.Application;
import ro.pippo.core.PippoFilter;
import ro.pippo.core.PippoRuntimeException;
import ro.pippo.core.PippoServlet;
import ro.pippo.core.util.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Daniel Jipa
 */
public class TomcatServer extends AbstractWebServer<TomcatSettings> {

    private static final Logger log = LoggerFactory.getLogger(TomcatServer.class);

    private Application application;

    private Tomcat tomcat;

    @Override
    public void setPippoFilter(PippoFilter pippoFilter) {
        super.setPippoFilter(pippoFilter);
        application = pippoFilter.getApplication();
    }

    @Override
    public void start() {
        if (StringUtils.isNullOrEmpty(pippoFilterPath)) {
            pippoFilterPath = "/*";
        }
        createServer();
        File docBase = new File(System.getProperty("java.io.tmpdir"));
        Context context = tomcat.addContext(getSettings().getContextPath(), docBase.getAbsolutePath());

        PippoServlet pippoServlet = new PippoServlet();
        pippoServlet.setApplication(application);

        Wrapper wrapper = context.createWrapper();
        String name = "pippoServlet";

        wrapper.setName(name);
        wrapper.setLoadOnStartup(1);
        wrapper.setServlet(pippoServlet);
        context.addChild(wrapper);
        context.addServletMapping(pippoFilterPath, name);

        try {
            String version = tomcat.getClass().getPackage().getImplementationVersion();
            log.info("Starting Tomcat Server {} on port {}", version, getSettings().getPort());
            tomcat.start();
        } catch (LifecycleException e) {
            throw new PippoRuntimeException(e);
        }
        tomcat.getServer().await();
    }

    @Override
    public void stop() {
        if (tomcat != null) {
            try {
                tomcat.stop();
            } catch (Exception e) {
                throw new PippoRuntimeException("Cannot stop Tomcat Server", e);
            }
        }
    }

    @Override
    protected TomcatSettings createDefaultSettings() {
        return new TomcatSettings(pippoSettings);
    }

    protected void createServer() {
    	tomcat = new Tomcat();
    	// basic (required) values
    	tomcat.setPort(getSettings().getPort());
    	tomcat.getConnector().setAttribute("address", getSettings().getHost());
    	// optional config
    	Map<String,Object> allSettings = getSettings().getSettingsMap();
    	@SuppressWarnings("rawtypes")
		Iterator settings = allSettings.entrySet().iterator();
    	while(settings.hasNext()) {
    		@SuppressWarnings("rawtypes")
			Map.Entry setting = (Map.Entry)settings.next();
    		if(!setting.getKey().equals("") && setting.getValue() != null) {
    			tomcat.getConnector().setAttribute(setting.getKey().toString(), setting.getValue());
    			log.info("Tomcat setting: {} --> {}", setting.getKey().toString(), setting.getValue());
    		}
    	}
    }

}