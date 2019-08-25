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
package oshi.hardware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.CentralProcessor.TickType;
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

        ProcessorIdentifier pi = p.getProcessorIdentifier();
        assertNotNull(pi.getVendor());
        assertTrue(pi.getVendorFreq() == -1 || pi.getVendorFreq() > 0);
        assertNotNull(pi.getName());
        assertNotNull(pi.getIdentifier());
        assertNotNull(pi.getProcessorID());
        assertNotNull(pi.getStepping());
        assertNotNull(pi.getModel());
        assertNotNull(pi.getFamily());

        long[] ticks = p.getSystemCpuLoadTicks();
        long[][] procTicks = p.getProcessorCpuLoadTicks();
        assertEquals(ticks.length, TickType.values().length);

        Util.sleep(500);

        assertTrue(p.getSystemCpuLoadBetweenTicks(ticks) >= 0 && p.getSystemCpuLoadBetweenTicks(ticks) <= 1);
        assertEquals(3, p.getSystemLoadAverage(3).length);

        assertEquals(p.getProcessorCpuLoadBetweenTicks(procTicks).length, p.getLogicalProcessorCount());
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertTrue(p.getProcessorCpuLoadBetweenTicks(procTicks)[cpu] >= 0
                    && p.getProcessorCpuLoadBetweenTicks(procTicks)[cpu] <= 1);
            assertEquals(p.getProcessorCpuLoadTicks()[cpu].length, TickType.values().length);
        }

        assertTrue(p.getLogicalProcessorCount() >= p.getPhysicalProcessorCount());
        assertTrue(p.getPhysicalProcessorCount() > 0);
        assertTrue(p.getPhysicalProcessorCount() >= p.getPhysicalPackageCount());
        assertTrue(p.getPhysicalPackageCount() > 0);
        assertTrue(p.getContextSwitches() >= 0);
        assertTrue(p.getInterrupts() >= 0);

        long max = p.getMaxFreq();
        long[] curr = p.getCurrentFreq();
        assertEquals(curr.length, p.getLogicalProcessorCount());
        if (max >= 0) {
            for (int i = 0; i < curr.length; i++) {
                assertTrue(curr[i] <= max);
            }
        }
    }
}
