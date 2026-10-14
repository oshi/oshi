/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * BsdNetworkIF applicable to FreeBSD, DragonFly BSD, NetBSD and OpenBSD.
 */
@ThreadSafe
public final class BsdNetworkIF extends AbstractNetworkIF {

    /**
     * Constructs a new {@code BsdNetworkIF}.
     *
     * @param netint the underlying network interface
     * @throws InstantiationException if the instance could not be constructed
     */
    public BsdNetworkIF(NetworkInterface netint) throws InstantiationException {
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
        return getNetworks(includeLocalInterfaces, BsdNetworkIF::new);
    }

    @Override
    public synchronized boolean updateAttributes() {
        Map<String, Long> stats = parseNetstat(ExecutingCommand.runNative("netstat -bnI " + getName()));
        this.timeStamp = System.currentTimeMillis();
        if (!stats.containsKey("Ibytes")) {
            // No update
            return false;
        }
        if (!stats.containsKey("Ipkts")) {
            // OpenBSD's and NetBSD's -b prints only the byte counts
            stats.putAll(parseNetstat(ExecutingCommand.runNative("netstat -nI " + getName())));
        }
        this.bytesRecv = stats.getOrDefault("Ibytes", 0L);
        this.bytesSent = stats.getOrDefault("Obytes", 0L);
        this.packetsRecv = stats.getOrDefault("Ipkts", 0L);
        this.packetsSent = stats.getOrDefault("Opkts", 0L);
        // OpenBSD names the error columns differently, and OpenBSD and NetBSD the collision column
        this.inErrors = stats.getOrDefault("Ierrs", stats.getOrDefault("Ifail", 0L));
        this.outErrors = stats.getOrDefault("Oerrs", stats.getOrDefault("Ofail", 0L));
        this.collisions = stats.getOrDefault("Coll", stats.getOrDefault("Colls", 0L));
        this.inDrops = stats.getOrDefault("Idrop", 0L);
        return true;
    }

    /**
     * Parses the link-level row of {@code netstat -I <interface>} into its counters, keyed by column heading.
     * <p>
     * The counters follow the Name, Mtu, Network and Address columns, but which ones appear varies: FreeBSD prints an
     * Idrop column that DragonFly BSD does not, and OpenBSD and NetBSD print only the byte counts with {@code -b}. The
     * values are matched to the headings from the end of the row, because the Address column is empty on some loopback
     * rows.
     *
     * @param netstat the lines emitted by {@code netstat -I <interface>}, with or without {@code -b}
     * @return a map of column headings to counter values, empty if the output has no link-level row
     */
    static Map<String, Long> parseNetstat(List<String> netstat) {
        Map<String, Long> stats = new HashMap<>();
        if (netstat.size() < 2) {
            return stats;
        }
        String[] header = ParseUtil.whitespaces.split(netstat.get(0).trim(), -1);
        String[] row = ParseUtil.whitespaces.split(netstat.get(1).trim(), -1);
        int firstCounter = Arrays.asList(header).indexOf("Address") + 1;
        int counters = header.length - firstCounter;
        // The row needs at least the name and every counter
        if (firstCounter == 0 || row.length <= counters) {
            return stats;
        }
        for (int i = 0; i < counters; i++) {
            stats.put(header[firstCounter + i],
                    ParseUtil.parseUnsignedLongOrDefault(row[row.length - counters + i], 0L));
        }
        return stats;
    }
}
