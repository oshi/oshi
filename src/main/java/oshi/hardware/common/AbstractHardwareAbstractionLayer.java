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
package oshi.hardware.common;

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
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.software.common.OSFileStore;

public abstract class AbstractHardwareAbstractionLayer implements HardwareAbstractionLayer {
    protected CentralProcessor processor;

    protected GlobalMemory memory;

    protected Sensors sensors;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract GlobalMemory getMemory();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract CentralProcessor getProcessor();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract PowerSource[] getPowerSources();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract OSFileStore[] getFileStores();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Display[] getDisplays();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Sensors getSensors();

    /**
     * {@inheritDoc}
     */
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
