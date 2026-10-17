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

import org.jspecify.annotations.Nullable;

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
    private static final Pattern COLON_SPACE = Pattern.compile(": ");

    private final Supplier<Pair<Long, Long>> vmStats = memoize(this::queryVmStats, defaultExpiration());

    /** OpenBSD's topology line, {@code "cpu0: smt 0, core 0, package 0"}. */
    private static final Pattern DMESG_CPU = Pattern.compile("cpu(\\d+): smt (\\d+), core (\\d+), package (\\d+)");
    /**
     * NetBSD's topology line, which x86 prefixes with a NUMA node: {@code "cpu0: node 0, package 0, core 0, smt 0"}.
     */
    private static final Pattern DMESG_CPU_NETBSD = Pattern
            .compile("cpu(\\d+): (?:node \\d+, )?package (\\d+), core (\\d+), smt \\d+");
    /** The model line, {@code "cpu0: AMD EPYC 7763 64-Core Processor"} or {@code "cpu0 at acpi0: Arm Neoverse N1"}. */
    private static final Pattern DMESG_MODEL = Pattern.compile("cpu(\\d+).*: ((?:ARM|Arm|AMD|Intel|Apple).+)");
    /**
     * The identifier NetBSD appends to the model line: the CPUID signature on x86 and the affinity register on ARM,
     * which differs on every processor.
     */
    private static final Pattern DMESG_MODEL_ID = Pattern.compile("\\s*+, id 0x\\p{XDigit}++$");
    /** OpenBSD's cache lines, {@code "cpu0: 32KB 64b/line 8-way D-cache, 32KB 64b/line 8-way I-cache"}. */
    private static final Pattern DMESG_CACHE = Pattern.compile("cpu(\\d+).*: (.+(I-|D-|L\\d+\\s)cache)");
    /** NetBSD's ARM cache lines, {@code "cpu0: L1 64KB/64B 4-way (256 set) VIPT Instruction cache"}. */
    private static final Pattern DMESG_CACHE_ARM = Pattern
            .compile("cpu\\d+: L(\\d+) (\\d+)KB/(\\d+)B (\\d+)-way.* (Instruction|Data|Unified) cache");
    /** NetBSD prefixes each line with the seconds since boot, {@code "[     1.000004] "}. */
    private static final Pattern DMESG_TIMESTAMP = Pattern.compile("^\\[\\s*+\\d++\\.\\d++\\]\\s?+");

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
    protected Quartet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>, List<String>> initProcessorCounts() {
        List<String> dmesg = queryDmesg();
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
     * Reads the kernel message buffer, without the timestamps NetBSD prefixes to each line.
     *
     * @return the lines emitted by {@code dmesg}
     */
    protected static List<String> queryDmesg() {
        return stripDmesgTimestamps(ExecutingCommand.runNative("dmesg"));
    }

    /**
     * Removes the {@code "[     1.000004] "} timestamp NetBSD prefixes to each {@code dmesg} line.
     *
     * @param dmesg the lines emitted by {@code dmesg}
     * @return the lines without a leading timestamp
     */
    static List<String> stripDmesgTimestamps(List<String> dmesg) {
        List<String> stripped = new ArrayList<>(dmesg.size());
        for (String line : dmesg) {
            stripped.add(DMESG_TIMESTAMP.matcher(line).replaceFirst(""));
        }
        return stripped;
    }

    /**
     * Parses the model name of the first processor from {@code dmesg}.
     *
     * @param dmesg the lines emitted by {@code dmesg}, without timestamps
     * @return the model name, or an empty string if no line carries one
     */
    protected static String parseDmesgCpuName(List<String> dmesg) {
        return parseDmesgModelsAndCaches(dmesg).getCpuMap().getOrDefault(0, "");
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
            Matcher n = DMESG_CPU_NETBSD.matcher(line);
            if (m.matches()) {
                int cpu = ParseUtil.parseIntOrDefault(m.group(1), 0);
                coreMap.put(cpu, ParseUtil.parseIntOrDefault(m.group(3), 0));
                packageMap.put(cpu, ParseUtil.parseIntOrDefault(m.group(4), 0));
            } else if (n.matches()) {
                int cpu = ParseUtil.parseIntOrDefault(n.group(1), 0);
                coreMap.put(cpu, ParseUtil.parseIntOrDefault(n.group(3), 0));
                packageMap.put(cpu, ParseUtil.parseIntOrDefault(n.group(2), 0));
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
        Set<ProcessorCache> caches = new HashSet<>();
        Set<String> featureFlags = new LinkedHashSet<>();
        for (String s : dmesg) {
            Matcher m = DMESG_MODEL.matcher(s);
            Matcher arm = DMESG_CACHE_ARM.matcher(s);
            if (m.matches()) {
                int coreId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                cpuMap.put(coreId, DMESG_MODEL_ID.matcher(m.group(2)).replaceFirst("").trim());
            } else if (arm.matches()) {
                caches.add(new ProcessorCache(ParseUtil.parseIntOrDefault(arm.group(1), 0),
                        ParseUtil.parseIntOrDefault(arm.group(4), 0), ParseUtil.parseIntOrDefault(arm.group(3), 0),
                        ParseUtil.parseLongOrDefault(arm.group(2), 0L) << 10, armCacheType(arm.group(5))));
                continue;
            } else if (DMESG_CPU_NETBSD.matcher(s).matches()) {
                // Four comma-separated fields on x86, which would otherwise pass for a feature list
                continue;
            } else {
                Matcher n = DMESG_CACHE.matcher(s);
                if (n.matches()) {
                    for (String cacheStr : n.group(2).split(",", -1)) {
                        ProcessorCache cache = parseCacheStr(cacheStr);
                        if (cache != null) {
                            caches.add(cache);
                        }
                    }
                }
            }
            if (s.startsWith("cpu")) {
                String[] ss = COLON_SPACE.split(s.trim(), -1);
                if (ss.length == 2 && ss[1].split(",").length > 3) {
                    featureFlags.add(ss[1]);
                }
            }
        }
        return new DmesgStrings(cpuMap, caches, featureFlags);
    }

    private static ProcessorCache.Type armCacheType(String type) {
        switch (type) {
            case "Instruction":
                return ProcessorCache.Type.INSTRUCTION;
            case "Data":
                return ProcessorCache.Type.DATA;
            default:
                return ProcessorCache.Type.UNIFIED;
        }
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

    static @Nullable ProcessorCache parseCacheStr(String cacheStr) {
        String[] split = ParseUtil.whitespaces.split(cacheStr.trim(), -1);
        if (split.length > 3) {
            switch (split[split.length - 1]) {
                case "I-cache":
                    return new ProcessorCache(1, ParseUtil.getFirstIntValue(split[2]),
                            ParseUtil.getFirstIntValue(split[1]), ParseUtil.parseDecimalMemorySizeToBinary(split[0]),
                            ProcessorCache.Type.INSTRUCTION);
                case "D-cache":
                    return new ProcessorCache(1, ParseUtil.getFirstIntValue(split[2]),
                            ParseUtil.getFirstIntValue(split[1]), ParseUtil.parseDecimalMemorySizeToBinary(split[0]),
                            ProcessorCache.Type.DATA);
                default:
                    return new ProcessorCache(ParseUtil.getFirstIntValue(split[3]),
                            ParseUtil.getFirstIntValue(split[2]), ParseUtil.getFirstIntValue(split[1]),
                            ParseUtil.parseDecimalMemorySizeToBinary(split[0]), ProcessorCache.Type.UNIFIED);
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
