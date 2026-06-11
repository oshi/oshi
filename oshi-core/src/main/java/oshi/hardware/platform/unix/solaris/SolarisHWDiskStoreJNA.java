/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.software.os.unix.solaris.SolarisOperatingSystemJNA.HAS_KSTAT2;

import java.util.List;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;
import com.sun.jna.platform.unix.solaris.LibKstat.KstatIO;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.solaris.SolarisHWDiskStore;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * JNA-backed Solaris HWDiskStore. Uses Kstat2 where available, falling back to the legacy {@code kstat} chain.
 */
@ThreadSafe
public final class SolarisHWDiskStoreJNA extends SolarisHWDiskStore {

    private SolarisHWDiskStoreJNA(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return getDisks(SolarisHWDiskStoreJNA::new);
    }

    @Override
    protected DiskStats queryStats() {
        if (HAS_KSTAT2) {
            return queryStats2();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup(null, 0, getName());
            if (ksp != null && kc.read(ksp)) {
                KstatIO data = new KstatIO(ksp.ks_data);
                DiskStats stats = new DiskStats();
                stats.reads = data.reads;
                stats.writes = data.writes;
                stats.readBytes = data.nread;
                stats.writeBytes = data.nwritten;
                stats.currentQueueLength = (long) data.wcnt + data.rcnt;
                stats.transferTime = data.rtime / 1_000_000L;
                stats.timeStamp = ksp.ks_snaptime / 1_000_000L;
                return stats;
            }
        }
        return null;
    }

    private DiskStats queryStats2() {
        String fullName = getName();
        String alpha = fullName;
        String numeric = "";
        for (int c = 0; c < fullName.length(); c++) {
            if (fullName.charAt(c) >= '0' && fullName.charAt(c) <= '9') {
                alpha = fullName.substring(0, c);
                numeric = fullName.substring(c);
                break;
            }
        }
        Object[] results = KstatUtil.queryKstat2("kstat:/disk/" + alpha + "/" + getName() + "/0", "reads", "writes",
                "nread", "nwritten", "wcnt", "rcnt", "rtime", "snaptime");
        if (results[results.length - 1] == null) {
            results = KstatUtil.queryKstat2("kstat:/disk/" + alpha + "/" + numeric + "/io", "reads", "writes", "nread",
                    "nwritten", "wcnt", "rcnt", "rtime", "snaptime");
        }
        if (results[results.length - 1] == null) {
            return null;
        }
        DiskStats stats = new DiskStats();
        stats.reads = results[0] == null ? 0L : (long) results[0];
        stats.writes = results[1] == null ? 0L : (long) results[1];
        stats.readBytes = results[2] == null ? 0L : (long) results[2];
        stats.writeBytes = results[3] == null ? 0L : (long) results[3];
        long queueLen = results[4] == null ? 0L : (long) results[4];
        queueLen += results[5] == null ? 0L : (long) results[5];
        stats.currentQueueLength = queueLen;
        stats.transferTime = results[6] == null ? 0L : (long) results[6] / 1_000_000L;
        stats.timeStamp = (long) results[7] / 1_000_000L;
        return stats;
    }
}
