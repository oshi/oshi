/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Firmware;
import oshi.hardware.common.platform.unix.BsdComputerSystem;

/**
 * OpenBSD ComputerSystem implementation
 */
@Immutable
public class OpenBsdComputerSystem extends BsdComputerSystem {

    @Override
    protected Firmware createFirmware() {
        return new OpenBsdFirmware();
    }
}
