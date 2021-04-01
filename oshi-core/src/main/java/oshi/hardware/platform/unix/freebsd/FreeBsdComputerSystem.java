/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.unix.freebsd;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Quintet;

/**
 * Hardware data obtained from dmidecode.
 */
@Immutable
final class FreeBsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<Quintet<String, String, String, String, String>> manufModelSerialUuidVers = memoize(
            FreeBsdComputerSystem::readDmiDecode);

    @Override
    public String getManufacturer() {
        return manufModelSerialUuidVers.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelSerialUuidVers.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return manufModelSerialUuidVers.get().getC();
    }

    @Override
    public String getHardwareUUID() {
        return manufModelSerialUuidVers.get().getD();
    }

    @Override
    public Firmware createFirmware() {
        return new FreeBsdFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new UnixBaseboard(manufModelSerialUuidVers.get().getA(), manufModelSerialUuidVers.get().getB(),
                manufModelSerialUuidVers.get().getC(), manufModelSerialUuidVers.get().getE());
    }

    private static Quintet<String, String, String, String, String> readDmiDecode() {
        String manufacturer = null;
        String model = null;
        String serialNumber = null;
        String uuid = null;
        String version = null;

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
        final String uuidMarker = "UUID:";
        final String versionMarker = "Version:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = checkLine.split(manufacturerMarker)[1].trim();
            } else if (checkLine.contains(productNameMarker)) {
                model = checkLine.split(productNameMarker)[1].trim();
            } else if (checkLine.contains(serialNumMarker)) {
                serialNumber = checkLine.split(serialNumMarker)[1].trim();
            } else if (checkLine.contains(uuidMarker)) {
                uuid = checkLine.split(uuidMarker)[1].trim();
            } else if (checkLine.contains(versionMarker)) {
                version = checkLine.split(versionMarker)[1].trim();
            }
        }
        // If we get to end and haven't assigned, use fallback
        if (Util.isBlank(serialNumber)) {
            serialNumber = querySystemSerialNumber();
        }
        if (Util.isBlank(uuid)) {
            uuid = BsdSysctlUtil.sysctl("kern.hostuuid", Constants.UNKNOWN);
        }
        return new Quintet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model,
                Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber,
                Util.isBlank(uuid) ? Constants.UNKNOWN : uuid, Util.isBlank(version) ? Constants.UNKNOWN : version);
    }

    private static String querySystemSerialNumber() {
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return Constants.UNKNOWN;
    }
}
