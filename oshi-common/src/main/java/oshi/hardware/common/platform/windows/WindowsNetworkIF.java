/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.net.NetworkInterface;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Common Windows NetworkIF logic shared between the JNA and FFM implementations. The per-interface statistics are
 * gathered from the IP Helper API by the subclasses (JNA {@code MIB_IF_ROW2}/{@code MIB_IFROW} structures or FFM
 * {@code GetIfEntry2} offsets) into an {@link IfStats} carrier; mapping those onto the interface fields lives here.
 */
@ThreadSafe
public abstract class WindowsNetworkIF extends AbstractNetworkIF {

    /** Bit in the interface flags indicating a connector is present. */
    protected static final byte CONNECTOR_PRESENT_BIT = 0b00000100;

    private volatile int ifType;
    private volatile int ndisPhysicalMediumType;
    private volatile boolean connectorPresent;
    private volatile String ifAlias = "";
    private volatile IfOperStatus ifOperStatus = IfOperStatus.UNKNOWN;

    /**
     * Creates a WindowsNetworkIF.
     *
     * @param netint the network interface
     * @throws InstantiationException if the interface cannot be instantiated
     */
    protected WindowsNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    /** Interface statistics gathered by a backend from the IP Helper API. */
    public static final class IfStats {
        public int ifType;
        public int ndisPhysicalMediumType;
        public boolean connectorPresent;
        public long bytesSent;
        public long bytesRecv;
        public long packetsSent;
        public long packetsRecv;
        public long outErrors;
        public long inErrors;
        public long collisions;
        public long inDrops;
        public long speed;
        public String ifAlias = "";
        public IfOperStatus ifOperStatus = IfOperStatus.UNKNOWN;
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public int getNdisPhysicalMediumType() {
        return this.ndisPhysicalMediumType;
    }

    @Override
    public boolean isConnectorPresent() {
        return this.connectorPresent;
    }

    @Override
    public String getIfAlias() {
        return ifAlias;
    }

    @Override
    public IfOperStatus getIfOperStatus() {
        return ifOperStatus;
    }

    @Override
    public boolean updateAttributes() {
        IfStats stats = queryStats();
        if (stats == null) {
            return false;
        }
        this.ifType = stats.ifType;
        this.ndisPhysicalMediumType = stats.ndisPhysicalMediumType;
        this.connectorPresent = stats.connectorPresent;
        this.bytesSent = stats.bytesSent;
        this.bytesRecv = stats.bytesRecv;
        this.packetsSent = stats.packetsSent;
        this.packetsRecv = stats.packetsRecv;
        this.outErrors = stats.outErrors;
        this.inErrors = stats.inErrors;
        this.collisions = stats.collisions;
        this.inDrops = stats.inDrops;
        this.speed = stats.speed;
        this.ifAlias = stats.ifAlias;
        this.ifOperStatus = stats.ifOperStatus;
        this.timeStamp = System.currentTimeMillis();
        return true;
    }

    /**
     * Queries this interface's statistics from the native (JNA or FFM) IP Helper API.
     *
     * @return the stats, or {@code null} if the query failed
     */
    protected abstract IfStats queryStats();
}
