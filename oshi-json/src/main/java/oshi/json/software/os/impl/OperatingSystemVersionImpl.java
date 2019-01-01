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
package oshi.json.software.os.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.OperatingSystemVersion;
import oshi.json.util.PropertiesUtil;

/**
 * Wrapper class to implement OperatingSystemVersion interface with
 * platform-specific objects
 */
public class OperatingSystemVersionImpl extends AbstractOshiJsonObject implements OperatingSystemVersion {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OperatingSystemVersion version;

    /**
     * Creates a new platform-specific OperatingSystemVersion object wrapping
     * the provided argument
     *
     * @param version
     *            a platform-specific OperatingSystemVersion object
     */
    public OperatingSystemVersionImpl(oshi.software.os.OperatingSystemVersion version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.version.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVersion(String version) {
        this.version.setVersion(version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCodeName() {
        return this.version.getCodeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCodeName(String codeName) {
        this.version.setCodeName(codeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuildNumber() {
        return this.version.getBuildNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuildNumber(String buildNumber) {
        this.version.setBuildNumber(buildNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version.version")) {
            json.add("version", getVersion());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version.codeName")) {
            json.add("codeName", getCodeName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version.build")) {
            json.add("build", getBuildNumber());
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.version.toString();
    }
}
