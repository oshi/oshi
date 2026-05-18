/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.mac.IOKitProvider;
import oshi.hardware.common.platform.mac.MacBaseboard;

/**
 * Baseboard data obtained from ioreg using FFM.
 */
@Immutable
final class MacBaseboardFFM extends MacBaseboard {

    @Override
    protected IOKitProvider ioKitProvider() {
        return IOKitProviderFFM.INSTANCE;
    }
}
