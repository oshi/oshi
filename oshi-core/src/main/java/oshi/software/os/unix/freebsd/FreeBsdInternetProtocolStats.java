/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStat;
import oshi.jna.platform.unix.CLibrary.BsdTcpstat;
import oshi.jna.platform.unix.CLibrary.BsdUdpstat;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class FreeBsdInternetProtocolStats extends AbstractInternetProtocolStats {

    private Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStat::queryTcpnetstat, defaultExpiration());
    private Supplier<BsdTcpstat> tcpstat = memoize(FreeBsdInternetProtocolStats::queryTcpstat, defaultExpiration());
    private Supplier<BsdUdpstat> udpstat = memoize(FreeBsdInternetProtocolStats::queryUdpstat, defaultExpiration());

    @Override
    public TcpStats getTCPv4Stats() {
        BsdTcpstat tcp = tcpstat.get();
        return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                ParseUtil.unsignedIntToLong(tcp.tcps_drops), ParseUtil.unsignedIntToLong(tcp.tcps_sndpack),
                ParseUtil.unsignedIntToLong(tcp.tcps_rcvpack), ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                ParseUtil.unsignedIntToLong(
                        tcp.tcps_rcvbadsum + tcp.tcps_rcvbadoff + tcp.tcps_rcvmemdrop + tcp.tcps_rcvshort),
                0L);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        BsdUdpstat stat = udpstat.get();
        return new UdpStats(ParseUtil.unsignedIntToLong(stat.udps_opackets),
                ParseUtil.unsignedIntToLong(stat.udps_ipackets), ParseUtil.unsignedIntToLong(stat.udps_noportmcast),
                ParseUtil.unsignedIntToLong(stat.udps_hdrops + stat.udps_badsum + stat.udps_badlen));
    }

    @Override
    public UdpStats getUDPv6Stats() {
        BsdUdpstat stat = udpstat.get();
        return new UdpStats(ParseUtil.unsignedIntToLong(stat.udps_snd6_swcsum),
                ParseUtil.unsignedIntToLong(stat.udps_rcv6_swcsum), 0L, 0L);
    }

    private static BsdTcpstat queryTcpstat() {
        BsdTcpstat ft = new BsdTcpstat();
        try (Memory m = BsdSysctlUtil.sysctl("net.inet.tcp.stats")) {
            if (m != null && m.size() >= 128) {
                ft.tcps_connattempt = m.getInt(0);
                ft.tcps_accepts = m.getInt(4);
                ft.tcps_drops = m.getInt(12);
                ft.tcps_conndrops = m.getInt(16);
                ft.tcps_sndpack = m.getInt(64);
                ft.tcps_sndrexmitpack = m.getInt(72);
                ft.tcps_rcvpack = m.getInt(104);
                ft.tcps_rcvbadsum = m.getInt(112);
                ft.tcps_rcvbadoff = m.getInt(116);
                ft.tcps_rcvmemdrop = m.getInt(120);
                ft.tcps_rcvshort = m.getInt(124);
            }
        }
        return ft;
    }

    private static BsdUdpstat queryUdpstat() {
        BsdUdpstat ut = new BsdUdpstat();
        try (Memory m = BsdSysctlUtil.sysctl("net.inet.udp.stats")) {
            if (m != null && m.size() >= 1644) {
                ut.udps_ipackets = m.getInt(0);
                ut.udps_hdrops = m.getInt(4);
                ut.udps_badsum = m.getInt(8);
                ut.udps_badlen = m.getInt(12);
                ut.udps_opackets = m.getInt(36);
                ut.udps_noportmcast = m.getInt(48);
                ut.udps_rcv6_swcsum = m.getInt(64);
                ut.udps_snd6_swcsum = m.getInt(80);
            }
        }
        return ut;
    }
}
