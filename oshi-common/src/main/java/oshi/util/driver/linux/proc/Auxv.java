/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
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
     * Retrieve the auxiliary vector for the current process.
     *
     * @return A map of auxiliary vector keys to their respective values, empty if it could not be read
     * @see <a href="https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h">auxvec.h</a>
     */
    public static Map<Integer, Long> queryAuxv() {
        // Each entry is a pair of C longs, which on Linux are the width of the process's pointers
        return parseAuxv(FileUtil.readAllBytesAsBuffer(ProcPath.AUXV), Util.is64Bit());
    }

    /**
     * Parses an auxiliary vector. Package-private for testing.
     *
     * @param buff    the vector, in the byte order of the process that wrote it
     * @param is64Bit whether each key and value is 8 bytes rather than 4
     * @return A map of auxiliary vector keys to their respective values
     */
    static Map<Integer, Long> parseAuxv(ByteBuffer buff, boolean is64Bit) {
        Map<Integer, Long> auxvMap = new HashMap<>();
        int key;
        do {
            key = (int) readLong(buff, is64Bit);
            if (key != AT_NULL) {
                auxvMap.put(key, readLong(buff, is64Bit));
            }
        } while (key != AT_NULL);
        return auxvMap;
    }

    private static long readLong(ByteBuffer buff, boolean is64Bit) {
        return is64Bit ? FileUtil.readLongFromBuffer(buff) : FileUtil.readIntFromBuffer(buff);
    }

    /**
     * Gets the clock ticks per second from an auxiliary vector, falling back to {@code getconf CLK_TCK}.
     *
     * @param auxv the auxiliary vector, from {@link #queryAuxv()}
     * @return clock ticks per second, 100 if neither source gives a value
     */
    public static long userHz(Map<Integer, Long> auxv) {
        long hz = auxv.getOrDefault(AT_CLKTCK, 0L);
        return hz > 0 ? hz : ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"), 100L);
    }

    /**
     * Gets the page size from an auxiliary vector, falling back to {@code getconf PAGE_SIZE}.
     *
     * @param auxv the auxiliary vector, from {@link #queryAuxv()}
     * @return the page size in bytes, 4096 if neither source gives a value
     */
    public static long pageSize(Map<Integer, Long> auxv) {
        long pagesz = auxv.getOrDefault(AT_PAGESZ, 0L);
        return pagesz > 0 ? pagesz
                : ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGE_SIZE"), 4096L);
    }
}
