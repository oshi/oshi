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
import oshi.util.Constants;
import oshi.util.FileUtil;

/**
 * Baseboard data obtained by sysfs
 */
final class LinuxBaseboard extends AbstractBaseboard {

    private static final long serialVersionUID = 1L;

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information

    // $ ls /sys/devices/virtual/dmi/id/
    // bios_date board_vendor chassis_version product_version
    // bios_vendor board_version modalias subsystem
    // bios_version chassis_asset_tag power sys_vendor
    // board_asset_tag chassis_serial product_name uevent
    // board_name chassis_type product_serial
    // board_serial chassis_vendor product_uuid

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null) {
            final String boardVendor = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_vendor").trim();
            this.manufacturer = (boardVendor.isEmpty()) ? Constants.UNKNOWN : boardVendor;
        }
        return this.manufacturer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        if (this.model == null) {
            final String boardName = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_name").trim();
            this.model = (boardName.isEmpty()) ? Constants.UNKNOWN : boardName;
        }
        return this.model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        if (this.version == null) {
            final String boardVersion = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_version")
                    .trim();
            this.version = (boardVersion.isEmpty()) ? Constants.UNKNOWN : boardVersion;
        }
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null) {
            final String boardSerialNumber = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_serial")
                    .trim();
            this.serialNumber = (boardSerialNumber.isEmpty()) ? Constants.UNKNOWN : boardSerialNumber;
        }
        return this.serialNumber;
    }
}
