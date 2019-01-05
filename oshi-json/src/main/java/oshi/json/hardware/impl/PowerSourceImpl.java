/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
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

import oshi.json.hardware.PowerSource;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement PowerSource interface with platform-specific
 * objects
 */
public class PowerSourceImpl extends AbstractOshiJsonObject implements PowerSource {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.PowerSource powerSource;

    /**
     * Creates a new platform-specific PowerSource object wrapping the provided
     * argument
     *
     * @param powerSource
     *            a platform-specific PowerSource object
     */
    public PowerSourceImpl(oshi.hardware.PowerSource powerSource) {
        this.powerSource = powerSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.powerSource.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRemainingCapacity() {
        return this.powerSource.getRemainingCapacity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTimeRemaining() {
        return this.powerSource.getTimeRemaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
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
