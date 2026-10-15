/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.freebsd.FreeBsdFileSystem;

/**
 * FFM-backed FreeBSD file system. All file-store enumeration (geom, df, mount parsing) lives on
 * {@link FreeBsdFileSystem}; only the file-descriptor sysctl reads are FFM-specific.
 */
@ThreadSafe
public final class FreeBsdFileSystemFFM extends FreeBsdFileSystem {

    @Override
    protected long querySysctl(String name) {
        return BsdSysctlUtilFFM.sysctl(name, 0);
    }
}
