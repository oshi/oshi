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

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from sysfs.
 */
final class LinuxComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null) {
            final String sysVendor = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "sys_vendor").trim();
            if (!sysVendor.isEmpty()) {
                this.manufacturer = sysVendor;
            }
        }
        return super.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        if (this.model == null) {
            final String productName = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_name").trim();
            final String productVersion = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_version")
                    .trim();
            if (productName.isEmpty()) {
                if (!productVersion.isEmpty()) {
                    this.model = productVersion;
                }
            } else {
                if (!productVersion.isEmpty() && !"None".equals(productVersion)) {
                    this.model = productName + " (version: " + productVersion + ")";
                } else {
                    this.model = productName;
                }
            }
        }
        return super.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null) {
            querySystemSerialNumber();
        }
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Firmware getFirmware() {
        if (this.firmware == null) {
            this.firmware = new LinuxFirmware();
        }
        return this.firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Baseboard getBaseboard() {
        if (this.baseboard == null) {
            this.baseboard = new LinuxBaseboard();
        }
        return this.baseboard;
    }

    private void querySystemSerialNumber() {
        if (!querySerialFromSysfs() && !querySerialFromDmiDecode() && !querySerialFromLshal()) {
            this.serialNumber = Constants.UNKNOWN;
        }
    }

    private boolean querySerialFromSysfs() {
        // These sysfs files accessible by root, or can be chmod'd at boot time
        // to enable access without root
        String serialNumber = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_serial");
        if (serialNumber.isEmpty() || "None".equals(serialNumber)) {
            serialNumber = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_serial");
            if (serialNumber.isEmpty() || "None".equals(serialNumber)) {
                return false;
            }
            this.serialNumber = serialNumber;
        }
        return this.serialNumber != null && !this.serialNumber.isEmpty();
    }

    private boolean querySerialFromDmiDecode() {
        // If root privileges this will work
        String marker = "Serial Number:";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(marker)) {
                this.serialNumber = checkLine.split(marker)[1].trim();
                break;
            }
        }
        return this.serialNumber != null && !this.serialNumber.isEmpty();
    }

    private boolean querySerialFromLshal() {
        // if lshal command available (HAL deprecated in newer linuxes)
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                this.serialNumber = ParseUtil.getSingleQuoteStringValue(checkLine);
                break;
            }
        }
        return this.serialNumber != null && !this.serialNumber.isEmpty();
    }
}
