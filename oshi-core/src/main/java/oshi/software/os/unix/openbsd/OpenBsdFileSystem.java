/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * The OpenBSD File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage.
 */
@ThreadSafe
public class OpenBsdFileSystem extends oshi.software.common.os.unix.openbsd.OpenBsdFileSystem {

    @Override
    public long getOpenFileDescriptors() {
        return OpenBsdSysctlUtil.sysctl("kern.nfiles", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return OpenBsdSysctlUtil.sysctl("kern.maxfiles", 0);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return OpenBsdSysctlUtil.sysctl("kern.maxfilesperproc", 0);
    }
}
