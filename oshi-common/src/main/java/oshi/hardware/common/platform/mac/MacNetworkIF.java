/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.net.NetworkInterface;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * Base class for macOS NetworkIF implementations. Subclasses provide platform-specific display name resolution and
 * network statistics updates.
 */
@ThreadSafe
public abstract class MacNetworkIF extends AbstractNetworkIF {

    private int ifType;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long inErrors;
    private long outErrors;
    private long inDrops;
    private long collisions;
    private long speed;
    private long timeStamp;

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
    public long getBytesRecv() {
        return this.bytesRecv;
    }

    @Override
    public long getBytesSent() {
        return this.bytesSent;
    }

    @Override
    public long getPacketsRecv() {
        return this.packetsRecv;
    }

    @Override
    public long getPacketsSent() {
        return this.packetsSent;
    }

    @Override
    public long getInErrors() {
        return this.inErrors;
    }

    @Override
    public long getOutErrors() {
        return this.outErrors;
    }

    @Override
    public long getInDrops() {
        return this.inDrops;
    }

    @Override
    public long getCollisions() {
        return this.collisions;
    }

    @Override
    public long getSpeed() {
        return this.speed;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * Sets the interface type.
     *
     * @param ifType the interface type to set
     */
    protected void setIfType(int ifType) {
        this.ifType = ifType;
    }

    /**
     * Sets the bytes received.
     *
     * @param bytesRecv the bytes received to set
     */
    protected void setBytesRecv(long bytesRecv) {
        this.bytesRecv = bytesRecv;
    }

    /**
     * Sets the bytes sent.
     *
     * @param bytesSent the bytes sent to set
     */
    protected void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    /**
     * Sets the packets received.
     *
     * @param packetsRecv the packets received to set
     */
    protected void setPacketsRecv(long packetsRecv) {
        this.packetsRecv = packetsRecv;
    }

    /**
     * Sets the packets sent.
     *
     * @param packetsSent the packets sent to set
     */
    protected void setPacketsSent(long packetsSent) {
        this.packetsSent = packetsSent;
    }

    /**
     * Sets the input errors.
     *
     * @param inErrors the input errors to set
     */
    protected void setInErrors(long inErrors) {
        this.inErrors = inErrors;
    }

    /**
     * Sets the output errors.
     *
     * @param outErrors the output errors to set
     */
    protected void setOutErrors(long outErrors) {
        this.outErrors = outErrors;
    }

    /**
     * Sets the input drops.
     *
     * @param inDrops the input drops to set
     */
    protected void setInDrops(long inDrops) {
        this.inDrops = inDrops;
    }

    /**
     * Sets the collisions.
     *
     * @param collisions the collisions to set
     */
    protected void setCollisions(long collisions) {
        this.collisions = collisions;
    }

    /**
     * Sets the speed.
     *
     * @param speed the speed to set
     */
    protected void setSpeed(long speed) {
        this.speed = speed;
    }

    /**
     * Sets the timestamp.
     *
     * @param timeStamp the timestamp to set
     */
    protected void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
