/*
 * Copyright (c) 2016 com.github.dblock.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.hardware;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class HWDiskStore implements OshiJsonObject {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private String name;
    private String model;
    private String serial;
    private long reads;
    private long writes;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the serial
     */
    public String getSerial() {
        return serial;
    }

    /**
     * @param serial the serial to set
     */
    public void setSerial(String serial) {
        this.serial = serial;
    }

    /**
     * @return the reads
     */
    public long getReads() {
        return reads;
    }

    /**
     * @param reads the reads to set
     */
    public void setReads(long reads) {
        this.reads = reads;
    }

    /**
     * @return the writes
     */
    public long getWrites() {
        return writes;
    }

    /**
     * @param writes the writes to set
     */
    public void setWrites(long writes) {
        this.writes = writes;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("name", getName())
                .add("model", getModel())
                .add("serial", getSerial())
                .add("reads", getReads())
                .add("writes", getWrites())
                .build();
    }
}
