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
package oshi.hardware.platform.unix.solaris;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from smbios.
 */
final class SolarisComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null) {
            readSmbios();
        }
        return this.manufacturer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        if (this.model == null) {
            readSmbios();
        }
        return this.model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null) {
            this.serialNumber = getSystemSerialNumber();
        }
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Firmware getFirmware() {
        if (this.firmware == null) {
            this.firmware = new SolarisFirmware();
        }
        return this.firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Baseboard getBaseboard() {
        if (this.baseboard == null) {
            this.baseboard = new SolarisBaseboard();
        }
        return this.baseboard;
    }

    private void readSmbios() {
        // $ smbios
        // ID SIZE TYPE
        // 0 87 SMB_TYPE_BIOS (BIOS Information)
        //
        // Vendor: Parallels Software International Inc.
        // Version String: 11.2.1 (32686)
        // Release Date: 07/15/2016
        // Address Segment: 0xf000
        // ... <snip> ...
        //
        // ID SIZE TYPE
        // 1 177 SMB_TYPE_SYSTEM (system information)
        //
        // Manufacturer: Parallels Software International Inc.
        // Product: Parallels Virtual Platforom
        // Version: None
        // Serial Number: Parallels-45 2E 7E 2D 57 5C 4B 59 B1 30 28 81 B7 81 89
        // 34
        //
        // UUID: 452e7e2d-575c04b59-b130-2881b7818934
        // Wake-up Event: 0x6 (Power Switch)
        // SKU Number: Undefined
        // Family: Parallels VM
        //
        // ID SIZE TYPE
        // 2 90 SMB_TYPE_BASEBOARD (base board)
        //
        // Manufacturer: Parallels Software International Inc.
        // Product: Parallels Virtual Platform
        // Version: None
        // Serial Number: None
        // ... <snip> ...
        //
        // ID SIZE TYPE
        // 3 .... <snip> ...

        int smbTypeId = -1;
        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("smbios")) {
            if (checkLine.contains("SMB_TYPE_")) {
                if (checkLine.contains("SMB_TYPE_BIOS")) {
                    smbTypeId = 0; // BIOS
                } else if (checkLine.contains("SMB_TYPE_SYSTEM")) {
                    smbTypeId = 1; // SYSTEM
                } else if (checkLine.contains("SMB_TYPE_BASEBOARD")) {
                    smbTypeId = 2; // BASEBOARD
                } else {
                    // First 3 SMB_TYPE_* options are what we need. After that
                    // no need to continue processing
                    break;
                }
            }
            switch (smbTypeId) {
            case 0: // BIOS
                setFirmwareAttributes(checkLine);
                break;
            case 1: // SYSTEM
                setComputerSystemAttributes(checkLine);
                break;
            case 2: // BASEBOARD
                setBaseboardAttributes(checkLine);
                break;
            default:
                // Do nothing; continue loop
                break;
            }
        }
    }

    private void setComputerSystemAttributes(String checkLine) {
        final String manufacturerMarker = "Manufacturer:";
        final String productMarker = "Product:";
        final String serialNumMarker = "Serial Number:";

        if (checkLine.contains(manufacturerMarker)) {
            String manufacturer = checkLine.split(manufacturerMarker)[1].trim();
            if (!manufacturer.isEmpty()) {
                this.manufacturer = manufacturer;
            }
        } else if (checkLine.contains(productMarker)) {
            String product = checkLine.split(productMarker)[1].trim();
            if (!product.isEmpty()) {
                this.model = product;
            }
        } else if (checkLine.contains(serialNumMarker)) {
            serialNumber = checkLine.split(serialNumMarker)[1].trim();
        }
    }

    private void setFirmwareAttributes(String checkLine) {
        final String vendorMarker = "Vendor:";
        final String biosDateMarker = "Release Date:";
        final String biosVersionMarker = "VersionString:";

        if (checkLine.contains(vendorMarker)) {
            String vendor = checkLine.split(vendorMarker)[1].trim();
            if (!vendor.isEmpty()) {
                ((SolarisFirmware) getFirmware()).setManufacturer(vendor);
            }
        } else if (checkLine.contains(biosVersionMarker)) {
            String biosVersion = checkLine.split(biosVersionMarker)[1].trim();
            if (!biosVersion.isEmpty()) {
                ((SolarisFirmware) getFirmware()).setVersion(biosVersion);
            }
        } else if (checkLine.contains(biosDateMarker)) {
            String biosDate = checkLine.split(biosDateMarker)[1].trim();
            if (!biosDate.isEmpty()) {
                ((SolarisFirmware) getFirmware()).setReleaseDate(ParseUtil.parseMmDdYyyyToYyyyMmDD(biosDate));
            }
        }
    }

    private void setBaseboardAttributes(String checkLine) {
        final String manufacturerMarker = "Manufacturer:";
        final String productMarker = "Product:";
        final String versionMarker = "Version:";
        final String serialNumMarker = "Serial Number:";

        if (checkLine.contains(manufacturerMarker)) {
            String boardManufacturer = checkLine.split(manufacturerMarker)[1].trim();
            if (!boardManufacturer.isEmpty()) {
                ((SolarisBaseboard) getBaseboard()).setManufacturer(boardManufacturer);
            }
        } else if (checkLine.contains(productMarker)) {
            String product = checkLine.split(productMarker)[1].trim();
            if (!product.isEmpty()) {
                ((SolarisBaseboard) getBaseboard()).setModel(product);
            }
        } else if (checkLine.contains(versionMarker)) {
            String version = checkLine.split(versionMarker)[1].trim();
            if (!version.isEmpty()) {
                ((SolarisBaseboard) getBaseboard()).setVersion(version);
            }
        } else if (checkLine.contains(serialNumMarker)) {
            String boardSerialNumber = checkLine.split(serialNumMarker)[1].trim();
            if (!boardSerialNumber.isEmpty()) {
                ((SolarisBaseboard) getBaseboard()).setSerialNumber(boardSerialNumber);
            }
        }
    }

    private String getSystemSerialNumber() {
        // If they've installed STB (Sun Explorer) this should work
        String serialNumber = ExecutingCommand.getFirstAnswer("sneep");
        // if that didn't work, try...
        if (serialNumber.isEmpty()) {
            String marker = "chassis-sn:";
            for (String checkLine : ExecutingCommand.runNative("prtconf -pv")) {
                if (checkLine.contains(marker)) {
                    serialNumber = ParseUtil.getSingleQuoteStringValue(checkLine);
                    break;
                }
            }
        }
        if (serialNumber.isEmpty()) {
            serialNumber = Constants.UNKNOWN;
        }
        return serialNumber;
    }
}
