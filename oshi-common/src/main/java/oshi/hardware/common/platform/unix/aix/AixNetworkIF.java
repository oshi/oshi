/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.net.NetworkInterface;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Abstract base for AIX NetworkIF. {@link #updateAttributes()} populates fields from a per-interface stats POJO that
 * subclasses fetch from their respective perfstat driver.
 */
@ThreadSafe
public abstract class AixNetworkIF extends AbstractNetworkIF {

    /** Per-interface stats fields the AIX HAL reads. JNA and FFM concrete subclasses both fill this. */
    public static final class IfStats {
        public long bytesSent;
        public long bytesRecv;
        public long packetsSent;
        public long packetsRecv;
        public long outErrors;
        public long inErrors;
        public long collisions;
        public long inDrops;
        public long speed;
    }

    protected AixNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
    }

    @Override
    public boolean updateAttributes() {
        IfStats stats = queryStats();
        if (stats == null) {
            return false;
        }
        setBytesSent(stats.bytesSent);
        setBytesRecv(stats.bytesRecv);
        setPacketsSent(stats.packetsSent);
        setPacketsRecv(stats.packetsRecv);
        setOutErrors(stats.outErrors);
        setInErrors(stats.inErrors);
        setCollisions(stats.collisions);
        setInDrops(stats.inDrops);
        setSpeed(stats.speed);
        setTimeStamp(System.currentTimeMillis());
        return true;
    }

    /**
     * Looks up this interface's per-interface stats from the subclass's perfstat data source.
     *
     * @return stats POJO, or {@code null} if no entry was found for this interface name
     */
    protected abstract IfStats queryStats();
}
