/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.util.FormatUtil;

/**
 * Common methods for platform HWDiskStore classes
 */
@ThreadSafe
public abstract class AbstractHWDiskStore implements HWDiskStore {

    private final String name;
    private final String model;
    private final String serial;
    private final long size;

    protected AbstractHWDiskStore(String name, String model, String serial, long size) {
        this.name = name;
        this.model = model;
        this.serial = serial;
        this.size = size;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public String getSerial() {
        return this.serial;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public String toString() {
        boolean readwrite = getReads() > 0 || getWrites() > 0;
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(": ");
        sb.append("(model: ").append(getModel());
        sb.append(" - S/N: ").append(getSerial()).append(") ");
        sb.append("size: ").append(getSize() > 0 ? FormatUtil.formatBytesDecimal(getSize()) : "?").append(", ");
        sb.append("reads: ").append(readwrite ? getReads() : "?");
        sb.append(" (").append(readwrite ? FormatUtil.formatBytes(getReadBytes()) : "?").append("), ");
        sb.append("writes: ").append(readwrite ? getWrites() : "?");
        sb.append(" (").append(readwrite ? FormatUtil.formatBytes(getWriteBytes()) : "?").append("), ");
        sb.append("xfer: ").append(readwrite ? getTransferTime() : "?");
        return sb.toString();
    }
}
