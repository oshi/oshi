/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import com.sun.jna.platform.unix.Resource;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.common.os.unix.openbsd.OpenBsdFileSystem;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * The OpenBSD File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage.
 */
@ThreadSafe
public class OpenBsdFileSystemJNA extends OpenBsdFileSystem {

    @Override
    protected long querySysctl(String name) {
        return BsdSysctlUtil.sysctl(name, 0);
    }

    @Override
    protected long queryHardOpenFileLimit() {
        Resource.Rlimit rlimit = new Resource.Rlimit();
        if (OpenBsdLibc.INSTANCE.getrlimit(OpenBsdLibc.RLIMIT_NOFILE, rlimit) != 0) {
            return -1L;
        }
        return rlimit.rlim_max;
    }
}
