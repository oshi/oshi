/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.software.common.os.unix.solaris.SolarisFileSystem;

/**
 * FFM-backed Solaris File System. Uses the legacy {@code kstat} chain only; Kstat2 exists only on the JDK 17-capped
 * latest Solaris, so FFM (JDK 25) never needs it.
 */
@ThreadSafe
public final class SolarisFileSystemFFM extends SolarisFileSystem {

    @Override
    public long getOpenFileDescriptors() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup(null, -1, "file_cache");
            if (ksp.address() != 0L && kc.read(ksp)) {
                return KstatUtilFFM.dataLookupLong(ksp, "buf_inuse");
            }
        }
        return 0L;
    }

    @Override
    public long getMaxFileDescriptors() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup(null, -1, "file_cache");
            if (ksp.address() != 0L && kc.read(ksp)) {
                return KstatUtilFFM.dataLookupLong(ksp, "buf_max");
            }
        }
        return 0L;
    }
}
