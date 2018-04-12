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
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.Sensors;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement Sensors interface with platform-specific objects
 */
public class SensorsImpl extends AbstractOshiJsonObject implements Sensors {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Sensors sensors;

    /**
     * Creates a new platform-specific Sensors object wrapping the provided
     * argument
     *
     * @param sensors
     *            a platform-specific Sensors object
     */
    public SensorsImpl(oshi.hardware.Sensors sensors) {
        this.sensors = sensors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        return this.sensors.getCpuTemperature();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        return this.sensors.getFanSpeeds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        return this.sensors.getCpuVoltage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.sensors.cpuTemperature")) {
            json.add("cpuTemperature", getCpuTemperature());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.sensors.fanSpeeds")) {
            JsonArrayBuilder fanSpeedsArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (int speed : getFanSpeeds()) {
                fanSpeedsArrayBuilder.add(speed);
            }
            json.add("fanSpeeds", fanSpeedsArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.sensors.cpuVoltage")) {
            json.add("cpuVoltage", getCpuVoltage());
        }
        return json.build();
    }

}
