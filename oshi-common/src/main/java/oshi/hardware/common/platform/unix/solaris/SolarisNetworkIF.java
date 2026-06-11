/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Abstract base for Solaris NetworkIF. The interface enumeration ({@link #getNetworks}) is shared; the {@code kstat}
 * per-interface stats are native and fetched by the JNA and FFM subclasses via {@link #queryStats()}.
 */
@ThreadSafe
public abstract class SolarisNetworkIF extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisNetworkIF.class);

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

    /** Creates a concrete (JNA or FFM) Solaris network interface. */
    @FunctionalInterface
    protected interface NetworkIFFactory {
        SolarisNetworkIF create(NetworkInterface netint) throws InstantiationException;
    }

    protected SolarisNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
    }

    @Override
    public boolean updateAttributes() {
        // Initialize to a sane default value
        setTimeStamp(System.currentTimeMillis());
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
        setTimeStamp(stats.timeStamp);
        return true;
    }

    /**
     * Gets all network interfaces on this machine, building each with the given concrete factory.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @param factory                creates the platform-specific interface instances
     * @return a list of {@link NetworkIF} objects representing the interfaces
     */
    protected static List<NetworkIF> getNetworks(boolean includeLocalInterfaces, NetworkIFFactory factory) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(factory.create(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    /**
     * Looks up this interface's stats from the subclass's kstat data source.
     *
     * @return stats POJO, or {@code null} if no entry was found for this interface name
     */
    protected abstract IfStats queryStats();
}
