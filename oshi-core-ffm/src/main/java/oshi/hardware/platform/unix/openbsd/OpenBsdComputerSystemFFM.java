/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.common.platform.unix.UnixBaseboard;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdFirmware;
import oshi.util.Constants;

/**
 * FFM-backed OpenBSD ComputerSystem implementation.
 */
@Immutable
public class OpenBsdComputerSystemFFM extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(OpenBsdComputerSystemFFM::queryManufacturer);

    private final Supplier<String> model = memoize(OpenBsdComputerSystemFFM::queryModel);

    private final Supplier<String> serialNumber = memoize(OpenBsdComputerSystemFFM::querySerialNumber);

    private final Supplier<String> uuid = memoize(OpenBsdComputerSystemFFM::queryUUID);

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
    protected Firmware createFirmware() {
        return new OpenBsdFirmware();
    }

    @Override
    protected Baseboard createBaseboard() {
        return new UnixBaseboard(manufacturer.get(), model.get(), serialNumber.get(),
                OpenBsdSysctlUtilFFM.sysctl("hw.product", Constants.UNKNOWN));
    }

    private static String queryManufacturer() {
        return OpenBsdSysctlUtilFFM.sysctl("hw.vendor", Constants.UNKNOWN);
    }

    private static String queryModel() {
        return OpenBsdSysctlUtilFFM.sysctl("hw.version", Constants.UNKNOWN);
    }

    private static String querySerialNumber() {
        return OpenBsdSysctlUtilFFM.sysctl("hw.serialno", Constants.UNKNOWN);
    }

    private static String queryUUID() {
        return OpenBsdSysctlUtilFFM.sysctl("hw.uuid", Constants.UNKNOWN);
    }
}
