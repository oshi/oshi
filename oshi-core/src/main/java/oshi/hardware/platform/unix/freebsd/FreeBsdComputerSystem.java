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
package oshi.hardware.platform.unix.freebsd;

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from dmidecode
 *
 * @author widdis [at] gmail [dot] com
 */
final class FreeBsdComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    FreeBsdComputerSystem() {
        init();
    }

    private void init() {

        // $ sudo dmidecode -t system
        // # dmidecode 3.0
        // Scanning /dev/mem for entry point.
        // SMBIOS 2.7 present.
        //
        // Handle 0x0001, DMI type 1, 27 bytes
        // System Information
        // Manufacturer: Parallels Software International Inc.
        // Product Name: Parallels Virtual Platform
        // Version: None
        // Serial Number: Parallels-47 EC 38 2A 33 1B 4C 75 94 0F F7 AF 86 63 C0
        // C4
        // UUID: 2A38EC47-1B33-854C-940F-F7AF8663C0C4
        // Wake-up Type: Power Switch
        // SKU Number: Undefined
        // Family: Parallels VM
        //
        // Handle 0x0016, DMI type 32, 20 bytes
        // System Boot Information
        // Status: No errors detected

        String manufacturer = "";
        final String manufacturerMarker = "Manufacturer:";
        String productName = "";
        final String productNameMarker = "Product Name:";
        String serialNumber = "";
        final String serialNumMarker = "Serial Number:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = checkLine.split(manufacturerMarker)[1].trim();
            }
            if (checkLine.contains(productNameMarker)) {
                productName = checkLine.split(productNameMarker)[1].trim();
            }
            if (checkLine.contains(serialNumMarker)) {
                serialNumber = checkLine.split(serialNumMarker)[1].trim();
            }
        }
        if (!manufacturer.isEmpty()) {
            setManufacturer(manufacturer);
        }
        if (!productName.isEmpty()) {
            setModel(productName);
        }
        if (serialNumber.isEmpty()) {
            serialNumber = getSystemSerialNumber();
        }
        setSerialNumber(serialNumber);

        setFirmware(new FreeBsdFirmware());

        setBaseboard(new FreeBsdBaseboard());
    }

    private String getSystemSerialNumber() {
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return "unknown";
    }
}
