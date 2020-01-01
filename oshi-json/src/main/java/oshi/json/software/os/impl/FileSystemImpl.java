/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

/**
 * Wrapper class to implement FileSystem interface with platform-specific
 * objects
 */
public class FileSystemImpl extends AbstractOshiJsonObject implements FileSystem {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.FileSystem fileSystem;

    /**
     * Creates a new platform-specific FileSystem object wrapping the provided
     * argument
     *
     * @param fileSystem
     *            a platform-specific FileSystem object
     */
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
            fileStores[i] = new OSFileStore(fs[i]);
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
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem.fileStores")) {
            JsonArrayBuilder fileStoreArrayBuilder = this.jsonFactory.createArrayBuilder();
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
