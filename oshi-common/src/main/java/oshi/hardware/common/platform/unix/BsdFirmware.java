/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Common BSD Firmware implementation. The BSDs report the same three firmware attributes, memoized from a single
 * command, and differ only in which command supplies them: most read the boot-time {@code dmesg} banner, while FreeBSD
 * uses {@code dmidecode}. Name and description are not available from any of those sources, so both keep the
 * {@link AbstractFirmware} defaults.
 */
@Immutable
public abstract class BsdFirmware extends AbstractFirmware {
    private static final Pattern VENDOR = Pattern.compile("vendor");

    private final Supplier<Triplet<String, String, String>> manufVersRelease = memoize(this::queryFirmware);

    /**
     * Default constructor.
     */
    protected BsdFirmware() {
    }

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

    /**
     * Reads the platform's firmware attributes. Any attribute the platform cannot supply may be returned as null or
     * blank; the caller substitutes {@link Constants#UNKNOWN}.
     * <p>
     * Defaults to the boot-time {@code dmesg} banner, which is what most of the BSDs report firmware through. FreeBSD
     * overrides this because it reads {@code dmidecode} instead.
     *
     * @return a {@link Triplet} of manufacturer, version, and release date
     */
    protected Triplet<String, String, String> readFirmware() {
        return parseDmesg(ExecutingCommand.runNative("dmesg"));
    }

    private Triplet<String, String, String> queryFirmware() {
        Triplet<String, String, String> dmi = readFirmware();
        return new Triplet<>(unknownIfBlank(dmi.getA()), unknownIfBlank(dmi.getB()), unknownIfBlank(dmi.getC()));
    }

    private static String unknownIfBlank(String value) {
        return ParseUtil.getStringValueOrUnknown(value);
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
                String afterVendor = VENDOR.split(line, -1)[1].trim();
                int versionIdx = afterVendor.indexOf(" version ");
                vendor = versionIdx > 0 ? afterVendor.substring(0, versionIdx)
                        : ParseUtil.whitespaces.split(afterVendor, -1)[0];
            }
        }
        return new Triplet<>(vendor, version, releaseDate);
    }
}
