/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.netbsd;

import static oshi.jna.platform.unix.NetBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.NetBsdLibc.KERN_CP_TIME;

import java.util.Arrays;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.netbsd.NetBsdCentralProcessor;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * A CentralProcessor for NetBSD that uses JNA native sysctl calls when the JNA native library is available, falling
 * back to the command-line {@link NetBsdCentralProcessor} implementation otherwise.
 */
@ThreadSafe
public class NetBsdCentralProcessorJNA extends NetBsdCentralProcessor {

    @Override
    protected long[] queryCpTime(int cpu) {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.queryCpTime(cpu);
        }
        // On NetBSD, kern.cp_time with a third MIB element gives per-CPU data
        int[] mib = cpu < 0 ? new int[] { CTL_KERN, KERN_CP_TIME } : new int[] { CTL_KERN, KERN_CP_TIME, cpu };
        try (Memory m = NetBsdSysctlUtil.sysctl(mib)) {
            if (m == null) {
                return new long[0];
            }
            return m.getLongArray(0, (int) (m.size() / Long.BYTES));
        }
    }

    @Override
    protected double[] queryLoadAverage(int nelem) {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.queryLoadAverage(nelem);
        }
        double[] average = new double[nelem];
        int retval = NetBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }
}
