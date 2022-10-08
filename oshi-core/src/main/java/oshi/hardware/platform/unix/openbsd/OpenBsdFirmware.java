/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

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
 * OpenBSD Firmware implementation
 */
@Immutable
public class OpenBsdFirmware extends AbstractFirmware {
    private final Supplier<Triplet<String, String, String>> manufVersRelease = memoize(OpenBsdFirmware::readDmesg);

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
        String version = null;
        String vendor = null;
        String releaseDate = "";

        List<String> dmesg = ExecutingCommand.runNative("dmesg");
        for (String line : dmesg) {
            // bios0 at mainbus0: SMBIOS rev. 2.7 @ 0xdcc0e000 (67 entries)
            // bios0: vendor LENOVO version "GLET90WW (2.44 )" date 09/13/2017
            // bios0: LENOVO 20AWA08J00
            if (line.startsWith("bios0: vendor")) {
                version = ParseUtil.getStringBetween(line, '"');
                releaseDate = ParseUtil.parseMmDdYyyyToYyyyMmDD(ParseUtil.parseLastString(line));
                vendor = line.split("vendor")[1].trim();
            }
        }
        return new Triplet<>(Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate);
    }
}
