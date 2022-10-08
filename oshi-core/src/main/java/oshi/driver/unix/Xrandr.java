/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query xrandr
 */
@ThreadSafe
public final class Xrandr {

    private static final String[] XRANDR_VERBOSE = { "xrandr", "--verbose" };

    private Xrandr() {
    }

    public static List<byte[]> getEdidArrays() {
        // Special handling for X commands, don't use LC_ALL
        List<String> xrandr = ExecutingCommand.runNative(XRANDR_VERBOSE, null);
        // xrandr reports edid in multiple lines. After seeing a line containing
        // EDID, read subsequent lines of hex until 256 characters are reached
        if (xrandr.isEmpty()) {
            return Collections.emptyList();
        }
        List<byte[]> displays = new ArrayList<>();
        StringBuilder sb = null;
        for (String s : xrandr) {
            if (s.contains("EDID")) {
                sb = new StringBuilder();
            } else if (sb != null) {
                sb.append(s.trim());
                if (sb.length() < 256) {
                    continue;
                }
                String edidStr = sb.toString();
                byte[] edid = ParseUtil.hexStringToByteArray(edidStr);
                if (edid.length >= 128) {
                    displays.add(edid);
                }
                sb = null;
            }
        }
        return displays;
    }
}
