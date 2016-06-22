/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.json.hardware.impl;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.hardware.Sensors;
import oshi.json.json.NullAwareJsonObjectBuilder;

public class SensorsImpl implements Sensors {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Sensors sensors;

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
    public JsonObject toJSON() {
        JsonArrayBuilder fanSpeedsArrayBuilder = jsonFactory.createArrayBuilder();
        for (int speed : getFanSpeeds()) {
            fanSpeedsArrayBuilder.add(speed);
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("cpuTemperature", getCpuTemperature()).add("fanSpeeds", fanSpeedsArrayBuilder.build())
                .add("cpuVoltage", getCpuVoltage()).build();
    }

}
