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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertNotNull("Processor Identifier's vendor shouldn't be Null", pi.getVendor());
        assertTrue("Processor Identifier's vendor frequency should either be -1, or a greater than 0",
                pi.getVendorFreq() == -1 || pi.getVendorFreq() > 0);
        assertNotNull("Processor Identifier's name shouldn't be Null", pi.getName());
        assertNotNull("Processor Identifier's identifier shouldn't be Null", pi.getIdentifier());
        assertNotNull("Processor Identifier's ID shouldn't be Null", pi.getProcessorID());
        assertNotNull("Processor Identifier's stepping shouldn't be Null", pi.getStepping());
        assertNotNull("Processor Identifier's model shouldn't be null", pi.getModel());
        assertNotNull("Processor Identifier's family shouldn't be null", pi.getFamily());
        assertNotNull("Processor Identifier's toString shouldn't be null", pi.toString());
        assertFalse("Processor Identifier's micro-architecture shouldn't be blank",
                Util.isBlank(pi.getMicroarchitecture()));

        long[] ticks = p.getSystemCpuLoadTicks();
        long[][] procTicks = p.getProcessorCpuLoadTicks();
        assertEquals("System should have the same amount of cpu-load-tick counters as there are TickType values",
                ticks.length, TickType.values().length);

        Util.sleep(500);

        assertTrue("System's cpu load between ticks should be inclusively between 0 and 1",
                p.getSystemCpuLoadBetweenTicks(ticks) >= 0 && p.getSystemCpuLoadBetweenTicks(ticks) <= 1);
        assertEquals("System's load averages length for 3 elements should equal 3", 3,
                p.getSystemLoadAverage(3).length);

        assertEquals("Central Processor's cpu load between ticks should equal the logical processor count",
                p.getProcessorCpuLoadBetweenTicks(procTicks).length, p.getLogicalProcessorCount());
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertTrue(
                    "Central Processor's cpu number " + cpu
                            + "'s load between ticks should be inclusively between 0 and 1",
                    p.getProcessorCpuLoadBetweenTicks(procTicks)[cpu] >= 0
                            && p.getProcessorCpuLoadBetweenTicks(procTicks)[cpu] <= 1);
            assertEquals(
                    "Central Processor's cpu number " + cpu
                            + " should have the same amount of cpu-load-tick counters as there are TickType values",
                    p.getProcessorCpuLoadTicks()[cpu].length, TickType.values().length);
        }

        assertTrue(
                "Central Processor's logical processor count should be at least as high as its physical processor count",
                p.getLogicalProcessorCount() >= p.getPhysicalProcessorCount());
        assertTrue("Central Processor's physical processor count should by higher than 0",
                p.getPhysicalProcessorCount() > 0);
        assertTrue("Central Processor's physical processor count should be higher than its physical package count",
                p.getPhysicalProcessorCount() >= p.getPhysicalPackageCount());
        assertTrue("Central Processor's physical package count should be higher than 0",
                p.getPhysicalPackageCount() > 0);
        assertTrue("Central Processor's context switch count should be 0 or higher", p.getContextSwitches() >= 0);
        assertTrue("Central Processor's interrupt count should be 0 or higher", p.getInterrupts() >= 0);

        long max = p.getMaxFreq();
        long[] curr = p.getCurrentFreq();
        assertEquals(
                "Central Processor's logical processor frequency array length should be the same as its logical processor count",
                curr.length, p.getLogicalProcessorCount());
        if (max >= 0) {
            for (int i = 0; i < curr.length; i++) {
                assertTrue("Central Processor's logical processor frequency should be at most it's max frequency",
                        curr[i] <= max);
            }
        }

        for (int lp = 0; lp < p.getLogicalProcessorCount(); lp++) {
            assertTrue("Logical processor number is negative",
                    p.getLogicalProcessors().get(lp).getProcessorNumber() >= 0);
            switch (SystemInfo.getCurrentPlatformEnum()) {
            case WINDOWS:
                if (p.getLogicalProcessorCount() < 64) {
                    assertEquals("Processor group should be 0 for Windows systems with less than 64 logical processors",
                            0, p.getLogicalProcessors().get(lp).getProcessorGroup());
                }
                assertTrue("NUMA node number is negative", p.getLogicalProcessors().get(lp).getNumaNode() >= 0);
                break;
            case LINUX:
                assertEquals("Processor group should be 0 for Linux systems", 0,
                        p.getLogicalProcessors().get(lp).getProcessorGroup());
                assertTrue("NUMA node number is negative", p.getLogicalProcessors().get(lp).getNumaNode() >= 0);
                break;
            case MACOSX:
                assertEquals("Processor group should be 0 for macOS systems", 0,
                        p.getLogicalProcessors().get(lp).getProcessorGroup());
                assertEquals("NUMA Node should be 0 for macOS systems", 0,
                        p.getLogicalProcessors().get(lp).getNumaNode());
                break;
            case SOLARIS:
                assertEquals("Processor group should be 0 for Solaris systems", 0,
                        p.getLogicalProcessors().get(lp).getProcessorGroup());
                assertTrue("NUMA node number is negative", p.getLogicalProcessors().get(lp).getNumaNode() >= 0);
                break;
            case FREEBSD:
                assertEquals("Processor group should be 0 for FreeBSD systems", 0,
                        p.getLogicalProcessors().get(lp).getProcessorGroup());
                assertEquals("NUMA Node should be 0 for FreeBSD systems", 0,
                        p.getLogicalProcessors().get(lp).getNumaNode());
                break;
            default:
                break;
            }
        }
    }
}
