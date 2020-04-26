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
            assertNotNull(net.queryNetworkInterface());
            assertNotNull(net.getName());
            assertNotNull(net.getDisplayName());
            assertNotNull(net.getMacaddr());
            assertNotNull(net.getIPv4addr());
            assertNotNull(net.getSubnetMasks());
            assertNotNull(net.getIPv6addr());
            assertNotNull(net.getPrefixLengths());
            assertTrue(net.getIfType() >= 0);
            assertTrue(net.getNdisPhysicalMediumType() >= 0);
            assertTrue(net.getBytesRecv() >= 0);
            assertTrue(net.getBytesSent() >= 0);
            assertTrue(net.getPacketsRecv() >= 0);
            assertTrue(net.getPacketsSent() >= 0);
            assertTrue(net.getInErrors() >= 0);
            assertTrue(net.getOutErrors() >= 0);
            assertTrue(net.getInDrops() >= 0);
            assertTrue(net.getCollisions() >= 0);
            assertTrue(net.getSpeed() >= 0);
            assertTrue(net.getMTU() >= 0);
            assertTrue(net.getTimeStamp() > 0);

            net.updateAttributes();
            assertTrue(net.getBytesRecv() >= 0);
            assertTrue(net.getBytesSent() >= 0);
            assertTrue(net.getPacketsRecv() >= 0);
            assertTrue(net.getPacketsSent() >= 0);
            assertTrue(net.getInErrors() >= 0);
            assertTrue(net.getOutErrors() >= 0);
            assertTrue(net.getInDrops() >= 0);
            assertTrue(net.getCollisions() >= 0);
            assertTrue(net.getSpeed() >= 0);
            assertTrue(net.getTimeStamp() > 0);

            if (net.getMacaddr().startsWith("00:00:00") || net.getMacaddr().length() < 8) {
                assertFalse(net.isKnownVmMacAddr());
            }
        }
    }
}
