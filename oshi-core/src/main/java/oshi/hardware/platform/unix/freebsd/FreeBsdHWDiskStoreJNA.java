/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdHWDiskStore;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * FreeBSD hard disk implementation.
 */
@ThreadSafe
public final class FreeBsdHWDiskStoreJNA extends FreeBsdHWDiskStore {

    private FreeBsdHWDiskStoreJNA(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return FreeBsdHWDiskStore.getDisks(BsdSysctlUtil::sysctl, FreeBsdHWDiskStoreJNA::new);
    }
}
