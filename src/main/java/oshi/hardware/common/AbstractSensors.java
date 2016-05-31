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
package oshi.hardware.common;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Sensors;
import oshi.json.NullAwareJsonObjectBuilder;

public abstract class AbstractSensors implements Sensors {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract double getCpuTemperature();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int[] getFanSpeeds();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract double getCpuVoltage();

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
