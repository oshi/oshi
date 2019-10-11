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
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.jna.platform.mac.IOKit.IORegistryEntry;
import oshi.jna.platform.mac.IOKitUtil;
import oshi.util.Constants;
import oshi.util.Util;

/**
 * Hardware data obtained from ioreg.
 */
final class MacComputerSystem extends AbstractComputerSystem {

    private final Supplier<ManufacturerModelSerial> profileSystem = memoize(this::platformExpert);

    @Override
    public String getManufacturer() {
        return profileSystem.get().manufacturer;
    }

    @Override
    public String getModel() {
        return profileSystem.get().model;
    }

    @Override
    public String getSerialNumber() {
        return profileSystem.get().serialNumber;
    }

    @Override
    public Firmware createFirmware() {
        return new MacFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new MacBaseboard();
    }

    private ManufacturerModelSerial platformExpert() {
        String manufacturer = null;
        String model = null;
        String serialNumber = null;
        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = new String(data);
            }
            data = platformExpert.getByteArrayProperty("model");
            if (data != null) {
                model = new String(data);
            }
            serialNumber = platformExpert.getStringProperty("IOPlatformSerialNumber");
            platformExpert.release();
        }
        return new ManufacturerModelSerial(manufacturer, model, serialNumber);
    }

    private static final class ManufacturerModelSerial {
        private final String manufacturer;
        private final String model;
        private final String serialNumber;

        private ManufacturerModelSerial(String manufacturer, String model, String serialNumber) {
            this.manufacturer = Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
            this.model = Util.isBlank(model) ? Constants.UNKNOWN : model;
            this.serialNumber = Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber;
        }
    }
}
