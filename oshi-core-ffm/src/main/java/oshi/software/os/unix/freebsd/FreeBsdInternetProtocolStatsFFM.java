/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.MemorySegment;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.software.common.os.unix.freebsd.FreeBsdInternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.NetStat;
import oshi.util.tuples.Pair;

/**
 * FFM-backed FreeBSD Internet Protocol stats. Reads {@code net.inet.tcp.stats} and {@code net.inet.udp.stats} as raw
 * memory and pulls counters at fixed offsets matching the kernel struct layout. Established-connection counts come from
 * the shared {@link NetStat} command-line driver.
 */
@ThreadSafe
public class FreeBsdInternetProtocolStatsFFM extends FreeBsdInternetProtocolStats {

    // Minimum sizes that contain all the fields we read. Sysctl outputs may grow over time but never shrink.
    private static final int TCPSTAT_MIN_SIZE = 128;
    private static final int UDPSTAT_MIN_SIZE = 84;

    private Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStat::queryTcpnetstat, defaultExpiration());
    private Supplier<MemorySegment> tcpstat = memoize(FreeBsdInternetProtocolStatsFFM::queryTcpstat,
            defaultExpiration());
    private Supplier<MemorySegment> udpstat = memoize(FreeBsdInternetProtocolStatsFFM::queryUdpstat,
            defaultExpiration());

    @Override
    public TcpStats getTCPv4Stats() {
        MemorySegment m = tcpstat.get();
        if (m == null || m.byteSize() < TCPSTAT_MIN_SIZE) {
            return new TcpStats(establishedv4v6.get().getA(), 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
        long connattempt = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 0));
        long accepts = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 4));
        long drops = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 12));
        long conndrops = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 16));
        long sndpack = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 64));
        long sndrexmitpack = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 72));
        long rcvpack = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 104));
        long inErrors = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 112)) // rcvbadsum
                + ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 116)) // rcvbadoff
                + ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 120)) // rcvmemdrop
                + ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 124)); // rcvshort
        return new TcpStats(establishedv4v6.get().getA(), connattempt, accepts, conndrops, drops, sndpack, rcvpack,
                sndrexmitpack, inErrors, 0L);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        MemorySegment m = udpstat.get();
        if (m == null || m.byteSize() < UDPSTAT_MIN_SIZE) {
            return new UdpStats(0L, 0L, 0L, 0L);
        }
        long ipackets = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 0));
        long hdrops = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 4));
        long badsum = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 8));
        long badlen = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 12));
        long opackets = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 36));
        long noportmcast = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 48));
        return new UdpStats(opackets, ipackets, noportmcast, hdrops + badsum + badlen);
    }

    @Override
    public UdpStats getUDPv6Stats() {
        MemorySegment m = udpstat.get();
        if (m == null || m.byteSize() < UDPSTAT_MIN_SIZE) {
            return new UdpStats(0L, 0L, 0L, 0L);
        }
        long rcv6 = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 64));
        long snd6 = ParseUtil.unsignedIntToLong(m.get(JAVA_INT, 80));
        return new UdpStats(snd6, rcv6, 0L, 0L);
    }

    private static MemorySegment queryTcpstat() {
        return BsdSysctlUtilFFM.sysctl("net.inet.tcp.stats");
    }

    private static MemorySegment queryUdpstat() {
        return BsdSysctlUtilFFM.sysctl("net.inet.udp.stats");
    }
}
