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
package oshi.json.hardware.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.Firmware;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement Firmware interface with platform-specific objects
 */
public class FirmwareImpl extends AbstractOshiJsonObject implements Firmware {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Firmware firmware;

    /**
     * Creates a new platform-specific Firmware object wrapping the provided
     * argument
     *
     * @param firmware
     *            a platform-specific Firmware object
     */
    public FirmwareImpl(oshi.hardware.Firmware firmware) {
        this.firmware = firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.firmware.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.firmware.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return this.firmware.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.firmware.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReleaseDate() {
        return this.firmware.getReleaseDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.manufacturer")) {
            json.add("manufacturer", getManufacturer());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.description")) {
            json.add("description", getDescription());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.version")) {
            json.add("version", getVersion());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.releaseDate")) {
            json.add("releaseDate", getReleaseDate());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.firmware.toString();
    }

}
