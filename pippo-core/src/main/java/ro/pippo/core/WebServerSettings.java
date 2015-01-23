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

import java.io.Serializable;

/**
 * @author Decebal Suiu
 */
public class WebServerSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int defaultPort = 8338;

    private String host = "localhost";
    private int port = defaultPort;
    private String contextPath = "/";
    private String externalStaticFilesPath = "/ext";
    private String externalStaticFilesLocation;
    private String keystoreFile;
    private String keystorePassword;
    private String truststoreFile;
    private String truststorePassword;

    public WebServerSettings() {
    }

    public WebServerSettings(PippoSettings pippoSettings) {
        this.port = pippoSettings.getInteger(PippoConstants.SETTING_SERVER_PORT, defaultPort);
        this.host = pippoSettings.getString(PippoConstants.SETTING_SERVER_HOST, host);
        this.contextPath = pippoSettings.getString(PippoConstants.SETTING_SERVER_CONTEXT_PATH, contextPath);
        this.externalStaticFilesPath = pippoSettings.getString(PippoConstants.SETTING_SERVER_EXTERNAL_FILES_PATH, externalStaticFilesPath);
        this.externalStaticFilesLocation = pippoSettings.getString(PippoConstants.SETTING_SERVER_EXTERNAL_FILES_LOCATION, externalStaticFilesLocation);
        this.keystoreFile = pippoSettings.getString(PippoConstants.SETTING_SERVER_KEYSTORE_FILE, keystoreFile);
        this.keystorePassword = pippoSettings.getString(PippoConstants.SETTING_SERVER_KEYSTORE_PASSWORD, keystorePassword);
        this.truststoreFile = pippoSettings.getString(PippoConstants.SETTING_SERVER_TRUSTSTORE_FILE, truststoreFile);
        this.truststorePassword = pippoSettings.getString(PippoConstants.SETTING_SERVER_TRUSTSTORE_PASSWORD, truststorePassword);
    }

    public String getHost() {
        return host;
    }

    public WebServerSettings host(String host) {
        this.host = host;

        return this;
    }

    public int getPort() {
        return port;
    }

    public WebServerSettings port(int port) {
        this.port = port;

        return this;
    }

    public String getContextPath() {
        return contextPath;
    }

    public WebServerSettings contextPath(String contextPath) {
        this.contextPath = contextPath;

        return this;
    }

    public String getExternalStaticFilesPath() {
        return externalStaticFilesPath;
    }

    public WebServerSettings externalStaticFilesPath(String externalStaticFilesPath) {
        this.externalStaticFilesPath = externalStaticFilesPath;

        return this;
    }

    public String getExternalStaticFilesLocation() {
        return externalStaticFilesLocation;
    }

    public WebServerSettings externalStaticFilesLocation(String externalStaticFilesLocation) {
        this.externalStaticFilesLocation = externalStaticFilesLocation;

        return this;
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public WebServerSettings keystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;

        return this;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public WebServerSettings keystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;

        return this;
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public WebServerSettings truststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;

        return this;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public WebServerSettings truststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;

        return this;
    }

}