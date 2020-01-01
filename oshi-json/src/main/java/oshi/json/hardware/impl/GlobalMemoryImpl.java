/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
