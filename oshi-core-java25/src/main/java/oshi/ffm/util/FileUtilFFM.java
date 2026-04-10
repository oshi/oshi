/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util;

import java.nio.ByteBuffer;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.util.FileUtil;

/**
 * FFM-specific extensions to {@link FileUtil} for reading native types from buffers.
 */
@ThreadSafe
public final class FileUtilFFM {

    private FileUtilFFM() {
    }

    /**
     * Reads a native {@code long} value from a ByteBuffer, using the platform's native long size.
     *
     * @param buff The bytebuffer to read from
     * @return The next value as a Java long
     */
    public static long readNativeLongFromBuffer(ByteBuffer buff) {
        return ForeignFunctions.NATIVE_LONG_SIZE == 4 ? FileUtil.readIntFromBuffer(buff)
                : FileUtil.readLongFromBuffer(buff);
    }

    /**
     * Reads a native {@code size_t} value from a ByteBuffer, using the platform's native size_t size.
     *
     * @param buff The bytebuffer to read from
     * @return The next value as a Java long
     */
    public static long readSizeTFromBuffer(ByteBuffer buff) {
        return ForeignFunctions.NATIVE_SIZE_T_SIZE == 4 ? Integer.toUnsignedLong(FileUtil.readIntFromBuffer(buff))
                : FileUtil.readLongFromBuffer(buff);
    }

    /**
     * Reads a native pointer value from a ByteBuffer, using the platform's native pointer size.
     *
     * @param buff The bytebuffer to read from
     * @return The next value as a Java long representing the pointer address, or 0 if insufficient data
     */
    public static long readPointerFromBuffer(ByteBuffer buff) {
        if (buff.position() <= buff.limit() - ForeignFunctions.NATIVE_POINTER_SIZE) {
            return ForeignFunctions.NATIVE_POINTER_SIZE == 4 ? Integer.toUnsignedLong(buff.getInt()) : buff.getLong();
        }
        return 0L;
    }
}
