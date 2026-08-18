/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Random access to a block of HKEY_PERFORMANCE_DATA, so the walk over it can live in one place instead of once per
 * native backend.
 * <p>
 * The walk is entirely offset arithmetic over a flat buffer, identical whichever way the buffer was obtained. Only
 * these three reads differ between JNA and FFM, so they are the whole seam.
 */
@ThreadSafe
public interface PerfDataBuffer {

    /**
     * Reads a 32-bit value.
     *
     * @param offset Byte offset from the start of the buffer
     * @return The value at that offset
     */
    int getInt(long offset);

    /**
     * Reads a 64-bit value.
     *
     * @param offset Byte offset from the start of the buffer
     * @return The value at that offset
     */
    long getLong(long offset);

    /**
     * Reads a null-terminated UTF-16LE string.
     *
     * @param offset Byte offset from the start of the buffer
     * @return The string at that offset
     */
    String getWideString(long offset);
}
