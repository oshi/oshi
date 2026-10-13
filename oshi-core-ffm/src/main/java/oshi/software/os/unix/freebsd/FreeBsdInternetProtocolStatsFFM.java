/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.freebsd.FreeBsdInternetProtocolStats;

/**
 * FFM-backed FreeBSD Internet Protocol stats. Reads the {@code net.inet.tcp.stats} and {@code net.inet.udp.stats}
 * sysctls as raw bytes for the shared parser in {@link FreeBsdInternetProtocolStats}.
 */
@ThreadSafe
public class FreeBsdInternetProtocolStatsFFM extends FreeBsdInternetProtocolStats {

    @Override
    protected byte[] querySysctl(String name) {
        MemorySegment m = BsdSysctlUtilFFM.sysctl(name);
        return m == null ? new byte[0] : m.toArray(JAVA_BYTE);
    }
}
