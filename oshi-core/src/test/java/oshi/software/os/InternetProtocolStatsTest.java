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
package oshi.software.os;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

/**
 * Test network parameters
 */
public class InternetProtocolStatsTest {

    /**
     * Test network parameters
     */
    @Test
    public void testIPStats() {
        SystemInfo si = new SystemInfo();
        InternetProtocolStats ipStats = si.getOperatingSystem().getInternetProtocolStats();
        TcpStats tcp4 = ipStats.getTCPv4Stats();
        TcpStats tcp6 = ipStats.getTCPv6Stats();
        UdpStats udp4 = ipStats.getUDPv4Stats();
        UdpStats udp6 = ipStats.getUDPv6Stats();
        assertTrue(tcp4.getInErrors() <= tcp4.getSegmentsReceived());
        assertTrue(tcp6.getInErrors() <= tcp6.getSegmentsReceived());
        assertTrue(udp4.getDatagramsNoPort() <= udp4.getDatagramsReceived());
        assertTrue(udp6.getDatagramsNoPort() <= udp6.getDatagramsReceived());
        assertNotNull(tcp4.toString());
        assertNotNull(tcp6.toString());
        assertNotNull(udp4.toString());
        assertNotNull(udp6.toString());
    }
}
