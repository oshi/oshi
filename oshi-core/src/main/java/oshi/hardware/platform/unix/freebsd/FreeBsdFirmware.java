/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Triplet;

/**
 * Firmware information from dmidecode
 */
@Immutable
final class FreeBsdFirmware extends AbstractFirmware {

    private final Supplier<Triplet<String, String, String>> manufVersRelease = memoize(FreeBsdFirmware::readDmiDecode);

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

    /*
     * Name and Description not set
     */

    private static Triplet<String, String, String> readDmiDecode() {
        String manufacturer = null;
        String version = null;
        String releaseDate = "";

        // $ sudo dmidecode -t bios
        // # dmidecode 3.0
        // Scanning /dev/mem for entry point.
        // SMBIOS 2.7 present.
        //
        // Handle 0x0000, DMI type 0, 24 bytes
        // BIOS Information
        // Vendor: Parallels Software International Inc.
        // Version: 11.2.1 (32626)
        // Release Date: 07/15/2016
        // ... <snip> ...
        // BIOS Revision: 11.2
        // Firmware Revision: 11.2

        final String manufacturerMarker = "Vendor:";
        final String versionMarker = "Version:";
        final String releaseDateMarker = "Release Date:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t bios")) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = checkLine.split(manufacturerMarker)[1].trim();
            } else if (checkLine.contains(versionMarker)) {
                version = checkLine.split(versionMarker)[1].trim();
            } else if (checkLine.contains(releaseDateMarker)) {
                releaseDate = checkLine.split(releaseDateMarker)[1].trim();
            }
        }
        releaseDate = ParseUtil.parseMmDdYyyyToYyyyMmDD(releaseDate);
        return new Triplet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate);
    }
}
