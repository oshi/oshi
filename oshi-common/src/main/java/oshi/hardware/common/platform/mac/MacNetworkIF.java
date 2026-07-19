/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.net.NetworkInterface;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.mac.IFdata;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Base class for macOS NetworkIF implementations. The per-interface statistics come from a shared {@link IFdata} map;
 * subclasses provide the native display-name resolution and the {@code IFdata} query (JNA or FFM).
 */
@ThreadSafe
public abstract class MacNetworkIF extends AbstractNetworkIF {

    private volatile int ifType;

    /**
     * Creates a MacNetworkIF.
     *
     * @param netint      the network interface
     * @param displayName the display name
     * @throws InstantiationException if the interface cannot be instantiated
     */
    protected MacNetworkIF(NetworkInterface netint, String displayName) throws InstantiationException {
        super(netint, displayName);
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public boolean updateAttributes() {
        return updateNetworkStats(queryIFdata(queryNetworkInterface().getIndex()));
    }

    /**
     * Populates this interface's statistics from the given {@link IFdata} map.
     *
     * @param data map of interface index to {@link IFdata}
     * @return {@code true} if this interface's index was present in the map
     */
    protected boolean updateNetworkStats(Map<Integer, IFdata> data) {
        int index = queryNetworkInterface().getIndex();
        if (data.containsKey(index)) {
            IFdata ifData = data.get(index);
            this.ifType = ifData.getIfType();
            this.bytesSent = ifData.getOBytes();
            this.bytesRecv = ifData.getIBytes();
            this.packetsSent = ifData.getOPackets();
            this.packetsRecv = ifData.getIPackets();
            this.outErrors = ifData.getOErrors();
            this.inErrors = ifData.getIErrors();
            this.collisions = ifData.getCollisions();
            this.inDrops = ifData.getIDrops();
            this.speed = ifData.getSpeed();
            this.timeStamp = ifData.getTimeStamp();
            return true;
        }
        return false;
    }

    /**
     * Queries interface statistics via the platform-specific (JNA or FFM) {@code getifaddrs} backend.
     *
     * @param index the interface index, or {@code -1} for all interfaces
     * @return map of interface index to {@link IFdata}
     */
    protected abstract Map<Integer, IFdata> queryIFdata(int index);
}
