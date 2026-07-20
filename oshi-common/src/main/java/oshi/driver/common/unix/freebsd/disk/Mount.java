/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;

/**
 * Utility to query mount
 */
@ThreadSafe
public final class Mount {

    private static final String MOUNT_CMD = "mount";
    private static final Pattern MOUNT_PATTERN = Pattern.compile("/dev/(\\S+p\\d+) on (\\S+) .*");

    private Mount() {
    }

    /**
     * Query mount to map partitions to mount points
     *
     * @return A map with partitions as the key and mount points as the value
     */
    public static Map<String, String> queryPartitionToMountMap() {
        return parseMountOutput(ExecutingCommand.runNative(MOUNT_CMD));
    }

    /**
     * Parses the output of the {@code mount} command into a partition-to-mount-point map.
     *
     * @param mountOutput the lines of output from {@code mount}
     * @return A map with partitions as the key and mount points as the value
     */
    static Map<String, String> parseMountOutput(List<String> mountOutput) {
        Map<String, String> mountMap = new HashMap<>();
        for (String mnt : mountOutput) {
            Matcher m = MOUNT_PATTERN.matcher(mnt);
            if (m.matches()) {
                mountMap.put(m.group(1), m.group(2));
            }
        }
        return mountMap;
    }
}
