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
package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractAssembly;
import oshi.util.FileUtil;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:58
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class LinuxAssembly extends AbstractAssembly {

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information
    private static final String SYSFS_SERIAL_PATH = "/sys/devices/virtual/dmi/id/";

    LinuxAssembly() {

        init();
    }

    private void init() {

//        $ sudo dmidecode -t system
//        # dmidecode 2.12
//        SMBIOS 2.7 present.
//
//        Handle 0x0001, DMI type 1, 27 bytes
//        System Information
//            Manufacturer: Parallels Software International Inc.                                 <-- "manufacturer"
//            Product Name: Parallels Virtual Platform                                            <-- "model"
//            Version: None
//            Serial Number: Parallels-8E A6 8E 66 FF 9F 41 A1 91 26 6B E3 D3 C7 B2 A9            <-- "serialNumber"
//            UUID: 668EA68E-9FFF-A141-9126-6BE3D3C7B2A9
//            Wake-up Type: Power Switch
//            SKU Number: Undefined
//            Family: Parallels VM
//
//        Handle 0x0016, DMI type 32, 20 bytes
//        System Boot Information
//            Status: No errors detected
//
//
//        or fields in sysfs here:
//
//        $ ls /sys/devices/virtual/dmi/id/
//        bios_date        board_vendor       chassis_version  product_version
//        bios_vendor      board_version      modalias         subsystem
//        bios_version     chassis_asset_tag  power            sys_vendor
//        board_asset_tag  chassis_serial     product_name     uevent
//        board_name       chassis_type       product_serial
//        board_serial     chassis_vendor     product_uuid

        final String sysVendor = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "sys_vendor");
        if (sysVendor != null && !sysVendor.trim().isEmpty()) {
            setManufacturer(sysVendor.trim());
        }

        final String productName = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_name");
        final String productVersion = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_version");

        if (productName != null && !productName.trim().isEmpty()) {

            if (productVersion != null
                && !productVersion.trim().isEmpty()
                && !productVersion.trim().equals("None")) {

                setModel(productName.trim() + " (version: " + productVersion.trim() + ")");
            } else {
                setModel(productName.trim());
            }
        } else {
            if (productVersion != null && !productVersion.trim().isEmpty()) {

                setModel(productVersion.trim());
            }
        }

        final String productSerial = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_serial");
        if (productSerial != null && !productSerial.trim().isEmpty()) {
            setSerialNumber(productSerial.trim());
        } else {
            final String boardSerial = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_serial");
            if (boardSerial != null && !boardSerial.trim().isEmpty()) {
                setSerialNumber(boardSerial.trim());
            }
        }
    }
}
