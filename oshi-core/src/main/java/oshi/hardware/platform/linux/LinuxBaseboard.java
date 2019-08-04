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

import java.util.List;

import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;

/**
 * Baseboard data obtained by sysfs
 */
final class LinuxBaseboard extends AbstractBaseboard {

    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null && !queryManufacturerFromSysfs() && !queryProcCpuinfo()) {
            this.manufacturer = Constants.UNKNOWN;
        }
        return super.getManufacturer();
    }

    /** {@inheritDoc} */
    @Override
    public String getModel() {
        if (this.model == null && !queryModelFromSysfs() && !queryProcCpuinfo()) {
            this.model = Constants.UNKNOWN;
        }
        return super.getModel();
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        if (this.version == null && !queryVersionFromSysfs() && !queryProcCpuinfo()) {
            this.version = Constants.UNKNOWN;
        }
        return super.getVersion();
    }

    /** {@inheritDoc} */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null && !querySerialNumberFromSysfs() && !queryProcCpuinfo()) {
            this.serialNumber = Constants.UNKNOWN;
        }
        return super.getSerialNumber();
    }

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information

    // $ ls /sys/devices/virtual/dmi/id/
    // bios_date board_vendor chassis_version product_version
    // bios_vendor board_version modalias subsystem
    // bios_version chassis_asset_tag power sys_vendor
    // board_asset_tag chassis_serial product_name uevent
    // board_name chassis_type product_serial
    // board_serial chassis_vendor product_uuid

    private boolean queryManufacturerFromSysfs() {
        final String boardVendor = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_vendor").trim();
        if (!boardVendor.isEmpty()) {
            this.manufacturer = boardVendor;
            return true;
        }
        return false;

    }

    private boolean queryModelFromSysfs() {
        final String boardName = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_name").trim();
        if (!boardName.isEmpty()) {
            this.model = boardName;
            return true;
        }
        return false;

    }

    private boolean queryVersionFromSysfs() {
        final String boardVersion = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_version").trim();
        if (!boardVersion.isEmpty()) {
            this.version = boardVersion;
            return true;
        }
        return false;

    }

    private boolean querySerialNumberFromSysfs() {
        final String boardSerialNumber = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_serial")
                .trim();
        if (!boardSerialNumber.isEmpty()) {
            this.serialNumber = boardSerialNumber;
            return true;
        }
        return false;

    }

    private boolean queryProcCpuinfo() {
        List<String> cpuInfo = FileUtil.readFile(ProcUtil.getProcPath() + "/cpuinfo");
        for (String line : cpuInfo) {
            String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
            if (splitLine.length < 2) {
                continue;
            }
            switch (splitLine[0]) {
            case "Hardware":
                this.model = splitLine[1];
                break;
            case "Revision":
                this.version = splitLine[1];
                if (this.version.length() > 1) {
                    this.manufacturer = queryBoardManufacturer(this.version.charAt(1));
                }
                break;
            case "Serial":
                this.serialNumber = splitLine[1];
                break;
            default:
                // Do nothing
            }
            if (this.model != null && this.version != null && this.serialNumber != null) {
                return true;
            }
        }
        return false;
    }

    private String queryBoardManufacturer(char digit) {
        switch (digit) {
        case '0':
            return "Sony UK";
        case '1':
            return "Egoman";
        case '2':
            return "Embest";
        case '3':
            return "Sony Japan";
        case '4':
            return "Embest";
        case '5':
            return "Stadium";
        default:
            return Constants.UNKNOWN;
        }
    }
}
