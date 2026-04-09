/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.util;

import java.nio.ByteBuffer;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;

/**
 * JNA-specific extensions to {@link FileUtil} for reading native types from buffers and freeing native memory.
 */
@ThreadSafe
public final class FileUtilJNA {

    private FileUtilJNA() {
    }

    /**
     * Reads a NativeLong value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next value
     */
    public static NativeLong readNativeLongFromBuffer(ByteBuffer buff) {
        return new NativeLong(
                Native.LONG_SIZE == 4 ? FileUtil.readIntFromBuffer(buff) : FileUtil.readLongFromBuffer(buff));
    }

    /**
     * Reads a size_t value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next value
     */
    public static size_t readSizeTFromBuffer(ByteBuffer buff) {
        return new size_t(
                Native.SIZE_T_SIZE == 4 ? FileUtil.readIntFromBuffer(buff) : FileUtil.readLongFromBuffer(buff));
    }

    /**
     * Reads a Pointer value from a ByteBuffer
     *
     * @param buff The bytebuffer to read from
     * @return The next value
     */
    public static Pointer readPointerFromBuffer(ByteBuffer buff) {
        if (buff.position() <= buff.limit() - Native.POINTER_SIZE) {
            return Native.POINTER_SIZE == 4 ? new Pointer(buff.getInt()) : new Pointer(buff.getLong());
        }
        return Pointer.NULL;
    }

    /**
     * If the given Pointer is of class Memory, executes the close method on it to free its native allocation
     *
     * @param p A pointer
     */
    public static void freeMemory(Pointer p) {
        if (p instanceof Memory) {
            ((Memory) p).close();
        }
    }
}
