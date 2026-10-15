/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.RLIMIT_NOFILE;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.PosixLibcFunctions;
import oshi.software.common.os.unix.openbsd.OpenBsdFileSystem;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * FFM-backed OpenBSD file system. All file-store enumeration (mount/df parsing) lives on {@link OpenBsdFileSystem};
 * only the file-descriptor sysctl and getrlimit reads are FFM-specific.
 */
@ThreadSafe
public final class OpenBsdFileSystemFFM extends OpenBsdFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdFileSystemFFM.class);

    @Override
    protected long querySysctl(String name) {
        return BsdSysctlUtil.sysctl(name, 0);
    }

    @Override
    protected long queryHardOpenFileLimit() {
        return ForeignFunctions.callInArenaLongOrDefault(arena -> {
            MemorySegment rlim = arena.allocate(PosixLibcFunctions.RLIMIT_LAYOUT);
            if (PosixLibcFunctions.getrlimit(RLIMIT_NOFILE, rlim) != 0) {
                return -1L;
            }
            return PosixLibcFunctions.rlimitMax(rlim);
        }, LOG, WARN, "Failed getrlimit", -1L);
    }
}
