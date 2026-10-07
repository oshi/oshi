/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;

import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.platform.unix.solaris.Kstat2;
import com.sun.jna.platform.unix.solaris.Kstat2.Kstat2Handle;
import com.sun.jna.platform.unix.solaris.Kstat2.Kstat2Map;
import com.sun.jna.platform.unix.solaris.Kstat2StatusException;
import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.Who;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.GlobalConfig;
import oshi.util.Memoizer;
import oshi.util.NativeLibraryUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * JNA-backed Solaris OperatingSystem. Uses Kstat2 (Solaris 11.4+) where available, falling back to the legacy
 * {@code kstat} chain.
 */
@ThreadSafe
public class SolarisOperatingSystemJNA extends SolarisOperatingSystem {

    private static final boolean ALLOW_KSTAT2 = GlobalConfig.get(GlobalConfig.OSHI_OS_SOLARIS_ALLOWKSTAT2, true);

    /**
     * This static field identifies if the kstat2 library (available in Solaris 11.4 or greater) can be loaded and
     * returns valid data.
     */
    public static final boolean HAS_KSTAT2;
    static {
        boolean kstat2Available = false;
        // illumos and Solaris < 11.4 have no libkstat2, and a failed load there can crash the JVM; see
        // NativeLibraryUtil.
        try {
            if (ALLOW_KSTAT2 && NativeLibraryUtil.isSafeToLoad(System.mapLibraryName("kstat2"))) {
                Kstat2 lib = Kstat2.INSTANCE;
                if (lib != null) {
                    // Validate kstat2 returns data with a universal kstat path
                    Kstat2Handle handle = new Kstat2Handle();
                    try {
                        Kstat2Map map = handle.lookupMap("kstat:/pages/unix/system_pages");
                        kstat2Available = map != null && map.getValue("physmem") != null;
                    } catch (Kstat2StatusException e) {
                        // kstat2 loaded but can't read data (e.g., LDOM restrictions)
                    } finally {
                        handle.close();
                    }
                }
            }
        } catch (UnsatisfiedLinkError | Kstat2StatusException e) {
            // 11.3 or earlier, no kstat2
        }
        HAS_KSTAT2 = kstat2Available;
    }

    private static final long BOOTTIME = querySystemBootTime();

    private final Supplier<Long> uptimeSupplier = Memoizer.memoize(SolarisOperatingSystemJNA::queryUptime,
            defaultExpiration());

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystemJNA();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    protected OSProcess createProcess(int pid) {
        return new SolarisOSProcessJNA(pid, this);
    }

    @Override
    public int getProcessId() {
        return SolarisLibc.INSTANCE.getpid();
    }

    @Override
    public int getThreadId() {
        return SolarisLibc.INSTANCE.thr_self();
    }

    @Override
    public OSThread getCurrentThread() {
        return new SolarisOSThreadJNA(getProcessId(), getThreadId());
    }

    @Override
    public long getSystemUptime() {
        return uptimeSupplier.get();
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        if (HAS_KSTAT2) {
            Object[] results = KstatUtil.queryKstat2("kstat:/misc/unix/system_misc", "boot_time");
            if (results[0] != null) {
                return (long) results[0];
            }
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp != null && kc.read(ksp)) {
                return KstatUtil.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L;
    }

    private static long queryUptime() {
        if (HAS_KSTAT2) {
            Object[] results = KstatUtil.queryKstat2("kstat:/misc/unix/system_misc", "snaptime");
            if (results[0] != null) {
                // Snap Time is in nanoseconds; divide for seconds
                return (long) results[0] / 1_000_000_000L;
            }
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp != null && kc.read(ksp)) {
                // Snap Time is in nanoseconds; divide for seconds
                return ksp.ks_snaptime / 1_000_000_000L;
            }
        }
        return 0L;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParamsJNA();
    }
}
