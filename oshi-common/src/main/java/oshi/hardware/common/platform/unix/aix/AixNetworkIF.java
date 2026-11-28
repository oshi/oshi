/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.net.NetworkInterface;
import java.net.SocketException;

import org.jspecify.annotations.Nullable;

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
        public int ifType;
    }

    private volatile int ifType;
    private volatile IfOperStatus ifOperStatus = IfOperStatus.UNKNOWN;

    /**
     * Constructs a new {@code AixNetworkIF}.
     *
     * @param netint the underlying network interface
     * @throws InstantiationException if the instance could not be constructed
     */
    protected AixNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public IfOperStatus getIfOperStatus() {
        return this.ifOperStatus;
    }

    @Override
    public synchronized boolean updateAttributes() {
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
        this.ifType = stats.ifType;
        this.ifOperStatus = queryOperStatus(queryNetworkInterface());
        this.timeStamp = System.currentTimeMillis();
        return true;
    }

    /**
     * Gets an interface's operational status from its flags. perfstat carries none, and AIX's {@code ifconfig} prints
     * no {@code status:} line, so the interface is up when its flags include both {@code UP} and {@code RUNNING}: the
     * test {@link NetworkInterface#isUp()} makes with a single {@code SIOCGIFFLAGS} ioctl, where launching
     * {@code ifconfig} for each interface on every update took about 15 ms.
     *
     * @param netint the interface
     * @return {@link IfOperStatus#UP} or {@link IfOperStatus#DOWN}, or {@link IfOperStatus#UNKNOWN} if the flags could
     *         not be read, as when the interface has gone away
     */
    static IfOperStatus queryOperStatus(NetworkInterface netint) {
        try {
            return netint.isUp() ? IfOperStatus.UP : IfOperStatus.DOWN;
        } catch (SocketException e) {
            return IfOperStatus.UNKNOWN;
        }
    }

    /**
     * Looks up this interface's per-interface stats from the subclass's perfstat data source.
     *
     * @return stats POJO, or {@code null} if no entry was found for this interface name
     */
    protected abstract @Nullable IfStats queryStats();
}
