/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.netbsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions.KERN_CP_TIME;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.netbsd.NetBsdLibcFunctions;
import oshi.ffm.util.platform.unix.netbsd.NetBsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.netbsd.NetBsdCentralProcessor;

/**
 * A CentralProcessor for NetBSD that uses FFM native sysctl calls when FFM native access is available, falling back to
 * the command-line {@link NetBsdCentralProcessor} implementation otherwise.
 */
@ThreadSafe
public class NetBsdCentralProcessorFFM extends NetBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(NetBsdCentralProcessorFFM.class);

    @Override
    protected long[] queryCpTime(int cpu) {
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE) {
            return super.queryCpTime(cpu);
        }
        // On NetBSD, kern.cp_time with a third MIB element gives per-CPU data
        int[] mib = cpu < 0 ? new int[] { CTL_KERN, KERN_CP_TIME } : new int[] { CTL_KERN, KERN_CP_TIME, cpu };
        MemorySegment m = NetBsdSysctlUtilFFM.sysctl(mib);
        return m == null ? new long[0] : m.asSlice(0, m.byteSize() / Long.BYTES * Long.BYTES).toArray(JAVA_LONG);
    }

    @Override
    protected double[] queryLoadAverage(int nelem) {
        if (!NetBsdSysctlUtilFFM.FFM_AVAILABLE) {
            return super.queryLoadAverage(nelem);
        }
        double[] failed = new double[nelem];
        Arrays.fill(failed, -1d);
        return callInArenaOrDefault(arena -> {
            MemorySegment loadavg = arena.allocate(JAVA_DOUBLE, nelem);
            if (NetBsdLibcFunctions.getloadavg(loadavg, nelem) < nelem) {
                return failed;
            }
            return loadavg.toArray(JAVA_DOUBLE);
        }, LOG, WARN, "Failed getloadavg", failed);
    }
}
