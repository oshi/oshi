/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.memoize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.platform.unix.BsdComputerSystem;
import oshi.hardware.common.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * NetBSD ComputerSystem implementation. Reads the SMBIOS strings the kernel publishes under {@code machdep.dmi}. The
 * serial numbers and UUID there are readable only by root, and the board values are absent where the firmware has no
 * baseboard table, in which case the system values stand in for them.
 */
@Immutable
public class NetBsdComputerSystem extends BsdComputerSystem {

    private static final String DMI_PREFIX = "machdep.dmi.";

    private final Supplier<Map<String, String>> dmi = memoize(NetBsdComputerSystem::queryDmi);

    @Override
    protected String queryManufacturer() {
        return dmi("system-vendor");
    }

    @Override
    protected String queryModel() {
        return dmi("system-product");
    }

    @Override
    protected String querySerialNumber() {
        return dmi("system-serial");
    }

    @Override
    protected String queryUUID() {
        return dmi("system-uuid");
    }

    @Override
    protected Firmware createFirmware() {
        return new NetBsdFirmware();
    }

    @Override
    protected Baseboard createBaseboard() {
        return new UnixBaseboard(boardOrSystem("vendor"), boardOrSystem("product"), boardOrSystem("serial"),
                boardOrSystem("version"));
    }

    private String dmi(String key) {
        return ParseUtil.getStringValueOrUnknown(dmi.get().get(key));
    }

    private String boardOrSystem(String field) {
        String board = dmi("board-" + field);
        return Constants.UNKNOWN.equals(board) ? dmi("system-" + field) : board;
    }

    /**
     * Reads the {@code machdep.dmi} sysctls.
     *
     * @return the values keyed by the name after {@code machdep.dmi.}, e.g. {@code system-vendor}
     */
    static Map<String, String> queryDmi() {
        return parseDmi(ExecutingCommand.runNative("sysctl machdep.dmi"));
    }

    /**
     * Parses the output of {@code sysctl machdep.dmi}, one {@code machdep.dmi.key = value} line per string.
     *
     * @param lines the command output
     * @return the values keyed by the name after {@code machdep.dmi.}, e.g. {@code system-vendor}
     */
    static Map<String, String> parseDmi(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, String> e : ParseUtil.parseStringListToMap(lines, " = ").entrySet()) {
            if (e.getKey().startsWith(DMI_PREFIX)) {
                values.put(e.getKey().substring(DMI_PREFIX.length()), e.getValue());
            }
        }
        return values;
    }
}
