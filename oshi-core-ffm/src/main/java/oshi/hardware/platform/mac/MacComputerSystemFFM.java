/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.platform.mac.IOKitProvider;
import oshi.hardware.common.platform.mac.MacComputerSystem;

/**
 * Hardware data obtained from ioreg using FFM.
 */
@Immutable
final class MacComputerSystemFFM extends MacComputerSystem {

    @Override
    public Firmware createFirmware() {
        return new MacFirmwareFFM();
    }

    @Override
    public Baseboard createBaseboard() {
        return new MacBaseboardFFM();
    }

    @Override
    protected IOKitProvider ioKitProvider() {
        return IOKitProviderFFM.INSTANCE;
    }
}
