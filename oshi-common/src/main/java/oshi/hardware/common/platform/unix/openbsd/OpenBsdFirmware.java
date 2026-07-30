/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.BsdFirmware;

/**
 * OpenBSD Firmware implementation. OpenBSD reports firmware through the {@code dmesg} banner the base reads by default,
 * so there is nothing platform-specific to add.
 */
@Immutable
public class OpenBsdFirmware extends BsdFirmware {
}
