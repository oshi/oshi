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
package oshi.json.hardware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sun.jna.Platform;

import oshi.PlatformEnum;
import oshi.hardware.CentralProcessor.TickType;
import oshi.json.SystemInfo;
import oshi.util.Util;

/**
 * Test CPU
 */
public class CentralProcessorTest {

    /**
     * Test central processor.
     */
    @SuppressWarnings("deprecation") // serialNumber until removed
    @Test
    public void testCentralProcessor() {
        SystemInfo si = new SystemInfo();
        CentralProcessor p = si.getHardware().getProcessor();

        assertNotNull(p.getVendor());
        assertTrue(p.getVendorFreq() == -1 || p.getVendorFreq() > 0);
        p.setVendor("v");
        assertEquals(p.getVendor(), "v");

        assertNotNull(p.getName());
        p.setName("n");
        assertEquals(p.getName(), "n");

        assertNotNull(p.getIdentifier());
        p.setIdentifier("i");
        assertEquals(p.getIdentifier(), "i");

        assertNotNull(p.getProcessorID());
        p.setProcessorID("p");
        assertEquals(p.getProcessorID(), "p");

        p.setCpu64(true);
        assertTrue(p.isCpu64bit());

        assertNotNull(p.getStepping());
        p.setStepping("s");
        assertEquals(p.getStepping(), "s");

        assertNotNull(p.getModel());
        p.setModel("m");
        assertEquals(p.getModel(), "m");

        assertNotNull(p.getFamily());
        p.setFamily("f");
        assertEquals(p.getFamily(), "f");

        assertTrue(p.getSystemCpuLoadBetweenTicks() >= 0 && p.getSystemCpuLoadBetweenTicks() <= 1);
        assertEquals(p.getSystemCpuLoadTicks().length, TickType.values().length);

        Util.sleep(500);
        // This test fails on FreeBSD due to error in Java MXBean
        if (SystemInfo.getCurrentPlatformEnum() != PlatformEnum.FREEBSD) {
            assertTrue(p.getSystemCpuLoad() >= 0.0 && p.getSystemCpuLoad() <= 1.0);
        }
        assertEquals(p.getSystemLoadAverage(3).length, 3);
        if (Platform.isMac() || Platform.isLinux()) {
            assertTrue(p.getSystemLoadAverage() >= 0.0);
        }

        assertEquals(p.getProcessorCpuLoadBetweenTicks().length, p.getLogicalProcessorCount());
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertTrue(p.getProcessorCpuLoadBetweenTicks()[cpu] >= 0 && p.getProcessorCpuLoadBetweenTicks()[cpu] <= 1);
            assertEquals(p.getProcessorCpuLoadTicks()[cpu].length, TickType.values().length);
        }

        assertTrue(p.getSystemUptime() > 0);
        assertNotNull(p.getSystemSerialNumber());
        assertTrue(p.getLogicalProcessorCount() >= p.getPhysicalProcessorCount());
        assertTrue(p.getPhysicalProcessorCount() > 0);
        assertTrue(p.getPhysicalProcessorCount() >= p.getPhysicalPackageCount());
        assertTrue(p.getPhysicalPackageCount() > 0);
        assertTrue(p.getContextSwitches() >= 0);
        assertTrue(p.getInterrupts() >= 0);
    }
}
