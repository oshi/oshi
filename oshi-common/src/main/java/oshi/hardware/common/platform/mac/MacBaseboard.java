/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained from ioreg
 */
@Immutable
public abstract class MacBaseboard extends AbstractBaseboard {

    /**
     * Default constructor.
     */
    protected MacBaseboard() {
    }

    private final Supplier<Quartet<String, String, String, String>> manufModelVersSerial = memoize(this::queryPlatform);

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

    /**
     * Returns the IOKit provider for this implementation.
     *
     * @return the IOKit provider
     */
    protected abstract IOKitProvider ioKitProvider();

    /**
     * Queries platform baseboard information.
     *
     * @return a quartet of manufacturer, model, version, serial number
     */
    protected Quartet<String, String, String, String> queryPlatform() {
        Quartet<String, String, String, String> result = ioKitProvider().withMatchingService("IOPlatformExpertDevice",
                entry -> {
                    String mfr = bytesToString(entry.getByteArrayProperty("manufacturer"));
                    String mdl = bytesToString(entry.getByteArrayProperty("board-id"));
                    if (Util.isBlank(mdl)) {
                        mdl = bytesToString(entry.getByteArrayProperty("model-number"));
                    }
                    String ver = bytesToString(entry.getByteArrayProperty("version"));
                    String sn = bytesToString(entry.getByteArrayProperty("mlb-serial-number"));
                    if (Util.isBlank(sn)) {
                        sn = entry.getStringProperty("IOPlatformSerialNumber");
                    }
                    return new Quartet<>(mfr, mdl, ver, sn);
                });
        if (result == null) {
            result = new Quartet<>(null, null, null, null);
        }
        return new Quartet<>(Util.isBlank(result.getA()) ? "Apple Inc." : result.getA(),
                Util.isBlank(result.getB()) ? Constants.UNKNOWN : result.getB(),
                Util.isBlank(result.getC()) ? Constants.UNKNOWN : result.getC(),
                Util.isBlank(result.getD()) ? Constants.UNKNOWN : result.getD());
    }

    private static String bytesToString(byte[] data) {
        return data != null ? new String(data, StandardCharsets.UTF_8).replace("\0", "").trim() : null;
    }
}
