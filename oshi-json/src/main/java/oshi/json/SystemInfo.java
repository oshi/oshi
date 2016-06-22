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
package oshi.json;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.hardware.HardwareAbstractionLayer;
import oshi.json.hardware.impl.HardwareAbstractionLayerImpl;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.json.OshiJsonObject;
import oshi.json.software.os.OperatingSystem;
import oshi.json.software.os.impl.OperatingSystemImpl;

/**
 * {@inheritDoc}
 */
public class SystemInfo extends oshi.SystemInfo implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private oshi.SystemInfo si;

    private OperatingSystem os = null;

    private HardwareAbstractionLayer hardware = null;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public SystemInfo() {
        this.si = new oshi.SystemInfo();
    }

    /**
     * {@inheritDoc}
     */
    public OperatingSystem getOperatingSystem() {
        if (this.os == null) {
            this.os = new OperatingSystemImpl(si.getOperatingSystem());
        }
        return this.os;
    }

    /**
     * {@inheritDoc}
     */
    public HardwareAbstractionLayer getHardware() {
        if (this.hardware == null) {
            this.hardware = new HardwareAbstractionLayerImpl(si.getHardware());
        }
        return this.hardware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("platform", si.getCurrentPlatformEnum().toString())
                .add("operatingSystem", getOperatingSystem().toJSON()).add("hardware", getHardware().toJSON()).build();
    }
}
