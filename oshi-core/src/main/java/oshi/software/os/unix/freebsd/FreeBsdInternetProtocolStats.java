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
package oshi.software.os.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStatTcp;
import oshi.jna.platform.unix.CLibrary.Tcpstat;
import oshi.jna.platform.unix.CLibrary.Udpstat;
import oshi.software.os.InternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class FreeBsdInternetProtocolStats implements InternetProtocolStats {

    private Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStatTcp::queryTcpnetstat, defaultExpiration());
    private Supplier<Tcpstat> tcpstat = memoize(FreeBsdInternetProtocolStats::queryTcpstat, defaultExpiration());
    private Supplier<Udpstat> udpstat = memoize(FreeBsdInternetProtocolStats::queryUdpstat, defaultExpiration());

    @Override
    public TcpStats getTCPv4Stats() {
        Tcpstat tcp = tcpstat.get();
        return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                ParseUtil.unsignedIntToLong(tcp.tcps_drops),
                ParseUtil.unsignedIntToLong(tcp.tcps_snd_swcsum - tcp.tcps_sndrexmitpack),
                ParseUtil.unsignedIntToLong(tcp.tcps_rcv_swcsum), ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                ParseUtil.unsignedIntToLong(
                        tcp.tcps_rcvbadsum + tcp.tcps_rcvbadoff + tcp.tcps_rcvmemdrop + tcp.tcps_rcvshort),
                0L);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        return new TcpStats(establishedv4v6.get().getB(), 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
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
        BsdSysctlUtil.sysctl("net.inet.tcp.stats", tcpstat);
        return tcpstat;
    }

    private static Udpstat queryUdpstat() {
        Udpstat udpstat = new Udpstat();
        BsdSysctlUtil.sysctl("net.inet.udp.stats", udpstat);
        return udpstat;
    }
}
