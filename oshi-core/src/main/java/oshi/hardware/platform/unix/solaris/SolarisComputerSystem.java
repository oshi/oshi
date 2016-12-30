/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.solaris;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ExecutingCommand;

/**
 * Hardware data obtained from smbios
 * 
 * @author widdis [at] gmail [dot] com
 */
final class SolarisComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SolarisComputerSystem.class);

    // TODO: Is release really not language-dependent?
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);

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
                final Date result = DATE_FORMAT.parse(biosDate.trim());
                if (result != null) {
                    firmware.setReleaseDate(result);
                }
            } catch (final ParseException e) {
                LOG.warn("could not parse date string: " + biosDate, e);
            }
        }

        if (!manufacturer.isEmpty()) {
            setManufacturer(manufacturer);
        }
        if (!product.isEmpty()) {
            setModel(product);
        }
        if (!serialNumber.isEmpty()) {
            setSerialNumber(serialNumber);
        }

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
}
