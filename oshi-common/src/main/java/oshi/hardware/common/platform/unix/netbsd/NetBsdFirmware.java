/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Triplet;

/**
 * NetBSD Firmware implementation
 */
@Immutable
public class NetBsdFirmware extends AbstractFirmware {
    private final Supplier<Triplet<String, String, String>> manufVersRelease = memoize(NetBsdFirmware::readDmesg);

    @Override
    public String getManufacturer() {
        return manufVersRelease.get().getA();
    }

    @Override
    public String getVersion() {
        return manufVersRelease.get().getB();
    }

    @Override
    public String getReleaseDate() {
        return manufVersRelease.get().getC();
    }

    private static Triplet<String, String, String> readDmesg() {
        Triplet<String, String, String> dmi = parseDmesg(ExecutingCommand.runNative("dmesg"));
        String vendor = dmi.getA();
        String version = dmi.getB();
        String releaseDate = dmi.getC();
        return new Triplet<>(Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate);
    }

    /**
     * Parses the output of {@code dmesg} for BIOS firmware information. Looks for lines starting with
     * {@code "bios0: vendor"} to extract manufacturer, version, and release date. Any field not present in the output
     * is returned as {@code null} (or an empty date); the caller applies fallbacks.
     *
     * @param dmesg the lines emitted by {@code dmesg}
     * @return a {@link Triplet} of vendor, version, and release date
     */
    static Triplet<String, String, String> parseDmesg(List<String> dmesg) {
        String version = null;
        String vendor = null;
        String releaseDate = "";

        // bios0 at mainbus0: SMBIOS rev. 2.7 @ 0xdcc0e000 (67 entries)
        // bios0: vendor LENOVO version "GLET90WW (2.44 )" date 09/13/2017
        // bios0: LENOVO 20AWA08J00
        for (String line : dmesg) {
            if (line.startsWith("bios0: vendor")) {
                version = ParseUtil.getStringBetween(line, '"');
                releaseDate = ParseUtil.parseMmDdYyyyToYyyyMmDD(ParseUtil.parseLastString(line));
                String afterVendor = line.split("vendor")[1].trim();
                vendor = afterVendor.split("\\s+")[0];
            }
        }
        return new Triplet<>(vendor, version, releaseDate);
    }
}
