/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.BsdFirmware;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * NetBSD Firmware implementation, from the SMBIOS strings the kernel publishes under {@code machdep.dmi}. These are
 * absent on machines without SMBIOS BIOS information, which includes many ARM systems.
 */
@Immutable
public class NetBsdFirmware extends BsdFirmware {

    @Override
    protected Triplet<@Nullable String, @Nullable String, @Nullable String> readFirmware() {
        Map<String, String> dmi = NetBsdComputerSystem.queryDmi();
        return new Triplet<>(dmi.get("bios-vendor"), dmi.get("bios-version"),
                ParseUtil.parseYyyyMmDdToIso(dmi.getOrDefault("bios-date", "")));
    }
}
