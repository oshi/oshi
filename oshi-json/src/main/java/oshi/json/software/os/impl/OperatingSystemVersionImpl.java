/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.software.os.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.OperatingSystemVersion;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement OperatingSystemVersion interface with
 * platform-specific objects
 */
public class OperatingSystemVersionImpl extends AbstractOshiJsonObject implements OperatingSystemVersion {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OperatingSystemVersion version;

    /**
     * Creates a new platform-specific OperatingSystemVersion object wrapping
     * the provided argument
     *
     * @param version
     *            a platform-specific OperatingSystemVersion object
     */
    public OperatingSystemVersionImpl(oshi.software.os.OperatingSystemVersion version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.version.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVersion(String version) {
        this.version.setVersion(version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCodeName() {
        return this.version.getCodeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCodeName(String codeName) {
        this.version.setCodeName(codeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuildNumber() {
        return this.version.getBuildNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuildNumber(String buildNumber) {
        this.version.setBuildNumber(buildNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version.version")) {
            json.add("version", getVersion());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version.codeName")) {
            json.add("codeName", getCodeName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version.build")) {
            json.add("build", getBuildNumber());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.version.toString();
    }
}
