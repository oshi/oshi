/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.driver.unix.NetStat;
import oshi.util.tuples.Pair;

/**
 * Abstract base for the macOS InternetProtocolStats. The TCP/UDP statistics computation is shared; the JNA and FFM
 * subclasses read the {@code net.inet.*.stats} sysctls into the
 * {@link BsdTcpstat}/{@link BsdUdpstat}/{@link BsdIpstat}/ {@link BsdIp6stat} carriers, and supply their own native
 * {@link #getConnections()} implementation.
 */
@ThreadSafe
public abstract class MacInternetProtocolStats extends AbstractInternetProtocolStats {

    /** Whether the queries run with elevated permissions, enabling the {@code tcpstat}-only TCP path. */
    private final boolean isElevated;

    /**
     * Constructs a Mac InternetProtocolStats.
     *
     * @param elevated whether running with elevated permissions
     */
    protected MacInternetProtocolStats(boolean elevated) {
        this.isElevated = elevated;
    }

    private final Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStat::queryTcpnetstat, defaultExpiration());
    private final Supplier<BsdTcpstat> tcpstat = memoize(this::queryTcpstat, defaultExpiration());
    private final Supplier<BsdUdpstat> udpstat = memoize(this::queryUdpstat, defaultExpiration());
    // With elevated permissions use tcpstat only
    // Backup estimate get ipstat and subtract off udp
    private final Supplier<BsdIpstat> ipstat = memoize(this::queryIpstat, defaultExpiration());
    private final Supplier<BsdIp6stat> ip6stat = memoize(this::queryIp6stat, defaultExpiration());

    /**
     * Reads the {@code net.inet.tcp.stats} sysctl into a carrier.
     *
     * @return the TCP statistics
     */
    protected abstract BsdTcpstat queryTcpstat();

    /**
     * Reads the {@code net.inet.udp.stats} sysctl into a carrier.
     *
     * @return the UDP statistics
     */
    protected abstract BsdUdpstat queryUdpstat();

    /**
     * Reads the {@code net.inet.ip.stats} sysctl into a carrier.
     *
     * @return the IP statistics
     */
    protected abstract BsdIpstat queryIpstat();

    /**
     * Reads the {@code net.inet6.ip6.stats} sysctl into a carrier.
     *
     * @return the IPv6 statistics
     */
    protected abstract BsdIp6stat queryIp6stat();

    @Override
    public TcpStats getTCPv4Stats() {
        BsdTcpstat tcp = tcpstat.get();
        if (this.isElevated) {
            return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                    ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                    ParseUtil.unsignedIntToLong(tcp.tcps_drops), ParseUtil.unsignedIntToLong(tcp.tcps_sndpack),
                    ParseUtil.unsignedIntToLong(tcp.tcps_rcvpack), ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                    ParseUtil.unsignedIntToLong(
                            tcp.tcps_rcvbadsum + tcp.tcps_rcvbadoff + tcp.tcps_rcvmemdrop + tcp.tcps_rcvshort),
                    0L);
        }
        BsdIpstat ip = ipstat.get();
        BsdUdpstat udp = udpstat.get();
        return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                ParseUtil.unsignedIntToLong(tcp.tcps_drops),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_delivered - udp.udps_opackets)),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_total - udp.udps_ipackets)),
                ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_badsum + ip.ips_tooshort + ip.ips_toosmall
                        + ip.ips_badhlen + ip.ips_badlen - udp.udps_hdrops + udp.udps_badsum + udp.udps_badlen)),
                0L);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        BsdIp6stat ip6 = ip6stat.get();
        BsdUdpstat udp = udpstat.get();
        return new TcpStats(establishedv4v6.get().getB(), 0L, 0L, 0L, 0L,
                Math.max(0L, ip6.ip6s_localout - ParseUtil.unsignedIntToLong(udp.udps_snd6_swcsum)),
                Math.max(0L, ip6.ip6s_total - ParseUtil.unsignedIntToLong(udp.udps_rcv6_swcsum)), 0L, 0L, 0L);
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

    /*
     * There are multiple versions of some tcp/udp/ip stats structures in macOS. Since we only need a few of the
     * hundreds of fields, we can improve performance by selectively reading the ints from the appropriate offsets,
     * which are consistent across the structure. The carriers below hold those selected fields; the subclasses read
     * them from the sysctl buffer (JNA Memory or FFM MemorySegment).
     */

    /** Selected fields of the macOS {@code tcpstat} structure. */
    public static final class BsdTcpstat {
        private final int tcps_connattempt;
        private final int tcps_accepts;
        private final int tcps_drops;
        private final int tcps_conndrops;
        private final int tcps_sndpack;
        private final int tcps_sndrexmitpack;
        private final int tcps_rcvpack;
        private final int tcps_rcvbadsum;
        private final int tcps_rcvbadoff;
        private final int tcps_rcvmemdrop;
        private final int tcps_rcvshort;

        /**
         * Creates a {@code tcpstat} carrier.
         *
         * @param connattempt   connection attempts
         * @param accepts       accepted connections
         * @param drops         dropped connections
         * @param conndrops     connections dropped during establishment
         * @param sndpack       packets sent
         * @param sndrexmitpack packets retransmitted
         * @param rcvpack       packets received
         * @param rcvbadsum     packets received with bad checksum
         * @param rcvbadoff     packets received with bad offset
         * @param rcvmemdrop    packets dropped for lack of memory
         * @param rcvshort      packets received too short
         */
        public BsdTcpstat(int connattempt, int accepts, int drops, int conndrops, int sndpack, int sndrexmitpack,
                int rcvpack, int rcvbadsum, int rcvbadoff, int rcvmemdrop, int rcvshort) {
            this.tcps_connattempt = connattempt;
            this.tcps_accepts = accepts;
            this.tcps_drops = drops;
            this.tcps_conndrops = conndrops;
            this.tcps_sndpack = sndpack;
            this.tcps_sndrexmitpack = sndrexmitpack;
            this.tcps_rcvpack = rcvpack;
            this.tcps_rcvbadsum = rcvbadsum;
            this.tcps_rcvbadoff = rcvbadoff;
            this.tcps_rcvmemdrop = rcvmemdrop;
            this.tcps_rcvshort = rcvshort;
        }
    }

    /** Selected fields of the macOS {@code udpstat} structure. */
    public static final class BsdUdpstat {
        private final int udps_ipackets;
        private final int udps_hdrops;
        private final int udps_badsum;
        private final int udps_badlen;
        private final int udps_opackets;
        private final int udps_noportmcast;
        private final int udps_rcv6_swcsum;
        private final int udps_snd6_swcsum;

        /**
         * Creates a {@code udpstat} carrier.
         *
         * @param ipackets    datagrams received
         * @param hdrops      datagrams dropped for bad header
         * @param badsum      datagrams dropped for bad checksum
         * @param badlen      datagrams dropped for bad length
         * @param opackets    datagrams sent
         * @param noportmcast multicast datagrams with no port
         * @param rcv6Swcsum  IPv6 datagrams received with software checksum
         * @param snd6Swcsum  IPv6 datagrams sent with software checksum
         */
        public BsdUdpstat(int ipackets, int hdrops, int badsum, int badlen, int opackets, int noportmcast,
                int rcv6Swcsum, int snd6Swcsum) {
            this.udps_ipackets = ipackets;
            this.udps_hdrops = hdrops;
            this.udps_badsum = badsum;
            this.udps_badlen = badlen;
            this.udps_opackets = opackets;
            this.udps_noportmcast = noportmcast;
            this.udps_rcv6_swcsum = rcv6Swcsum;
            this.udps_snd6_swcsum = snd6Swcsum;
        }
    }

    /** Selected fields of the macOS {@code ipstat} structure. */
    public static final class BsdIpstat {
        private final int ips_total;
        private final int ips_badsum;
        private final int ips_tooshort;
        private final int ips_toosmall;
        private final int ips_badhlen;
        private final int ips_badlen;
        private final int ips_delivered;

        /**
         * Creates an {@code ipstat} carrier.
         *
         * @param total     total packets received
         * @param badsum    packets with bad checksum
         * @param tooshort  packets too short
         * @param toosmall  packets with not enough data
         * @param badhlen   packets with bad header length
         * @param badlen    packets with bad length
         * @param delivered datagrams delivered to upper level
         */
        public BsdIpstat(int total, int badsum, int tooshort, int toosmall, int badhlen, int badlen, int delivered) {
            this.ips_total = total;
            this.ips_badsum = badsum;
            this.ips_tooshort = tooshort;
            this.ips_toosmall = toosmall;
            this.ips_badhlen = badhlen;
            this.ips_badlen = badlen;
            this.ips_delivered = delivered;
        }
    }

    /** Selected fields of the macOS {@code ip6stat} structure. */
    public static final class BsdIp6stat {
        private final long ip6s_total;
        private final long ip6s_localout;

        /**
         * Creates an {@code ip6stat} carrier.
         *
         * @param total    total IPv6 packets received
         * @param localout IPv6 packets sent from this host
         */
        public BsdIp6stat(long total, long localout) {
            this.ip6s_total = total;
            this.ip6s_localout = localout;
        }
    }
}
