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
import oshi.json.hardware.ComputerSystem;
import oshi.json.hardware.Firmware;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement ComputerSystem interface with platform-specific
 * objects
 */
public class ComputerSystemImpl extends AbstractOshiJsonObject implements ComputerSystem {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.ComputerSystem computerSystem;

    /**
     * Creates a new platform-specific ComputerSystem object wrapping the
     * provided argument
     *
     * @param computerSystem
     *            a platform-specific ComputerSystem object
     */
    public ComputerSystemImpl(oshi.hardware.ComputerSystem computerSystem) {
        this.computerSystem = computerSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.computerSystem.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        return this.computerSystem.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        return this.computerSystem.getSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Firmware getFirmware() {
        return new FirmwareImpl(this.computerSystem.getFirmware());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Baseboard getBaseboard() {
        return new BaseboardImpl(this.computerSystem.getBaseboard());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.manufacturer")) {
            json.add("manufacturer", getManufacturer());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.model")) {
            json.add("model", getModel());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.serialNumber")) {
            json.add("serialNumber", getSerialNumber());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware")) {
            json.add("firmware", getFirmware().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.baseboard")) {
            json.add("baseboard", getBaseboard().toJSON(properties));
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.computerSystem.toString();
    }

}
