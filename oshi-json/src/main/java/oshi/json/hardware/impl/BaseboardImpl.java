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
