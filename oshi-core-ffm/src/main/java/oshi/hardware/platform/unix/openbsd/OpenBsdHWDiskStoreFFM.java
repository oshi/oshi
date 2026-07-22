/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdHWDiskStore;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * FFM-backed OpenBSD hard disk implementation.
 */
@ThreadSafe
public final class OpenBsdHWDiskStoreFFM extends OpenBsdHWDiskStore {

    private OpenBsdHWDiskStoreFFM(String name, String model, String serial, long size,
            Supplier<List<String>> iostatSupplier) {
        super(name, model, serial, size, iostatSupplier);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return OpenBsdHWDiskStore.getDisks(BsdSysctlUtil::sysctl, OpenBsdHWDiskStoreFFM::new);
    }
}
