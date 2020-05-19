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
package oshi.hardware;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test Networks
 */
public class NetworksTest {
    /**
     * Test network interfaces extraction.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testNetworkInterfaces() throws IOException {
        SystemInfo si = new SystemInfo();

        for (NetworkIF net : si.getHardware().getNetworkIFs()) {
            assertNotNull("NetworkIF should not be null", net.queryNetworkInterface());
            assertNotNull("NetworkIF name should not be null", net.getName());
            assertNotNull("NetworkIF display name should not be null", net.getDisplayName());
            assertNotNull("NetworkIF MacAddress should not be null", net.getMacaddr());
            assertNotNull("NetworkIF IPv4 address should not be null", net.getIPv4addr());
            assertNotNull("NetworkIF SubnetMasks should not be null", net.getSubnetMasks());
            assertNotNull("NetworkIF IPv6 should not be null", net.getIPv6addr());
            assertNotNull("NetworkIF prefix lengths should not be null", net.getPrefixLengths());
            assertTrue("NetworkIF type should not be negative", net.getIfType() >= 0);
            assertTrue("NetworkIF NDisPhysicalMediumType should not be negative", net.getNdisPhysicalMediumType() >= 0);
            assertTrue("NetworkIF bytes received should not be negative", net.getBytesRecv() >= 0);
            assertTrue("NetworkIF bytes sent should not be negative", net.getBytesSent() >= 0);
            assertTrue("NetworkIF packets received should not be negative", net.getPacketsRecv() >= 0);
            assertTrue("NetworkIF packets sent should not be negative", net.getPacketsSent() >= 0);
            assertTrue("NetworkIF InErrors should not be negative", net.getInErrors() >= 0);
            assertTrue("NetworkIF out errors should not be negative", net.getOutErrors() >= 0);
            assertTrue("NetworkIF in drops should not be negative", net.getInDrops() >= 0);
            assertTrue("NetworkIF collisions should not be negative", net.getCollisions() >= 0);
            assertTrue("NetworkIF speed should not be negative", net.getSpeed() >= 0);
            assertTrue("NetworkIF MTU should not be negative", net.getMTU() >= 0);
            assertTrue("NetworkIF time stamp should be positive", net.getTimeStamp() > 0);

            net.updateAttributes();
            assertTrue("NetworkIF bytes received after update attr should not be negative", net.getBytesRecv() >= 0);
            assertTrue("NetworkIF bytes sent after update attr should not be negative", net.getBytesSent() >= 0);
            assertTrue("NetworkIF packets received after update attr should not be negative",
                    net.getPacketsRecv() >= 0);
            assertTrue("NetworkIF packets sent after update attr should not be negative", net.getPacketsSent() >= 0);
            assertTrue("NetworkIF in errors after update attr should not be negative", net.getInErrors() >= 0);
            assertTrue("NetworkIF out errors after update attr should not be negative", net.getOutErrors() >= 0);
            assertTrue("NetworkIF in drops after update attr should not be negative", net.getInDrops() >= 0);
            assertTrue("NetworkIF collisions after update attr should not be negative", net.getCollisions() >= 0);
            assertTrue("NetworkIF speed after update attr should not be negative", net.getSpeed() >= 0);
            assertTrue("NetworkIF time stamp after update attr should not be negative", net.getTimeStamp() > 0);

            if (net.getMacaddr().startsWith("00:00:00") || net.getMacaddr().length() < 8) {
                assertFalse(net.isKnownVmMacAddr());
            }
        }
    }
}
