/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.freebsd.FreeBsdInternetProtocolStats;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class FreeBsdInternetProtocolStatsJNA extends FreeBsdInternetProtocolStats {

    @Override
    protected byte[] querySysctl(String name) {
        try (Memory m = BsdSysctlUtil.sysctl(name)) {
            return m == null ? new byte[0] : m.getByteArray(0, (int) m.size());
        }
    }
}
