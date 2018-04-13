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
package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from sysfs
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
final class LinuxComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information
    private static final String SYSFS_SERIAL_PATH = "/sys/devices/virtual/dmi/id/";
    private static final String UNKNOWN = "unknown";

    LinuxComputerSystem() {
        init();
    }

    private void init() {

        // $ ls /sys/devices/virtual/dmi/id/
        // bios_date board_vendor chassis_version product_version
        // bios_vendor board_version modalias subsystem
        // bios_version chassis_asset_tag power sys_vendor
        // board_asset_tag chassis_serial product_name uevent
        // board_name chassis_type product_serial
        // board_serial chassis_vendor product_uuid

        final String sysVendor = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "sys_vendor");
        if (sysVendor != null && !sysVendor.trim().isEmpty()) {
            setManufacturer(sysVendor.trim());
        }

        final String productName = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_name");
        final String productVersion = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_version");

        if (productName != null && !productName.trim().isEmpty()) {

            if (productVersion != null && !productVersion.trim().isEmpty() && !"None".equals(productVersion.trim())) {

                setModel(productName.trim() + " (version: " + productVersion.trim() + ")");
            } else {
                setModel(productName.trim());
            }
        } else {
            if (productVersion != null && !productVersion.trim().isEmpty()) {

                setModel(productVersion.trim());
            }
        }

        setSerialNumber(getSystemSerialNumber());

        setFirmware(new LinuxFirmware());

        setBaseboard(new LinuxBaseboard());
    }

    private String getSystemSerialNumber() {
        // These sysfs files accessible by root, or can be chmod'd at boot time
        // to enable access without root
        String serialNumber = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_serial");
        if (serialNumber.isEmpty() || "None".equals(serialNumber)) {
            serialNumber = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_serial");
            if (serialNumber.isEmpty() || "None".equals(serialNumber)) {
                serialNumber = UNKNOWN;
            }
        }
        // If root privileges this will work
        String marker = "Serial Number:";
        if (UNKNOWN.equals(serialNumber)) {
            for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
                if (checkLine.contains(marker)) {
                    serialNumber = checkLine.split(marker)[1].trim();
                    break;
                }
            }
        }
        // if lshal command available (HAL deprecated in newer linuxes)
        if (UNKNOWN.equals(serialNumber)) {
            marker = "system.hardware.serial =";
            for (String checkLine : ExecutingCommand.runNative("lshal")) {
                if (checkLine.contains(marker)) {
                    serialNumber = ParseUtil.getSingleQuoteStringValue(checkLine);
                    break;
                }
            }
        }
        return serialNumber;
    }
}
