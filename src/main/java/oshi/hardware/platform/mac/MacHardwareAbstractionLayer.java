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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.software.os.OSFileStore;
import oshi.software.os.mac.MacFileSystem;

/**
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class MacHardwareAbstractionLayer implements HardwareAbstractionLayer {

    private CentralProcessor processor;

    private GlobalMemory _memory;

    private PowerSource[] _powerSources;

    private Sensors sensors;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /*
     * (non-Javadoc)
     * 
     * @see oshi.hardware.HardwareAbstractionLayer#getProcessor()
     */
    @Override
    public CentralProcessor getProcessor() {
        if (this.processor == null) {
            this.processor = new MacCentralProcessor();
        }
        return this.processor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see oshi.hardware.HardwareAbstractionLayer#getMemory()
     */
    @Override
    public GlobalMemory getMemory() {
        if (this._memory == null) {
            this._memory = new MacGlobalMemory();
        }
        return this._memory;
    }

    @Override
    public PowerSource[] getPowerSources() {
        if (this._powerSources == null) {
            this._powerSources = MacPowerSource.getPowerSources();
        }
        return this._powerSources;
    }

    @Override
    public OSFileStore[] getFileStores() {
        return MacFileSystem.getFileStores();
    }

    @Override
    public Display[] getDisplays() {
        return MacDisplay.getDisplays();
    }

    @Override
    public Sensors getSensors() {
        if (this.sensors == null) {
            this.sensors = new MacSensors();
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
        return jsonFactory.createObjectBuilder().add("processor", getProcessor().toJSON())
                .add("memory", getMemory().toJSON()).add("powerSources", powerSourceArrayBuilder.build())
                .add("fileStores", fileStoreArrayBuilder.build()).add("displays", displayArrayBuilder.build())
                .add("sensors", getSensors().toJSON()).build();
    }
}
