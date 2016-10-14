package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractFirmware;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:59
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class LinuxFirmware extends AbstractFirmware {

    LinuxFirmware() {

        init();
    }

    private void init() {
        // TODO

//        $ ls /sys/devices/virtual/dmi/id/
//        bios_date        board_vendor       chassis_version  product_version
//        bios_vendor      board_version      modalias         subsystem
//        bios_version     chassis_asset_tag  power            sys_vendor
//        board_asset_tag  chassis_serial     product_name     uevent
//        board_name       chassis_type       product_serial
//        board_serial     chassis_vendor     product_uuid

        // TODO : bios_vendor          --> manufacturer
        // TODO : <empty/unknown/...>  --> name
        // TODO : modalias             --> description
        // TODO : bios_version         --> version
        // TODO : bios_date            --> releaseDate
    }
}
