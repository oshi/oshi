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

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ExecutingCommand;

/**
 * Hardware data obtained from smbios
 * 
 * @author widdis [at] gmail [dot] com
 */
final class SolarisComputerSystem extends AbstractComputerSystem {

    SolarisComputerSystem() {
        init();
    }

    private void init() {

        // $ smbios
        // ... <snip> ...
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

        String manufacturer = "";
        final String manufacturerMarker = "Manufacturer:";
        String product = "";
        final String productMarker = "Product:";
        String serialNumber = "";
        final String serialNumMarker = "Serial Number:";

        boolean smbTypeSystem = false;
        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("smbios")) {
            if (smbTypeSystem) {
                if (checkLine.contains("SMB_TYPE_")) {
                    break;
                }
                if (checkLine.contains(manufacturerMarker)) {
                    manufacturer = checkLine.split(manufacturerMarker)[1].trim();
                }
                if (checkLine.contains(productMarker)) {
                    product = checkLine.split(productMarker)[1].trim();
                }
                if (checkLine.contains(serialNumMarker)) {
                    serialNumber = checkLine.split(serialNumMarker)[1].trim();
                }
            } else {
                if (checkLine.contains("SMB_TYPE_SYSTEM")) {
                    smbTypeSystem = true;
                }
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
    }
}
