/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.BsdFirmware;
import oshi.util.ExecutingCommand;
import oshi.util.tuples.Triplet;

/**
 * NetBSD Firmware implementation
 */
@Immutable
public class NetBsdFirmware extends BsdFirmware {

    @Override
    protected Triplet<String, String, String> readFirmware() {
        return parseDmesg(ExecutingCommand.runNative("dmesg"));
    }
}
