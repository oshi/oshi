/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.ParseUtil;
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
        Quartet<@Nullable String, @Nullable String, @Nullable String, @Nullable String> result = ioKitProvider()
                .withMatchingService("IOPlatformExpertDevice", entry -> {
                    String mfr = decodeProperty(entry.getByteArrayProperty("manufacturer"));
                    String mdl = decodeProperty(entry.getByteArrayProperty("board-id"));
                    if (Util.isBlank(mdl)) {
                        mdl = decodeProperty(entry.getByteArrayProperty("model-number"));
                    }
                    String ver = decodeProperty(entry.getByteArrayProperty("version"));
                    String sn = decodeProperty(entry.getByteArrayProperty("mlb-serial-number"));
                    if (Util.isBlank(sn)) {
                        sn = entry.getStringProperty("IOPlatformSerialNumber");
                    }
                    return new Quartet<@Nullable String, @Nullable String, @Nullable String, @Nullable String>(mfr, mdl,
                            ver, sn);
                });
        if (result == null) {
            result = new Quartet<>(null, null, null, null);
        }
        String mfr = result.getA();
        return new Quartet<>(mfr == null || mfr.isEmpty() ? "Apple Inc." : mfr,
                ParseUtil.getStringValueOrUnknown(result.getB()), ParseUtil.getStringValueOrUnknown(result.getC()),
                ParseUtil.getStringValueOrUnknown(result.getD()));
    }

    private static @Nullable String decodeProperty(byte @Nullable [] data) {
        return data != null ? ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8) : null;
    }
}
