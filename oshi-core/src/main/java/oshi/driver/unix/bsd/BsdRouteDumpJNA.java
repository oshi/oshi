/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.bsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.CLibrary;

/**
 * Fetches the kernel's routing table through a {@code NET_RT_DUMP} sysctl. Shared by the BSDs, which take the same
 * call; only the message layout inside the buffer differs between them.
 */
@ThreadSafe
public final class BsdRouteDumpJNA {

    private static final Logger LOG = LoggerFactory.getLogger(BsdRouteDumpJNA.class);

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_DUMP = 1;

    private BsdRouteDumpJNA() {
    }

    /**
     * Fetches the routing table dump.
     *
     * @param libc This platform's C library
     * @return The bytes the kernel returned, or an empty array if the query failed
     */
    public static byte[] queryRouteDump(CLibrary libc) {
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_DUMP, 0 };
        try (CloseableSizeTByReference len = new CloseableSizeTByReference()) {
            if (0 != libc.sysctl(mib, 6, null, len, null, new size_t(0))) {
                LOG.error("Didn't get buffer length for NET_RT_DUMP");
                return new byte[0];
            }
            long size = len.longValue();
            if (size <= 0) {
                return new byte[0];
            }
            try (Memory buf = new Memory(size)) {
                if (0 != libc.sysctl(mib, 6, buf, len, null, new size_t(0))) {
                    LOG.error("Didn't get buffer for NET_RT_DUMP");
                    return new byte[0];
                }
                // The second call may report fewer bytes than the first reserved
                return buf.getByteArray(0, (int) Math.min(size, len.longValue()));
            }
        }
    }
}
