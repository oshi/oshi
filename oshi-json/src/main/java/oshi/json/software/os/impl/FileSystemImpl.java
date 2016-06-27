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

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.FileSystem;
import oshi.json.software.os.OSFileStore;
import oshi.json.util.PropertiesUtil;

public class FileSystemImpl extends AbstractOshiJsonObject implements FileSystem {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.FileSystem fileSystem;

    public FileSystemImpl(oshi.software.os.FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSFileStore[] getFileStores() {
        oshi.software.os.OSFileStore[] fs = this.fileSystem.getFileStores();
        OSFileStore[] fileStores = new OSFileStore[fs.length];
        for (int i = 0; i < fs.length; i++) {
            fileStores[i] = new OSFileStore(fs[i].getName(), fs[i].getVolume(), fs[i].getMount(),
                    fs[i].getDescription(), fs[i].getType(), fs[i].getUUID(), fs[i].getUsableSpace(),
                    fs[i].getTotalSpace());
        }
        return fileStores;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOpenFileDescriptors() {
        return this.fileSystem.getOpenFileDescriptors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxFileDescriptors() {
        return this.fileSystem.getMaxFileDescriptors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores")) {
            JsonArrayBuilder fileStoreArrayBuilder = jsonFactory.createArrayBuilder();
            for (OSFileStore fileStore : getFileStores()) {
                fileStoreArrayBuilder.add(fileStore.toJSON(properties));
            }
            json.add("fileStores", fileStoreArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.openFileDescriptors")) {
            json.add("openFileDescriptors", getOpenFileDescriptors());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.maxFileDescriptors")) {
            json.add("maxFileDescriptors", getMaxFileDescriptors());
        }
        return json.build();
    }
}
