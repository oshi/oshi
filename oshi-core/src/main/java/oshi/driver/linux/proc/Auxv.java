/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Utility to read auxiliary vector from {@code /proc/self/auxv}
 */
@ThreadSafe
public final class Auxv {

    private Auxv() {
    }

    /**
     * system page size
     */
    public static final int AT_PAGESZ = 6;
    /**
     * arch dependent hints at CPU capabilities
     */
    public static final int AT_HWCAP = 16;
    /**
     * frequency at which times() increments
     */
    public static final int AT_CLKTCK = 17;

    /**
     * Retrieve the auxiliary vector for the current process
     *
     * @return A map of auxiliary vector keys to their respective values
     * @see <a href= "https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h">auxvec.h</a>
     */
    public static Map<Integer, Long> queryAuxv() {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(ProcPath.AUXV);
        Map<Integer, Long> auxvMap = new HashMap<>();
        int key;
        do {
            key = FileUtil.readNativeLongFromBuffer(buff).intValue();
            if (key > 0) {
                auxvMap.put(key, FileUtil.readNativeLongFromBuffer(buff).longValue());
            }
        } while (key > 0);
        return auxvMap;

    }
}
