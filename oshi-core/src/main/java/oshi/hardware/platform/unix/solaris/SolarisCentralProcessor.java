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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.unix.Kstat2;
import oshi.jna.platform.unix.Kstat2.Kstat2Handle;
import oshi.jna.platform.unix.Kstat2.Kstat2Map;
import oshi.jna.platform.unix.Kstat2.Kstat2MatcherList;
import oshi.jna.platform.unix.Kstat2StatusException;
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

    private static final Logger LOG = LoggerFactory.getLogger(SolarisCentralProcessor.class);

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

    private ProcessorIdentifier queryProcessorId2(boolean cpu64bit) {
        String cpuVendor = "";
        String cpuName = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long cpuFreq = 0L;

        // Get first result
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_STRING, "kstat:/system/cpu/0/info");
            Kstat2Handle handle = new Kstat2Handle();
            try {
                Kstat2Map map = handle.lookupMap("kstat:/system/cpu/0/info");
                cpuVendor = (String) map.getValue("vendor_id");
                cpuName = (String) map.getValue("brand");
                cpuFamily = map.getValue("family").toString();
                cpuModel = map.getValue("model").toString();
                cpuStepping = map.getValue("stepping").toString();
                cpuFreq = (long) map.getValue("clock_MHz") * 1_000_000L;
            } finally {
                handle.close();
            }
        } catch (Kstat2StatusException e) {
            LOG.debug("Failed to get info stats on cpu0: {}", e.getMessage());
        } finally {
            matchers.free();
        }
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

    private List<LogicalProcessor> initProcessorCounts2(Map<Integer, Integer> numaNodeMap) {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_GLOB, "kstat:/system/cpu/*/info");
            Kstat2Handle handle = new Kstat2Handle();
            try {
                int cpu = 0;
                while (cpu < Integer.MAX_VALUE) {
                    Kstat2Map map;
                    try {
                        map = handle.lookupMap("kstat:/system/cpu/" + cpu + "/info");
                        int procId = logProcs.size(); // 0-indexed
                        long chipId = (long) map.getValue("chip_id");
                        long coreId = (long) map.getValue("core_id");
                        LogicalProcessor logProc = new LogicalProcessor(procId, (int) coreId, (int) chipId,
                                numaNodeMap.getOrDefault(procId, 0));
                        logProcs.add(logProc);
                    } catch (Kstat2StatusException e) {
                        // Expect error 7 when we finish iteration
                        break;
                    }
                    cpu++;
                }
            } finally {
                handle.close();
            }
        } finally {
            matchers.free();
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
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_GLOB, "kstat:/system/cpu/*/info");
            Kstat2Handle handle = new Kstat2Handle(matchers);
            try {
                Kstat2Map map;
                for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                    try {
                        map = handle.lookupMap("kstat:/system/cpu/" + cpu + "/info");
                        freqs[cpu] = (long) map.getValue("current_clock_Hz");
                    } catch (Kstat2StatusException e) {
                        LOG.debug("Failed to get current_clock_Hz on cpu {}: {}", cpu, e.getMessage());
                    }
                }
            } finally {
                handle.close();
            }
        } finally {
            matchers.free();
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
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_GLOB, "kstat:/pm/cpu/*/pstate");
            Kstat2Handle handle = new Kstat2Handle(matchers);
            try {
                Kstat2Map map;
                for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                    try {
                        map = handle.lookupMap("kstat:/pm/cpu/" + cpu + "/pstate");
                        for (long freq : (long[]) map.getValue("supported_frequencies")) {
                            if (freq > max) {
                                max = freq;
                            }
                        }
                    } catch (Kstat2StatusException e) {
                        LOG.debug("Failed to get current_clock_Hz on cpu {}: {}", cpu, e.getMessage());
                    }
                }
            } finally {
                handle.close();
            }
        } finally {
            matchers.free();
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
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_GLOB, "kstat:/system/cpu/*/sys");
            Kstat2Handle handle = new Kstat2Handle(matchers);
            try {
                Kstat2Map map;
                for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                    try {
                        map = handle.lookupMap("kstat:/system/cpu/" + cpu + "/sys");
                        ticks[cpu][TickType.IDLE.getIndex()] = (long) map.getValue("cpu_ticks_idle");
                        ticks[cpu][TickType.SYSTEM.getIndex()] = (long) map.getValue("cpu_ticks_kernel");
                        ticks[cpu][TickType.USER.getIndex()] = (long) map.getValue("cpu_ticks_user");
                    } catch (Kstat2StatusException e) {
                        LOG.debug("Failed to get stats on cpu {}: {}", cpu, e.getMessage());
                    }
                }
            } finally {
                handle.close();
            }
        } finally {
            matchers.free();
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
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_GLOB, "kstat:/system/cpu/*/sys");
            Kstat2Handle handle = new Kstat2Handle(matchers);
            try {
                Kstat2Map map;
                for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                    try {
                        map = handle.lookupMap("kstat:/system/cpu/" + cpu + "/sys");
                        swtch += (long) map.getValue("pswitch");
                        swtch += (long) map.getValue("inv_swtch");
                    } catch (Kstat2StatusException e) {
                        LOG.debug("Failed to get context switch stats on cpu {}: {}", cpu, e.getMessage());
                    }
                }
            } finally {
                handle.close();
            }
        } finally {
            matchers.free();
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
        Kstat2MatcherList matchers = new Kstat2MatcherList();
        try {
            matchers.addMatcher(Kstat2.KSTAT2_M_GLOB, "kstat:/system/cpu/*/sys");
            Kstat2Handle handle = new Kstat2Handle(matchers);
            try {
                Kstat2Map map;
                for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                    try {
                        map = handle.lookupMap("kstat:/system/cpu/" + cpu + "/sys");
                        intr += (long) map.getValue("intr");
                    } catch (Kstat2StatusException e) {
                        LOG.debug("Failed to get interrupt stats on cpu {}: {}", cpu, e.getMessage());
                    }
                }
            } finally {
                handle.close();
            }
        } finally {
            matchers.free();
        }
        return intr;
    }
}
