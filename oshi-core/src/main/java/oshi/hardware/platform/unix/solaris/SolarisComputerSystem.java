/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.util.Memoizer.memoize;
import static oshi.util.ParseUtil.getValueOrUnknown;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.platform.unix.UnixBaseboard;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Hardware data obtained from smbios.
 */
@Immutable
final class SolarisComputerSystem extends AbstractComputerSystem {
    private enum SmbType {
        /**
         * BIOS
         */
        SMB_TYPE_BIOS,
        /**
         * System
         */
        SMB_TYPE_SYSTEM,
        /**
         * Baseboard
         */
        SMB_TYPE_BASEBOARD
    }

    private final Supplier<SmbiosStrings> smbiosStrings = memoize(SolarisComputerSystem::readSmbios);

    @Override
    public String getManufacturer() {
        return smbiosStrings.get().manufacturer;
    }

    @Override
    public String getModel() {
        return smbiosStrings.get().model;
    }

    @Override
    public String getSerialNumber() {
        return smbiosStrings.get().serialNumber;
    }

    @Override
    public String getHardwareUUID() {
        return smbiosStrings.get().uuid;
    }

    @Override
    public Firmware createFirmware() {
        return new SolarisFirmware(smbiosStrings.get().biosVendor, smbiosStrings.get().biosVersion,
                smbiosStrings.get().biosDate);
    }

    @Override
    public Baseboard createBaseboard() {
        return new UnixBaseboard(smbiosStrings.get().boardManufacturer, smbiosStrings.get().boardModel,
                smbiosStrings.get().boardSerialNumber, smbiosStrings.get().boardVersion);
    }

    private static SmbiosStrings readSmbios() {
        String biosVendor = null;
        String biosVersion = null;
        String biosDate = null;

        String manufacturer = null;
        String model = null;
        String serialNumber = null;
        String uuid = null;

        String boardManufacturer = null;
        String boardModel = null;
        String boardVersion = null;
        String boardSerialNumber = null;

        Map<String, String> biosStrings = new HashMap<>();

        // $ smbios
        // ID SIZE TYPE
        // 0 87 SMB_TYPE_BIOS (BIOS Information)
        //
        // Vendor: Parallels Software International Inc.
        // Version String: 11.2.1 (32686)
        // Release Date: 07/15/2016
        // Address Segment: 0xf000
        // ... <snip> ...
        //
        // ID SIZE TYPE
        // 1 177 SMB_TYPE_SYSTEM (system information)
        //
        // Manufacturer: Parallels Software International Inc.
        // Product: Parallels Virtual Platforom
        // Version: None
        // Serial Number: Parallels-45 2E 7E 2D 57 5C 4B 59 B1 30 28 81 B7 81 89
        // 34
        //
        // UUID: 452e7e2d-575c04b59-b130-2881b7818934
        // Wake-up Event: 0x6 (Power Switch)
        // SKU Number: Undefined
        // Family: Parallels VM
        //
        // ID SIZE TYPE
        // 2 90 SMB_TYPE_BASEBOARD (base board)
        //
        // Manufacturer: Parallels Software International Inc.
        // Product: Parallels Virtual Platform
        // Version: None
        // Serial Number: None
        // ... <snip> ...
        //
        // ID SIZE TYPE
        // 3 .... <snip> ...

        final String vendorMarker = "Vendor:";
        final String biosDateMarker = "Release Date:";
        final String biosVersionMarker = "VersionString:";

        final String manufacturerMarker = "Manufacturer:";
        final String productMarker = "Product:";
        final String serialNumMarker = "Serial Number:";
        final String uuidMarker = "UUID:";
        final String versionMarker = "Version:";

        SmbType smbTypeId = null;

        EnumMap<SmbType, Map<String, String>> smbTypesMap = new EnumMap<>(SmbType.class);
        smbTypesMap.put(SmbType.SMB_TYPE_BIOS, new HashMap<>());
        smbTypesMap.put(SmbType.SMB_TYPE_SYSTEM, new HashMap<>());
        smbTypesMap.put(SmbType.SMB_TYPE_BASEBOARD, new HashMap<>());

//        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("smbios")) {
            // Change the smbTypeId when hitting a new header
            if (checkLine.contains("SMB_TYPE_") && (smbTypeId = getSmbType(checkLine)) == null) {
                // If we get past what we need, stop iterating
                break;
            }
            // Based on the smbTypeID we are processing for
            if (smbTypeId != null) {
                String key = checkLine.substring(0, checkLine.indexOf(":")).trim();
                String val = checkLine.substring(checkLine.indexOf(":") + 1).trim();
                smbTypesMap.get(smbTypeId).put(key, val);
            }
        }

        // If we get to end and haven't assigned, use fallback
        if (!smbTypesMap.get(SmbType.SMB_TYPE_SYSTEM).containsKey(serialNumMarker)
                || Util.isBlank(smbTypesMap.get(SmbType.SMB_TYPE_SYSTEM).get(serialNumMarker))) {
            smbTypesMap.get(SmbType.SMB_TYPE_SYSTEM).put(serialNumMarker, readSerialNumber());
        }
        return new SmbiosStrings(smbTypesMap.get(SmbType.SMB_TYPE_BIOS), smbTypesMap.get(SmbType.SMB_TYPE_SYSTEM),
                smbTypesMap.get(SmbType.SMB_TYPE_BASEBOARD));
    }

    private static SmbType getSmbType(String checkLine) {
        if (checkLine.contains("SMB_TYPE_BIOS")) {
            return SmbType.SMB_TYPE_BIOS; // BIOS
        } else if (checkLine.contains("SMB_TYPE_SYSTEM")) {
            return SmbType.SMB_TYPE_SYSTEM; // SYSTEM
        } else if (checkLine.contains("SMB_TYPE_BASEBOARD")) {
            return SmbType.SMB_TYPE_BASEBOARD; // BASEBOARD
        } else {
            // First 3 SMB_TYPEs are what we need. After that no need to
            // continue processing the output
            return null;
        }
    }

    private static String readSerialNumber() {
        // If they've installed STB (Sun Explorer) this should work
        String serialNumber = ExecutingCommand.getFirstAnswer("sneep");
        // if that didn't work, try...
        if (serialNumber.isEmpty()) {
            String marker = "chassis-sn:";
            for (String checkLine : ExecutingCommand.runNative("prtconf -pv")) {
                if (checkLine.contains(marker)) {
                    serialNumber = ParseUtil.getSingleQuoteStringValue(checkLine);
                    break;
                }
            }
        }
        return serialNumber;
    }

    private static final class SmbiosStrings {
        private final String biosVendor;
        private final String biosVersion;
        private final String biosDate;

        private final String manufacturer;
        private final String model;
        private final String serialNumber;
        private final String uuid;

        private final String boardManufacturer;
        private final String boardModel;
        private final String boardVersion;
        private final String boardSerialNumber;

        private SmbiosStrings(Map<String, String> smbTypeBIOSStrings, Map<String, String> smbTypeSystemStrings,
                Map<String, String> smbTypeBaseboardStrings) {
            final String vendorMarker = "Vendor:";
            final String biosDateMarker = "Release Date:";
            final String biosVersionMarker = "VersionString:";

            final String manufacturerMarker = "Manufacturer:";
            final String productMarker = "Product:";
            final String serialNumMarker = "Serial Number:";
            final String uuidMarker = "UUID:";
            final String versionMarker = "Version:";

            this.biosVendor = getValueOrUnknown(smbTypeBIOSStrings, vendorMarker);
            this.biosVersion = getValueOrUnknown(smbTypeBIOSStrings, biosVersionMarker);
            this.biosDate = getValueOrUnknown(smbTypeBIOSStrings, biosDateMarker);
            this.manufacturer = getValueOrUnknown(smbTypeSystemStrings, manufacturerMarker);
            this.model = getValueOrUnknown(smbTypeSystemStrings, productMarker);
            this.serialNumber = getValueOrUnknown(smbTypeSystemStrings, serialNumMarker);
            this.uuid = getValueOrUnknown(smbTypeSystemStrings, uuidMarker);
            this.boardManufacturer = getValueOrUnknown(smbTypeBaseboardStrings, manufacturerMarker);
            this.boardModel = getValueOrUnknown(smbTypeBaseboardStrings, productMarker);
            this.boardVersion = getValueOrUnknown(smbTypeBaseboardStrings, versionMarker);
            this.boardSerialNumber = getValueOrUnknown(smbTypeBaseboardStrings, serialNumMarker);
        }
    }

    /**
     * Generate a map of strings parsing the BIOS Strings
     *
     * @param checkLines The lines received by running the bios command
     * @param smbTypeId  The smbTypeID fetched
     * @param param      contains the biosStrings data to be fetched for
     * @return Map of strings based on the param and smbTypeId passed in the argument.
     */
    private static Map<String, String> parseBIOSStrings(List<String> checkLines, int smbTypeId, String... param) {
//        Map<String, String> biosStrings = Arrays.stream(param).filter(checkLine::contains)
//                .collect(Collectors.toMap(p -> p, p -> checkLine.split(p)[1].trim()));
//        Map<String, String> biosStrings = checkLines.stream()
//            .filter(line -> Arrays.stream(param).anyMatch(line::startsWith))
//            .collect(Collectors.toMap(
//                s -> s.substring(0, s.indexOf(":")).trim(),
//                s -> s.substring(s.indexOf(":") + 1).trim()));
        Map<String, String> biosStrings = Arrays.stream(param)
                .filter(key -> checkLines.stream().anyMatch(line -> line.startsWith(key)))
                .collect(Collectors.toMap(s -> s.substring(0, s.indexOf(":")).trim(),
                        s -> s.substring(s.indexOf(":") + 1).trim(), (oldValue, newValue) -> newValue));
        biosStrings.put("smbTypeId", Integer.toString(smbTypeId));
        return biosStrings;
    }
}
