/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris;

import java.nio.ByteBuffer;

import oshi.util.FileUtil;

/**
 * 64-bit Solaris {@code timestruc_t}: seconds and nanoseconds, each 8 bytes.
 */
public class SolarisTimestruc {

    public long tv_sec; // seconds
    public long tv_nsec; // nanoseconds

    public SolarisTimestruc(ByteBuffer buff) {
        this.tv_sec = FileUtil.readLongFromBuffer(buff);
        this.tv_nsec = FileUtil.readLongFromBuffer(buff);
    }

    /**
     * Returns this timestamp converted to milliseconds since the epoch.
     *
     * @return the timestamp in milliseconds
     */
    public long toMillis() {
        return tv_sec * 1000L + tv_nsec / 1_000_000L;
    }
}
