/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.platform.mac.IOKitUtilFFM;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained from ioreg
 */
@Immutable
final class MacBaseboardFFM extends AbstractBaseboard {

    private final Supplier<Quartet<String, String, String, String>> manufModelVersSerial = memoize(
            MacBaseboardFFM::queryPlatform);

    @Override
    public String getManufacturer() {
        return manufModelVersSerial.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelVersSerial.get().getB();
    }

    @Override
    public String getVersion() {
        return manufModelVersSerial.get().getC();
    }

    @Override
    public String getSerialNumber() {
        return manufModelVersSerial.get().getD();
    }

    private static Quartet<String, String, String, String> queryPlatform() {
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
