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

import oshi.json.hardware.GlobalMemory;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;

public class GlobalMemoryImpl extends AbstractOshiJsonObject implements GlobalMemory {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.GlobalMemory memory;

    public GlobalMemoryImpl(oshi.hardware.GlobalMemory memory) {
        this.memory = memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotal() {
        return this.memory.getTotal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
        return this.memory.getAvailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapTotal() {
        return this.memory.getSwapTotal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapUsed() {
        return this.memory.getSwapUsed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable())
                .add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapUsed", getSwapUsed()).build();
    }

}
