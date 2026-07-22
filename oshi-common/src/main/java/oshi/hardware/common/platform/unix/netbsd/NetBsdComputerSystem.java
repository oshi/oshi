/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.common.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * NetBSD ComputerSystem implementation
 */
@Immutable
public class NetBsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(NetBsdComputerSystem::queryManufacturer);

    private final Supplier<String> model = memoize(NetBsdComputerSystem::queryModel);

    private final Supplier<String> serialNumber = memoize(NetBsdComputerSystem::querySerialNumber);

    private final Supplier<String> uuid = memoize(NetBsdComputerSystem::queryUUID);

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
        return new NetBsdFirmware();
    }

    @Override
    protected Baseboard createBaseboard() {
        return new UnixBaseboard(manufacturer.get(), model.get(), serialNumber.get(),
                BsdSysctlUtil.sysctl("hw.product", Constants.UNKNOWN));
    }

    private static String queryManufacturer() {
        return BsdSysctlUtil.sysctl("hw.vendor", Constants.UNKNOWN);
    }

    private static String queryModel() {
        return BsdSysctlUtil.sysctl("hw.version", Constants.UNKNOWN);
    }

    private static String querySerialNumber() {
        return BsdSysctlUtil.sysctl("hw.serialno", Constants.UNKNOWN);
    }

    private static String queryUUID() {
        return BsdSysctlUtil.sysctl("hw.uuid", Constants.UNKNOWN);
    }
}
