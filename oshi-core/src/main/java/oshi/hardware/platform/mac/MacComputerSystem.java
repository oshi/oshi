/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.regex.Pattern;

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

        final MacBaseboard baseboard = new MacBaseboard();
        baseboard.setManufacturer(APPLE);
        baseboard.setModel("SMC");

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
        baseboard.setSerialNumber(serialNumberSystem);
        if (!smcVersion.isEmpty()) {
            baseboard.setVersion(smcVersion);
        }
        if (bootRomVersion != null && !bootRomVersion.isEmpty()) {
            firmware.setVersion(bootRomVersion);
        }

        setFirmware(firmware);
        setBaseboard(baseboard);
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
            serialNumber = "unknown";
        }
        return serialNumber;
    }

}
