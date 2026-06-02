/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import oshi.hardware.common.AbstractHWDiskStore;

public abstract class FreeBsdHWDiskStore extends AbstractHWDiskStore {

    protected FreeBsdHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }
}
