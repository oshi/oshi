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

import oshi.json.hardware.Baseboard;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement Baseboard interface with platform-specific objects
 */
public class BaseboardImpl extends AbstractOshiJsonObject implements Baseboard {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Baseboard baseboard;

    /**
     * Creates a new platform-specific baseboard object wrapping the provided
     * argument
     *
     * @param baseboard
     *            a platform-specific baseboard object
     */
    public BaseboardImpl(oshi.hardware.Baseboard baseboard) {
        this.baseboard = baseboard;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.baseboard.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        return this.baseboard.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.baseboard.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        return this.baseboard.getSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.baseboard.manufacturer")) {
            json.add("manufacturer", getManufacturer());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.baseboard.model")) {
            json.add("model", getModel());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.baseboard.version")) {
            json.add("version", getVersion());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.baseboard.serialNumber")) {
            json.add("serialNumber", getSerialNumber());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.baseboard.toString();
    }
}
