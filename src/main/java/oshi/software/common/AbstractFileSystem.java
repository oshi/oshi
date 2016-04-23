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
package oshi.software.common;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.NullAwareJsonObjectBuilder;
import oshi.software.os.OSFileStore;

/**
 * The File System is a storage pool, device, partition, volume, concrete file
 * system or other implementation specific means of file storage. See subclasses
 * for definitions as they apply to specific platforms.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class AbstractFileSystem implements OSFileStore {

    protected String name;

    protected String description;

    protected long usableSpace;

    protected long totalSpace;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * Creates a {@link AbstractFileSystem} with the specified parameters.
     * 
     * @param newName
     *            Name of the filestore
     * @param newDescription
     *            Description of the file store
     * @param newUsableSpace
     *            Available/usable bytes
     * @param newTotalSpace
     *            Total bytes
     */
    public AbstractFileSystem(String newName, String newDescription, long newUsableSpace, long newTotalSpace) {
        this.setName(newName);
        this.setDescription(newDescription);
        this.setUsableSpace(newUsableSpace);
        this.setTotalSpace(newTotalSpace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(String value) {
        this.name = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsableSpace() {
        return this.usableSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUsableSpace(long value) {
        this.usableSpace = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalSpace() {
        return this.totalSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTotalSpace(long value) {
        this.totalSpace = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("description", getDescription()).add("usableSpace", getUsableSpace())
                .add("totalSpace", getTotalSpace()).build();
    }
}
