/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.net;

import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.util.LogLevel.ERROR;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.MacSystemFunctions;

/**
 * Fetches the kernel's routing table through a {@code NET_RT_DUMP} sysctl.
 */
@ThreadSafe
public final class RouteDumpFFM {

    private static final Logger LOG = LoggerFactory.getLogger(RouteDumpFFM.class);

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_DUMP = 1;

    private RouteDumpFFM() {
    }

    /**
     * Fetches the routing table dump.
     *
     * @return The bytes the kernel returned, or an empty array if the query failed
     */
    public static byte[] queryRouteDump() {
        return callInArenaOrDefault(arena -> {
            int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_DUMP, 0 };
            MemorySegment mibSeg = arena.allocate(ValueLayout.JAVA_INT, mib.length);
            for (int i = 0; i < mib.length; i++) {
                mibSeg.setAtIndex(ValueLayout.JAVA_INT, i, mib[i]);
            }
            MemorySegment lenSeg = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            if (0 != MacSystemFunctions.sysctl(callState, mibSeg, mib.length, MemorySegment.NULL, lenSeg,
                    MemorySegment.NULL, 0)) {
                LOG.error("Didn't get buffer length for NET_RT_DUMP");
                return new byte[0];
            }
            long len = lenSeg.get(ValueLayout.JAVA_LONG, 0);
            if (len <= 0) {
                return new byte[0];
            }
            MemorySegment buf = arena.allocate(len);
            if (0 != MacSystemFunctions.sysctl(callState, mibSeg, mib.length, buf, lenSeg, MemorySegment.NULL, 0)) {
                LOG.error("Didn't get buffer for NET_RT_DUMP");
                return new byte[0];
            }
            // The second call may report fewer bytes than the first reserved
            long actual = Math.min(len, lenSeg.get(ValueLayout.JAVA_LONG, 0));
            return buf.asSlice(0, actual).toArray(ValueLayout.JAVA_BYTE);
        }, LOG, ERROR, "Failed to read the routing table", new byte[0]);
    }
}
