/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;

import com.sun.jna.Platform;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Tests for {@link CpuStat}.
 */
public class CpuStatTest {

    @Test
    public void testSystemCpuLoadTicks() {
        if (Platform.isLinux()) {
            long[] systemCpuLoadTicks = CpuStat.getSystemCpuLoadTicks();
            for (long systemCpuTick : systemCpuLoadTicks) {
                assertThat("CPU tick should be greater than or equal to 0", systemCpuTick, greaterThanOrEqualTo(0L));
            }
        }
    }

    @Test
    public void testGetProcessorCpuLoadTicks() {
        if (Platform.isLinux()) {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor processor = hal.getProcessor();
            int logicalProcessorCount = processor.getLogicalProcessorCount();
            long[][] processorCpuLoadTicks = CpuStat.getProcessorCpuLoadTicks(logicalProcessorCount);
            for (long[] cpuTicks : processorCpuLoadTicks) {
                for (long cpuTick : cpuTicks) {
                    assertThat("CPU tick should be greater than or equal to 0", cpuTick, greaterThanOrEqualTo(0L));
                }
            }
        }
    }

    @Test
    public void testGetContextSwitches() {
        if (Platform.isLinux()) {
            assertThat("Context switches should be greater than or equal to -1", CpuStat.getContextSwitches(),
                    greaterThanOrEqualTo(-1L));
        }
    }

    @Test
    public void testGetInterrupts() {
        if (Platform.isLinux()) {
            assertThat("Interrupts should be greater than or equal to -1", CpuStat.getInterrupts(),
                    greaterThanOrEqualTo(-1L));
        }
    }

    @Test
    public void testGetBootTime() {
        if (Platform.isLinux()) {
            assertThat("Boot time should be greater than or equal to 0", CpuStat.getBootTime(),
                    greaterThanOrEqualTo(0L));
        }
    }
}
