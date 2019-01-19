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
package oshi.hardware.platform.unix.freebsd;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from dmidecode.
 */
final class FreeBsdComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null) {
            readDmiDecode();
        }
        return this.manufacturer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        if (this.model == null) {
            readDmiDecode();
        }
        return this.model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null) {
            readDmiDecode();
        }
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Firmware getFirmware() {
        if (this.firmware == null) {
            this.firmware = new FreeBsdFirmware();
        }
        return this.firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Baseboard getBaseboard() {
        if (this.baseboard == null) {
            this.baseboard = new FreeBsdBaseboard();
        }
        return this.baseboard;
    }

    private void readDmiDecode() {

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

        final String manufacturerMarker = "Manufacturer:";
        final String productNameMarker = "Product Name:";
        final String serialNumMarker = "Serial Number:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(manufacturerMarker)) {
                String manufacturer = checkLine.split(manufacturerMarker)[1].trim();
                if (!manufacturer.isEmpty()) {
                    this.manufacturer = manufacturer;
                }
            }
            if (checkLine.contains(productNameMarker)) {
                String productName = checkLine.split(productNameMarker)[1].trim();
                if (!productName.isEmpty()) {
                    this.model = productName;
                }
            }
            if (checkLine.contains(serialNumMarker)) {
                String serialNumber = checkLine.split(serialNumMarker)[1].trim();
                this.serialNumber = serialNumber;
            }
        }

        if (this.serialNumber == null || serialNumber.isEmpty()) {
            this.serialNumber = getSystemSerialNumber();
        }
    }

    private String getSystemSerialNumber() {
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return Constants.UNKNOWN;
    }
}
