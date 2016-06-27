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

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.hardware.PowerSource;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

public class PowerSourceImpl extends AbstractOshiJsonObject implements PowerSource {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.PowerSource powerSource;

    public PowerSourceImpl(oshi.hardware.PowerSource powerSource) {
        this.powerSource = powerSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return powerSource.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRemainingCapacity() {
        return powerSource.getRemainingCapacity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTimeRemaining() {
        return powerSource.getTimeRemaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.powerSources.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.powerSources.remainingCapacity")) {
            json.add("remainingCapacity", getRemainingCapacity());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.powerSources.timeRemaining")) {
            json.add("timeRemaining", getTimeRemaining());
        }
        return json.build();
    }

}
