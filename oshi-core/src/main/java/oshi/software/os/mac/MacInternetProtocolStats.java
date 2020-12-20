/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.mac;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Memory; // NOSONAR squid:S1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStat;
import oshi.jna.platform.unix.CLibrary.BsdIp6stat;
import oshi.jna.platform.unix.CLibrary.BsdIpstat;
import oshi.jna.platform.unix.CLibrary.BsdTcpstat;
import oshi.jna.platform.unix.CLibrary.BsdUdpstat;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class MacInternetProtocolStats extends AbstractInternetProtocolStats {

    private boolean isElevated;

    public MacInternetProtocolStats(boolean elevated) {
        this.isElevated = elevated;
    }

    private Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStat::queryTcpnetstat, defaultExpiration());
    private Supplier<BsdTcpstat> tcpstat = memoize(MacInternetProtocolStats::queryTcpstat, defaultExpiration());
    private Supplier<BsdUdpstat> udpstat = memoize(MacInternetProtocolStats::queryUdpstat, defaultExpiration());
    // With elevated permissions use tcpstat only
    // Backup estimate get ipstat and subtract off udp
    private Supplier<BsdIpstat> ipstat = memoize(MacInternetProtocolStats::queryIpstat, defaultExpiration());
    private Supplier<BsdIp6stat> ip6stat = memoize(MacInternetProtocolStats::queryIp6stat, defaultExpiration());

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
                ip6.ip6s_localout - ParseUtil.unsignedIntToLong(udp.udps_snd6_swcsum),
                ip6.ip6s_total - ParseUtil.unsignedIntToLong(udp.udps_rcv6_swcsum), 0L, 0L, 0L);
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
     * There are multiple versions of some tcp/udp/ip stats structures in macOS.
     * Since we only need a few of the hundreds of fields, we can improve
     * performance by selectively reading the ints from the appropriate offsets,
     * which are consistent across the structure.
     */

    private static BsdTcpstat queryTcpstat() {
        BsdTcpstat mt = new BsdTcpstat();
        Memory m = SysctlUtil.sysctl("net.inet.tcp.stats");
        if (m != null && m.size() >= 128) {
            mt.tcps_connattempt = m.getInt(0);
            mt.tcps_accepts = m.getInt(4);
            mt.tcps_drops = m.getInt(12);
            mt.tcps_conndrops = m.getInt(16);
            mt.tcps_sndpack = m.getInt(64);
            mt.tcps_sndrexmitpack = m.getInt(72);
            mt.tcps_rcvpack = m.getInt(104);
            mt.tcps_rcvbadsum = m.getInt(112);
            mt.tcps_rcvbadoff = m.getInt(116);
            mt.tcps_rcvmemdrop = m.getInt(120);
            mt.tcps_rcvshort = m.getInt(124);
        }
        return mt;
    }

    private static BsdIpstat queryIpstat() {
        BsdIpstat mi = new BsdIpstat();
        Memory m = SysctlUtil.sysctl("net.inet.ip.stats");
        if (m != null && m.size() >= 60) {
            mi.ips_total = m.getInt(0);
            mi.ips_badsum = m.getInt(4);
            mi.ips_tooshort = m.getInt(8);
            mi.ips_toosmall = m.getInt(12);
            mi.ips_badhlen = m.getInt(16);
            mi.ips_badlen = m.getInt(20);
            mi.ips_delivered = m.getInt(56);
        }
        return mi;
    }

    private static BsdIp6stat queryIp6stat() {
        BsdIp6stat mi6 = new BsdIp6stat();
        Memory m = SysctlUtil.sysctl("net.inet6.ip6.stats");
        if (m != null && m.size() >= 96) {
            mi6.ip6s_total = m.getLong(0);
            mi6.ip6s_localout = m.getLong(88);
        }
        return mi6;
    }

    public static BsdUdpstat queryUdpstat() {
        BsdUdpstat ut = new BsdUdpstat();
        Memory m = SysctlUtil.sysctl("net.inet.udp.stats");
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
        return ut;
    }
}
