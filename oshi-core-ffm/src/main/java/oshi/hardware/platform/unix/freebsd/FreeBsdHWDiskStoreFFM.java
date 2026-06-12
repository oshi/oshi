/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdHWDiskStore;

/**
 * FFM-backed FreeBSD hard disk implementation.
 */
@ThreadSafe
public final class FreeBsdHWDiskStoreFFM extends FreeBsdHWDiskStore {

    private FreeBsdHWDiskStoreFFM(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return FreeBsdHWDiskStore.getDisks(BsdSysctlUtilFFM::sysctl, FreeBsdHWDiskStoreFFM::new);
    }
}
