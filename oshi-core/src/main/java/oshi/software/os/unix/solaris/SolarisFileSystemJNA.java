/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.unix.solaris.SolarisOperatingSystemJNA.HAS_KSTAT2;
import static oshi.util.Memoizer.defaultExpiration;

import java.util.function.Supplier;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.solaris.SolarisFileSystem;
import oshi.util.Memoizer;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Pair;

/**
 * JNA-backed Solaris File System. Uses Kstat2 where available, falling back to the legacy {@code kstat} chain.
 */
@ThreadSafe
public final class SolarisFileSystemJNA extends SolarisFileSystem {

    private final Supplier<Pair<Long, Long>> fileDesc = Memoizer.memoize(SolarisFileSystemJNA::queryFileDescriptors,
            defaultExpiration());

    @Override
    public long getOpenFileDescriptors() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return fileDesc.get().getA();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup(null, -1, "file_cache");
            // Set values
            if (ksp != null && kc.read(ksp)) {
                return KstatUtil.dataLookupLong(ksp, "buf_inuse");
            }
        }
        return 0L;
    }

    @Override
    public long getMaxFileDescriptors() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return fileDesc.get().getB();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup(null, -1, "file_cache");
            // Set values
            if (ksp != null && kc.read(ksp)) {
                return KstatUtil.dataLookupLong(ksp, "buf_max");
            }
        }
        return 0L;
    }

    private static Pair<Long, Long> queryFileDescriptors() {
        Object[] results = KstatUtil.queryKstat2("kstat:/kmem_cache/kmem_default/file_cache", "buf_inuse", "buf_max");
        long inuse = results[0] == null ? 0L : (long) results[0];
        long max = results[1] == null ? 0L : (long) results[1];
        return new Pair<>(inuse, max);
    }
}
