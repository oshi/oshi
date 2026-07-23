/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_CPTIME;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_CPTIME2;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdCentralProcessor;
import oshi.util.LogLevel;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;

/**
 * OpenBSD Central Processor implementation, backed by FFM sysctl and native library calls.
 */
@ThreadSafe
public class OpenBsdCentralProcessorFFM extends OpenBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdCentralProcessorFFM.class);

    @Override
    protected int sysctl(String name, int def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected String sysctl(String name, String def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected String sysctl(int[] mib, String def) {
        return OpenBsdSysctlUtilFFM.sysctl(mib, def);
    }

    @Override
    protected int sysctl(int[] mib, int def) {
        return OpenBsdSysctlUtilFFM.sysctl(mib, def);
    }

    @Override
    protected long[] querySystemCpTime() {
        return readCpTime(new int[] { CTL_KERN, KERN_CPTIME });
    }

    @Override
    protected long[] queryProcessorCpTime(int cpu) {
        return readCpTime(new int[] { CTL_KERN, KERN_CPTIME2, cpu });
    }

    private static long[] readCpTime(int[] mib) {
        MemorySegment buf = OpenBsdSysctlUtilFFM.sysctl(mib);
        if (buf == null) {
            return new long[0];
        }
        int arraySize = (int) (buf.byteSize() / JAVA_LONG.byteSize());
        long[] result = new long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            result[i] = buf.getAtIndex(JAVA_LONG, i);
        }
        return result;
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment avg = arena.allocate(JAVA_DOUBLE, nelem);
            int retval = OpenBsdLibcFunctions.getloadavg(avg, nelem);
            for (int i = 0; i < nelem && i < retval; i++) {
                loadavg[i] = avg.getAtIndex(JAVA_DOUBLE, i);
            }
            return retval;
        }, LOG, LogLevel.WARN, "Failed to read load average", 0);
    }
}
