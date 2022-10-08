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
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained from ioreg
 */
@Immutable
final class MacBaseboard extends AbstractBaseboard {

    private final Supplier<Quartet<String, String, String, String>> manufModelVersSerial = memoize(
            MacBaseboard::queryPlatform);

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

        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = Native.toString(data, StandardCharsets.UTF_8);
            }
            data = platformExpert.getByteArrayProperty("board-id");
            if (data != null) {
                model = Native.toString(data, StandardCharsets.UTF_8);
            }
            if (Util.isBlank(model)) {
                data = platformExpert.getByteArrayProperty("model-number");
                if (data != null) {
                    model = Native.toString(data, StandardCharsets.UTF_8);
                }
            }
            data = platformExpert.getByteArrayProperty("version");
            if (data != null) {
                version = Native.toString(data, StandardCharsets.UTF_8);
            }
            data = platformExpert.getByteArrayProperty("mlb-serial-number");
            if (data != null) {
                serialNumber = Native.toString(data, StandardCharsets.UTF_8);
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
