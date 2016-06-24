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
package oshi.json.software.os.impl;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.FileSystem;
import oshi.json.software.os.OperatingSystem;
import oshi.json.software.os.OperatingSystemVersion;

public class OperatingSystemImpl implements OperatingSystem {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OperatingSystem os;

    private OperatingSystemVersion version;

    public OperatingSystemImpl(oshi.software.os.OperatingSystem operatingSystem) {
        this.os = operatingSystem;
        this.version = new OperatingSystemVersionImpl(os.getVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFamily() {
        return this.os.getFamily();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.os.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperatingSystemVersion getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new FileSystemImpl(this.os.getFileSystem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("manufacturer", getManufacturer())
                .add("family", getFamily()).add("version", getVersion().toJSON())
                .add("fileSystem", getFileSystem().toJSON()).build();
    }

    @Override
    public String toString() {
        return this.os.toString();
    }
}
