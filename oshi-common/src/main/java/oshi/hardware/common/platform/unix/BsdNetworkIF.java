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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // The rate of an Ethernet media type (1000baseT, 10Gbase-T, 2500Base-T) or a wireless one (OFDM/54Mbps)
    private static final Pattern MEDIA_SPEED = Pattern.compile("(\\d+(?:\\.\\d+)?)([GM]?)(?:base|bps)",
            Pattern.CASE_INSENSITIVE);

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
        this.speed = parseMediaSpeed(ExecutingCommand.runNative("/sbin/ifconfig " + getName()));
        return true;
    }

    /**
     * Parses the link speed from the {@code media:} line of {@code ifconfig <interface>}. Package-private for testing.
     * <p>
     * The active media type is in parentheses when the interface autoselects, as in
     * {@code media: Ethernet autoselect (10Gbase-T <full-duplex>)} on FreeBSD or
     * {@code media: Ethernet autoselect (1000baseT full-duplex,rxpause,txpause)} on OpenBSD; otherwise the line names
     * the fixed media type itself. An Ethernet type gives its rate before {@code base}, in megabits unless marked
     * {@code G}, and a wireless one in {@code Mbps}. An interface with no link shows {@code (none)} or no parentheses,
     * and one without media, such as a virtio interface on NetBSD, no line at all.
     *
     * @param ifconfig the lines emitted by {@code ifconfig <interface>}
     * @return the link speed in bits per second, or 0 if none is shown
     */
    static long parseMediaSpeed(List<String> ifconfig) {
        for (String line : ifconfig) {
            String media = line.trim();
            if (!media.startsWith("media:")) {
                continue;
            }
            int open = media.indexOf('(');
            int close = media.lastIndexOf(')');
            if (open >= 0 && close > open) {
                media = media.substring(open + 1, close);
            }
            Matcher m = MEDIA_SPEED.matcher(media);
            if (m.find()) {
                double rate = ParseUtil.parseDoubleOrDefault(m.group(1), 0d);
                return (long) (rate * ("G".equalsIgnoreCase(m.group(2)) ? 1_000_000_000L : 1_000_000L));
            }
            return 0L;
        }
        return 0L;
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
