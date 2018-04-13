/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware;

import static org.junit.Assert.assertEquals;
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
        long timeStamp = System.currentTimeMillis();
        SystemInfo si = new SystemInfo();

        for (NetworkIF net : si.getHardware().getNetworkIFs()) {
            assertNotNull(net.getNetworkInterface());
            assertNotNull(net.getName());
            assertNotNull(net.getDisplayName());
            assertNotNull(net.getMacaddr());
            assertNotNull(net.getIPv4addr());
            assertNotNull(net.getIPv6addr());
            assertTrue(net.getBytesRecv() >= 0);
            assertTrue(net.getBytesSent() >= 0);
            assertTrue(net.getPacketsRecv() >= 0);
            assertTrue(net.getPacketsSent() >= 0);
            assertTrue(net.getInErrors() >= 0);
            assertTrue(net.getOutErrors() >= 0);
            assertTrue(net.getSpeed() >= 0);
            assertTrue(net.getMTU() >= 0);
            assertTrue(net.getTimeStamp() > 0);

            net.setBytesRecv(10L);
            net.setBytesSent(20L);
            net.setPacketsRecv(30L);
            net.setPacketsSent(40L);
            net.setInErrors(60L);
            net.setOutErrors(70L);
            net.setSpeed(50L);
            net.setTimeStamp(timeStamp);

            assertEquals(10L, net.getBytesRecv());
            assertEquals(20L, net.getBytesSent());
            assertEquals(30L, net.getPacketsRecv());
            assertEquals(40L, net.getPacketsSent());
            assertEquals(60L, net.getInErrors());
            assertEquals(70L, net.getOutErrors());
            assertEquals(50L, net.getSpeed());
            assertEquals(timeStamp, net.getTimeStamp());

            net.updateNetworkStats();
            assertTrue(net.getBytesRecv() >= 0);
            assertTrue(net.getBytesSent() >= 0);
            assertTrue(net.getPacketsRecv() >= 0);
            assertTrue(net.getPacketsSent() >= 0);
            assertTrue(net.getInErrors() >= 0);
            assertTrue(net.getOutErrors() >= 0);
            assertTrue(net.getSpeed() >= 0);
            assertTrue(net.getTimeStamp() > 0);
        }
    }
}
