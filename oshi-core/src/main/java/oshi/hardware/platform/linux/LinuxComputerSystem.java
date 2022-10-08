/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.linux.Devicetree;
import oshi.driver.linux.Dmidecode;
import oshi.driver.linux.Lshal;
import oshi.driver.linux.Lshw;
import oshi.driver.linux.Sysfs;
import oshi.driver.linux.proc.CpuInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;

/**
 * Hardware data obtained from sysfs.
 */
@Immutable
final class LinuxComputerSystem extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(LinuxComputerSystem::queryManufacturer);

    private final Supplier<String> model = memoize(LinuxComputerSystem::queryModel);

    private final Supplier<String> serialNumber = memoize(LinuxComputerSystem::querySerialNumber);

    private final Supplier<String> uuid = memoize(LinuxComputerSystem::queryUUID);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    @Override
    public String getModel() {
        return model.get();
    }

    @Override
    public String getSerialNumber() {
        return serialNumber.get();
    }

    @Override
    public String getHardwareUUID() {
        return uuid.get();
    }

    @Override
    public Firmware createFirmware() {
        return new LinuxFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new LinuxBaseboard();
    }

    private static String queryManufacturer() {
        String result = null;
        if ((result = Sysfs.querySystemVendor()) == null && (result = CpuInfo.queryCpuManufacturer()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static String queryModel() {
        String result = null;
        if ((result = Sysfs.queryProductModel()) == null && (result = Devicetree.queryModel()) == null
                && (result = Lshw.queryModel()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static String querySerialNumber() {
        String result = null;
        if ((result = Sysfs.queryProductSerial()) == null && (result = Dmidecode.querySerialNumber()) == null
                && (result = Lshal.querySerialNumber()) == null && (result = Lshw.querySerialNumber()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private static String queryUUID() {
        String result = null;
        if ((result = Sysfs.queryUUID()) == null && (result = Dmidecode.queryUUID()) == null
                && (result = Lshal.queryUUID()) == null && (result = Lshw.queryUUID()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }
}
