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
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Hardware data obtained from ioreg.
 */
@Immutable
public abstract class MacComputerSystem extends AbstractComputerSystem {

    /**
     * Default constructor.
     */
    protected MacComputerSystem() {
    }

    private final Supplier<Quartet<String, String, String, String>> manufacturerModelSerialUUID = memoize(
            this::platformExpert);

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

    /**
     * Returns the IOKit provider for this implementation.
     *
     * @return the IOKit provider
     */
    protected abstract IOKitProvider ioKitProvider();

    /**
     * Queries platform expert computer system information.
     *
     * @return a quartet of manufacturer, model, serial number, UUID
     */
    protected Quartet<String, String, String, String> platformExpert() {
        Quartet<@Nullable String, @Nullable String, @Nullable String, @Nullable String> result = ioKitProvider()
                .withMatchingService("IOPlatformExpertDevice", entry -> {
                    byte[] data = entry.getByteArrayProperty("manufacturer");
                    String mfr = data != null ? ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8) : null;
                    data = entry.getByteArrayProperty("model");
                    String mdl = data != null ? ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8) : null;
                    String sn = entry.getStringProperty("IOPlatformSerialNumber");
                    String uuid = entry.getStringProperty("IOPlatformUUID");
                    return new Quartet<@Nullable String, @Nullable String, @Nullable String, @Nullable String>(mfr, mdl,
                            sn, uuid);
                });
        if (result == null) {
            result = new Quartet<>(null, null, null, null);
        }
        String mfr = result.getA();
        return new Quartet<>(mfr == null || mfr.isEmpty() ? "Apple Inc." : mfr,
                ParseUtil.getStringValueOrUnknown(result.getB()), ParseUtil.getStringValueOrUnknown(result.getC()),
                ParseUtil.getStringValueOrUnknown(result.getD()));
    }
}
