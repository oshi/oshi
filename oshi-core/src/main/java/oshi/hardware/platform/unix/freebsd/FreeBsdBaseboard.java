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

import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;

/**
 * Baseboard data obtained by dmidecode
 */
final class FreeBsdBaseboard extends AbstractBaseboard {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null) {
            readDmiDecode();
        }
        return super.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        if (this.model == null) {
            readDmiDecode();
        }
        return super.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        if (this.version == null) {
            readDmiDecode();
        }
        return super.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null) {
            readDmiDecode();
        }
        return super.getSerialNumber();
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
        final String versionMarker = "Version:";
        final String serialNumMarker = "Serial Number:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t baseboard")) {
            if (checkLine.contains(manufacturerMarker)) {
                String manufacturer = checkLine.split(manufacturerMarker)[1].trim();
                this.manufacturer = manufacturer.isEmpty() ? Constants.UNKNOWN : manufacturer;
            }
            if (checkLine.contains(productNameMarker)) {
                String model = checkLine.split(productNameMarker)[1].trim();
                this.model = model.isEmpty() ? Constants.UNKNOWN : model;
            }
            if (checkLine.contains(versionMarker)) {
                String version = checkLine.split(versionMarker)[1].trim();
                this.version = version.isEmpty() ? Constants.UNKNOWN : version;
            }
            if (checkLine.contains(serialNumMarker)) {
                String serialNumber = checkLine.split(serialNumMarker)[1].trim();
                this.serialNumber = serialNumber.isEmpty() ? Constants.UNKNOWN : serialNumber;
            }
        }
    }
}
