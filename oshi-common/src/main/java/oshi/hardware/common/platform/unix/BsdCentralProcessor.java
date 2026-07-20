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
        List<String> dmesg = ExecutingCommand.runNative("dmesg");
        Pair<Map<Integer, Integer>, Map<Integer, Integer>> topology = parseTopology(dmesg);
        Map<Integer, Integer> coreMap = topology.getA();
        Map<Integer, Integer> packageMap = topology.getB();

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

        DmesgStrings parsed = parseDmesgModelsAndCaches(dmesg);
        List<PhysicalProcessor> physProcs = parsed.getCpuMap().isEmpty() ? null
                : createProcListFromDmesg(logProcs, parsed.getCpuMap());
        return new Quartet<>(logProcs, physProcs, orderedProcCaches(parsed.getCaches()),
                new ArrayList<>(parsed.getFeatureFlags()));
    }

    /**
     * Parses topology lines from {@code dmesg} to extract per-CPU core and package mappings.
     *
     * @param dmesg the lines emitted by {@code dmesg}
     * @return a {@link Pair} of (cpu-to-core map, cpu-to-package map)
     */
    static Pair<Map<Integer, Integer>, Map<Integer, Integer>> parseTopology(List<String> dmesg) {
        Map<Integer, Integer> coreMap = new HashMap<>();
        Map<Integer, Integer> packageMap = new HashMap<>();
        for (String line : dmesg) {
            Matcher m = DMESG_CPU.matcher(line);
            if (m.matches()) {
                int cpu = ParseUtil.parseIntOrDefault(m.group(1), 0);
                coreMap.put(cpu, ParseUtil.parseIntOrDefault(m.group(3), 0));
                packageMap.put(cpu, ParseUtil.parseIntOrDefault(m.group(4), 0));
            }
        }
        return new Pair<>(coreMap, packageMap);
    }

    /**
     * Parses {@code dmesg} output for CPU model names, cache descriptions, and feature flags.
     *
     * @param dmesg the lines emitted by {@code dmesg}
     * @return a {@link DmesgStrings} containing the parsed data
     */
    static DmesgStrings parseDmesgModelsAndCaches(List<String> dmesg) {
        Map<Integer, String> cpuMap = new HashMap<>();
        Pattern p = Pattern.compile("cpu(\\d+).*: ((ARM|AMD|Intel|Apple).+)");
        Set<ProcessorCache> caches = new HashSet<>();
        Pattern q = Pattern.compile("cpu(\\d+).*: (.+(I-|D-|L\\d+\\s)cache)");
        Set<String> featureFlags = new LinkedHashSet<>();
        for (String s : dmesg) {
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
        return new DmesgStrings(cpuMap, caches, featureFlags);
    }

    /**
     * Holds the results of parsing {@code dmesg} output for CPU models, caches, and feature flags.
     */
    static final class DmesgStrings {
        private final Map<Integer, String> cpuMap;
        private final Set<ProcessorCache> caches;
        private final Set<String> featureFlags;

        DmesgStrings(Map<Integer, String> cpuMap, Set<ProcessorCache> caches, Set<String> featureFlags) {
            this.cpuMap = cpuMap;
            this.caches = caches;
            this.featureFlags = featureFlags;
        }

        Map<Integer, String> getCpuMap() {
            return cpuMap;
        }

        Set<ProcessorCache> getCaches() {
            return caches;
        }

        Set<String> getFeatureFlags() {
            return featureFlags;
        }
    }

    static ProcessorCache parseCacheStr(String cacheStr) {
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
