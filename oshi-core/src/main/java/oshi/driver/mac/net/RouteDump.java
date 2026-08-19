/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;

/**
 * Fetches the kernel's routing table through a {@code NET_RT_DUMP} sysctl.
 */
@ThreadSafe
public final class RouteDump {

    private static final Logger LOG = LoggerFactory.getLogger(RouteDump.class);

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_DUMP = 1;

    private RouteDump() {
    }

    /**
     * Fetches the routing table dump.
     *
     * @return The bytes the kernel returned, or an empty array if the query failed
     */
    public static byte[] queryRouteDump() {
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_DUMP, 0 };
        try (CloseableSizeTByReference len = new CloseableSizeTByReference()) {
            if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, size_t.ZERO)) {
                LOG.error("Didn't get buffer length for NET_RT_DUMP");
                return new byte[0];
            }
            long size = len.longValue();
            if (size <= 0) {
                return new byte[0];
            }
            try (Memory buf = new Memory(size)) {
                if (0 != SystemB.INSTANCE.sysctl(mib, 6, buf, len, null, size_t.ZERO)) {
                    LOG.error("Didn't get buffer for NET_RT_DUMP");
                    return new byte[0];
                }
                // The second call may report fewer bytes than the first reserved
                return buf.getByteArray(0, (int) Math.min(size, len.longValue()));
            }
        }
    }
}
