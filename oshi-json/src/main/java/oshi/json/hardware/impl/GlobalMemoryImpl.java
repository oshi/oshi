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

import oshi.json.hardware.GlobalMemory;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement GlobalMemory interface with platform-specific
 * objects
 */
public class GlobalMemoryImpl extends AbstractOshiJsonObject implements GlobalMemory {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.GlobalMemory memory;

    /**
     * Creates a new platform-specific GlobalMemory object wrapping the provided
     * argument
     *
     * @param memory
     *            a platform-specific GlobalMemory object
     */
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
    public long getSwapPagesIn() {
        return this.memory.getSwapPagesIn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesOut() {
        return this.memory.getSwapPagesOut();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPageSize() {
        return this.memory.getPageSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.available")) {
            json.add("available", getAvailable());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.total")) {
            json.add("total", getTotal());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.swapTotal")) {
            json.add("swapTotal", getSwapTotal());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.swapUsed")) {
            json.add("swapUsed", getSwapUsed());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.swapPagesIn")) {
            json.add("swapPagesIn", getSwapPagesIn());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.swapPagesOut")) {
            json.add("swapPagesOut", getSwapPagesOut());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.memory.pageSize")) {
            json.add("pageSize", getPageSize());
        }
        return json.build();
    }
}
