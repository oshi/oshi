/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Abstract base for the Solaris CPU. The command-based pieces (NUMA mapping, processor-ID feature flags, the {@code
 * dmesg} physical-processor parsing, the averaged tick rollup, and the {@code kstat -p} context-switch/interrupt
 * fallbacks) are shared; the {@code kstat}-chain queries are native and implemented by the JNA and FFM subclasses.
 */
@ThreadSafe
public abstract class SolarisCentralProcessor extends AbstractCentralProcessor {

    // SPARC prints decimal (:l2-dcache-size 131072), x86 prints hex (:l2-cache-size 0x80000)
    private static final Pattern PICL_CACHE_PROPERTY = Pattern
            .compile("\\s*:l(\\d)-([id]?)cache-(size|line-size|associativity)\\s+(0x\\p{XDigit}+|\\d+)\\s*");

    @Override
    protected Quartet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>, List<String>> initProcessorCounts() {
        // Defensive copy: the base appends a fallback entry, so don't assume the subclass returned a mutable list
        List<LogicalProcessor> logProcs = new ArrayList<>(queryLogicalProcessors());
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        List<ProcessorCache> caches = parsePiclCaches(ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c cpu"));
        Map<Integer, String> dmesg = parseDmesgCpuInfo(ExecutingCommand.runNative("dmesg"));
        if (dmesg.isEmpty()) {
            return new Quartet<>(logProcs, null, caches, Collections.emptyList());
        }
        List<String> featureFlags = ExecutingCommand.runNative("isainfo -x");
        return new Quartet<>(logProcs, createProcListFromDmesg(logProcs, dmesg), caches, featureFlags);
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
    public long queryContextSwitches() {
        // Shell-escaped pipe: Java \\\\| -> shell \\| -> kstat regex \| -> matches literal pipe.
        return sumKstatLong(ExecutingCommand.runNative("kstat -p cpu_stat:::/pswitch\\\\|inv_swtch/"));
    }

    @Override
    public long queryInterrupts() {
        return sumKstatLong(ExecutingCommand.runNative("kstat -p cpu_stat:::/intr/"));
    }

    /**
     * Enumerates the logical processors from the subclass's kstat data source.
     *
     * @return the logical processor list (may be empty; the caller substitutes a single default processor)
     */
    protected abstract List<LogicalProcessor> queryLogicalProcessors();

    /**
     * Maps each CPU id to its NUMA node (lgroup) via {@code lgrpinfo}.
     *
     * @return a map of processor id to NUMA node
     */
    protected static Map<Integer, Integer> mapNumaNodes() {
        return parseNumaNodes(ExecutingCommand.runNative("lgrpinfo -c leaves"));
    }

    /**
     * Fetches the ProcessorID by encoding the stepping, model, family, and feature flags.
     *
     * @param stepping the stepping
     * @param model    the model
     * @param family   the family
     * @return the Processor ID string
     */
    protected static String getProcessorID(String stepping, String model, String family) {
        String[] flags = parseIsainfoFlags(ExecutingCommand.runNative("isainfo -v"));
        return createProcessorID(stepping, model, family, flags);
    }

    /**
     * Parses the output of {@code dmesg} to extract CPU name strings keyed by core id.
     *
     * @param dmesg the lines emitted by {@code dmesg}
     * @return a map of core id to CPU name string
     */
    static Map<Integer, String> parseDmesgCpuInfo(List<String> dmesg) {
        Map<Integer, String> cpuMap = new HashMap<>();
        // Jan 9 14:04:28 solaris unix: [ID 950921 kern.info] cpu0: Intel(r) Celeron(r)
        // CPU J3455 @ 1.50GHz
        // but NOT: Jan 9 14:04:28 solaris unix: [ID 950921 kern.info] cpu0: x86 (chipid
        // 0x0 GenuineIntel 506C9 family 6 model 92 step 9 clock 1500 MHz)
        Pattern p = Pattern.compile(".* cpu(\\d+): ((ARM|AMD|Intel).+)");
        for (String s : dmesg) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
                int coreId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                cpuMap.put(coreId, m.group(2).trim());
            }
        }
        return cpuMap;
    }

    /**
     * Parses the output of {@code prtpicl -v -c cpu} into the set of distinct processor caches. Each {@code cpu} node
     * carries {@code l<n>-icache-*}, {@code l<n>-dcache-*} or (for a unified cache) {@code l<n>-cache-*} properties
     * giving the size, line size and associativity of every cache level that CPU sees. SPARC prints the values in
     * decimal and x86 in hexadecimal.
     *
     * @param prtpicl the lines emitted by {@code prtpicl -v -c cpu}
     * @return the distinct caches, ordered by level and type; empty if the CPU nodes carry no cache properties
     */
    static List<ProcessorCache> parsePiclCaches(List<String> prtpicl) {
        Set<ProcessorCache> caches = new HashSet<>();
        // Per CPU node: cache key (level + type letter) to {size, line size, associativity}
        Map<String, long[]> nodeCaches = new HashMap<>();
        for (String line : prtpicl) {
            Matcher m = PICL_CACHE_PROPERTY.matcher(line);
            if (m.matches()) {
                long[] values = nodeCaches.computeIfAbsent(m.group(1) + m.group(2), k -> new long[3]);
                long value = ParseUtil.parseLastLong(m.group(4), 0L);
                switch (m.group(3)) {
                    case "size":
                        values[0] = value;
                        break;
                    case "line-size":
                        values[1] = value;
                        break;
                    default:
                        values[2] = value;
                        break;
                }
            } else if (!line.trim().startsWith(":")) {
                // A node header line ("cpu (cpu, 298000001c1)") ends the previous node
                addPiclCaches(nodeCaches, caches);
            }
        }
        addPiclCaches(nodeCaches, caches);
        return orderedProcCaches(caches);
    }

    /**
     * Converts one CPU node's accumulated cache properties into {@link ProcessorCache} entries, then clears them.
     *
     * @param nodeCaches cache key (level digit plus {@code i}, {@code d} or nothing) to {size, line size,
     *                   associativity}
     * @param caches     the set to add the node's caches to
     */
    private static void addPiclCaches(Map<String, long[]> nodeCaches, Set<ProcessorCache> caches) {
        for (Map.Entry<String, long[]> e : nodeCaches.entrySet()) {
            long[] values = e.getValue();
            if (values[0] > 0) {
                String key = e.getKey();
                ProcessorCache.Type type = ProcessorCache.Type.UNIFIED;
                if (key.endsWith("i")) {
                    type = ProcessorCache.Type.INSTRUCTION;
                } else if (key.endsWith("d")) {
                    type = ProcessorCache.Type.DATA;
                }
                caches.add(new ProcessorCache(key.charAt(0) - '0', (int) values[2], (int) values[1], values[0], type));
            }
        }
        nodeCaches.clear();
    }

    /**
     * Parses the output of {@code lgrpinfo -c leaves} into a map of CPU id to NUMA node.
     *
     * @param lgrpinfo the lines emitted by {@code lgrpinfo -c leaves}
     * @return a map of processor id to NUMA node number
     */
    static Map<Integer, Integer> parseNumaNodes(List<String> lgrpinfo) {
        // Get numa node info from lgrpinfo
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        int lgroup = 0;
        for (String line : lgrpinfo) {
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
                for (Integer cpu : ParseUtil.parseHyphenatedIntList(line.split(":", -1)[1])) {
                    numaNodeMap.put(cpu, lgroup);
                }
            }
        }
        return numaNodeMap;
    }

    /**
     * Parses the output of {@code isainfo -v} to extract the 64-bit feature flags.
     *
     * @param isainfo the lines emitted by {@code isainfo -v}
     * @return an array of feature flag strings (lowercase)
     */
    static String[] parseIsainfoFlags(List<String> isainfo) {
        StringBuilder flags = new StringBuilder();
        for (String line : isainfo) {
            if (line.startsWith("32-bit")) {
                break;
            } else if (!line.startsWith("64-bit")) {
                flags.append(' ').append(line.trim());
            }
        }
        return ParseUtil.whitespaces.split(flags.toString().toLowerCase(Locale.ROOT));
    }

    /**
     * Sums the last long value from each line of {@code kstat -p} output.
     *
     * @param kstat the lines emitted by a {@code kstat -p} command
     * @return the sum of the trailing long values across all lines
     */
    static long sumKstatLong(List<String> kstat) {
        long total = 0L;
        for (String s : kstat) {
            total += ParseUtil.parseLastLong(s, 0L);
        }
        return total;
    }
}
