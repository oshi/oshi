/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.BsdFirmware;

/**
 * NetBSD Firmware implementation. NetBSD reports firmware through the {@code dmesg} banner the base reads by default,
 * so there is nothing platform-specific to add.
 */
@Immutable
public class NetBsdFirmware extends BsdFirmware {
}
