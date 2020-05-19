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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStatTcp;
import oshi.jna.platform.unix.CLibrary.Ip6stat;
import oshi.jna.platform.unix.CLibrary.Ipstat;
import oshi.jna.platform.unix.CLibrary.Tcpstat;
import oshi.jna.platform.unix.CLibrary.Udpstat;
import oshi.software.os.InternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class MacInternetProtocolStats implements InternetProtocolStats {

    private boolean isElevated;

    public MacInternetProtocolStats(boolean elevated) {
        this.isElevated = elevated;
    }

    private Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStatTcp::queryTcpnetstat, defaultExpiration());
    private Supplier<Tcpstat> tcpstat = memoize(MacInternetProtocolStats::queryTcpstat, defaultExpiration());
    private Supplier<Udpstat> udpstat = memoize(MacInternetProtocolStats::queryUdpstat, defaultExpiration());
    // With elevated permissions use tcpstat only
    // Backup estimate get ipstat and subtract off udp
    private Supplier<Ipstat> ipstat = memoize(MacInternetProtocolStats::queryIpstat, defaultExpiration());
    private Supplier<Ip6stat> ip6stat = memoize(MacInternetProtocolStats::queryIp6stat, defaultExpiration());

    @Override
    public TcpStats getTCPv4Stats() {
        Tcpstat tcp = tcpstat.get();
        if (this.isElevated) {
            return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                    ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                    ParseUtil.unsignedIntToLong(tcp.tcps_drops),
                    ParseUtil.unsignedIntToLong(tcp.tcps_snd_swcsum - tcp.tcps_sndrexmitpack),
                    ParseUtil.unsignedIntToLong(tcp.tcps_rcv_swcsum),
                    ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack), ParseUtil.unsignedIntToLong(
                            tcp.tcps_rcvbadsum + tcp.tcps_rcvbadoff + tcp.tcps_rcvmemdrop + tcp.tcps_rcvshort),
                    0L);
        }
        Ipstat ip = ipstat.get();
        Udpstat udp = udpstat.get();
        return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                ParseUtil.unsignedIntToLong(tcp.tcps_drops),
                Math.max(0L,
                        ParseUtil.unsignedIntToLong(ip.ips_snd_swcsum - udp.udps_snd_swcsum - tcp.tcps_sndrexmitpack)),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_rcv_swcsum - udp.udps_rcv_swcsum)),
                ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_badsum + ip.ips_tooshort + ip.ips_toosmall
                        + ip.ips_badhlen + ip.ips_badlen - udp.udps_hdrops + udp.udps_badsum + udp.udps_badlen)),
                0L);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        Ip6stat ip6 = ip6stat.get();
        Udpstat udp = udpstat.get();
        return new TcpStats(establishedv4v6.get().getB(), 0L, 0L, 0L, 0L,
                ip6.ip6s_localout - ParseUtil.unsignedIntToLong(udp.udps_snd6_swcsum),
                ip6.ip6s_total - ParseUtil.unsignedIntToLong(udp.udps_rcv6_swcsum), 0L, 0L, 0L);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        Udpstat stat = udpstat.get();
        return new UdpStats(ParseUtil.unsignedIntToLong(stat.udps_snd_swcsum),
                ParseUtil.unsignedIntToLong(stat.udps_rcv_swcsum), ParseUtil.unsignedIntToLong(stat.udps_noportmcast),
                ParseUtil.unsignedIntToLong(stat.udps_hdrops + stat.udps_badsum + stat.udps_badlen));
    }

    @Override
    public UdpStats getUDPv6Stats() {
        Udpstat stat = udpstat.get();
        return new UdpStats(ParseUtil.unsignedIntToLong(stat.udps_snd6_swcsum),
                ParseUtil.unsignedIntToLong(stat.udps_rcv6_swcsum), 0L, 0L);
    }

    private static Tcpstat queryTcpstat() {
        Tcpstat tcpstat = new Tcpstat();
        SysctlUtil.sysctl("net.inet.tcp.stats", tcpstat);
        return tcpstat;
    }

    private static Ipstat queryIpstat() {
        Ipstat ipstat = new Ipstat();
        SysctlUtil.sysctl("net.inet.ip.stats", ipstat);
        return ipstat;
    }

    private static Ip6stat queryIp6stat() {
        Ip6stat ip6stat = new Ip6stat();
        SysctlUtil.sysctl("net.inet6.ip6.stats", ip6stat);
        return ip6stat;
    }

    private static Udpstat queryUdpstat() {
        Udpstat udpstat = new Udpstat();
        SysctlUtil.sysctl("net.inet.udp.stats", udpstat);
        return udpstat;
    }
}
