/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * A CPU
 */
@ThreadSafe
final class SolarisCentralProcessor extends AbstractCentralProcessor {

    private static final String KSTAT_SYSTEM_CPU = "kstat:/system/cpu/";
    private static final String INFO = "/info";
    private static final String SYS = "/sys";

    private static final String KSTAT_PM_CPU = "kstat:/pm/cpu/";
    private static final String PSTATE = "/pstate";

    private static final String CPU_INFO = "cpu_info";

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        boolean cpu64bit = "64".equals(ExecutingCommand.getFirstAnswer("isainfo -b").trim());
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return queryProcessorId2(cpu64bit);
        }
        String cpuVendor = "";
        String cpuName = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long cpuFreq = 0L;

        // Get first result
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = KstatChain.lookup(CPU_INFO, -1, null);
            // Set values
            if (ksp != null && KstatChain.read(ksp)) {
                cpuVendor = KstatUtil.dataLookupString(ksp, "vendor_id");
                cpuName = KstatUtil.dataLookupString(ksp, "brand");
                cpuFamily = KstatUtil.dataLookupString(ksp, "family");
                cpuModel = KstatUtil.dataLookupString(ksp, "model");
                cpuStepping = KstatUtil.dataLookupString(ksp, "stepping");
                cpuFreq = KstatUtil.dataLookupLong(ksp, "clock_MHz") * 1_000_000L;
            }
        }
        String processorID = getProcessorID(cpuStepping, cpuModel, cpuFamily);

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    private static ProcessorIdentifier queryProcessorId2(boolean cpu64bit) {
        Object[] results = KstatUtil.queryKstat2(KSTAT_SYSTEM_CPU + "0" + INFO, "vendor_id", "brand", "family", "model",
                "stepping", "clock_MHz");

        String cpuVendor = results[0] == null ? "" : (String) results[0];
        String cpuName = results[1] == null ? "" : (String) results[1];
        String cpuFamily = results[2] == null ? "" : results[2].toString();
        String cpuModel = results[3] == null ? "" : results[3].toString();
        String cpuStepping = results[4] == null ? "" : results[4].toString();
        long cpuFreq = results[5] == null ? 0L : (long) results[5];

        String processorID = getProcessorID(cpuStepping, cpuModel, cpuFamily);
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected List<LogicalProcessor> initProcessorCounts() {
        Map<Integer, Integer> numaNodeMap = mapNumaNodes();
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return initProcessorCounts2(numaNodeMap);
        }
        List<LogicalProcessor> logProcs = new ArrayList<>();
        try (KstatChain kc = KstatUtil.openChain()) {
            List<Kstat> kstats = KstatChain.lookupAll(CPU_INFO, -1, null);

            for (Kstat ksp : kstats) {
                if (ksp != null && KstatChain.read(ksp)) {
                    int procId = logProcs.size(); // 0-indexed
                    String chipId = KstatUtil.dataLookupString(ksp, "chip_id");
                    String coreId = KstatUtil.dataLookupString(ksp, "core_id");
                    LogicalProcessor logProc = new LogicalProcessor(procId, ParseUtil.parseIntOrDefault(coreId, 0),
                            ParseUtil.parseIntOrDefault(chipId, 0), numaNodeMap.getOrDefault(procId, 0));
                    logProcs.add(logProc);
                }
            }
        }
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        return logProcs;
    }

    private static List<LogicalProcessor> initProcessorCounts2(Map<Integer, Integer> numaNodeMap) {
        List<LogicalProcessor> logProcs = new ArrayList<>();

        List<Object[]> results = KstatUtil.queryKstat2List(KSTAT_SYSTEM_CPU, INFO, "chip_id", "core_id");
        for (Object[] result : results) {
            int procId = logProcs.size();
            long chipId = result[0] == null ? 0L : (long) result[0];
            long coreId = result[1] == null ? 0L : (long) result[1];
            LogicalProcessor logProc = new LogicalProcessor(procId, (int) coreId, (int) chipId,
                    numaNodeMap.getOrDefault(procId, 0));
            logProcs.add(logProc);
        }
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        return logProcs;
    }

    private static Map<Integer, Integer> mapNumaNodes() {
        // Get numa node info from lgrpinfo
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        int lgroup = 0;
        for (String line : ExecutingCommand.runNative("lgrpinfo -c leaves")) {
            // Format:
            // lgroup 0 (root):
            // CPUs: 0 1
            // CPUs: 0-7
            // CPUs: 0-3 6 7 12 13
            // CPU: 0
            // CPU: 1
            if (line.startsWith("lgroup")) {
                lgroup = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("CPUs:") || line.contains("CPU:")) {
                for (Integer cpu : ParseUtil.parseHyphenatedIntList(line.split(":")[1])) {
                    numaNodeMap.put(cpu, lgroup);
                }
            }
        }
        return numaNodeMap;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // Average processor ticks
        long[][] procTicks = getProcessorCpuLoadTicks();
        for (int i = 0; i < ticks.length; i++) {
            for (long[] procTick : procTicks) {
                ticks[i] += procTick[i];
            }
            ticks[i] /= procTicks.length;
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return queryCurrentFreq2();
        }
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, -1);
        try (KstatChain kc = KstatUtil.openChain()) {
            for (int i = 0; i < freqs.length; i++) {
                for (Kstat ksp : KstatChain.lookupAll(CPU_INFO, i, null)) {
                    if (KstatChain.read(ksp)) {
                        freqs[i] = KstatUtil.dataLookupLong(ksp, "current_clock_Hz");
                    }
                }
            }
        }
        return freqs;
    }

    public long[] queryCurrentFreq2() {
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, -1);

        List<Object[]> results = KstatUtil.queryKstat2List(KSTAT_SYSTEM_CPU, INFO, "current_clock_Hz");
        int cpu = -1;
        for (Object[] result : results) {
            if (++cpu >= freqs.length) {
                break;
            }
            freqs[cpu] = result[0] == null ? -1L : (long) result[0];
        }
        return freqs;
    }

    @Override
    public long queryMaxFreq() {
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return queryMaxFreq2();
        }
        long max = -1L;
        try (KstatChain kc = KstatUtil.openChain()) {
            for (Kstat ksp : KstatChain.lookupAll(CPU_INFO, 0, null)) {
                if (KstatChain.read(ksp)) {
                    String suppFreq = KstatUtil.dataLookupString(ksp, "supported_frequencies_Hz");
                    if (!suppFreq.isEmpty()) {
                        for (String s : suppFreq.split(":")) {
                            long freq = ParseUtil.parseLongOrDefault(s, -1L);
                            if (max < freq) {
                                max = freq;
                            }
                        }
                    }
                }
            }
        }
        return max;
    }

    public long queryMaxFreq2() {
        long max = -1L;
        List<Object[]> results = KstatUtil.queryKstat2List(KSTAT_PM_CPU, PSTATE, "supported_frequencies");
        for (Object[] result : results) {
            for (long freq : result[0] == null ? new long[0] : (long[]) result[0]) {
                if (freq > max) {
                    max = freq;
                }
            }
        }
        return max;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = SolarisLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return queryProcessorCpuLoadTicks2();
        }
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        int cpu = -1;
        try (KstatChain kc = KstatUtil.openChain()) {
            for (Kstat ksp : KstatChain.lookupAll("cpu", -1, "sys")) {
                // This is a new CPU
                if (++cpu >= ticks.length) {
                    // Shouldn't happen
                    break;
                }
                if (KstatChain.read(ksp)) {
                    ticks[cpu][TickType.IDLE.getIndex()] = KstatUtil.dataLookupLong(ksp, "cpu_ticks_idle");
                    ticks[cpu][TickType.SYSTEM.getIndex()] = KstatUtil.dataLookupLong(ksp, "cpu_ticks_kernel");
                    ticks[cpu][TickType.USER.getIndex()] = KstatUtil.dataLookupLong(ksp, "cpu_ticks_user");
                }
            }
        }
        return ticks;
    }

    private long[][] queryProcessorCpuLoadTicks2() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        List<Object[]> results = KstatUtil.queryKstat2List(KSTAT_SYSTEM_CPU, SYS, "cpu_ticks_idle", "cpu_ticks_kernel",
                "cpu_ticks_user");
        int cpu = -1;
        for (Object[] result : results) {
            if (++cpu >= ticks.length) {
                break;
            }
            ticks[cpu][TickType.IDLE.getIndex()] = result[0] == null ? 0L : (long) result[0];
            ticks[cpu][TickType.SYSTEM.getIndex()] = result[1] == null ? 0L : (long) result[1];
            ticks[cpu][TickType.USER.getIndex()] = result[2] == null ? 0L : (long) result[2];
        }
        return ticks;
    }

    /**
     * Fetches the ProcessorID by encoding the stepping, model, family, and feature
     * flags.
     *
     * @param stepping
     *            The stepping
     * @param model
     *            The model
     * @param family
     *            The family
     * @return The Processor ID string
     */
    private static String getProcessorID(String stepping, String model, String family) {
        List<String> isainfo = ExecutingCommand.runNative("isainfo -v");
        StringBuilder flags = new StringBuilder();
        for (String line : isainfo) {
            if (line.startsWith("32-bit")) {
                break;
            } else if (!line.startsWith("64-bit")) {
                flags.append(' ').append(line.trim());
            }
        }
        return createProcessorID(stepping, model, family, ParseUtil.whitespaces.split(flags.toString().toLowerCase()));
    }

    @Override
    public long queryContextSwitches() {
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return queryContextSwitches2();
        }
        long swtch = 0;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/pswitch\\\\|inv_swtch/");
        for (String s : kstat) {
            swtch += ParseUtil.parseLastLong(s, 0L);
        }
        return swtch;
    }

    public long queryContextSwitches2() {
        long swtch = 0;
        List<Object[]> results = KstatUtil.queryKstat2List(KSTAT_SYSTEM_CPU, SYS, "pswitch", "inv_swtch");
        for (Object[] result : results) {
            swtch += result[0] == null ? 0L : (long) result[0];
            swtch += result[1] == null ? 0L : (long) result[1];
        }
        return swtch;
    }

    @Override
    public long queryInterrupts() {
        if (SolarisOperatingSystem.IS_11_4_OR_HIGHER) {
            // Use Kstat2 implementation
            return queryInterrupts2();
        }
        long intr = 0;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/intr/");
        for (String s : kstat) {
            intr += ParseUtil.parseLastLong(s, 0L);
        }
        return intr;
    }

    public long queryInterrupts2() {
        long intr = 0;
        List<Object[]> results = KstatUtil.queryKstat2List(KSTAT_SYSTEM_CPU, SYS, "intr");
        for (Object[] result : results) {
            intr += result[0] == null ? 0L : (long) result[0];
        }
        return intr;
    }
}
