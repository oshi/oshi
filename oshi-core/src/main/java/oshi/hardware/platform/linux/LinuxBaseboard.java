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

import oshi.hardware.common.AbstractBaseboard;
import oshi.util.FileUtil;

/**
 * Baseboard data obtained by sysfs
 *
 * @author widdis [at] gmail [dot] com
 */
final class LinuxBaseboard extends AbstractBaseboard {

    private static final long serialVersionUID = 1L;

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information
    private static final String SYSFS_SERIAL_PATH = "/sys/devices/virtual/dmi/id/";

    LinuxBaseboard() {
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

        final String boardVendor = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_vendor");
        if (boardVendor != null && !boardVendor.trim().isEmpty()) {
            setManufacturer(boardVendor.trim());
        }

        final String boardName = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_name");
        if (boardName != null && !boardName.trim().isEmpty()) {
            setModel(boardName.trim());
        }

        final String boardVersion = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_version");
        if (boardVersion != null && !boardVersion.trim().isEmpty()) {
            setVersion(boardVersion.trim());
        }

        final String boardSerialNumber = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "board_serial");
        if (boardSerialNumber != null && !boardSerialNumber.trim().isEmpty()) {
            setSerialNumber(boardSerialNumber.trim());
        }
    }
}
