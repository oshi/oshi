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
package oshi.hardware.platform.unix.openbsd;

import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CP_IDLE;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CP_INTR;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CP_NICE;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CP_SYS;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CP_USER;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CTL_HW;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.HW_CPUSPEED;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.HW_MACHINE;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.HW_MODEL;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.HW_NCPU;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.KERN_CPTIME;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.unix.openbsd.OpenBsdLibc.CpTime;
import oshi.jna.platform.unix.openbsd.OpenBsdLibc.CpTimeNew;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

public class OpenBsdCentralProcessor extends AbstractCentralProcessor {

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = OpenBsdSysctlUtil.sysctl("machdep.cpuvendor", "");
        int[] mib = new int[2];
        mib[0] = CTL_HW;
        mib[1] = HW_MODEL;
        String cpuName = OpenBsdSysctlUtil.sysctl(mib, "");
        // TODO, probably parse processorID=CPUID?
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long cpuFreq = queryMaxFreq();
        mib[1] = HW_MACHINE;
        String machine = OpenBsdSysctlUtil.sysctl(mib, "");
        boolean cpu64bit = machine != null && machine.contains("64")
                || ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
        String processorID = OpenBsdSysctlUtil.sysctl("machdep.cpuid", "");

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected long queryMaxFreq() {
        return queryCurrentFreq()[0];
        // This is vendor freq.
        // return ParseUtil.parseHertz(OpenBsdSysctlUtil.sysctl("hw.model", "0")) *
        // 1_000_000L;
    }

    @Override
    protected long[] queryCurrentFreq() {
        int[] mib = new int[2];
        mib[0] = CTL_HW;
        mib[1] = HW_CPUSPEED;
        return new long[] { OpenBsdSysctlUtil.sysctl(mib, 0L) * 1_000_000L };
    }

    @Override
    protected List<LogicalProcessor> initProcessorCounts() {
        int[] mib = new int[2];
        mib[0] = CTL_HW;
        mib[1] = HW_NCPU;
        int logicalProcessorCount = OpenBsdSysctlUtil.sysctl(mib, 1);
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        for (int i = 0; i < logicalProcessorCount; i++) {
            logProcs.add(new LogicalProcessor(i, 1, 1));
        }
        return logProcs;
    }

    /**
     * Get number of context switches
     *
     * @return The context switches
     */
    @Override
    protected long queryContextSwitches() {
        return 0;
    }

    /**
     * Get number of interrupts
     *
     * @return The interrupts
     */
    @Override
    protected long queryInterrupts() {
        return 0;
    }

    /**
     * Get the system CPU load ticks
     *
     * @return The system CPU load ticks
     */
    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // use
        // └─ $ ▶ sysctl kern.cp_time
        // kern.cp_time=765981,576,193424,42002,3534,3819889
        // An array of longs of size CPUSTATES is returned, containing statistics about
        // the number of ticks spent by the system in
        // interrupt processing, user processes (nice(1) or normal), system processing,
        // lock spinning, or idling.
        int[] mib = new int[2];
        mib[0] = CTL_KERN;
        mib[1] = KERN_CPTIME;
        Memory m = OpenBsdSysctlUtil.sysctl(mib);
        // array of 5 or 6 longs
        long[] cpuTicks = null;
        if (m != null) {
            if (m.size() == 5 * Native.LONG_SIZE) {
                cpuTicks = new CpTime(m).cpu_ticks;
            } else if (m.size() == 6 * Native.LONG_SIZE) {
                // Version 6.4 and later has CP_SPIN at index 3
                cpuTicks = new CpTimeNew(m).cpu_ticks;
            }
        }
        if (cpuTicks != null) {
            ticks[TickType.USER.getIndex()] = cpuTicks[CP_USER];
            ticks[TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
            int offset = cpuTicks.length > 5 ? 1 : 0;
            ticks[TickType.IRQ.getIndex()] = cpuTicks[CP_INTR + offset];
            ticks[TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE + offset];
        }

//        String cpu = OpenBsdSysctlUtil.sysctl("kern.cp_time", "");
//        String[] split = cpu.split(",");
//        if (split.length > 4) {
//            ticks[TickType.USER.getIndex()] = ParseUtil.parseLongOrDefault(split[0], 0L);
//            ticks[TickType.NICE.getIndex()] = ParseUtil.parseLongOrDefault(split[1], 0L);
//            ticks[TickType.SYSTEM.getIndex()] = ParseUtil.parseLongOrDefault(split[2], 0L);
//            int offset = split.length > 5 ? 1 : 0;
//            // Version 6.4 and later has CP_SPIN at index 3
//            ticks[TickType.IRQ.getIndex()] = ParseUtil.parseLongOrDefault(split[3 + offset], 0L);
//            ticks[TickType.IDLE.getIndex()] = ParseUtil.parseLongOrDefault(split[4 + offset], 0L);
//        }
        return ticks;
    }

    /**
     * Get the processor CPU load ticks
     *
     * @return The processor CPU load ticks
     */
    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        // Need to use binary sysctl to access CPU parameter. As a placeholder just set
        // CPU 0 as total
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        ticks[0] = querySystemCpuLoadTicks();
        return ticks;
    }

    /**
     * Returns the system load average for the number of elements specified, up to
     * 3, representing 1, 5, and 15 minutes. The system load average is the sum of
     * the number of runnable entities queued to the available processors and the
     * number of runnable entities running on the available processors averaged over
     * a period of time. The way in which the load average is calculated is
     * operating system specific but is typically a damped time-dependent average.
     * If the load average is not available, a negative value is returned. This
     * method is designed to provide a hint about the system load and may be queried
     * frequently.
     * <p>
     * The load average may be unavailable on some platforms (e.g., Windows) where
     * it is expensive to implement this method.
     *
     * @param nelem
     *            Number of elements to return.
     * @return an array of the system load averages for 1, 5, and 15 minutes with
     *         the size of the array specified by nelem; or negative values if not
     *         available.
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        // vm.loadavg=0.30 0.58 0.75
        double[] load = new double[nelem];
        String avg = OpenBsdSysctlUtil.sysctl("vm.loadavg", "");
        String[] split = ParseUtil.whitespaces.split(avg);
        for (int i = 0; i < nelem && i < split.length; i++) {
            load[i] = ParseUtil.parseDoubleOrDefault(split[i], -1d);
        }
        return load;
    }
}
