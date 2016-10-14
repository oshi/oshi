package oshi.hardware.platform.mac;

import oshi.hardware.common.AbstractFirmware;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:59
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class MacFirmware extends AbstractFirmware {

    MacFirmware() {

        init();
    }

    private void init() {
        // TODO

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

        // TODO : <"Apple Inc."> (hard coded) --> manufacturer
        // TODO : <empty/unknown/...>   --> name
        // TODO : <empty/unknown/...>   --> description
        // TODO : "Boot ROM Version"    --> version
        // TODO : <empty/unknown/...>   --> releaseDate
    }
}
