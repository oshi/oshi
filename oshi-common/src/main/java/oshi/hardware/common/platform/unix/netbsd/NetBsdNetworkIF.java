/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import java.net.NetworkInterface;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * NetBSD-specific NetworkIF implementation. NetBSD's netstat output format differs from FreeBSD/OpenBSD.
 */
@ThreadSafe
public final class NetBsdNetworkIF extends AbstractNetworkIF {

    public NetBsdNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        return getNetworks(includeLocalInterfaces, NetBsdNetworkIF::new);
    }

    @Override
    public boolean updateAttributes() {
        // NetBSD netstat -bI <name> output:
        // Name Mtu Network Address Ibytes Obytes
        // wm0 1500 <Link> 52:54:00:12:34:56 689584214 13656411
        String stats = ExecutingCommand.getAnswerAt("netstat -bI " + getName(), 1);
        this.timeStamp = System.currentTimeMillis();
        String[] split = ParseUtil.whitespaces.split(stats);
        if (split.length < 6) {
            return false;
        }
        this.bytesRecv = ParseUtil.parseUnsignedLongOrDefault(split[4], 0L);
        this.bytesSent = ParseUtil.parseUnsignedLongOrDefault(split[5], 0L);

        // Get packet counts from netstat -iI <name> (without -b)
        // Name Mtu Network Address Ipkts Ierrs Opkts Oerrs Coll
        String pktStats = ExecutingCommand.getAnswerAt("netstat -iI " + getName(), 1);
        String[] pktSplit = ParseUtil.whitespaces.split(pktStats);
        if (pktSplit.length >= 9) {
            this.packetsRecv = ParseUtil.parseUnsignedLongOrDefault(pktSplit[4], 0L);
            this.inErrors = ParseUtil.parseUnsignedLongOrDefault(pktSplit[5], 0L);
            this.packetsSent = ParseUtil.parseUnsignedLongOrDefault(pktSplit[6], 0L);
            this.outErrors = ParseUtil.parseUnsignedLongOrDefault(pktSplit[7], 0L);
            this.collisions = ParseUtil.parseUnsignedLongOrDefault(pktSplit[8], 0L);
        }
        return true;
    }
}
