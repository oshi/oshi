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
import oshi.json.software.os.OSFileStore;

public class FileSystemImpl implements FileSystem {

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
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("openFileDescriptors", getOpenFileDescriptors()).add("maxFileDescriptors", getMaxFileDescriptors())
                // TODO add Filestores here, move from separate HAL line
                .build();
    }
}
