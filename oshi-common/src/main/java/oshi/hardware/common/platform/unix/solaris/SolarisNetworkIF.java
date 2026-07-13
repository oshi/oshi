/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.net.NetworkInterface;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Abstract base for Solaris NetworkIF. The interface enumeration ({@link #getNetworks}) is shared; the {@code kstat}
 * per-interface stats are native and fetched by the JNA and FFM subclasses via {@link #queryStats()}.
 */
@ThreadSafe
public abstract class SolarisNetworkIF extends AbstractNetworkIF {

    /** Per-interface stats the Solaris HAL reads. Both JNA and FFM concrete subclasses populate this. */
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
        public long timeStamp;
    }

    protected SolarisNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
    }

    @Override
    public boolean updateAttributes() {
        // Initialize to a sane default value
        this.timeStamp = System.currentTimeMillis();
        IfStats stats = queryStats();
        if (stats == null) {
            return false;
        }
        this.bytesSent = stats.bytesSent;
        this.bytesRecv = stats.bytesRecv;
        this.packetsSent = stats.packetsSent;
        this.packetsRecv = stats.packetsRecv;
        this.outErrors = stats.outErrors;
        this.inErrors = stats.inErrors;
        this.collisions = stats.collisions;
        this.inDrops = stats.inDrops;
        this.speed = stats.speed;
        this.timeStamp = stats.timeStamp;
        return true;
    }

    /**
     * Looks up this interface's stats from the subclass's kstat data source.
     *
     * @return stats POJO, or {@code null} if no entry was found for this interface name
     */
    protected abstract IfStats queryStats();
}
