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
package oshi.software.os.windows;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.windows.IPHlpAPI;
import oshi.jna.platform.windows.IPHlpAPI.MIB_TCPSTATS;
import oshi.jna.platform.windows.IPHlpAPI.MIB_UDPSTATS;
import oshi.software.os.InternetProtocolStats;

@ThreadSafe
public class WindowsInternetProtocolStats implements InternetProtocolStats {

    private static final IPHlpAPI IPHLP = IPHlpAPI.INSTANCE;

    @Override
    public TcpStats getTCPv4Stats() {
        MIB_TCPSTATS stats = new MIB_TCPSTATS();
        IPHLP.GetTcpStatisticsEx(stats, IPHlpAPI.AF_INET);
        return new TcpStats(stats.dwCurrEstab, stats.dwActiveOpens, stats.dwPassiveOpens, stats.dwAttemptFails,
                stats.dwEstabResets, stats.dwOutSegs, stats.dwInSegs, stats.dwRetransSegs, stats.dwInErrs,
                stats.dwOutRsts);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        MIB_TCPSTATS stats = new MIB_TCPSTATS();
        IPHLP.GetTcpStatisticsEx(stats, IPHlpAPI.AF_INET6);
        return new TcpStats(stats.dwCurrEstab, stats.dwActiveOpens, stats.dwPassiveOpens, stats.dwAttemptFails,
                stats.dwEstabResets, stats.dwOutSegs, stats.dwInSegs, stats.dwRetransSegs, stats.dwInErrs,
                stats.dwOutRsts);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        MIB_UDPSTATS stats = new MIB_UDPSTATS();
        IPHLP.GetUdpStatisticsEx(stats, IPHlpAPI.AF_INET);
        return new UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
    }

    @Override
    public UdpStats getUDPv6Stats() {
        MIB_UDPSTATS stats = new MIB_UDPSTATS();
        IPHLP.GetUdpStatisticsEx(stats, IPHlpAPI.AF_INET6);
        return new UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
    }
}
