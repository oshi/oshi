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
        assertTrue("IPV4 TCP connections should be 0 or higher", tcp4.getConnectionsEstablished() >= 0);
        assertTrue("IPV4 TCP active connections should be 0 or higher", tcp4.getConnectionsActive() >= 0);
        assertTrue("IPV4 TCP passive connections should be 0 or higher", tcp4.getConnectionsPassive() >= 0);
        assertTrue("IPV4 TCP connection failures should be 0 or higher", tcp4.getConnectionFailures() >= 0);
        assertTrue("IPV4 TCP connections reset should be 0 or higher", tcp4.getConnectionsReset() >= 0);
        assertTrue("IPV4 TCP segments received should be 0 or higher", tcp4.getSegmentsReceived() >= 0);
        assertTrue("IPV4 TCP segments sent should be 0 or higher", tcp4.getSegmentsSent() >= 0);
        assertTrue("IPV4 TCP segments retransmitted should be 0 or higher", tcp4.getSegmentsRetransmitted() >= 0);
        assertTrue("IPV4 TCP errors should be or higher ", tcp4.getInErrors() >= 0);
        assertTrue("IPV4 TCP segments transmitted with the reset flag should be 0 or higher", tcp4.getOutResets() >= 0);

        TcpStats tcp6 = ipStats.getTCPv6Stats();
        assertTrue("IPV6 TCP connections should be 0 or higher", tcp6.getConnectionsEstablished() >= 0);
        assertTrue("IPV6 TCP active connections should be 0 or higher", tcp6.getConnectionsActive() >= 0);
        assertTrue("IPV6 TCP passive connections should be 0 or higher", tcp6.getConnectionsPassive() >= 0);
        assertTrue("IPV6 TCP connection failures should be 0 or higher", tcp6.getConnectionFailures() >= 0);
        assertTrue("IPV6 TCP connections reset should be 0 or higher", tcp6.getConnectionsReset() >= 0);
        assertTrue("IPV6 TCP segments received should be 0 or higher", tcp6.getSegmentsReceived() >= 0);
        assertTrue("IPV6 TCP segments sent should be 0 or higher", tcp6.getSegmentsSent() >= 0);
        assertTrue("IPV6 TCP segments retransmitted should be 0 or higher", tcp6.getSegmentsRetransmitted() >= 0);
        assertTrue("IPV6 TCP errors should be or higher ", tcp6.getInErrors() >= 0);
        assertTrue("IPV6 TCP segments transmitted with the reset flag should be 0 or higher", tcp6.getOutResets() >= 0);

        UdpStats udp4 = ipStats.getUDPv4Stats();
        assertTrue("IPV4 UDP datagrams delivered to UDP users should be 0 or higher", udp4.getDatagramsReceived() >= 0);
        assertTrue("IPV4 UDP datagrams sent should be 0 or higher", udp4.getDatagramsSent() >= 0);
        assertTrue("IPV4 UDP datagrams received with no application at the destination port should be 0 or higher", udp4.getDatagramsNoPort() >= 0);
        assertTrue("IPV4 UDP datagrams received that could not be delivered for reasons other than lack of application at the destination port should be 0 or higher",
                udp4.getDatagramsReceivedErrors() >= 0);

        UdpStats udp6 = ipStats.getUDPv6Stats();
        assertTrue("IPV6 UDP datagrams delivered to UDP users should be 0 or higher", udp6.getDatagramsReceived() >= 0);
        assertTrue("IPV6 UDP datagrams sent should be 0 or higher", udp6.getDatagramsSent() >= 0);
        assertTrue("IPV6 UDP datagrams received with no application at the destination port should be 0 or higher", udp6.getDatagramsNoPort() >= 0);
        assertTrue("IPV6 UDP datagrams received that could not be delivered for reasons other than lack of application at the destination port should be 0 or higher",
                udp6.getDatagramsReceivedErrors() >= 0);

        assertNotNull("IPV4 TCP stats shouldn't be null", tcp4.toString());
        assertNotNull("IPV6 TCP stats shouldn't be null", tcp6.toString());
        assertNotNull("IPV4 UDP stats shouldn't be null", udp4.toString());
        assertNotNull("IPV6 UDP stats shouldn't be null", udp6.toString());
    }
}
