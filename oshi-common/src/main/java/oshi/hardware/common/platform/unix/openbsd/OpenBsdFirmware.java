/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

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

        for (String line : dmesg) {
            if (line.startsWith("bios0: vendor")) {
                version = ParseUtil.getStringBetween(line, '"');
                releaseDate = ParseUtil.parseMmDdYyyyToYyyyMmDD(ParseUtil.parseLastString(line));
                vendor = line.split("vendor")[1].trim();
            }
        }
        return new Triplet<>(vendor, version, releaseDate);
    }
}
