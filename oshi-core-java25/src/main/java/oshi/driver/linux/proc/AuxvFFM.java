/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.FileUtilFFM;
import oshi.util.FileUtil;
import oshi.util.driver.linux.proc.Auxv;
import oshi.util.linux.ProcPath;

/**
 * FFM-based utility to read the auxiliary vector from {@code /proc/self/auxv}.
 */
@ThreadSafe
public final class AuxvFFM {

    private AuxvFFM() {
    }

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
            key = (int) FileUtilFFM.readNativeLongFromBuffer(buff);
            if (key != Auxv.AT_NULL) {
                auxvMap.put(key, FileUtilFFM.readNativeLongFromBuffer(buff));
            }
        } while (key != Auxv.AT_NULL);
        return auxvMap;
    }
}
