/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.Util;
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
        Quartet<String, String, String, String> result = ioKitProvider().withMatchingService("IOPlatformExpertDevice",
                entry -> {
                    byte[] data = entry.getByteArrayProperty("manufacturer");
                    String mfr = data != null ? new String(data, StandardCharsets.UTF_8).replace("\0", "").trim()
                            : null;
                    data = entry.getByteArrayProperty("model");
                    String mdl = data != null ? new String(data, StandardCharsets.UTF_8).replace("\0", "").trim()
                            : null;
                    String sn = entry.getStringProperty("IOPlatformSerialNumber");
                    String uuid = entry.getStringProperty("IOPlatformUUID");
                    return new Quartet<>(mfr, mdl, sn, uuid);
                });
        if (result == null) {
            result = new Quartet<>(null, null, null, null);
        }
        return new Quartet<>(Util.isBlank(result.getA()) ? "Apple Inc." : result.getA(),
                Util.isBlank(result.getB()) ? Constants.UNKNOWN : result.getB(),
                Util.isBlank(result.getC()) ? Constants.UNKNOWN : result.getC(),
                Util.isBlank(result.getD()) ? Constants.UNKNOWN : result.getD());
    }
}
