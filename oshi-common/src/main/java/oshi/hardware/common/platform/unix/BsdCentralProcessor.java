/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Shared CentralProcessor logic for the {@code dmesg}-parsing BSDs (OpenBSD and NetBSD). Both enumerate processors,
 * caches, and feature flags from {@code dmesg} output and read counters from {@code vmstat}; the platform-specific
 * sysctl backend and the {@code vmstat} parsing are supplied by the subclasses.
 */
public abstract class BsdCentralProcessor extends AbstractCentralProcessor {

    private final Supplier<Pair<Long, Long>> vmStats = memoize(this::queryVmStats, defaultExpiration());

    private static final Pattern DMESG_CPU = Pattern.compile("cpu(\\d+): smt (\\d+), core (\\d+), package (\\d+)");

    /**
     * Reads an integer sysctl value by name.
     *
     * @param name the sysctl name
     * @param def  the default value
     * @return the sysctl integer value or the default
     */
    protected abstract int sysctl(String name, int def);

    /**
     * Reads the number of context switches and interrupts from {@code vmstat}. The label text and parsing differ
     * between OpenBSD and NetBSD, so each subclass supplies its own implementation.
     *
     * @return a {@link Pair} of (context switches, interrupts)
     */
    protected abstract Pair<Long, Long> queryVmStats();

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
        int logicalProcessorCount = sysctl("hw.ncpuonline", 1);
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
                    for (String cacheStr : n.group(2).split(",")) {
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
        String[] split = ParseUtil.whitespaces.split(cacheStr.trim());
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
}
