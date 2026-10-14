/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.driver.unix.NetStat;
import oshi.util.tuples.Pair;

/**
 * Abstract base for the FreeBSD InternetProtocolStats. Parses the {@code net.inet.tcp.stats} and
 * {@code net.inet.udp.stats} sysctls, falling back to {@code netstat -s} when the structure layout is not recognized.
 * The JNA and FFM subclasses supply the raw sysctl reads and the per-connection reads.
 * <p>
 * Since FreeBSD 10.0 every counter in {@code struct tcpstat} and {@code struct udpstat} is a {@code uint64_t}. FreeBSD
 * 14.0 inserted {@code tcps_progdrops} into the middle of {@code struct tcpstat}, moving every counter from
 * {@code tcps_sndtotal} onward by 8 bytes. There is no version field, but the structure size identifies the layout: 880
 * bytes (10.x and 11.x) or 904 bytes (12.x and 13.x) before the insertion, and 984 bytes or more after it.
 */
@ThreadSafe
public abstract class FreeBsdInternetProtocolStats extends AbstractInternetProtocolStats {

    /** The TCP statistics sysctl. */
    protected static final String TCPSTAT_SYSCTL = "net.inet.tcp.stats";
    /** The UDP statistics sysctl. */
    protected static final String UDPSTAT_SYSCTL = "net.inet.udp.stats";

    // struct tcpstat sizes bounding each known layout
    private static final int TCPSTAT_MIN_SIZE = 880;
    private static final int TCPSTAT_MAX_SIZE_BEFORE_PROGDROPS = 904;
    private static final int TCPSTAT_MIN_SIZE_WITH_PROGDROPS = 984;
    // struct tcpstat offsets ahead of tcps_progdrops, the same in every layout
    private static final int TCPS_CONNATTEMPT = 0;
    private static final int TCPS_ACCEPTS = 8;
    private static final int TCPS_DROPS = 24;
    private static final int TCPS_CONNDROPS = 32;
    // struct tcpstat offsets after tcps_progdrops, as laid out before FreeBSD 14.0
    private static final int TCPS_SNDPACK = 136;
    private static final int TCPS_SNDREXMITPACK = 152;
    private static final int TCPS_RCVPACK = 224;
    private static final int TCPS_RCVBADSUM = 240;
    private static final int TCPS_RCVBADOFF = 248;
    private static final int TCPS_RCVSHORT = 264;
    // The shift FreeBSD 14.0's tcps_progdrops applies to the offsets above
    private static final int PROGDROPS_SHIFT = 8;

    // struct udpstat, unchanged since FreeBSD 10.0
    private static final int UDPSTAT_SIZE = 112;
    private static final int UDPS_IPACKETS = 0;
    private static final int UDPS_HDROPS = 8;
    private static final int UDPS_BADSUM = 16;
    private static final int UDPS_BADLEN = 32;
    private static final int UDPS_NOPORT = 40;
    private static final int UDPS_NOPORTBCAST = 48;
    private static final int UDPS_OPACKETS = 80;
    private static final int UDPS_NOPORTMCAST = 96;

    private final Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStat::queryTcpnetstat, defaultExpiration());
    private final Supplier<TcpStats> tcpv4 = memoize(this::queryTcpv4Stats, defaultExpiration());
    private final Supplier<UdpStats> udpv4 = memoize(this::queryUdpv4Stats, defaultExpiration());

    /**
     * Reads a sysctl as raw bytes.
     *
     * @param name the sysctl name
     * @return the sysctl value, or an empty array if the query failed
     */
    protected abstract byte[] querySysctl(String name);

    @Override
    public TcpStats getTCPv4Stats() {
        return tcpv4.get();
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return udpv4.get();
    }

    private TcpStats queryTcpv4Stats() {
        long established = establishedv4v6.get().getA();
        TcpStats stats = parseTcpstat(querySysctl(TCPSTAT_SYSCTL), established);
        if (stats != null) {
            return stats;
        }
        return NetStat.queryTcpStats("netstat -s -p tcp", established);
    }

    private UdpStats queryUdpv4Stats() {
        UdpStats stats = parseUdpstat(querySysctl(UDPSTAT_SYSCTL));
        return stats != null ? stats : NetStat.queryUdpStats("netstat -s -p udp");
    }

    /**
     * Parses a {@code struct tcpstat} read from the {@code net.inet.tcp.stats} sysctl.
     *
     * @param tcpstat     the raw structure
     * @param established the number of connections currently established, which the structure does not hold
     * @return the statistics, or {@code null} if the structure size matches no known layout
     */
    static @Nullable TcpStats parseTcpstat(byte[] tcpstat, long established) {
        int shift;
        if (tcpstat.length >= TCPSTAT_MIN_SIZE_WITH_PROGDROPS) {
            shift = PROGDROPS_SHIFT;
        } else if (tcpstat.length >= TCPSTAT_MIN_SIZE && tcpstat.length <= TCPSTAT_MAX_SIZE_BEFORE_PROGDROPS) {
            shift = 0;
        } else {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(tcpstat).order(ByteOrder.nativeOrder());
        return new TcpStats(established, bb.getLong(TCPS_CONNATTEMPT), bb.getLong(TCPS_ACCEPTS),
                bb.getLong(TCPS_CONNDROPS), bb.getLong(TCPS_DROPS), bb.getLong(TCPS_SNDPACK + shift),
                bb.getLong(TCPS_RCVPACK + shift), bb.getLong(TCPS_SNDREXMITPACK + shift),
                bb.getLong(TCPS_RCVBADSUM + shift) + bb.getLong(TCPS_RCVBADOFF + shift)
                        + bb.getLong(TCPS_RCVSHORT + shift),
                0L);
    }

    /**
     * Parses a {@code struct udpstat} read from the {@code net.inet.udp.stats} sysctl.
     *
     * @param udpstat the raw structure
     * @return the statistics, or {@code null} if the structure size matches no known layout
     */
    static @Nullable UdpStats parseUdpstat(byte[] udpstat) {
        if (udpstat.length < UDPSTAT_SIZE) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(udpstat).order(ByteOrder.nativeOrder());
        return new UdpStats(bb.getLong(UDPS_OPACKETS), bb.getLong(UDPS_IPACKETS),
                bb.getLong(UDPS_NOPORT) + bb.getLong(UDPS_NOPORTBCAST) + bb.getLong(UDPS_NOPORTMCAST),
                bb.getLong(UDPS_HDROPS) + bb.getLong(UDPS_BADSUM) + bb.getLong(UDPS_BADLEN));
    }
}
