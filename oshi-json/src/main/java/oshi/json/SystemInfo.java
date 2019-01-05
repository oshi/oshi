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
package oshi.json;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.PlatformEnum;
import oshi.json.hardware.HardwareAbstractionLayer;
import oshi.json.hardware.impl.HardwareAbstractionLayerImpl;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.json.OshiJsonObject;
import oshi.json.software.os.OperatingSystem;
import oshi.json.software.os.impl.OperatingSystemImpl;
import oshi.json.util.PropertiesUtil;

/**
 * System information. This is the main entry point to Oshi. This object
 * provides getters which instantiate the appropriate platform-specific
 * implementations of {@link OperatingSystem} (software) and
 * {@link HardwareAbstractionLayer} (hardware).
 *
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfo extends AbstractOshiJsonObject implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private oshi.SystemInfo si;

    private OperatingSystem os = null;

    private HardwareAbstractionLayer hardware = null;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * Primary entry point for OSHI API.
     */
    public SystemInfo() {
        this.si = new oshi.SystemInfo();
    }

    public oshi.SystemInfo getImpl() {
        return si;
    }

    /**
     * @return Returns the currentPlatformEnum.
     */
    public static PlatformEnum getCurrentPlatformEnum() {
        return oshi.SystemInfo.getCurrentPlatformEnum();
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link OperatingSystem}.
     *
     * @return A new instance of {@link OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        if (this.os == null) {
            this.os = new OperatingSystemImpl(this.si.getOperatingSystem());
        }
        return this.os;
    }

    /**
     * Creates a new instance of the appropriate platform-specific
     * {@link HardwareAbstractionLayer}.
     *
     * @return A new instance of {@link HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        if (this.hardware == null) {
            this.hardware = new HardwareAbstractionLayerImpl(this.si.getHardware());
        }
        return this.hardware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "platform")) {
            json.add("platform", getCurrentPlatformEnum().toString());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem")) {
            json.add("operatingSystem", getOperatingSystem().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "hardware")) {
            json.add("hardware", getHardware().toJSON(properties));
        }
        return json.build();
    }
}
