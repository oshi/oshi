/*
 * MIT License
 *
 * Copyright (c) 2016-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.Util;

/**
 * Test CPU
 */
@TestInstance(Lifecycle.PER_CLASS)
class CentralProcessorTest {

    private CentralProcessor p = null;

    @BeforeAll
    void setUp() {
        SystemInfo si = new SystemInfo();
        this.p = si.getHardware().getProcessor();
    }

    @Test
    void testProcessorIdentifier() {
        ProcessorIdentifier pi = p.getProcessorIdentifier();
        assertThat("Processor Identifier's vendor shouldn't be Null", pi.getVendor(), is(notNullValue()));
        assertThat("Processor Identifier's vendor frequency should either be -1, or a greater than 0",
                pi.getVendorFreq(), is(either(equalTo(-1L)).or(greaterThan(0L))));
        assertThat("Processor Identifier's name shouldn't be Null", pi.getName(), is(notNullValue()));
        assertThat("Processor Identifier's identifier shouldn't be Null", pi.getIdentifier(), is(notNullValue()));
        assertThat("Processor Identifier's ID shouldn't be Null", pi.getProcessorID(), is(notNullValue()));
        assertThat("Processor Identifier's stepping shouldn't be Null", pi.getStepping(), is(notNullValue()));
        assertThat("Processor Identifier's model shouldn't be null", pi.getModel(), is(notNullValue()));
        assertThat("Processor Identifier's family shouldn't be null", pi.getFamily(), is(notNullValue()));
        assertThat("Processor Identifier's toString shouldn't be null", pi.toString(), is(notNullValue()));
        assertThat("Processor Identifier's micro-architecture shouldn't be blank", pi.getMicroarchitecture(),
                is(not(emptyOrNullString())));
    }

    @Test
    void testFrequencies() {
        long max = p.getMaxFreq();
        long[] curr = p.getCurrentFreq();
        assertThat("Logical processor frequency array length should be the same as its logical processor count",
                p.getLogicalProcessorCount(), is(curr.length));
        if (max >= 0) {
            for (long freq : curr) {
                assertThat("Logical processor frequency should be at most it's max frequency", freq,
                        is(lessThanOrEqualTo(max)));
            }
        }
    }

    @Test
    void testTicks() {
        long[] ticks = p.getSystemCpuLoadTicks();
        long[][] procTicks = p.getProcessorCpuLoadTicks();
        assertThat("System should have the same amount of cpu-load-tick counters as there are TickType values",
                TickType.values().length, is(ticks.length));

        Util.sleep(500);

        assertThat("System's cpu load between ticks should be inclusively between 0 and 1",
                p.getSystemCpuLoadBetweenTicks(ticks), is(both(greaterThanOrEqualTo(0d)).and(lessThanOrEqualTo(1d))));
        assertThat("System's load averages length for 3 elements should equal 3", p.getSystemLoadAverage(3).length,
                is(3));

        assertThat("Cpu load between ticks should equal the logical processor count", p.getLogicalProcessorCount(),
                is(p.getProcessorCpuLoadBetweenTicks(procTicks).length));
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertThat("Cpu number " + cpu + "'s load between ticks should be inclusively between 0 and 1",
                    p.getProcessorCpuLoadBetweenTicks(procTicks)[cpu],
                    is(both(greaterThanOrEqualTo(0d)).and(lessThanOrEqualTo(1d))));
            assertThat(
                    "Cpu number " + cpu
                            + " should have the same amount of cpu-load-tick counters as there are TickType values",
                    TickType.values().length, is(p.getProcessorCpuLoadTicks()[cpu].length));
        }
    }

    @Test
    void testDelayTicks() {
        assertThat("System's cpu load should be inclusively between 0 and 1", p.getSystemCpuLoad(500),
                is(both(greaterThanOrEqualTo(0d)).and(lessThanOrEqualTo(1d))));

        double[] procCpuLoad = p.getProcessorCpuLoad(500);
        assertThat("Cpu load array size should equal the logical processor count", p.getLogicalProcessorCount(),
                is(procCpuLoad.length));
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertThat("Cpu number " + cpu + "'s load should be inclusively between 0 and 1", procCpuLoad[cpu],
                    is(both(greaterThanOrEqualTo(0d)).and(lessThanOrEqualTo(1d))));
        }
    }

    @Test
    void testCounts() {
        assertThat("Logical processor count should be at least as high as its physical processor count",
                p.getLogicalProcessorCount(), is(greaterThanOrEqualTo(p.getPhysicalProcessorCount())));
        assertThat("Physical processor count should by higher than 0", p.getPhysicalProcessorCount(),
                is(greaterThan(0)));
        assertThat("Physical processor count should be higher than its physical package count",
                p.getPhysicalProcessorCount(), is(greaterThanOrEqualTo(p.getPhysicalPackageCount())));
        assertThat("Physical package count should be higher than 0", p.getPhysicalPackageCount(), is(greaterThan(0)));

        assertThat("Logical processor list size should match count", p.getLogicalProcessors().size(),
                is(p.getLogicalProcessorCount()));
        assertThat("Physical processor list size should match count", p.getPhysicalProcessors().size(),
                is(p.getPhysicalProcessorCount()));

        assertThat("Context switch count should be 0 or higher", p.getContextSwitches(), is(greaterThanOrEqualTo(0L)));
        assertThat("Interrupt count should be 0 or higher", p.getInterrupts(), is(greaterThanOrEqualTo(0L)));
        for (int lp = 0; lp < p.getLogicalProcessorCount(); lp++) {
            assertThat("Logical processor number is negative", p.getLogicalProcessors().get(lp).getProcessorNumber(),
                    is(greaterThanOrEqualTo(0)));
            switch (SystemInfo.getCurrentPlatform()) {
            case WINDOWS:
                if (p.getLogicalProcessorCount() < 64) {
                    assertThat("Processor group should be 0 for Windows systems with less than 64 logical processors",
                            p.getLogicalProcessors().get(lp).getProcessorGroup(), is(0));
                }
                assertThat("NUMA node number is negative", p.getLogicalProcessors().get(lp).getNumaNode(),
                        is(greaterThanOrEqualTo(0)));
                break;
            case LINUX:
                assertThat("Processor group should be 0 for Linux systems",
                        p.getLogicalProcessors().get(lp).getProcessorGroup(), is(0));
                assertThat("NUMA node number is negative", p.getLogicalProcessors().get(lp).getNumaNode(),
                        is(greaterThanOrEqualTo(0)));
                break;
            case MACOS:
                assertThat("Processor group should be 0 for macOS systems",
                        p.getLogicalProcessors().get(lp).getProcessorGroup(), is(0));
                assertThat("NUMA Node should be 0 for macOS systems", p.getLogicalProcessors().get(lp).getNumaNode(),
                        is(0));
                break;
            case SOLARIS:
                assertThat("Processor group should be 0 for Solaris systems",
                        p.getLogicalProcessors().get(lp).getProcessorGroup(), is(0));
                assertThat("NUMA node number is negative", p.getLogicalProcessors().get(lp).getNumaNode(),
                        is(greaterThanOrEqualTo(0)));
                break;
            case FREEBSD:
            case AIX:
                assertThat("Processor group should be 0 for FreeBSD or AIX systems",
                        p.getLogicalProcessors().get(lp).getProcessorGroup(), is(0));
                assertThat("NUMA Node should be 0 for FreeBSD or AIX systems",
                        p.getLogicalProcessors().get(lp).getNumaNode(), is(0));
                break;
            default:
                break;
            }
        }
    }
}
