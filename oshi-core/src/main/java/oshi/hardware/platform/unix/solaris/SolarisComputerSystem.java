/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.util.Memoizer.memoize;
import static oshi.util.Util.parseBIOSStrings;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Hardware data obtained from smbios.
 */
@Immutable
final class SolarisComputerSystem extends AbstractComputerSystem {

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

        int smbTypeId = -1;
        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("smbios")) {
            // Change the smbTypeId when hitting a new header
            if (checkLine.contains("SMB_TYPE_") && (smbTypeId = getSmbType(checkLine)) == Integer.MAX_VALUE) {
                // If we get past what we need, stop iterating
                break;
            }
            // Based on the smbTypeID we are processing for
            switch (smbTypeId) {
            case 0: // BIOS
                biosStrings = parseBIOSStrings(checkLine, smbTypeId, vendorMarker, biosVersionMarker, biosDateMarker);
                break;
            case 1: // SYSTEM
                biosStrings = parseBIOSStrings(checkLine, smbTypeId, manufacturerMarker, productMarker, serialNumMarker,
                        uuidMarker);
                break;
            case 2: // BASEBOARD
                biosStrings = parseBIOSStrings(checkLine, smbTypeId, manufacturerMarker, productMarker, versionMarker,
                        serialNumMarker);
                break;
            default:
                break;
            }
        }
        // If we get to end and haven't assigned, use fallback
        if (!biosStrings.containsKey(serialNumMarker) || Util.isBlank(biosStrings.get(serialNumMarker))) {
            biosStrings.put(serialNumMarker, readSerialNumber());
        }
        return new SmbiosStrings(biosStrings);
    }

    private static int getSmbType(String checkLine) {
        if (checkLine.contains("SMB_TYPE_BIOS")) {
            return 0; // BIOS
        } else if (checkLine.contains("SMB_TYPE_SYSTEM")) {
            return 1; // SYSTEM
        } else if (checkLine.contains("SMB_TYPE_BASEBOARD")) {
            return 2; // BASEBOARD
        } else {
            // First 3 SMB_TYPEs are what we need. After that no need to
            // continue processing the output
            return Integer.MAX_VALUE;
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

        private SmbiosStrings(Map<String, String> biosStrings) {
            final String vendorMarker = "Vendor:";
            final String biosDateMarker = "Release Date:";
            final String biosVersionMarker = "VersionString:";

            final String manufacturerMarker = "Manufacturer:";
            final String productMarker = "Product:";
            final String serialNumMarker = "Serial Number:";
            final String uuidMarker = "UUID:";
            final String versionMarker = "Version:";

            this.biosVendor = !biosStrings.containsKey(vendorMarker) || Util.isBlank(biosStrings.get(vendorMarker))
                    ? Constants.UNKNOWN
                    : biosStrings.get(vendorMarker);
            this.biosVersion = !biosStrings.containsKey(biosVersionMarker)
                    || Util.isBlank(biosStrings.get(biosVersionMarker)) ? Constants.UNKNOWN
                            : biosStrings.get(biosVersionMarker);
            this.biosDate = !biosStrings.containsKey(biosDateMarker) || Util.isBlank(biosStrings.get(biosDateMarker))
                    ? Constants.UNKNOWN
                    : biosStrings.get(biosDateMarker);
            this.uuid = !biosStrings.containsKey(uuidMarker) || Util.isBlank(biosStrings.get(uuidMarker))
                    ? Constants.UNKNOWN
                    : biosStrings.get(uuidMarker);
            this.boardVersion = !biosStrings.containsKey(versionMarker) || Util.isBlank(biosStrings.get(versionMarker))
                    ? Constants.UNKNOWN
                    : biosStrings.get(versionMarker);
            if ("1".equals(biosStrings.get("smbTypeId"))) {
                this.manufacturer = !biosStrings.containsKey(manufacturerMarker)
                        || Util.isBlank(biosStrings.get(manufacturerMarker)) ? Constants.UNKNOWN
                                : biosStrings.get(manufacturerMarker);
                this.model = !biosStrings.containsKey(productMarker) || Util.isBlank(biosStrings.get(productMarker))
                        ? Constants.UNKNOWN
                        : biosStrings.get(productMarker);
                this.serialNumber = !biosStrings.containsKey(serialNumMarker)
                        || Util.isBlank(biosStrings.get(serialNumMarker)) ? Constants.UNKNOWN
                                : biosStrings.get(serialNumMarker);
                this.boardManufacturer = Constants.UNKNOWN;
                this.boardModel = Constants.UNKNOWN;
                this.boardSerialNumber = Constants.UNKNOWN;
            } else if (biosStrings.get("smbTypeId") == "2") {
                this.manufacturer = Constants.UNKNOWN;
                this.model = Constants.UNKNOWN;
                this.serialNumber = Constants.UNKNOWN;
                this.boardManufacturer = !biosStrings.containsKey(manufacturerMarker)
                        || Util.isBlank(biosStrings.get(manufacturerMarker)) ? Constants.UNKNOWN
                                : biosStrings.get(manufacturerMarker);
                this.boardModel = !biosStrings.containsKey(productMarker)
                        || Util.isBlank(biosStrings.get(productMarker)) ? Constants.UNKNOWN
                                : biosStrings.get(productMarker);
                this.boardSerialNumber = !biosStrings.containsKey(serialNumber)
                        || Util.isBlank(biosStrings.get(serialNumber)) ? Constants.UNKNOWN
                                : biosStrings.get(serialNumber);
            } else {
                this.manufacturer = Constants.UNKNOWN;
                this.model = Constants.UNKNOWN;
                this.serialNumber = Constants.UNKNOWN;
                this.boardManufacturer = Constants.UNKNOWN;
                this.boardModel = Constants.UNKNOWN;
                this.boardSerialNumber = Constants.UNKNOWN;
            }
        }
    }
}
