/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSFileStore;

/**
 * Common implementations for OSFileStore
 */
@ThreadSafe
public abstract class AbstractOSFileStore implements OSFileStore {

    private String name;
    private String volume;
    private String label;
    private String mount;
    private String options;
    private String uuid;
    private boolean local;

    /**
     * Creates an AbstractOSFileStore with the given parameters.
     *
     * @param name    the file store name
     * @param volume  the volume name
     * @param label   the volume label
     * @param mount   the mount point
     * @param options the mount options
     * @param uuid    the UUID
     * @param local   whether this is a local file store
     */
    protected AbstractOSFileStore(String name, String volume, String label, String mount, String options, String uuid,
            boolean local) {
        this.name = name;
        this.volume = volume;
        this.label = label;
        this.mount = mount;
        this.options = options;
        this.uuid = uuid;
        this.local = local;
    }

    /**
     * Default constructor for subclass use.
     */
    protected AbstractOSFileStore() {
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVolume() {
        return this.volume;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getMount() {
        return this.mount;
    }

    @Override
    public String getOptions() {
        return options;
    }

    @Override
    public String getUUID() {
        return this.uuid;
    }

    @Override
    public boolean isLocal() {
        return this.local;
    }

    @Override
    public String toString() {
        return "OSFileStore [name=" + getName() + ", volume=" + getVolume() + ", label=" + getLabel()
                + ", logicalVolume=" + getLogicalVolume() + ", mount=" + getMount() + ", description="
                + getDescription() + ", fsType=" + getType() + ", options=\"" + getOptions() + "\", uuid=" + getUUID()
                + ", isLocal=" + isLocal() + ", freeSpace=" + getFreeSpace() + ", usableSpace=" + getUsableSpace()
                + ", totalSpace=" + getTotalSpace() + ", freeInodes=" + getFreeInodes() + ", totalInodes="
                + getTotalInodes() + "]";
    }
}
