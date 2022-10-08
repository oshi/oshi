/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Hardware data obtained from ioreg.
 */
@Immutable
final class MacComputerSystem extends AbstractComputerSystem {

    private final Supplier<Quartet<String, String, String, String>> manufacturerModelSerialUUID = memoize(
            MacComputerSystem::platformExpert);

    @Override
    public String getManufacturer() {
        return manufacturerModelSerialUUID.get().getA();
    }

    @Override
    public String getModel() {
        return manufacturerModelSerialUUID.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return manufacturerModelSerialUUID.get().getC();
    }

    @Override
    public String getHardwareUUID() {
        return manufacturerModelSerialUUID.get().getD();
    }

    @Override
    public Firmware createFirmware() {
        return new MacFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new MacBaseboard();
    }

    private static Quartet<String, String, String, String> platformExpert() {
        String manufacturer = null;
        String model = null;
        String serialNumber = null;
        String uuid = null;
        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = Native.toString(data, StandardCharsets.UTF_8);
            }
            data = platformExpert.getByteArrayProperty("model");
            if (data != null) {
                model = Native.toString(data, StandardCharsets.UTF_8);
            }
            serialNumber = platformExpert.getStringProperty("IOPlatformSerialNumber");
            uuid = platformExpert.getStringProperty("IOPlatformUUID");
            platformExpert.release();
        }
        return new Quartet<>(Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model,
                Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber,
                Util.isBlank(uuid) ? Constants.UNKNOWN : uuid);
    }
}
