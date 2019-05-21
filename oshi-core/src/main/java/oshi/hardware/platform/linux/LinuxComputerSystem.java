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

import oshi.SystemInfo;
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
                serialNumber = SystemInfo.UNKNOWN;
            }
        }
        // If root privileges this will work
        String marker = "Serial Number:";
        if (SystemInfo.UNKNOWN.equals(serialNumber)) {
            for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
                if (checkLine.contains(marker)) {
                    serialNumber = checkLine.split(marker)[1].trim();
                    break;
                }
            }
        }
        // if lshal command available (HAL deprecated in newer linuxes)
        if (SystemInfo.UNKNOWN.equals(serialNumber)) {
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
