/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query lshal
 */
@ThreadSafe
public final class Lshal {

    private static final String LSHAL_CMD = "lshal";

    private Lshal() {
    }

    /**
     * Query lshal to get device major
     *
     * @return A map with disk names as the key and block device major as the value if lshal is installed; empty map
     *         otherwise
     */
    public static Map<String, Integer> queryDiskToMajorMap() {
        Map<String, Integer> majorMap = new HashMap<>();
        List<String> lshal = ExecutingCommand.runNative(LSHAL_CMD);
        String diskName = null;
        for (String line : lshal) {
            if (line.startsWith("udi ")) {
                String udi = ParseUtil.getSingleQuoteStringValue(line);
                diskName = udi.substring(udi.lastIndexOf('/') + 1);
            } else {
                line = line.trim();
                if (line.startsWith("block.major") && diskName != null) {
                    majorMap.put(diskName, ParseUtil.getFirstIntValue(line));
                }
            }
        }
        return majorMap;
    }
}
