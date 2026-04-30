/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.nio.charset.StandardCharsets;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.common.platform.mac.MacBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained from ioreg
 */
@Immutable
final class MacBaseboardFFM extends MacBaseboard {

    @Override
    protected Quartet<String, String, String, String> queryPlatform() {
        String manufacturer = null;
        String model = null;
        String version = null;
        String serialNumber = null;

        IORegistryEntry platformExpert = IOKitUtilFFM.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
            }
            data = platformExpert.getByteArrayProperty("board-id");
            if (data != null) {
                model = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
            }
            if (Util.isBlank(model)) {
                data = platformExpert.getByteArrayProperty("model-number");
                if (data != null) {
                    model = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
                }
            }
            data = platformExpert.getByteArrayProperty("version");
            if (data != null) {
                version = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
            }
            data = platformExpert.getByteArrayProperty("mlb-serial-number");
            if (data != null) {
                serialNumber = new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
            }
            if (Util.isBlank(serialNumber)) {
                serialNumber = platformExpert.getStringProperty("IOPlatformSerialNumber");
            }
            platformExpert.release();
        }
        return new Quartet<>(Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model, Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber);
    }
}
