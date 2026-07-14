/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A CPU
 */
@ThreadSafe
public class FreeBsdCentralProcessorJNA extends FreeBsdCentralProcessor {

    @Override
    protected String sysctlString(String name, String def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected long sysctlLong(String name, long def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected int sysctlInt(String name, int def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected long[] queryCpTimes(String name) {
        try (Memory p = BsdSysctlUtil.sysctl(name)) {
            if (p == null) {
                return null;
            }
            int count = (int) (p.size() / Long.BYTES);
            long[] result = new long[count];
            for (int i = 0; i < count; i++) {
                result[i] = p.getLong((long) i * Long.BYTES);
            }
            return result;
        }
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return FreeBsdLibc.INSTANCE.getloadavg(loadavg, nelem);
    }
}
