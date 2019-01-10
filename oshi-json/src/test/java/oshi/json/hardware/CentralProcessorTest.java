/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
    @Test
    public void testCentralProcessor() {
        SystemInfo si = new SystemInfo();
        CentralProcessor p = si.getHardware().getProcessor();

        assertNotNull(p.getVendor());
        assertTrue(p.getVendorFreq() == -1 || p.getVendorFreq() > 0);
        p.setVendor("v");
        assertEquals("v", p.getVendor());

        assertNotNull(p.getName());
        p.setName("n");
        assertEquals("n", p.getName());

        assertNotNull(p.getIdentifier());
        p.setIdentifier("i");
        assertEquals("i", p.getIdentifier());

        assertNotNull(p.getProcessorID());
        p.setProcessorID("p");
        assertEquals("p", p.getProcessorID());

        p.setCpu64(true);
        assertTrue(p.isCpu64bit());

        assertNotNull(p.getStepping());
        p.setStepping("s");
        assertEquals("s", p.getStepping());

        assertNotNull(p.getModel());
        p.setModel("m");
        assertEquals("m", p.getModel());

        assertNotNull(p.getFamily());
        p.setFamily("f");
        assertEquals("f", p.getFamily());

        assertTrue(p.getSystemCpuLoadBetweenTicks() >= 0 && p.getSystemCpuLoadBetweenTicks() <= 1);
        assertEquals(p.getSystemCpuLoadTicks().length, TickType.values().length);

        Util.sleep(500);
        // This test fails on FreeBSD due to error in Java MXBean
        if (SystemInfo.getCurrentPlatformEnum() != PlatformEnum.FREEBSD) {
            assertTrue(p.getSystemCpuLoad() <= 1.0);
        }
        assertEquals(3, p.getSystemLoadAverage(3).length);
        if (Platform.isMac() || Platform.isLinux()) {
            assertTrue(p.getSystemLoadAverage() >= 0.0);
        }

        assertEquals(p.getProcessorCpuLoadBetweenTicks().length, p.getLogicalProcessorCount());
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertTrue(p.getProcessorCpuLoadBetweenTicks()[cpu] >= 0 && p.getProcessorCpuLoadBetweenTicks()[cpu] <= 1);
            assertEquals(p.getProcessorCpuLoadTicks()[cpu].length, TickType.values().length);
        }

        assertTrue(p.getSystemUptime() > 0);
        assertTrue(p.getLogicalProcessorCount() >= p.getPhysicalProcessorCount());
        assertTrue(p.getPhysicalProcessorCount() > 0);
        assertTrue(p.getPhysicalProcessorCount() >= p.getPhysicalPackageCount());
        assertTrue(p.getPhysicalPackageCount() > 0);
        assertTrue(p.getContextSwitches() >= 0);
        assertTrue(p.getInterrupts() >= 0);
    }
}
