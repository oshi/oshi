/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import oshi.hardware.common.AbstractFirmware;
import oshi.util.ExecutingCommand;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:59
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class MacFirmware extends AbstractFirmware {

    private static final String CMD_SYSTEM_PROFILER_SPHARDWARE_DATA_TYPE = "system_profiler SPHardwareDataType";

    MacFirmware() {

        init();
    }

    private void init() {

//        $ system_profiler SPHardwareDataType
//        Hardware:
//
//            Hardware Overview:
//
//              Model Name: MacBook Pro
//              Model Identifier: MacBookPro8,2
//              Processor Name: Intel Core i7
//              Processor Speed: 2.3 GHz
//              Number of Processors: 1
//              Total Number of Cores: 4
//              L2 Cache (per Core): 256 KB
//              L3 Cache: 8 MB
//              Memory: 16 GB
//              Boot ROM Version: MBP81.0047.B2C
//              SMC Version (system): 1.69f4
//              Serial Number (system): C02FH4XYCB71
//              Hardware UUID: D92CE829-65AD-58FA-9C32-88968791B7BD
//              Sudden Motion Sensor:
//                  State: Enabled

        setManufacturer("Apple Inc.");

        final String bootRomVersion = parseCommandOutput(CMD_SYSTEM_PROFILER_SPHARDWARE_DATA_TYPE, "Boot ROM Version:");
        if (bootRomVersion != null && !bootRomVersion.isEmpty()) {
            setVersion(bootRomVersion);
        }

        // name, description and releaseDate --> not set
    }

    private String parseCommandOutput(final String nativeCall, final String marker) {
        for (final String checkLine : ExecutingCommand.runNative(nativeCall)) {
            if (checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }

        return null;
    }
}
