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

import java.util.regex.Pattern;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.jna.platform.mac.IOKit;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.platform.mac.IOKitUtil;

/**
 * Hardware data obtained by system_profiler.
 */
final class MacComputerSystem extends AbstractComputerSystem {

    private volatile String manufacturer;

    private volatile String model;

    private volatile String serialNumber;

    /** {@inheritDoc} */
    @Override
    public String getManufacturer() {
        String localRef = this.manufacturer;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.manufacturer;
                if (localRef == null) {
                    this.manufacturer = localRef = "Apple Inc.";
                }
            }
        }
        return localRef;
    }

    /** {@inheritDoc} */
    @Override
    public String getModel() {
        String localRef = this.model;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.model;
                if (localRef == null) {
                    profileSystem();
                    localRef = this.model;
                }
            }
        }
        return localRef;
    }

    /** {@inheritDoc} */
    @Override
    public String getSerialNumber() {
        String localRef = this.serialNumber;
        if (localRef == null) {
            synchronized (this) {
                localRef = this.serialNumber;
                if (localRef == null) {
                    profileSystem();
                    localRef = this.serialNumber;
                }
            }
        }
        return localRef;
    }

    /** {@inheritDoc} */
    @Override
    public Firmware createFirmware() {
        MacFirmware firmware = new MacFirmware();
        firmware.setManufacturer(getManufacturer());
        firmware.setName("EFI");
        return firmware;
    }

    /** {@inheritDoc} */
    @Override
    public Baseboard createBaseboard() {
        MacBaseboard baseboard = new MacBaseboard();
        baseboard.setManufacturer(getManufacturer());
        baseboard.setModel("SMC");
        return baseboard;
    }

    private void profileSystem() {
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

        // Name and ID can be used together
        String modelName = "";
        String modelIdentifier = "";
        for (final String checkLine : ExecutingCommand.runNative("system_profiler SPHardwareDataType")) {
            if (checkLine.contains(modelNameMarker)) {
                modelName = checkLine.split(modelNameMarker)[1].trim();
            } else if (checkLine.contains(modelIdMarker)) {
                modelIdentifier = checkLine.split(modelIdMarker)[1].trim();
            } else {
                setVersionAndSerialNumber(checkLine);
            }
        }
        setModelNameAndIdentifier(modelName, modelIdentifier);
    }

    private void setVersionAndSerialNumber(String checkLine) {
        final String serialNumMarker = "Serial Number (system):";
        final String smcMarker = "SMC Version (system):";
        final String bootRomMarker = "Boot ROM Version:";

        if (checkLine.contains(bootRomMarker)) {
            String bootRomVersion = checkLine.split(bootRomMarker)[1].trim();
            if (!bootRomVersion.isEmpty()) {
                ((MacFirmware) getFirmware()).setVersion(bootRomVersion);
            }
        }
        if (checkLine.contains(smcMarker)) {
            String smcVersion = checkLine.split(Pattern.quote(smcMarker))[1].trim();
            if (!smcVersion.isEmpty()) {
                ((MacBaseboard) getBaseboard()).setVersion(smcVersion);
            }
        }
        if (checkLine.contains(serialNumMarker)) {
            String serialNumberSystem = checkLine.split(Pattern.quote(serialNumMarker))[1].trim();
            this.serialNumber = serialNumberSystem.isEmpty() ? getIORegistryPlatformSerialNumber() : serialNumberSystem;
            ((MacBaseboard) getBaseboard()).setSerialNumber(this.serialNumber);
        }
    }

    private void setModelNameAndIdentifier(String modelName, String modelIdentifier) {
        // Use name (id) if both available; else either one
        if (modelName.isEmpty() && !modelIdentifier.isEmpty()) {
            this.model = modelIdentifier;
        } else {
            if (!modelName.isEmpty() && !modelIdentifier.isEmpty()) {
                this.model = modelName + " (" + modelIdentifier + ")";
            } else {
                this.model = modelName.isEmpty() ? Constants.UNKNOWN : modelName;
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

}
