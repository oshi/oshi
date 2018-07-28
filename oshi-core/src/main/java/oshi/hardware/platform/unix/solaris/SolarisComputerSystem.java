/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.solaris;

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from smbios
 *
 * @author widdis [at] gmail [dot] com
 */
final class SolarisComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    private static final String UNKNOWN = "unknown";

    SolarisComputerSystem() {
        init();
    }

    private void init() {

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

        String vendor = "";
        final String vendorMarker = "Vendor:";
        String biosDate = "";
        final String biosDateMarker = "Release Date:";
        String biosVersion = "";
        final String biosVersionMarker = "VersionString:";

        String manufacturer = "";
        String boardManufacturer = "";
        final String manufacturerMarker = "Manufacturer:";
        String product = "";
        String model = "";
        final String productMarker = "Product:";
        String version = "";
        final String versionMarker = "Version:";
        String serialNumber = "";
        String boardSerialNumber = "";
        final String serialNumMarker = "Serial Number:";

        SolarisFirmware firmware = new SolarisFirmware();
        SolarisBaseboard baseboard = new SolarisBaseboard();

        boolean smbTypeBIOS = false;
        boolean smbTypeSystem = false;
        boolean smbTypeBaseboard = false;
        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("smbios")) {
            // First 3 SMB_TYPE_* options are what we need. After that we quit
            if (checkLine.contains("SMB_TYPE_")) {
                if (checkLine.contains("SMB_TYPE_BIOS")) {
                    smbTypeBIOS = true;
                    smbTypeSystem = false;
                    smbTypeBaseboard = false;
                } else if (checkLine.contains("SMB_TYPE_SYSTEM")) {
                    smbTypeBIOS = false;
                    smbTypeSystem = true;
                    smbTypeBaseboard = false;
                } else if (checkLine.contains("SMB_TYPE_BASEBOARD")) {
                    smbTypeBIOS = false;
                    smbTypeSystem = false;
                    smbTypeBaseboard = true;
                } else {
                    break;
                }
            }

            if (smbTypeBIOS) {
                if (checkLine.contains(vendorMarker)) {
                    vendor = checkLine.split(vendorMarker)[1].trim();
                } else if (checkLine.contains(biosVersionMarker)) {
                    biosVersion = checkLine.split(biosVersionMarker)[1].trim();
                } else if (checkLine.contains(biosDateMarker)) {
                    biosDate = checkLine.split(biosDateMarker)[1].trim();
                }
            } else if (smbTypeSystem) {
                if (checkLine.contains(manufacturerMarker)) {
                    manufacturer = checkLine.split(manufacturerMarker)[1].trim();
                } else if (checkLine.contains(productMarker)) {
                    product = checkLine.split(productMarker)[1].trim();
                } else if (checkLine.contains(serialNumMarker)) {
                    serialNumber = checkLine.split(serialNumMarker)[1].trim();
                }
            } else if (smbTypeBaseboard) {
                if (checkLine.contains(manufacturerMarker)) {
                    boardManufacturer = checkLine.split(manufacturerMarker)[1].trim();
                } else if (checkLine.contains(productMarker)) {
                    model = checkLine.split(productMarker)[1].trim();
                } else if (checkLine.contains(versionMarker)) {
                    version = checkLine.split(versionMarker)[1].trim();
                } else if (checkLine.contains(serialNumMarker)) {
                    boardSerialNumber = checkLine.split(serialNumMarker)[1].trim();
                }
            }
        }

        if (!vendor.isEmpty()) {
            firmware.setManufacturer(vendor);
        }
        if (!biosVersion.isEmpty()) {
            firmware.setVersion(biosVersion);
        }
        if (!biosDate.isEmpty()) {
            try {
                // Date is MM-DD-YYYY, convert to YYYY-MM-DD
                firmware.setReleaseDate(String.format("%s-%s-%s", biosDate.substring(6, 10), biosDate.substring(0, 2),
                        biosDate.substring(3, 5)));
            } catch (StringIndexOutOfBoundsException e) {
                firmware.setReleaseDate(biosDate);
            }
        }

        if (!manufacturer.isEmpty()) {
            setManufacturer(manufacturer);
        }
        if (!product.isEmpty()) {
            setModel(product);
        }
        if (serialNumber.isEmpty()) {
            serialNumber = getSystemSerialNumber();
        }
        setSerialNumber(serialNumber);

        if (!boardManufacturer.isEmpty()) {
            baseboard.setManufacturer(boardManufacturer);
        }
        if (!model.isEmpty()) {
            baseboard.setModel(model);
        }
        if (!version.isEmpty()) {
            baseboard.setVersion(version);
        }
        if (!boardSerialNumber.isEmpty()) {
            baseboard.setSerialNumber(boardSerialNumber);
        }

        setFirmware(firmware);
        setBaseboard(baseboard);
    }

    private String getSystemSerialNumber() {
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
        if (serialNumber.isEmpty()) {
            serialNumber = UNKNOWN;
        }
        return serialNumber;
    }
}
