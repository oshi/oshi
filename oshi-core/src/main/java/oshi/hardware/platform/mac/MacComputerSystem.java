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
import java.util.regex.Pattern;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.jna.platform.mac.IOKit;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.Util;
import oshi.util.platform.mac.IOKitUtil;

/**
 * Hardware data obtained by system_profiler.
 */
final class MacComputerSystem extends AbstractComputerSystem {

    private final Supplier<ModelSerialSmcBootrom> profileSystem = memoize(this::profileSystem);

    @Override
    public String getManufacturer() {
        return "Apple Inc.";
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
        return new MacFirmware(getManufacturer(), profileSystem.get().bootRomVersion);
    }

    @Override
    public Baseboard createBaseboard() {
        return new MacBaseboard(getManufacturer(), getSerialNumber(), profileSystem.get().smcVersion);
    }

    private ModelSerialSmcBootrom profileSystem() {
        String model = null;
        String serialNumber = null;
        String smcVersion = null;
        String bootRomVersion = null;
        // $ system_profiler SPHardwareDataType
        // Hardware:
        //
        // Hardware Overview:
        //
        // Model Name: MacBook Pro
        // Model Identifier: MacBookPro8,2
        // Processor Name: Intel Core i7
        // Processor Speed: 2.3 GHz
        // Number of Processors: 1
        // Total Number of Cores: 4
        // L2 Cache (per Core): 256 KB
        // L3 Cache: 8 MB
        // Memory: 16 GB
        // Boot ROM Version: MBP81.0047.B2C
        // SMC Version (system): 1.69f4
        // Serial Number (system): C02FH4XYCB71
        // Hardware UUID: D92CE829-65AD-58FA-9C32-88968791B7BD
        // Sudden Motion Sensor:
        // State: Enabled

        final String modelNameMarker = "Model Name:";
        final String modelIdMarker = "Model Identifier:";
        final String serialNumMarker = "Serial Number (system):";
        final String smcMarker = "SMC Version (system):";
        final String bootRomMarker = "Boot ROM Version:";

        // Name and ID can be used together
        String modelName = "";
        String modelIdentifier = "";
        for (final String checkLine : ExecutingCommand.runNative("system_profiler SPHardwareDataType")) {
            if (checkLine.contains(modelNameMarker)) {
                modelName = checkLine.split(modelNameMarker)[1].trim();
            } else if (checkLine.contains(modelIdMarker)) {
                modelIdentifier = checkLine.split(modelIdMarker)[1].trim();
            } else if (checkLine.contains(serialNumMarker)) {
                serialNumber = checkLine.split(Pattern.quote(serialNumMarker))[1].trim();
            } else if (checkLine.contains(bootRomMarker)) {
                bootRomVersion = checkLine.split(bootRomMarker)[1].trim();
            } else if (checkLine.contains(smcMarker)) {
                smcVersion = checkLine.split(Pattern.quote(smcMarker))[1].trim();
            }
        }
        model = modelNameAndIdentifier(modelName, modelIdentifier);
        if (Util.isBlank(serialNumber)) {
            serialNumber = getIORegistryPlatformSerialNumber();
        }
        return new ModelSerialSmcBootrom(model, serialNumber, smcVersion, bootRomVersion);
    }

    private String modelNameAndIdentifier(String modelName, String modelIdentifier) {
        // Use name (id) if both available; else either one
        if (modelName.isEmpty() && !modelIdentifier.isEmpty()) {
            return modelIdentifier;
        } else {
            if (!modelName.isEmpty() && !modelIdentifier.isEmpty()) {
                return modelName + " (" + modelIdentifier + ")";
            } else {
                return modelName.isEmpty() ? Constants.UNKNOWN : modelName;
            }
        }
    }

    private String getIORegistryPlatformSerialNumber() {
        String serialNumber = null;
        int service = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (service != 0) {
            // Fetch the serial number
            serialNumber = IOKitUtil.getIORegistryStringProperty(service, "IOPlatformSerialNumber");
            IOKit.INSTANCE.IOObjectRelease(service);
        }
        return serialNumber;
    }

    private static final class ModelSerialSmcBootrom {
        private final String model;
        private final String serialNumber;
        private final String smcVersion;
        private final String bootRomVersion;

        private ModelSerialSmcBootrom(String model, String serialNumber, String smcVersion, String bootRomVersion) {
            this.model = Util.isBlank(model) ? Constants.UNKNOWN : model;
            this.serialNumber = Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber;
            this.smcVersion = Util.isBlank(smcVersion) ? Constants.UNKNOWN : smcVersion;
            this.bootRomVersion = Util.isBlank(bootRomVersion) ? Constants.UNKNOWN : bootRomVersion;
        }
    }
}
