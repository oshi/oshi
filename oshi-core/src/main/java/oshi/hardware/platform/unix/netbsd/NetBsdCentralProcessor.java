/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.netbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * NetBSD Central Processor implementation
 */
@ThreadSafe
public class NetBsdCentralProcessor extends AbstractCentralProcessor {

    private static final int CPUSTATES = 5;
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;

    private final Supplier<Pair<Long, Long>> vmStats = memoize(NetBsdCentralProcessor::queryVmStats,
            defaultExpiration());
    private static final Pattern DMESG_CPU = Pattern.compile("cpu(\\d+): smt (\\d+), core (\\d+), package (\\d+)");

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = NetBsdSysctlUtil.sysctl("machdep.cpu_vendor", "");
        if (cpuVendor.isEmpty()) {
            cpuVendor = ExecutingCommand.getFirstAnswer("sysctl -n machdep.dmi.processor-vendor").trim();
        }
        String cpuName = NetBsdSysctlUtil.sysctl("machdep.cpu_brand", "");
        if (cpuName.isEmpty()) {
            cpuName = NetBsdSysctlUtil.sysctl("hw.model", "");
        }
        // NetBSD provides family/model/stepping via dmesg
        // e.g., "cpu0: Intel(R) ... 06-7a-01" or from machdep
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        String processorID = "";
        // Try to parse from dmesg "cpu0: ... XX-YY-ZZ"
        for (String line : ExecutingCommand.runNative("dmesg")) {
            if (line.startsWith("cpu0:") && line.matches(".*\\d+-[\\da-fA-F]+-[\\da-fA-F]+.*")) {
                String[] parts = line.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.matches("[\\da-fA-F]+-[\\da-fA-F]+-[\\da-fA-F]+")) {
                        String[] fms = trimmed.split("-");
                        cpuFamily = fms[0];
                        cpuModel = fms[1];
                        cpuStepping = fms[2];
                        break;
                    }
                }
                break;
            }
        }
        long cpuFreq = NetBsdSysctlUtil.sysctl("machdep.tsc_freq", 0L);
        if (cpuFreq == 0L) {
            cpuFreq = ParseUtil.parseHertz(cpuName);
            if (cpuFreq < 0) {
                cpuFreq = queryMaxFreq();
            }
        }
        String machine = NetBsdSysctlUtil.sysctl("hw.machine", "");
        boolean cpu64bit = machine != null && machine.contains("64")
                || ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected long[] queryCurrentFreq() {
        long[] freq = new long[1];
        // machdep.tsc_freq gives frequency in Hz on x86
        freq[0] = NetBsdSysctlUtil.sysctl("machdep.tsc_freq", 0L);
        if (freq[0] == 0L) {
            // Fallback: parse from cpu name (e.g., "Intel ... @ 2.10GHz")
            freq[0] = queryMaxFreq();
        }
        return freq;
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        // Iterate dmesg, look for lines:
        // cpu0: smt 0, core 0, package 0
        // cpu1: smt 0, core 1, package 0
        Map<Integer, Integer> coreMap = new HashMap<>();
        Map<Integer, Integer> packageMap = new HashMap<>();
        for (String line : ExecutingCommand.runNative("dmesg")) {
            Matcher m = DMESG_CPU.matcher(line);
            if (m.matches()) {
                int cpu = ParseUtil.parseIntOrDefault(m.group(1), 0);
                coreMap.put(cpu, ParseUtil.parseIntOrDefault(m.group(3), 0));
                packageMap.put(cpu, ParseUtil.parseIntOrDefault(m.group(4), 0));
            }
        }
        // native call seems to fail here, use fallback
        int logicalProcessorCount = NetBsdSysctlUtil.sysctl("hw.ncpuonline", 1);
        // If we found more procs in dmesg, update
        if (logicalProcessorCount < coreMap.keySet().size()) {
            logicalProcessorCount = coreMap.keySet().size();
        }
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        for (int i = 0; i < logicalProcessorCount; i++) {
            logProcs.add(new LogicalProcessor(i, coreMap.getOrDefault(i, 0), packageMap.getOrDefault(i, 0)));
        }
        Map<Integer, String> cpuMap = new HashMap<>();
        // cpu0 at mainbus0 mpidr 0: ARM Cortex-A7 r0p5
        // but NOT: cpu0 at mainbus0: apid 0 (boot processor)
        // cpu0: AMD GX-412TC SOC, 998.28 MHz, 16-30-01
        // cpu0: AMD EPYC 7313P 16-Core Processor, 2994.74 MHz, 19-01-01
        // cpu0: Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01
        Pattern p = Pattern.compile("cpu(\\d+).*: ((ARM|AMD|Intel|Apple).+)");

        Set<ProcessorCache> caches = new HashSet<>();
        // cpu0: 48KB 64b/line 12-way D-cache, 32KB 64b/line 8-way I-cache,
        // ... 1MB 64b/line 10-way L2 cache, 24MB 64b/line 12-way L3 cache
        // cpu0: 32KB 64b/line 8-way D-cache, 64KB 64b/line 4-way I-cache,
        // ... 512KB 64b/line 8-way L2 cache, 4MB 64b/line 16-way L3 cache
        // cpu0: 32KB 64b/line 2-way I-cache, 32KB 64b/line 8-way D-cache,
        // ... 2MB 64b/line 16-way L2 cache
        // cpu0: 32KB 64b/line 8-way I-cache, 32KB 64b/line 8-way D-cache,
        // ... 512KB 64b/line 8-way L2 cache
        // cpu0: 256KB 64b/line disabled L2 cache
        // cpu0: 1MB 64b/line 16-way L2 cache
        // --- Mac M1 example ---
        // shows different for icestorm/firestrorm and multiple lines for L1 vs. L2
        // cpu0 at mainbus0 mpidr 0: Apple Icestorm Pro r2p0
        // cpu0: 128KB 64b/line 8-way L1 VIPT I-cache, 64KB 64b/line 8-way L1 D-cache
        // cpu0: 4096KB 128b/line 16-way L2 cache
        // cpu2 at mainbus0 mpidr 10100: Apple Firestorm Pro r2p0
        // cpu2: 192KB 64b/line 6-way L1 VIPT I-cache, 128KB 64b/line 8-way L1 D-cache
        // cpu2: 12288KB 128b/line 12-way L2 cache
        // --- BIG:little example ---
        // shows different for A53/A72 and multiple lines for L1 vs. L2
        // cpu0 at mainbus0 mpidr 0: ARM Cortex-A53 r0p4
        // cpu0: 32KB 64b/line 2-way L1 VIPT I-cache, 32KB 64b/line 4-way L1 D-cache
        // cpu0: 512KB 64b/line 16-way L2 cache
        // cpu4 at mainbus0 mpidr 100: ARM Cortex-A72 r0p2
        // cpu4: 48KB 64b/line 3-way L1 PIPT I-cache, 32KB 64b/line 2-way L1 D-cache
        // cpu4: 1024KB 64b/line 16-way L2 cache
        Pattern q = Pattern.compile("cpu(\\d+).*: (.+(I-|D-|L\\d+\\s)cache)");
        Set<String> featureFlags = new LinkedHashSet<>();
        for (String s : ExecutingCommand.runNative("dmesg")) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
                int coreId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                cpuMap.put(coreId, m.group(2).trim());
            } else {
                Matcher n = q.matcher(s);
                if (n.matches()) {
                    for (String cacheStr : n.group(1).split(",")) {
                        ProcessorCache cache = parseCacheStr(cacheStr);
                        if (cache != null) {
                            caches.add(cache);
                        }
                    }
                }
            }
            if (s.startsWith("cpu")) {
                String[] ss = s.trim().split(": ");
                if (ss.length == 2 && ss[1].split(",").length > 3) {
                    featureFlags.add(ss[1]);
                }
            }
        }
        List<PhysicalProcessor> physProcs = cpuMap.isEmpty() ? null : createProcListFromDmesg(logProcs, cpuMap);
        return new Quartet<>(logProcs, physProcs, orderedProcCaches(caches), new ArrayList<>(featureFlags));
    }

    private ProcessorCache parseCacheStr(String cacheStr) {
        String[] split = ParseUtil.whitespaces.split(cacheStr);
        if (split.length > 3) {
            switch (split[split.length - 1]) {
                case "I-cache":
                    return new ProcessorCache(1, ParseUtil.getFirstIntValue(split[2]),
                            ParseUtil.getFirstIntValue(split[1]), ParseUtil.parseDecimalMemorySizeToBinary(split[0]),
                            Type.INSTRUCTION);
                case "D-cache":
                    return new ProcessorCache(1, ParseUtil.getFirstIntValue(split[2]),
                            ParseUtil.getFirstIntValue(split[1]), ParseUtil.parseDecimalMemorySizeToBinary(split[0]),
                            Type.DATA);
                default:
                    return new ProcessorCache(ParseUtil.getFirstIntValue(split[3]),
                            ParseUtil.getFirstIntValue(split[2]), ParseUtil.getFirstIntValue(split[1]),
                            ParseUtil.parseDecimalMemorySizeToBinary(split[0]), Type.UNIFIED);
            }
        }
        return null;
    }

    /**
     * Get number of context switches
     *
     * @return The context switches
     */
    @Override
    protected long queryContextSwitches() {
        return vmStats.get().getA();
    }

    /**
     * Get number of interrupts
     *
     * @return The interrupts
     */
    @Override
    protected long queryInterrupts() {
        return vmStats.get().getB();
    }

    private static Pair<Long, Long> queryVmStats() {
        long contextSwitches = 0L;
        long interrupts = 0L;
        List<String> vmstat = ExecutingCommand.runNative("vmstat -s");
        for (String line : vmstat) {
            if (line.endsWith("CPU context switches")) {
                contextSwitches = ParseUtil.parseLongOrDefault(line.trim().split("\\s+")[0], 0L);
            } else if (line.endsWith("device interrupts")) {
                interrupts = ParseUtil.parseLongOrDefault(line.trim().split("\\s+")[0], 0L);
            }
        }
        return new Pair<>(contextSwitches, interrupts);
    }

    /**
     * Get the system CPU load ticks
     *
     * @return The system CPU load ticks
     */
    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // Parse "kern.cp_time: user = N, nice = N, sys = N, intr = N, idle = N"
        long[] cpuTicks = parseCpTime(ExecutingCommand.getFirstAnswer("sysctl kern.cp_time"));
        if (cpuTicks.length >= CPUSTATES) {
            ticks[TickType.USER.getIndex()] = cpuTicks[CP_USER];
            ticks[TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
            ticks[TickType.IRQ.getIndex()] = cpuTicks[CP_INTR];
            ticks[TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE];
        }
        return ticks;
    }

    /**
     * Get the processor CPU load ticks
     *
     * @return The processor CPU load ticks
     */
    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            // Per-CPU: "kern.cp_time.N: user = ..., nice = ..., sys = ..., intr = ..., idle = ..."
            long[] cpuTicks = parseCpTime(ExecutingCommand.getFirstAnswer("sysctl kern.cp_time." + cpu));
            if (cpuTicks.length >= CPUSTATES) {
                ticks[cpu][TickType.USER.getIndex()] = cpuTicks[CP_USER];
                ticks[cpu][TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                ticks[cpu][TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                ticks[cpu][TickType.IRQ.getIndex()] = cpuTicks[CP_INTR];
                ticks[cpu][TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE];
            }
        }
        return ticks;
    }

    /**
     * Parse sysctl kern.cp_time output to an array of tick values.
     * <p>
     * Input format: "kern.cp_time: user = 2930, nice = 42, sys = 1334, intr = 1877, idle = 46354"
     *
     * @param cpTimeStr the sysctl output string
     * @return array of [user, nice, sys, intr, idle] tick values
     */
    private static long[] parseCpTime(String cpTimeStr) {
        long[] ticks = new long[CPUSTATES];
        if (cpTimeStr == null || cpTimeStr.isEmpty()) {
            return ticks;
        }
        // Extract the values after the colon
        int colonIdx = cpTimeStr.indexOf(':');
        if (colonIdx < 0) {
            return ticks;
        }
        String[] pairs = cpTimeStr.substring(colonIdx + 1).split(",");
        for (String pair : pairs) {
            String[] kv = pair.trim().split("\\s*=\\s*");
            if (kv.length == 2) {
                long val = ParseUtil.parseLongOrDefault(kv[1].trim(), 0L);
                switch (kv[0].trim()) {
                    case "user":
                        ticks[CP_USER] = val;
                        break;
                    case "nice":
                        ticks[CP_NICE] = val;
                        break;
                    case "sys":
                        ticks[CP_SYS] = val;
                        break;
                    case "intr":
                        ticks[CP_INTR] = val;
                        break;
                    case "idle":
                        ticks[CP_IDLE] = val;
                        break;
                    default:
                        break;
                }
            }
        }
        return ticks;
    }

    /**
     * Returns the system load average for the number of elements specified, up to 3, representing 1, 5, and 15 minutes.
     * The system load average is the sum of the number of runnable entities queued to the available processors and the
     * number of runnable entities running on the available processors averaged over a period of time. The way in which
     * the load average is calculated is operating system specific but is typically a damped time-dependent average. If
     * the load average is not available, a negative value is returned. This method is designed to provide a hint about
     * the system load and may be queried frequently.
     * <p>
     * The load average may be unavailable on some platforms (e.g., Windows) where it is expensive to implement this
     * method.
     *
     * @param nelem Number of elements to return.
     * @return an array of the system load averages for 1, 5, and 15 minutes with the size of the array specified by
     *         nelem; or negative values if not available.
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        Arrays.fill(average, -1d);
        // Parse "vm.loadavg: 1.59 0.47 0.18"
        String loadavg = ExecutingCommand.getFirstAnswer("sysctl -n vm.loadavg");
        if (!loadavg.isEmpty()) {
            String[] loads = ParseUtil.whitespaces.split(loadavg.trim());
            for (int i = 0; i < nelem && i < loads.length; i++) {
                average[i] = ParseUtil.parseDoubleOrDefault(loads[i], -1d);
            }
        }
        return average;
    }
}
