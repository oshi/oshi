/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

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
        return parseNodesPackages(ExecutingCommand.runNative("lssrad -av"));
    }

    /**
     * Parses {@code lssrad -av} output into a map of processor number to its (ref, srad) pair.
     *
     * @param lssrad the lines of {@code lssrad -av} output (including the header row)
     * @return A map of processor number to a pair containing the ref (NUMA equivalent) and srad (package)
     */
    public static Map<Integer, Pair<Integer, Integer>> parseNodesPackages(List<String> lssrad) {
        int node = 0;
        int slot = 0;
        Map<Integer, Pair<Integer, Integer>> nodeMap = new HashMap<>();
        boolean header = true;
        for (String s : lssrad) {
            // Skip the "REF1 SRAD MEM CPU" header row without mutating the input list
            if (header) {
                header = false;
                continue;
            }
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
