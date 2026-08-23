/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.BsdFirmware;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Firmware information from dmidecode
 */
@Immutable
public class FreeBsdFirmware extends BsdFirmware {

    @Override
    protected Triplet<@Nullable String, @Nullable String, @Nullable String> readFirmware() {
        // Only works with root permissions but it's all we've got
        return parseDmiDecode(ExecutingCommand.runNative("dmidecode -t bios"));
    }

    /**
     * Parses the output of {@code dmidecode -t bios} into its manufacturer, version, and release date fields. Any field
     * not present in the output is returned as {@code null} (or an empty date); the caller applies fallbacks.
     *
     * @param dmidecode the lines emitted by {@code dmidecode -t bios}
     * @return a {@link Triplet} of manufacturer, version, and release date
     */
    static Triplet<@Nullable String, @Nullable String, @Nullable String> parseDmiDecode(List<String> dmidecode) {
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

        for (final String checkLine : dmidecode) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = ParseUtil.getTextAfterString(checkLine, manufacturerMarker).trim();
            } else if (checkLine.contains(versionMarker)) {
                version = ParseUtil.getTextAfterString(checkLine, versionMarker).trim();
            } else if (checkLine.contains(releaseDateMarker)) {
                releaseDate = ParseUtil.getTextAfterString(checkLine, releaseDateMarker).trim();
            }
        }
        releaseDate = ParseUtil.parseMmDdYyyyToYyyyMmDD(releaseDate);
        return new Triplet<>(manufacturer, version, releaseDate);
    }
}
