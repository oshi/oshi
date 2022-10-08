/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.jna.platform.unix.OpenBsdLibc.CP_IDLE;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_INTR;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_NICE;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_SYS;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_USER;
import static oshi.jna.platform.unix.OpenBsdLibc.CTL_HW;
import static oshi.jna.platform.unix.OpenBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.OpenBsdLibc.HW_CPUSPEED;
import static oshi.jna.platform.unix.OpenBsdLibc.HW_MACHINE;
import static oshi.jna.platform.unix.OpenBsdLibc.HW_MODEL;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_CPTIME;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_CPTIME2;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.Memory;
import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * OpenBSD Central Processor implementation
 */
@ThreadSafe
public class OpenBsdCentralProcessor extends AbstractCentralProcessor {
    private final Supplier<Pair<Long, Long>> vmStats = memoize(OpenBsdCentralProcessor::queryVmStats,
            defaultExpiration());
    private static final Pattern DMESG_CPU = Pattern.compile("cpu(\\d+): smt (\\d+), core (\\d+), package (\\d+)");

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = OpenBsdSysctlUtil.sysctl("machdep.cpuvendor", "");
        int[] mib = new int[2];
        mib[0] = CTL_HW;
        mib[1] = HW_MODEL;
        String cpuName = OpenBsdSysctlUtil.sysctl(mib, "");
        // CPUID: first 32 bits is cpufeature, last 32 bits is cpuid
        int cpuid = ParseUtil.hexStringToInt(OpenBsdSysctlUtil.sysctl("machdep.cpuid", ""), 0);
        int cpufeature = ParseUtil.hexStringToInt(OpenBsdSysctlUtil.sysctl("machdep.cpufeature", ""), 0);
        Triplet<Integer, Integer, Integer> cpu = cpuidToFamilyModelStepping(cpuid);
        String cpuFamily = cpu.getA().toString();
        String cpuModel = cpu.getB().toString();
        String cpuStepping = cpu.getC().toString();
        long cpuFreq = ParseUtil.parseHertz(cpuName);
        if (cpuFreq < 0) {
            cpuFreq = queryMaxFreq();
        }
        mib[1] = HW_MACHINE;
        String machine = OpenBsdSysctlUtil.sysctl(mib, "");
        boolean cpu64bit = machine != null && machine.contains("64")
                || ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
        String processorID = String.format("%08x%08x", cpufeature, cpuid);

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    private static Triplet<Integer, Integer, Integer> cpuidToFamilyModelStepping(int cpuid) {
        // family is bits 27:20 | 11:8
        int family = cpuid >> 16 & 0xff0 | cpuid >> 8 & 0xf;
        // model is bits 19:16 | 7:4
        int model = cpuid >> 12 & 0xf0 | cpuid >> 4 & 0xf;
        // stepping is bits 3:0
        int stepping = cpuid & 0xf;
        return new Triplet<>(family, model, stepping);
    }

    @Override
    protected long queryMaxFreq() {
        return queryCurrentFreq()[0];
    }

    @Override
    protected long[] queryCurrentFreq() {
        long[] freq = new long[1];
        int[] mib = new int[2];
        mib[0] = CTL_HW;
        mib[1] = HW_CPUSPEED;
        freq[0] = OpenBsdSysctlUtil.sysctl(mib, 0L) * 1_000_000L;
        return freq;
    }

    @Override
    protected Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> initProcessorCounts() {
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
        int logicalProcessorCount = OpenBsdSysctlUtil.sysctl("hw.ncpuonline", 1);
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
        Pattern p = Pattern.compile("cpu(\\\\d+).*: ((ARM|AMD|Intel|Apple).+)");

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
        Pattern q = Pattern.compile("cpu(\\\\d+).*: (.+(I-|D-|L\\d+\\s)cache)");
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
        }
        List<PhysicalProcessor> physProcs = cpuMap.isEmpty() ? null : createProcListFromDmesg(logProcs, cpuMap);
        return new Triplet<>(logProcs, physProcs, orderedProcCaches(caches));
    }

    private ProcessorCache parseCacheStr(String cacheStr) {
        String[] split = ParseUtil.whitespaces.split(cacheStr);
        if (split.length > 3) {
            switch (split[split.length - 1]) {
            case "I-cache":
                return new ProcessorCache(1, ParseUtil.getFirstIntValue(split[2]), ParseUtil.getFirstIntValue(split[1]),
                        ParseUtil.parseDecimalMemorySizeToBinary(split[0]), Type.INSTRUCTION);
            case "D-cache":
                return new ProcessorCache(1, ParseUtil.getFirstIntValue(split[2]), ParseUtil.getFirstIntValue(split[1]),
                        ParseUtil.parseDecimalMemorySizeToBinary(split[0]), Type.DATA);
            default:
                return new ProcessorCache(ParseUtil.getFirstIntValue(split[3]), ParseUtil.getFirstIntValue(split[2]),
                        ParseUtil.getFirstIntValue(split[1]), ParseUtil.parseDecimalMemorySizeToBinary(split[0]),
                        Type.UNIFIED);
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
            if (line.endsWith("cpu context switches")) {
                contextSwitches = ParseUtil.getFirstIntValue(line);
            } else if (line.endsWith("interrupts")) {
                interrupts = ParseUtil.getFirstIntValue(line);
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
        int[] mib = new int[2];
        mib[0] = CTL_KERN;
        mib[1] = KERN_CPTIME;
        try (Memory m = OpenBsdSysctlUtil.sysctl(mib)) {
            // array of 5 or 6 native longs
            long[] cpuTicks = cpTimeToTicks(m, false);
            if (cpuTicks.length >= 5) {
                ticks[TickType.USER.getIndex()] = cpuTicks[CP_USER];
                ticks[TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                ticks[TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                int offset = cpuTicks.length > 5 ? 1 : 0;
                ticks[TickType.IRQ.getIndex()] = cpuTicks[CP_INTR + offset];
                ticks[TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE + offset];
            }
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
        int[] mib = new int[3];
        mib[0] = CTL_KERN;
        mib[1] = KERN_CPTIME2;
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            mib[2] = cpu;
            try (Memory m = OpenBsdSysctlUtil.sysctl(mib)) {
                // array of 5 or 6 longs
                long[] cpuTicks = cpTimeToTicks(m, true);
                if (cpuTicks.length >= 5) {
                    ticks[cpu][TickType.USER.getIndex()] = cpuTicks[CP_USER];
                    ticks[cpu][TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                    ticks[cpu][TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                    int offset = cpuTicks.length > 5 ? 1 : 0;
                    ticks[cpu][TickType.IRQ.getIndex()] = cpuTicks[CP_INTR + offset];
                    ticks[cpu][TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE + offset];
                }
            }
        }
        return ticks;
    }

    /**
     * Parse memory buffer returned from sysctl kern.cptime or kern.cptime2 to an array of 5 or 6 longs depending on
     * version.
     * <p>
     * Versions 6.4 and later have a 6-element array while earlier versions have only 5 elements. Additionally
     * kern.cptime uses a native-sized long (32- or 64-bit) value while kern.cptime2 is always a 64-bit value.
     *
     * @param m          A buffer containing the array.
     * @param force64bit True if the buffer is filled with 64-bit longs, false if native long sized values
     * @return The array
     */
    private static long[] cpTimeToTicks(Memory m, boolean force64bit) {
        long longBytes = force64bit ? 8L : Native.LONG_SIZE;
        int arraySize = m == null ? 0 : (int) (m.size() / longBytes);
        if (force64bit && m != null) {
            return m.getLongArray(0, arraySize);
        }
        long[] ticks = new long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            ticks[i] = m.getNativeLong(i * longBytes).longValue();
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
        int retval = OpenBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }
}
