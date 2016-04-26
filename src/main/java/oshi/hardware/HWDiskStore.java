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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * Store object of disk attributes.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class HWDiskStore implements OshiJsonObject {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private String name;
    private String model;
    private String serial;
    private long size;
    private long reads;
    private long writes;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
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
     * @param model
     *            the model to set
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
     * @param serial
     *            the serial to set
     */
    public void setSerial(String serial) {
        this.serial = serial;
    }

    /**
     * @return Get size of disk (in bytes)
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size
     *            Set size of disk (in bytes)
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return the reads
     */
    public long getReads() {
        return reads;
    }

    /**
     * @param reads
     *            the reads to set
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
     * @param writes
     *            the writes to set
     */
    public void setWrites(long writes) {
        this.writes = writes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("model", getModel()).add("serial", getSerial()).add("size", getSize()).add("reads", getReads())
                .add("writes", getWrites()).build();
    }
}
