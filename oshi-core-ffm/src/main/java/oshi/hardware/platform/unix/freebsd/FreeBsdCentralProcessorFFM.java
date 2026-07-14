/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor;

/**
 * FFM-backed FreeBSD central processor.
 */
@ThreadSafe
public class FreeBsdCentralProcessorFFM extends FreeBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessorFFM.class);

    @Override
    protected String sysctlString(String name, String def) {
        return BsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    protected long sysctlLong(String name, long def) {
        return BsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    protected int sysctlInt(String name, int def) {
        return BsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    protected long[] queryCpTimes(String name) {
        MemorySegment buf = BsdSysctlUtilFFM.sysctl(name);
        if (buf == null) {
            return null;
        }
        int count = (int) (buf.byteSize() / Long.BYTES);
        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            result[i] = buf.getAtIndex(JAVA_LONG, i);
        }
        return result;
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment avg = arena.allocate(JAVA_DOUBLE, nelem);
            int retval = FreeBsdLibcFunctions.getloadavg(avg, nelem);
            for (int i = 0; i < nelem && i < retval; i++) {
                loadavg[i] = avg.getAtIndex(JAVA_DOUBLE, i);
            }
            return retval;
        }, LOG, Level.WARN, "Failed to read load average", 0);
    }
}
