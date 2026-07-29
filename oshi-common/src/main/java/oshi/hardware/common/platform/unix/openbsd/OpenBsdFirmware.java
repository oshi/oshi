/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.BsdFirmware;
import oshi.util.ExecutingCommand;
import oshi.util.tuples.Triplet;

/**
 * OpenBSD Firmware implementation
 */
@Immutable
public class OpenBsdFirmware extends BsdFirmware {

    @Override
    protected Triplet<String, String, String> readFirmware() {
        return parseDmesg(ExecutingCommand.runNative("dmesg"));
    }
}
