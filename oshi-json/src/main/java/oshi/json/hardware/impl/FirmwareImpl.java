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

import oshi.json.hardware.Firmware;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement Firmware interface with platform-specific objects
 */
public class FirmwareImpl extends AbstractOshiJsonObject implements Firmware {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.Firmware firmware;

    /**
     * Creates a new platform-specific Firmware object wrapping the provided
     * argument
     *
     * @param firmware
     *            a platform-specific Firmware object
     */
    public FirmwareImpl(oshi.hardware.Firmware firmware) {
        this.firmware = firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.firmware.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.firmware.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return this.firmware.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.firmware.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReleaseDate() {
        return this.firmware.getReleaseDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.manufacturer")) {
            json.add("manufacturer", getManufacturer());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.description")) {
            json.add("description", getDescription());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.version")) {
            json.add("version", getVersion());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.computerSystem.firmware.releaseDate")) {
            json.add("releaseDate", getReleaseDate());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.firmware.toString();
    }

}
