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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.software.os.OSFileStore;
import oshi.software.os.linux.LinuxFileSystem;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class LinuxHardwareAbstractionLayer implements HardwareAbstractionLayer {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxHardwareAbstractionLayer.class);

    private CentralProcessor processor;

    private GlobalMemory memory;

    private Sensors sensors;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    @Override
    public GlobalMemory getMemory() {
        if (this.memory == null) {
            this.memory = new LinuxGlobalMemory();
        }
        return this.memory;
    }

    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = new LinuxCentralProcessor();
        }
        return this.processor;
    }

    @Override
    public PowerSource[] getPowerSources() {
        return LinuxPowerSource.getPowerSources();
    }

    @Override
    public OSFileStore[] getFileStores() {
        return LinuxFileSystem.getFileStores();
    }

    @Override
    public Display[] getDisplays() {
        return LinuxDisplay.getDisplays();
    }

    @Override
    public Sensors getSensors() {
        if (this.sensors == null) {
            this.sensors = new LinuxSensors();
        }
        return this.sensors;
    }

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder powerSourceArrayBuilder = jsonFactory.createArrayBuilder();
        for (PowerSource powerSource : getPowerSources()) {
            powerSourceArrayBuilder.add(powerSource.toJSON());
        }
        JsonArrayBuilder fileStoreArrayBuilder = jsonFactory.createArrayBuilder();
        for (OSFileStore fileStore : getFileStores()) {
            fileStoreArrayBuilder.add(fileStore.toJSON());
        }
        JsonArrayBuilder displayArrayBuilder = jsonFactory.createArrayBuilder();
        for (Display display : getDisplays()) {
            displayArrayBuilder.add(display.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("processor", getProcessor().toJSON()).add("memory", getMemory().toJSON())
                .add("powerSources", powerSourceArrayBuilder.build()).add("fileStores", fileStoreArrayBuilder.build())
                .add("displays", displayArrayBuilder.build()).add("sensors", getSensors().toJSON()).build();
    }
}
