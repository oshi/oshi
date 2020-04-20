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
        assertTrue(tcp4.getConnectionsEstablished() >= 0);
        assertTrue(tcp4.getConnectionsActive() >= 0);
        assertTrue(tcp4.getConnectionsPassive() >= 0);
        assertTrue(tcp4.getConnectionFailures() >= 0);
        assertTrue(tcp4.getConnectionsReset() >= 0);
        assertTrue(tcp4.getSegmentsReceived() >= 0);
        assertTrue(tcp4.getSegmentsSent() >= 0);
        assertTrue(tcp4.getSegmentsRetransmitted() >= 0);
        assertTrue(tcp4.getInErrors() >= 0);
        assertTrue(tcp4.getOutResets() >= 0);

        TcpStats tcp6 = ipStats.getTCPv6Stats();
        assertTrue(tcp6.getConnectionsEstablished() >= 0);
        assertTrue(tcp6.getConnectionsActive() >= 0);
        assertTrue(tcp6.getConnectionsPassive() >= 0);
        assertTrue(tcp6.getConnectionFailures() >= 0);
        assertTrue(tcp6.getConnectionsReset() >= 0);
        assertTrue(tcp6.getSegmentsReceived() >= 0);
        assertTrue(tcp6.getSegmentsSent() >= 0);
        assertTrue(tcp6.getSegmentsRetransmitted() >= 0);
        assertTrue(tcp6.getInErrors() >= 0);
        assertTrue(tcp6.getOutResets() >= 0);

        UdpStats udp4 = ipStats.getUDPv4Stats();
        assertTrue(udp4.getDatagramsReceived() >= 0);
        assertTrue(udp4.getDatagramsSent() >= 0);
        assertTrue(udp4.getDatagramsNoPort() >= 0);
        assertTrue(udp4.getDatagramsReceivedErrors() >= 0);

        UdpStats udp6 = ipStats.getUDPv6Stats();
        assertTrue(udp6.getDatagramsReceived() >= 0);
        assertTrue(udp6.getDatagramsSent() >= 0);
        assertTrue(udp6.getDatagramsNoPort() >= 0);
        assertTrue(udp6.getDatagramsReceivedErrors() >= 0);

        assertNotNull(tcp4.toString());
        assertNotNull(tcp6.toString());
        assertNotNull(udp4.toString());
        assertNotNull(udp6.toString());
    }
}
