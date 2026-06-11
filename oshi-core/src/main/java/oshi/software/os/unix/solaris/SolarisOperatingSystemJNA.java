/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import static oshi.util.Memoizer.defaultExpiration;

import java.io.File;
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
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Pair;

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
        // Check the library file's existence on disk before letting JNA Native.load attempt
        // to dlopen it. On illumos / Solaris < 11.4 the file simply doesn't exist, and on
        // JDK 25 + JNA the failed Native.load has been seen to SIGSEGV in libc strlen
        // instead of throwing UnsatisfiedLinkError cleanly.
        try {
            if (ALLOW_KSTAT2 && libkstat2Present()) {
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

    /**
     * Returns {@code true} if any {@code libkstat2.so*} file is present on the standard Solaris/illumos library search
     * paths. Solaris 11.4+ ships it (currently as {@code libkstat2.so.1}); illumos and Solaris &lt; 11.4 do not.
     * Matches any suffix to survive future SONAME bumps.
     *
     * @return whether a libkstat2 shared object exists on disk
     */
    private static boolean libkstat2Present() {
        for (String dir : new String[] { "/lib/64", "/usr/lib/64", "/lib", "/usr/lib" }) {
            String[] hits = new File(dir).list((d, name) -> name.startsWith("libkstat2.so"));
            if (hits != null && hits.length > 0) {
                return true;
            }
        }
        return false;
    }

    private static final Supplier<Pair<Long, Long>> BOOT_UPTIME = Memoizer
            .memoize(SolarisOperatingSystemJNA::queryBootAndUptime, defaultExpiration());

    private static final long BOOTTIME = querySystemBootTime();

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
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return BOOT_UPTIME.get().getB();
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
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return BOOT_UPTIME.get().getA();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp != null && kc.read(ksp)) {
                return KstatUtil.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L - querySystemUptime();
    }

    private static Pair<Long, Long> queryBootAndUptime() {
        Object[] results = KstatUtil.queryKstat2("/misc/unix/system_misc", "boot_time", "snaptime");

        long boot = results[0] == null ? System.currentTimeMillis() : (long) results[0];
        // Snap Time is in nanoseconds; divide for seconds
        long snap = results[1] == null ? 0L : (long) results[1] / 1_000_000_000L;

        return new Pair<>(boot, snap);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParamsJNA();
    }
}
