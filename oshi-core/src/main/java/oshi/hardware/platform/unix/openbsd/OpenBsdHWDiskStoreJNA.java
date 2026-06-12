/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdHWDiskStore;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * OpenBSD hard disk implementation.
 */
@ThreadSafe
public final class OpenBsdHWDiskStoreJNA extends OpenBsdHWDiskStore {

    private OpenBsdHWDiskStoreJNA(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return OpenBsdHWDiskStore.getDisks(OpenBsdSysctlUtil::sysctl, OpenBsdHWDiskStoreJNA::new);
    }
}
