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
import java.util.function.Supplier;

import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.linux.ProcUtil;

/**
 * Baseboard data obtained by sysfs
 */
final class LinuxBaseboard extends AbstractBaseboard {

    private final Supplier<String> manufacturer = Memoizer.memoize(this::queryManufacturer);

    private final Supplier<String> model = Memoizer.memoize(this::queryModel);

    private final Supplier<String> version = Memoizer.memoize(this::queryVersion);

    private final Supplier<String> serialNumber = Memoizer.memoize(this::querySerialNumber);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    @Override
    public String getModel() {
        return model.get();
    }

    @Override
    public String getVersion() {
        return version.get();
    }

    @Override
    public String getSerialNumber() {
        return serialNumber.get();
    }

    private String queryManufacturer() {
        String result = null;
        if ((result = queryManufacturerFromSysfs()) == null && (result = queryProcCpu().manufacturer) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryModel() {
        String result = null;
        if ((result = queryModelFromSysfs()) == null && (result = queryProcCpu().model) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryVersion() {
        String result = null;
        if ((result = queryVersionFromSysfs()) == null && (result = queryProcCpu().version) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String querySerialNumber() {
        String result = null;
        if ((result = querySerialFromSysfs()) == null && (result = queryProcCpu().serialNumber) == null) {
            return Constants.UNKNOWN;
        }
        return result;
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

    private String queryManufacturerFromSysfs() {
        final String boardVendor = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_vendor").trim();
        if (!boardVendor.isEmpty()) {
            return boardVendor;
        }
        return null;
    }

    private String queryModelFromSysfs() {
        final String boardName = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_name").trim();
        if (!boardName.isEmpty()) {
            return boardName;
        }
        return null;
    }

    private String queryVersionFromSysfs() {
        final String boardVersion = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_version").trim();
        if (!boardVersion.isEmpty()) {
            return boardVersion;
        }
        return null;
    }

    private String querySerialFromSysfs() {
        final String boardSerial = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_serial").trim();
        if (!boardSerial.isEmpty()) {
            return boardSerial;
        }
        return null;
    }

    private ProcCpuStrings queryProcCpu() {
        String pcManufacturer = null;
        String pcModel = null;
        String pcVersion = null;
        String pcSerialNumber = null;

        List<String> cpuInfo = FileUtil.readFile(ProcUtil.getProcPath() + "/cpuinfo");
        for (String line : cpuInfo) {
            String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
            if (splitLine.length < 2) {
                continue;
            }
            switch (splitLine[0]) {
            case "Hardware":
                pcModel = splitLine[1];
                break;
            case "Revision":
                pcVersion = splitLine[1];
                if (pcVersion.length() > 1) {
                    pcManufacturer = queryBoardManufacturer(pcVersion.charAt(1));
                }
                break;
            case "Serial":
                pcSerialNumber = splitLine[1];
                break;
            default:
                // Do nothing
            }
        }
        if (Util.isBlank(pcManufacturer)) {
            pcManufacturer = Constants.UNKNOWN;
        }
        if (Util.isBlank(pcModel)) {
            pcModel = Constants.UNKNOWN;
        }
        if (Util.isBlank(pcVersion)) {
            pcVersion = Constants.UNKNOWN;
        }
        if (Util.isBlank(pcSerialNumber)) {
            pcSerialNumber = Constants.UNKNOWN;
        }
        return new ProcCpuStrings(pcManufacturer, pcModel, pcVersion, pcSerialNumber);
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

    private static final class ProcCpuStrings {
        private final String manufacturer;
        private final String model;
        private final String version;
        private final String serialNumber;

        private ProcCpuStrings(String manufacturer, String model, String version, String serialNumber) {
            this.manufacturer = manufacturer;
            this.model = model;
            this.version = version;
            this.serialNumber = serialNumber;
        }
    }
}
