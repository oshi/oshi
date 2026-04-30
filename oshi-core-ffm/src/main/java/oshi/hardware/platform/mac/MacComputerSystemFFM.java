/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.nio.charset.StandardCharsets;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.platform.mac.MacComputerSystem;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Hardware data obtained from ioreg.
 */
@Immutable
final class MacComputerSystemFFM extends MacComputerSystem {

    @Override
    public Firmware createFirmware() {
        return new MacFirmwareFFM();
    }

    @Override
    public Baseboard createBaseboard() {
        return new MacBaseboardFFM();
    }

    @Override
    protected Quartet<String, String, String, String> platformExpert() {
        String manufacturer = null;
        String model = null;
        String serialNumber = null;
        String uuid = null;

        IORegistryEntry platformExpert = IOKitUtilFFM.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
            }
            data = platformExpert.getByteArrayProperty("model");
            if (data != null) {
                model = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
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
