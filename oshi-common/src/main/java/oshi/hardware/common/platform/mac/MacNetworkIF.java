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

    protected void setIfType(int ifType) {
        this.ifType = ifType;
    }

    protected void setBytesRecv(long bytesRecv) {
        this.bytesRecv = bytesRecv;
    }

    protected void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    protected void setPacketsRecv(long packetsRecv) {
        this.packetsRecv = packetsRecv;
    }

    protected void setPacketsSent(long packetsSent) {
        this.packetsSent = packetsSent;
    }

    protected void setInErrors(long inErrors) {
        this.inErrors = inErrors;
    }

    protected void setOutErrors(long outErrors) {
        this.outErrors = outErrors;
    }

    protected void setInDrops(long inDrops) {
        this.inDrops = inDrops;
    }

    protected void setCollisions(long collisions) {
        this.collisions = collisions;
    }

    protected void setSpeed(long speed) {
        this.speed = speed;
    }

    protected void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
