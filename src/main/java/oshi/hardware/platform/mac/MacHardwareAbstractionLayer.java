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
package oshi.hardware.platform.mac;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.mac.MacFileSystem;

public class MacHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    /**
     * {@inheritDoc}
     */
    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = new MacCentralProcessor();
        }
        return this.processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GlobalMemory getMemory() {
        if (this.memory == null) {
            this.memory = new MacGlobalMemory();
        }
        return this.memory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PowerSource[] getPowerSources() {
        return MacPowerSource.getPowerSources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSFileStore[] getFileStores() {
        return MacFileSystem.getFileStores();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Display[] getDisplays() {
        return MacDisplay.getDisplays();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sensors getSensors() {
        if (this.sensors == null) {
            this.sensors = new MacSensors();
        }
        return this.sensors;
    }

    @Override
    public HWDiskStore[] getDisksStores() {
        return new MacDisks().getDisks();
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
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("processor", getProcessor().toJSON())
                .add("memory", getMemory().toJSON()).add("powerSources", powerSourceArrayBuilder.build())
                .add("fileStores", fileStoreArrayBuilder.build()).add("displays", displayArrayBuilder.build())
                .add("sensors", getSensors().toJSON()).build();
    }
}
