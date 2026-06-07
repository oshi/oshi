/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import java.nio.ByteBuffer;

import oshi.util.FileUtil;

/**
 * 64-bit AIX {@code timestruc_t} required by the {@link AixPsInfo} struct: seconds, nanoseconds, padding.
 */
public class Timestruc {

    public long tv_sec; // seconds
    public int tv_nsec; // nanoseconds
    public int pad; // alignment padding

    public Timestruc(ByteBuffer buff) {
        this.tv_sec = FileUtil.readLongFromBuffer(buff);
        this.tv_nsec = FileUtil.readIntFromBuffer(buff);
        this.pad = FileUtil.readIntFromBuffer(buff);
    }
}
