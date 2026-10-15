/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.freebsd.FreeBsdFileSystem;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD file system. All file-store enumeration (geom, df, mount parsing) lives on
 * {@link FreeBsdFileSystem}; only the file-descriptor sysctl reads are JNA-specific.
 */
@ThreadSafe
public final class FreeBsdFileSystemJNA extends FreeBsdFileSystem {

    @Override
    protected long querySysctl(String name) {
        return BsdSysctlUtil.sysctl(name, 0);
    }
}
