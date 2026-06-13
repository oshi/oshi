/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import oshi.annotation.concurrent.Immutable;

/**
 * Encapsulates network interface statistics read from the macOS {@code NET_RT_IFLIST2} sysctl, shared between the JNA
 * and FFM NetStat drivers.
 */
@Immutable
public class IFdata {
    private final int ifType;
    private final long oPackets;
    private final long iPackets;
    private final long oBytes;
    private final long iBytes;
    private final long oErrors;
    private final long iErrors;
    private final long collisions;
    private final long iDrops;
    private final long speed;
    private final long timeStamp;

    /**
     * Creates an immutable interface-data record. The {@code long} counters are masked to 32 bits because the
     * underlying {@code if_data64} fields wrap at {@code 2^32}.
     *
     * @param ifType     the interface type
     * @param oPackets   packets sent
     * @param iPackets   packets received
     * @param oBytes     bytes sent
     * @param iBytes     bytes received
     * @param oErrors    output errors
     * @param iErrors    input errors
     * @param collisions collisions
     * @param iDrops     input drops
     * @param speed      interface speed (baud rate)
     * @param timeStamp  the time the data was sampled, in milliseconds since the epoch
     */
    public IFdata(int ifType, // NOSONAR squid:S00107
            long oPackets, long iPackets, long oBytes, long iBytes, long oErrors, long iErrors, long collisions,
            long iDrops, long speed, long timeStamp) {
        this.ifType = ifType;
        this.oPackets = oPackets & 0xffffffffL;
        this.iPackets = iPackets & 0xffffffffL;
        this.oBytes = oBytes & 0xffffffffL;
        this.iBytes = iBytes & 0xffffffffL;
        this.oErrors = oErrors & 0xffffffffL;
        this.iErrors = iErrors & 0xffffffffL;
        this.collisions = collisions & 0xffffffffL;
        this.iDrops = iDrops & 0xffffffffL;
        this.speed = speed & 0xffffffffL;
        this.timeStamp = timeStamp;
    }

    /**
     * @return the ifType
     */
    public int getIfType() {
        return ifType;
    }

    /**
     * @return the oPackets
     */
    public long getOPackets() {
        return oPackets;
    }

    /**
     * @return the iPackets
     */
    public long getIPackets() {
        return iPackets;
    }

    /**
     * @return the oBytes
     */
    public long getOBytes() {
        return oBytes;
    }

    /**
     * @return the iBytes
     */
    public long getIBytes() {
        return iBytes;
    }

    /**
     * @return the oErrors
     */
    public long getOErrors() {
        return oErrors;
    }

    /**
     * @return the iErrors
     */
    public long getIErrors() {
        return iErrors;
    }

    /**
     * @return the collisions
     */
    public long getCollisions() {
        return collisions;
    }

    /**
     * @return the iDrops
     */
    public long getIDrops() {
        return iDrops;
    }

    /**
     * @return the speed
     */
    public long getSpeed() {
        return speed;
    }

    /**
     * @return the timeStamp
     */
    public long getTimeStamp() {
        return timeStamp;
    }
}
