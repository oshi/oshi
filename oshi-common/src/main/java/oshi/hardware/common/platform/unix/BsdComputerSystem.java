/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * Common BSD ComputerSystem implementation for the platforms that identify the machine through the {@code hw.*} string
 * sysctls, namely NetBSD and OpenBSD.
 * <p>
 * These are read with the command-line {@code sysctl} rather than natively, because OpenBSD has no {@code sysctlbyname}
 * and so cannot look a string sysctl up by name from a binding. That is also why these platforms need no per-binding
 * subclass: there is nothing backend-specific to vary.
 * <p>
 * FreeBSD deliberately does not extend this. It identifies the machine from {@code dmidecode} and only falls back to a
 * sysctl for the UUID, which it reads natively, so it keeps its own base and its per-binding subclasses.
 */
@Immutable
public abstract class BsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(BsdComputerSystem::queryManufacturer);

    private final Supplier<String> model = memoize(BsdComputerSystem::queryModel);

    private final Supplier<String> serialNumber = memoize(BsdComputerSystem::querySerialNumber);

    private final Supplier<String> uuid = memoize(BsdComputerSystem::queryUUID);

    /**
     * Default constructor.
     */
    protected BsdComputerSystem() {
    }

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
