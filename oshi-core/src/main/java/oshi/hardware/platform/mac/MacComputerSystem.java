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
import oshi.SystemInfo;
import oshi.hardware.common.AbstractBaseboard.BaseboardInitializer;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.jna.platform.mac.IOKit;
import oshi.util.ExecutingCommand;
import oshi.util.platform.mac.IOKitUtil;

/**
 * Hardware data obtained by system_profiler
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
final class MacComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    private static final String APPLE = "Apple Inc.";

    MacComputerSystem() {
        init();
    }

    private void init() {

        setManufacturer(APPLE);

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

        String modelName = "";
        final String modelNameMarker = "Model Name:";
        String modelIdentifier = "";
        final String modelIdMarker = "Model Identifier:";
        String serialNumberSystem = "";
        final String serialNumMarker = "Serial Number (system):";
        String smcVersion = "";
        final String smcMarker = "SMC Version (system):";
        String bootRomVersion = "";
        final String bootRomMarker = "Boot ROM Version:";

        final MacFirmware firmware = new MacFirmware();
        firmware.setManufacturer(APPLE);
        firmware.setName("EFI");

        BaseboardInitializer baseboardInitializer = new BaseboardInitializer();
        baseboardInitializer.manufacturer = APPLE;
        baseboardInitializer.model = "SMC";

        // Populate name and ID
        for (final String checkLine : ExecutingCommand.runNative("system_profiler SPHardwareDataType")) {
            if (checkLine.contains(modelNameMarker)) {
                modelName = checkLine.split(modelNameMarker)[1].trim();
            }
            if (checkLine.contains(modelIdMarker)) {
                modelIdentifier = checkLine.split(modelIdMarker)[1].trim();
            }
            if (checkLine.contains(bootRomMarker)) {
                bootRomVersion = checkLine.split(bootRomMarker)[1].trim();
            }
            if (checkLine.contains(smcMarker)) {
                smcVersion = checkLine.split(Pattern.quote(smcMarker))[1].trim();
            }
            if (checkLine.contains(serialNumMarker)) {
                serialNumberSystem = checkLine.split(Pattern.quote(serialNumMarker))[1].trim();
            }
        }
        // Use name (id) if both available; else either one
        if (!modelName.isEmpty()) {
            if (!modelIdentifier.isEmpty()) {
                setModel(modelName + " (" + modelIdentifier + ")");
            } else {
                setModel(modelName);
            }
        } else {
            if (!modelIdentifier.isEmpty()) {
                setModel(modelIdentifier);
            }
        }
        if (serialNumberSystem.isEmpty()) {
            serialNumberSystem = getSystemSerialNumber();
        }
        setSerialNumber(serialNumberSystem);
        baseboardInitializer.serialNumber = serialNumberSystem;
        if (!smcVersion.isEmpty()) {
            baseboardInitializer.version = smcVersion;
        }
        if (bootRomVersion != null && !bootRomVersion.isEmpty()) {
            firmware.setVersion(bootRomVersion);
        }

        setFirmware(firmware);
        setBaseboard(new MacBaseboard(baseboardInitializer));
    }

    private String getSystemSerialNumber() {
        String serialNumber = null;
        int service = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (service != 0) {
            // Fetch the serial number
            serialNumber = IOKitUtil.getIORegistryStringProperty(service, "IOPlatformSerialNumber");
            IOKit.INSTANCE.IOObjectRelease(service);
        }
        if (serialNumber == null) {
            serialNumber = SystemInfo.UNKNOWN;
        }
        return serialNumber;
    }

}
