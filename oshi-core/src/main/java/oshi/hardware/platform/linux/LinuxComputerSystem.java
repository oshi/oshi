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

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.FileUtil;

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

        final String productSerial = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "product_serial");
        if (productSerial != null && !productSerial.trim().isEmpty()) {
            setSerialNumber(productSerial.trim());
        } else {
            final String boardSerial = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_serial");
            if (boardSerial != null && !boardSerial.trim().isEmpty()) {
                setSerialNumber(boardSerial.trim());
            }
        }

        setFirmware(new LinuxFirmware());

        setBaseboard(new LinuxBaseboard());
    }
}
