package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractAssembly;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:58
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class LinuxAssembly extends AbstractAssembly {

    LinuxAssembly() {

        init();
    }

    private void init() {
        // TODO

//        $ sudo dmidecode -t system
//        # dmidecode 2.12
//        SMBIOS 2.7 present.
//
//        Handle 0x0001, DMI type 1, 27 bytes
//        System Information
//            Manufacturer: Parallels Software International Inc.                                 <-- TODO - put it in "manufacturer"
//            Product Name: Parallels Virtual Platform                                            <-- TODO - put it in "model"
//            Version: None
//            Serial Number: Parallels-8E A6 8E 66 FF 9F 41 A1 91 26 6B E3 D3 C7 B2 A9            <-- TODO - put it in serialNumber
//            UUID: 668EA68E-9FFF-A141-9126-6BE3D3C7B2A9
//            Wake-up Type: Power Switch
//            SKU Number: Undefined
//            Family: Parallels VM
//
//        Handle 0x0016, DMI type 32, 20 bytes
//        System Boot Information
//            Status: No errors detected
//
//
//        or fields in sysfs here:
//
//        $ ls /sys/devices/virtual/dmi/id/
//        bios_date        board_vendor       chassis_version  product_version
//        bios_vendor      board_version      modalias         subsystem
//        bios_version     chassis_asset_tag  power            sys_vendor
//        board_asset_tag  chassis_serial     product_name     uevent
//        board_name       chassis_type       product_serial
//        board_serial     chassis_vendor     product_uuid

        // TODO - or

        // TODO - sys_vendor                      --> "manufacturer"
        // TODO - product_name + product_version  --> "model"
        // TODO - product_serial                  --> "serialNumber" (if empty/null use: board_serial ?)

    }
}
