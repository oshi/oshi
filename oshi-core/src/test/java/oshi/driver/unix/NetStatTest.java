/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;
import oshi.util.tuples.Pair;

@DisabledOnOs(OS.WINDOWS)
class NetStatTest {

    @DisabledOnOs({ OS.WINDOWS, OS.MAC, OS.LINUX })
    @Test
    void testQueryNetstat() {
        for (IPConnection conn : NetStat.queryNetstat()) {
            assertThat("Connection is tcp or udp", conn.getType(), anyOf(startsWith("tcp"), startsWith("udp")));
        }
    }

    @EnabledOnOs({ OS.MAC, OS.FREEBSD })
    @Test
    void testQueryTcpNetstat() {
        Pair<Long, Long> tcpConns = NetStat.queryTcpnetstat();
        assertThat("ipv4 connections must be nonnegative", tcpConns.getA().intValue(), greaterThanOrEqualTo(0));
        assertThat("ipv6 connections must be nonnegative", tcpConns.getB().intValue(), greaterThanOrEqualTo(0));
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void testQueryStatsLinux() {
        TcpStats tcpStats = NetStat.queryTcpStats("netstat -st4");
        assertThat("tcp connections must be nonnegative", tcpStats.getConnectionsEstablished(),
                greaterThanOrEqualTo(0L));
        UdpStats udp4Stats = NetStat.queryUdpStats("netstat -su4");
        assertThat("udp4 datagrams sent must be nonnegative", udp4Stats.getDatagramsSent(), greaterThanOrEqualTo(0L));
        UdpStats udp6Stats = NetStat.queryUdpStats("netstat -su6");
        assertThat("udp4 datagrams sent must be nonnegative", udp6Stats.getDatagramsSent(), greaterThanOrEqualTo(0L));
    }

    @EnabledOnOs(OS.OPENBSD)
    @Test
    void testQueryStatsOpenBSD() {
        TcpStats tcpStats = NetStat.queryTcpStats("netstat -s -p tcp");
        assertThat("tcp connections must be nonnegative", tcpStats.getConnectionsEstablished(),
                greaterThanOrEqualTo(0L));
        UdpStats udpStats = NetStat.queryUdpStats("netstat -s -p udp");
        assertThat("udp  datagrams sent must be nonnegative", udpStats.getDatagramsSent(), greaterThanOrEqualTo(0L));
    }
}
