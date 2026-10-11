/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.freebsd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Reads the kernel environment through {@code kenv}. On FreeBSD and DragonFly BSD the loader copies the SMBIOS strings
 * into it as {@code smbios.*} variables, which any user can read, unlike {@code dmidecode}, which needs root.
 */
@ThreadSafe
public final class KenvUtil {

    private KenvUtil() {
    }

    /**
     * Reads every kernel environment variable.
     *
     * @return a map of variable names to their unquoted values, empty if {@code kenv} fails
     */
    public static Map<String, String> queryKenv() {
        return parseKenv(ExecutingCommand.runNative("kenv"));
    }

    /**
     * Parses the output of {@code kenv}, one {@code name="value"} pair per line.
     *
     * @param kenv the lines emitted by {@code kenv}
     * @return a map of variable names to their values, with the enclosing quotes removed
     */
    public static Map<String, String> parseKenv(List<String> kenv) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> e : ParseUtil.parseStringListToMap(kenv, "=").entrySet()) {
            map.put(e.getKey(), ParseUtil.getDoubleQuoteStringValue(e.getValue()));
        }
        return map;
    }
}
