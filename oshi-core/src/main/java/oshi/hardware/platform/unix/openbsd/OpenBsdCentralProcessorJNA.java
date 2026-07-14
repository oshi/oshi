/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.jna.platform.unix.OpenBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_CPTIME;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_CPTIME2;

import com.sun.jna.Memory;
import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdCentralProcessor;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * OpenBSD Central Processor implementation, backed by JNA sysctl and native library calls.
 */
@ThreadSafe
public class OpenBsdCentralProcessorJNA extends OpenBsdCentralProcessor {

    @Override
    protected int sysctl(String name, int def) {
        return OpenBsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected String sysctl(String name, String def) {
        return OpenBsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected String sysctl(int[] mib, String def) {
        return OpenBsdSysctlUtil.sysctl(mib, def);
    }

    @Override
    protected int sysctl(int[] mib, int def) {
        return OpenBsdSysctlUtil.sysctl(mib, def);
    }

    @Override
    protected long[] querySystemCpTime() {
        int[] mib = { CTL_KERN, KERN_CPTIME };
        try (Memory m = OpenBsdSysctlUtil.sysctl(mib)) {
            return cpTimeToTicks(m, false);
        }
    }

    @Override
    protected long[] queryProcessorCpTime(int cpu) {
        int[] mib = { CTL_KERN, KERN_CPTIME2, cpu };
        try (Memory m = OpenBsdSysctlUtil.sysctl(mib)) {
            return cpTimeToTicks(m, true);
        }
    }

    /**
     * Parse memory buffer returned from sysctl kern.cptime or kern.cptime2 to an array of 5 or 6 longs depending on
     * version.
     * <p>
     * Versions 6.4 and later have a 6-element array while earlier versions have only 5 elements. Additionally
     * kern.cptime uses a native-sized long (32- or 64-bit) value while kern.cptime2 is always a 64-bit value.
     *
     * @param m          A buffer containing the array.
     * @param force64bit True if the buffer is filled with 64-bit longs, false if native long sized values
     * @return The array
     */
    private static long[] cpTimeToTicks(Memory m, boolean force64bit) {
        long longBytes = force64bit ? 8L : Native.LONG_SIZE;
        int arraySize = m == null ? 0 : (int) (m.size() / longBytes);
        if (force64bit && m != null) {
            return m.getLongArray(0, arraySize);
        }
        long[] ticks = new long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            ticks[i] = m.getNativeLong(i * longBytes).longValue();
        }
        return ticks;
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return OpenBsdLibc.INSTANCE.getloadavg(loadavg, nelem);
    }
}
