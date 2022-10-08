/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query lssrad
 */
@ThreadSafe
public final class Lssrad {

    private Lssrad() {
    }

    /**
     * Query {@code lssrad} to get numa node and physical package info
     *
     * @return A map of processor number to a pair containing the ref (NUMA equivalent) and srad (package)
     */
    public static Map<Integer, Pair<Integer, Integer>> queryNodesPackages() {
        /*-
        # lssrad -av
        REF1        SRAD        MEM        CPU
        0
                       0       32749.12    0-63
                       1        9462.00    64-67 72-75
                                           80-83 88-91
        1
                       2        2471.19    92-95
        2
                       3        1992.00
                       4         249.00
         */
        int node = 0;
        int slot = 0;
        Map<Integer, Pair<Integer, Integer>> nodeMap = new HashMap<>();
        List<String> lssrad = ExecutingCommand.runNative("lssrad -av");
        // remove header
        if (!lssrad.isEmpty()) {
            lssrad.remove(0);
        }
        for (String s : lssrad) {
            String t = s.trim();
            if (!t.isEmpty()) {
                if (Character.isDigit(s.charAt(0))) {
                    node = ParseUtil.parseIntOrDefault(t, 0);
                } else {
                    if (t.contains(".")) {
                        String[] split = ParseUtil.whitespaces.split(t, 3);
                        slot = ParseUtil.parseIntOrDefault(split[0], 0);
                        t = split.length > 2 ? split[2] : "";
                    }
                    for (Integer proc : ParseUtil.parseHyphenatedIntList(t)) {
                        nodeMap.put(proc, new Pair<>(node, slot));
                    }
                }
            }
        }
        return nodeMap;
    }
}
