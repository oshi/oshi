/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * OpenBSD ComputerSystem implementation
 */
@Immutable
public class OpenBsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(OpenBsdComputerSystem::queryManufacturer);

    private final Supplier<String> model = memoize(OpenBsdComputerSystem::queryModel);

    private final Supplier<String> serialNumber = memoize(OpenBsdComputerSystem::querySerialNumber);

    private final Supplier<String> uuid = memoize(OpenBsdComputerSystem::queryUUID);

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
                OpenBsdSysctlUtil.sysctl("hw.product", Constants.UNKNOWN));
    }

    private static String queryManufacturer() {
        return OpenBsdSysctlUtil.sysctl("hw.vendor", Constants.UNKNOWN);
    }

    private static String queryModel() {
        return OpenBsdSysctlUtil.sysctl("hw.version", Constants.UNKNOWN);
    }

    private static String querySerialNumber() {
        return OpenBsdSysctlUtil.sysctl("hw.serialno", Constants.UNKNOWN);
    }

    private static String queryUUID() {
        return OpenBsdSysctlUtil.sysctl("hw.uuid", Constants.UNKNOWN);
    }
}
