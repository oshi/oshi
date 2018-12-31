/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
