/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.linux.ProcPath;

/**
 * Constants and shared logic for the Linux auxiliary vector ({@code /proc/self/auxv}).
 *
 * @see <a href="https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h">auxvec.h</a>
 */
@ThreadSafe
public final class Auxv {

    private Auxv() {
    }

    /** end of vector */
    public static final int AT_NULL = 0;
    /** system page size */
    public static final int AT_PAGESZ = 6;
    /** arch dependent hints at CPU capabilities */
    public static final int AT_HWCAP = 16;
    /** frequency at which times() increments */
    public static final int AT_CLKTCK = 17;

    /**
     * Reads a native-long value from a {@link ByteBuffer} and returns it as a Java {@code long}.
     */
    @FunctionalInterface
    public interface NativeLongReader {
        /**
         * Reads the next native-long value from the buffer.
         *
         * @param buffer the buffer to read from
         * @return the value as a Java long
         */
        long read(ByteBuffer buffer);
    }

    /**
     * Retrieve the auxiliary vector for the current process.
     *
     * @param reader a function that reads a native-long from a {@link ByteBuffer}
     * @return A map of auxiliary vector keys to their respective values
     * @see <a href="https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h">auxvec.h</a>
     */
    public static Map<Integer, Long> queryAuxv(NativeLongReader reader) {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(ProcPath.AUXV);
        Map<Integer, Long> auxvMap = new HashMap<>();
        int key;
        do {
            key = (int) reader.read(buff);
            if (key != AT_NULL) {
                auxvMap.put(key, reader.read(buff));
            }
        } while (key != AT_NULL);
        return auxvMap;
    }
}
