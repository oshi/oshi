/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import static oshi.util.Memoizer.memoize;
import static oshi.util.platform.linux.ProcPath.CPUINFO;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
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
@Immutable
final class LinuxComputerSystem extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(LinuxComputerSystem::queryManufacturer);

    private final Supplier<String> model = memoize(LinuxComputerSystem::queryModel);

    private final Supplier<String> serialNumber = memoize(LinuxComputerSystem::querySerialNumber);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    @Override
    public String getModel() {
        return model.get();
    }

    @Override
    public String getSerialNumber() {
        return serialNumber.get();
    }

    @Override
    public Firmware createFirmware() {
        return new LinuxFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new LinuxBaseboard();
    }

    private static String queryManufacturer() {
        String result = null;
        if ((result = queryManufacturerFromSysfs()) == null && (result = queryManufacturerFromProcCpu()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static String queryModel() {
        String result = null;
        if ((result = queryModelFromSysfs()) == null && (result = queryModelFromDeviceTree()) == null
                && (result = queryModelFromLshw()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static String querySerialNumber() {
        String result = null;
        if ((result = querySerialFromSysfs()) == null && (result = querySerialFromDmiDecode()) == null
                && (result = querySerialFromLshal()) == null && (result = querySerialFromLshw()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static String queryManufacturerFromSysfs() {
        final String sysVendor = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "sys_vendor").trim();
        if (!sysVendor.isEmpty()) {
            return sysVendor;
        }
        return null;
    }

    private static String queryManufacturerFromProcCpu() {
        List<String> cpuInfo = FileUtil.readFile(CPUINFO);
        for (String line : cpuInfo) {
            if (line.startsWith("CPU implementer")) {
                int part = ParseUtil.parseLastInt(line, 0);
                switch (part) {
                case 0x41:
                    return "ARM";
                case 0x42:
                    return "Broadcom";
                case 0x43:
                    return "Cavium";
                case 0x44:
                    return "DEC";
                case 0x4e:
                    return "Nvidia";
                case 0x50:
                    return "APM";
                case 0x51:
                    return "Qualcomm";
                case 0x53:
                    return "Samsung";
                case 0x56:
                    return "Marvell";
                case 0x66:
                    return "Faraday";
                case 0x69:
                    return "Intel";
                default:
                    return null;
                }
            }
        }
        return null;
    }

    private static String queryModelFromSysfs() {
        final String productName = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_name").trim();
        final String productVersion = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_version")
                .trim();
        if (productName.isEmpty()) {
            if (!productVersion.isEmpty()) {
                return productVersion;
            }
        } else {
            if (!productVersion.isEmpty() && !"None".equals(productVersion)) {
                return productName + " (version: " + productVersion + ")";
            } else {
                return productName;
            }
        }
        return null;
    }

    private static String queryModelFromDeviceTree() {
        String modelStr = FileUtil.getStringFromFile("/sys/firmware/devicetree/base/model");
        if (!modelStr.isEmpty()) {
            return modelStr.replace("Machine: ", "");
        }
        return null;
    }

    private static String queryModelFromLshw() {
        String modelMarker = "product:";
        for (String checkLine : ExecutingCommand.runNative("lshw -C system")) {
            if (checkLine.contains(modelMarker)) {
                return checkLine.split(modelMarker)[1].trim();
            }
        }
        return null;
    }

    private static String querySerialFromSysfs() {
        // These sysfs files accessible by root, or can be chmod'd at boot time
        // to enable access without root
        String serial = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_serial");
        if (serial.isEmpty() || "None".equals(serial)) {
            serial = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_serial");
            if (serial.isEmpty() || "None".equals(serial)) {
                return null;
            }
            return serial;
        }
        return null;
    }

    private static String querySerialFromDmiDecode() {
        // If root privileges this will work
        String marker = "Serial Number:";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        return null;
    }

    private static String querySerialFromLshal() {
        // if lshal command available (HAL deprecated in newer linuxes)
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return null;
    }

    private static String querySerialFromLshw() {
        String serialMarker = "serial:";
        for (String checkLine : ExecutingCommand.runNative("lshw -C system")) {
            if (checkLine.contains(serialMarker)) {
                return checkLine.split(serialMarker)[1].trim();
            }
        }
        return null;
    }
}
