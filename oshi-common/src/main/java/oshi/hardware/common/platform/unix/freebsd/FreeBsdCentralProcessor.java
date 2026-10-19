/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

/**
 * Shared FreeBSD CentralProcessor logic. Subclasses provide the sysctl and native-call backends (JNA or FFM).
 */
public abstract class FreeBsdCentralProcessor extends AbstractCentralProcessor {

    // Capture the CSV of hex values as group(1), clients should split on ','
    // The Origin line of dmesg.boot, and each name=value pair on it, with or without spaces around the =
    private static final Pattern ORIGIN = Pattern.compile("Origin\\s*=.*");
    private static final Pattern DMESG_FIELD = Pattern.compile("(?<!\\w)(\\w++)\\s*+=\\s*+(\"[^\"]*+\"|\\S++)");

    private static final Pattern CPUMASK = Pattern.compile("mask=\"([\\p{XDigit},]+)\"");

    // FreeBSD CPU state indices into the kern.cp_time / kern.cp_times uint64 arrays
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;
    private static final int CPUSTATES = 5;

    /**
     * Reads a string sysctl value.
     *
     * @param name the sysctl name
     * @param def  the default value
     * @return the sysctl string value or the default
     */
    protected abstract String sysctlString(String name, String def);

    /**
     * Reads a long sysctl value.
     *
     * @param name the sysctl name
     * @param def  the default value
     * @return the sysctl long value or the default
     */
    protected abstract long sysctlLong(String name, long def);

    /**
     * Reads an int sysctl value.
     *
     * @param name the sysctl name
     * @param def  the default value
     * @return the sysctl int value or the default
     */
    protected abstract int sysctlInt(String name, int def);

    /**
     * Reads a sysctl whose value is an array of {@code uint64} values, e.g. {@code kern.cp_time} (per-system) or
     * {@code kern.cp_times} (per-CPU).
     *
     * @param name the sysctl name
     * @return the values as a {@code long[]}, or {@code null} if the query failed
     */
    protected abstract long @Nullable [] queryCpTimes(String name);

    /**
     * Native {@code getloadavg(3)} call, filling {@code loadavg} with up to {@code nelem} samples.
     *
     * @param loadavg the array to populate
     * @param nelem   the number of elements requested
     * @return the number of samples retrieved
     */
    protected abstract int getloadavgNative(double[] loadavg, int nelem);

    /**
     * Maps the five FreeBSD CPU-state values starting at {@code offset} in {@code cpTimeArray} to the corresponding
     * {@link TickType} slots of {@code ticks}.
     *
     * @param ticks       the destination tick array
     * @param cpTimeArray the source uint64 values
     * @param offset      the starting index of this CPU's states within {@code cpTimeArray}
     */
    private static void fillTicks(long[] ticks, long[] cpTimeArray, int offset) {
        ticks[TickType.USER.getIndex()] = cpTimeArray[offset + CP_USER];
        ticks[TickType.NICE.getIndex()] = cpTimeArray[offset + CP_NICE];
        ticks[TickType.SYSTEM.getIndex()] = cpTimeArray[offset + CP_SYS];
        ticks[TickType.IRQ.getIndex()] = cpTimeArray[offset + CP_INTR];
        ticks[TickType.IDLE.getIndex()] = cpTimeArray[offset + CP_IDLE];
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        long[] cpTime = queryCpTimes("kern.cp_time");
        if (cpTime == null || cpTime.length < CPUSTATES) {
            return ticks;
        }
        fillTicks(ticks, cpTime, 0);
        return ticks;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        return parseProcessorTicks(queryCpTimes("kern.cp_times"), getLogicalProcessorCount(), CPUSTATES);
    }

    /**
     * Reads per-CPU ticks from DragonFly BSD's {@code kern.cputime}, an array of one {@code struct kinfo_cputime} per
     * CPU. The struct begins with the five CPU-state values and is followed by fields whose count may vary by release,
     * so the per-CPU stride is derived from the array length.
     *
     * @return per-CPU ticks, zero-filled if the sysctl is unavailable or too short
     */
    protected long[][] queryKinfoCputimeTicks() {
        int cpus = getLogicalProcessorCount();
        long[] cpTimes = queryCpTimes("kern.cputime");
        return parseProcessorTicks(cpTimes, cpus, cpTimes == null ? 0 : cpTimes.length / cpus);
    }

    /**
     * Maps an array of per-CPU records, each beginning with the five CPU-state values, to per-CPU tick arrays.
     *
     * @param cpTimes the uint64 values read from the sysctl, or {@code null} if the read failed
     * @param cpus    the number of logical processors
     * @param stride  the number of values in each CPU's record
     * @return per-CPU ticks; CPUs without a complete record in {@code cpTimes} are zero-filled
     */
    static long[][] parseProcessorTicks(long @Nullable [] cpTimes, int cpus, int stride) {
        long[][] ticks = new long[cpus][TickType.values().length];
        if (cpTimes == null || stride < CPUSTATES) {
            return ticks;
        }
        int records = Math.min(cpus, cpTimes.length / stride);
        for (int cpu = 0; cpu < records; cpu++) {
            fillTicks(ticks[cpu], cpTimes, cpu * stride);
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = getloadavgNative(average, nelem);
        if (retval < nelem) {
            // All-or-nothing: a partial result is more likely a misread than a real load average
            Arrays.fill(average, -1d);
        }
        return average;
    }

    @Override
    public long queryContextSwitches() {
        return sysctlLong("vm.stats.sys.v_swtch", 0L);
    }

    @Override
    public long queryInterrupts() {
        return sysctlLong("vm.stats.sys.v_intr", 0L);
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        // Parsing dmesg.boot is apparently the only reliable source for processor identification in FreeBSD. Read
        // quietly (reportError=false): its absence is tolerated (we fall back to sysctl), and warning on every
        // CentralProcessor construction would spam callers that recreate one.
        DmesgProcessorId id = parseProcessorIdFromDmesg(FileUtil.readFile("/var/run/dmesg.boot", false),
                sysctlString("hw.model", ""));
        long cpuFreq = sysctlLong("hw.clockrate", 0L) * 1_000_000L;
        boolean cpu64bit = ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
        String processorID = getProcessorIDfromDmiDecode(id.processorIdBits());
        return new ProcessorIdentifier(id.vendor(), id.name(), id.family(), id.model(), id.stepping(), processorID,
                cpu64bit, cpuFreq);
    }

    /** Immutable holder for the identifier fields parsed from {@code dmesg.boot}. Package-private for testing. */
    static final class DmesgProcessorId {
        private final String vendor;
        private final String name;
        private final String family;
        private final String model;
        private final String stepping;
        private final long processorIdBits;

        DmesgProcessorId(String vendor, String name, String family, String model, String stepping,
                long processorIdBits) {
            this.vendor = vendor;
            this.name = name;
            this.family = family;
            this.model = model;
            this.stepping = stepping;
            this.processorIdBits = processorIdBits;
        }

        String vendor() {
            return vendor;
        }

        String name() {
            return name;
        }

        String family() {
            return family;
        }

        String model() {
            return model;
        }

        String stepping() {
            return stepping;
        }

        long processorIdBits() {
            return processorIdBits;
        }
    }

    /**
     * Parses the processor identifier fields from {@code dmesg.boot}. Uses {@code initialName} (from
     * {@code sysctl hw.model}) unless the file supplies a {@code CPU:} line and the initial name is empty.
     * Package-private for testing.
     * <p>
     * FreeBSD writes {@code Origin="GenuineIntel"  Id=0x306f2  Family=0x6  Model=0x3f  Stepping=2}. DragonFly BSD puts
     * spaces around each {@code =} and omits the family and model, which are then decoded from the {@code Id}.
     *
     * @param dmesg       the lines of {@code dmesg.boot}
     * @param initialName the CPU name from {@code sysctl hw.model}
     * @return the parsed identifier fields
     */
    static DmesgProcessorId parseProcessorIdFromDmesg(List<String> dmesg, String initialName) {
        final Pattern featuresPattern = Pattern.compile("Features=(\\S+)<.*");

        String cpuVendor = "";
        String cpuName = initialName;
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long processorIdBits = 0L;
        for (String line : dmesg) {
            line = line.trim();
            // Prefer hw.model to this one
            if (line.startsWith("CPU:") && cpuName.isEmpty()) {
                cpuName = line.replace("CPU:", "").trim();
            } else if (ORIGIN.matcher(line).matches()) {
                Map<String, String> fields = new HashMap<>();
                Matcher m = DMESG_FIELD.matcher(line);
                while (m.find()) {
                    fields.put(m.group(1), m.group(2));
                }
                cpuVendor = ParseUtil.getStringBetween(fields.getOrDefault("Origin", ""), '"');
                int id = ParseUtil.decodeIntOrDefault(fields.getOrDefault("Id", ""), 0);
                processorIdBits |= ParseUtil.unsignedIntToLong(id);
                Triplet<Integer, Integer, Integer> fms = cpuidToFamilyModelStepping(id);
                cpuFamily = String.valueOf(ParseUtil.decodeIntOrDefault(fields.get("Family"), fms.getA()));
                cpuModel = String.valueOf(ParseUtil.decodeIntOrDefault(fields.get("Model"), fms.getB()));
                cpuStepping = String.valueOf(ParseUtil.decodeIntOrDefault(fields.get("Stepping"), fms.getC()));
            } else if (line.startsWith("Features=")) {
                Matcher m = featuresPattern.matcher(line);
                if (m.matches()) {
                    processorIdBits |= ParseUtil.decodeLongOrDefault(m.group(1), 0L) << 32;
                }
                // No further interest in this file
                break;
            }
        }
        return new DmesgProcessorId(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorIdBits);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>, List<String>> initProcessorCounts() {
        List<LogicalProcessor> logProcs = parseTopology();
        // Force at least one processor
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        Pair<Map<Integer, String>, List<String>> modelsAndFlags = parseProcModelsAndFlags(
                FileUtil.readFile("/var/run/dmesg.boot", false));
        Map<Integer, String> dmesg = modelsAndFlags.getA();
        List<String> featureFlags = modelsAndFlags.getB();
        List<PhysicalProcessor> physProcs = dmesg.isEmpty() ? null : createProcListFromDmesg(logProcs, dmesg);
        List<ProcessorCache> caches = getCacheInfoFromLscpu();
        return new Quartet<>(logProcs, physProcs, caches, featureFlags);
    }

    /**
     * Parses the per-core processor model strings and feature flags from {@code dmesg.boot}. Package-private for
     * testing.
     *
     * @param dmesg the lines of {@code dmesg.boot}
     * @return a pair of (core id to model-string map, feature-flag lines)
     */
    static Pair<Map<Integer, String>, List<String>> parseProcModelsAndFlags(List<String> dmesg) {
        Map<Integer, String> models = new HashMap<>();
        // cpu0: <Open Firmware CPU> on cpulist0
        Pattern normal = Pattern.compile("cpu(\\d+): (.+) on .*");
        // CPU 0: ARM Cortex-A53 r0p4 affinity: 0 0
        Pattern hybrid = Pattern.compile("CPU\\s*(\\d+): (.+) affinity:.*");
        List<String> featureFlags = new ArrayList<>();
        boolean readingFlags = false;
        for (String s : dmesg) {
            Matcher h = hybrid.matcher(s);
            if (h.matches()) {
                int coreId = ParseUtil.parseIntOrDefault(h.group(1), 0);
                // This always takes priority, overwrite if needed
                models.put(coreId, h.group(2).trim());
            } else {
                Matcher n = normal.matcher(s);
                if (n.matches()) {
                    int coreId = ParseUtil.parseIntOrDefault(n.group(1), 0);
                    // Don't overwrite if h matched earlier
                    models.putIfAbsent(coreId, n.group(2).trim());
                }
            }
            if (ORIGIN.matcher(s.trim()).matches()) {
                readingFlags = true;
            } else if (readingFlags) {
                if (s.startsWith("  ")) {
                    featureFlags.add(s.trim());
                } else {
                    readingFlags = false;
                }
            }
        }
        return new Pair<>(models, featureFlags);
    }

    private List<ProcessorCache> getCacheInfoFromLscpu() {
        return parseCachesFromLscpu(ExecutingCommand.runNative("lscpu"));
    }

    /**
     * Parses the CPU cache descriptions from {@code lscpu} output. Package-private for testing.
     *
     * @param lscpu the lines of {@code lscpu} output
     * @return the ordered list of processor caches
     */
    static List<ProcessorCache> parseCachesFromLscpu(List<String> lscpu) {
        Set<ProcessorCache> caches = new HashSet<>();
        for (String checkLine : lscpu) {
            if (checkLine.contains("L1d cache:")) {
                caches.add(new ProcessorCache(1, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":", -1)[1].trim()),
                        ProcessorCache.Type.DATA));
            } else if (checkLine.contains("L1i cache:")) {
                caches.add(new ProcessorCache(1, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":", -1)[1].trim()),
                        ProcessorCache.Type.INSTRUCTION));
            } else if (checkLine.contains("L2 cache:")) {
                caches.add(new ProcessorCache(2, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":", -1)[1].trim()),
                        ProcessorCache.Type.UNIFIED));
            } else if (checkLine.contains("L3 cache:")) {
                caches.add(new ProcessorCache(3, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":", -1)[1].trim()),
                        ProcessorCache.Type.UNIFIED));
            }
        }
        return orderedProcCaches(caches);
    }

    private List<LogicalProcessor> parseTopology() {
        String spec = sysctlString("kern.sched.topology_spec", "");
        if (spec.isEmpty()) {
            // DragonFly BSD has no topology_spec, and publishes each CPU's place under hw.cpu_topology instead
            int ncpu = sysctlInt("hw.ncpu", 0);
            int[] physicalIds = new int[ncpu];
            int[] coreIds = new int[ncpu];
            for (int i = 0; i < ncpu; i++) {
                physicalIds[i] = sysctlInt("hw.cpu_topology.cpu" + i + ".physical_id", 0);
                coreIds[i] = sysctlInt("hw.cpu_topology.cpu" + i + ".core_id", -1);
            }
            return buildTopologyFromIds(physicalIds, coreIds);
        }
        return parseTopologySpec(spec);
    }

    /**
     * Builds the logical processors from {@code kern.sched.topology_spec}. Package-private for testing.
     * <p>
     * The spec nests groups of CPUs that share a cache. A group flagged {@code THREAD}, {@code SMT} or {@code HTT}
     * holds the hardware threads of one core, and a CPU in no such group is a core of its own; virtual machines often
     * publish a single flat group, so every CPU is its own core. The groups at level 2 that are not thread groups are
     * taken as packages.
     *
     * <pre>{@code
     * <groups>
     *  <group level="1" cache-level="0">
     *   <cpu count="24" mask="ffffff">0, 1, 2, ..., 23</cpu>
     *   <children>
     *    <group level="2" cache-level="2">
     *     <cpu count="12" mask="fff">0, 1, ..., 11</cpu>
     *     <children>
     *      <group level="3" cache-level="1">
     *       <cpu count="2" mask="3">0, 1</cpu>
     *       <flags><flag name="THREAD">THREAD group</flag><flag name="SMT">SMT group</flag></flags>
     *      </group>
     * }</pre>
     *
     * @param spec the value of {@code kern.sched.topology_spec}
     * @return the logical processors, in CPU order
     */
    static List<LogicalProcessor> parseTopologySpec(String spec) {
        List<TopologyGroup> groups = new ArrayList<>();
        Deque<TopologyGroup> open = new ArrayDeque<>();
        for (String line : spec.split("[\\n\\r]", -1)) {
            if (line.contains("<group level=")) {
                TopologyGroup group = new TopologyGroup(open.size() + 1);
                groups.add(group);
                open.push(group);
            } else if (line.contains("</group>")) {
                open.poll();
            } else if (!open.isEmpty()) {
                TopologyGroup group = open.element();
                if (line.contains("<cpu")) {
                    group.cpus.addAll(parseCpuList(line));
                } else if (line.contains("<flag name=\"THREAD\"") || line.contains("<flag name=\"SMT\"")
                        || line.contains("<flag name=\"HTT\"")) {
                    // The kernel prints THREAD alongside either of the others, but any of them marks a core
                    group.thread = true;
                }
            }
        }
        if (groups.isEmpty()) {
            return new ArrayList<>();
        }
        // The CPUs of each core, keyed by the core's lowest CPU, and each CPU's package
        Map<Integer, Integer> coreOf = new HashMap<>();
        Map<Integer, Integer> packageOf = new HashMap<>();
        int packageNumber = 0;
        for (TopologyGroup group : groups) {
            if (group.thread && !group.cpus.isEmpty()) {
                int first = Collections.min(group.cpus);
                for (int cpu : group.cpus) {
                    coreOf.put(cpu, first);
                }
            } else if (group.level == 2) {
                for (int cpu : group.cpus) {
                    packageOf.put(cpu, packageNumber);
                }
                packageNumber++;
            }
        }
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Map<Integer, Integer> coreNumbers = new HashMap<>();
        for (int cpu : new TreeSet<>(groups.get(0).cpus)) {
            int core = coreNumbers.computeIfAbsent(coreOf.getOrDefault(cpu, cpu), k -> coreNumbers.size());
            logProcs.add(new LogicalProcessor(cpu, core, packageOf.getOrDefault(cpu, 0), 0));
        }
        return logProcs;
    }

    /**
     * Reads the CPU numbers of a {@code <cpu>} line, from the list it prints or, failing that, from its mask. The mask
     * is a comma-separated list of 64-bit hex words, lowest CPUs first.
     */
    private static List<Integer> parseCpuList(String line) {
        List<Integer> cpus = new ArrayList<>();
        int open = line.indexOf('>');
        int close = line.indexOf("</cpu>");
        if (open >= 0 && close > open) {
            for (String cpu : line.substring(open + 1, close).split(",", -1)) {
                int n = ParseUtil.parseIntOrDefault(cpu.trim(), -1);
                if (n >= 0) {
                    cpus.add(n);
                }
            }
        }
        if (cpus.isEmpty()) {
            Matcher m = CPUMASK.matcher(line);
            if (m.find()) {
                String[] words = m.group(1).split(",", -1);
                for (int w = 0; w < words.length; w++) {
                    long bits = ParseUtil.hexStringToLong(words[w], 0L);
                    for (int b = 0; b < Long.SIZE; b++) {
                        if ((bits & (1L << b)) != 0) {
                            cpus.add(w * Long.SIZE + b);
                        }
                    }
                }
            }
        }
        return cpus;
    }

    /** A group of CPUs in {@code kern.sched.topology_spec}. */
    private static final class TopologyGroup {
        private final int level;
        private final List<Integer> cpus = new ArrayList<>();
        private boolean thread;

        private TopologyGroup(int level) {
            this.level = level;
        }
    }

    /**
     * Builds the logical processors from each CPU's package and core IDs, as DragonFly BSD publishes them in
     * {@code hw.cpu_topology.cpu<n>.physical_id} and {@code core_id}. Package-private for testing.
     * <p>
     * A core ID is only unique within its package, so cores are numbered across the system in order of first
     * appearance. DragonFly BSD omits {@code core_id} when each chip has one core, in which case every CPU is its own
     * core.
     *
     * @param physicalIds each CPU's package ID
     * @param coreIds     each CPU's core ID within its package, or -1 where it has none
     * @return the logical processors, in CPU order
     */
    static List<LogicalProcessor> buildTopologyFromIds(int[] physicalIds, int[] coreIds) {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Map<String, Integer> coreNumbers = new HashMap<>();
        for (int i = 0; i < physicalIds.length; i++) {
            String core = physicalIds[i] + ":" + (coreIds[i] < 0 ? "cpu" + i : coreIds[i]);
            Integer coreNumber = coreNumbers.computeIfAbsent(core, k -> coreNumbers.size());
            logProcs.add(new LogicalProcessor(i, coreNumber, physicalIds[i], 0));
        }
        return logProcs;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freq = new long[1];
        freq[0] = sysctlLong("dev.cpu.0.freq", -1L);
        if (freq[0] > 0) {
            // If success, value is in MHz
            freq[0] *= 1_000_000L;
        } else {
            freq[0] = sysctlLong("machdep.tsc_freq", -1L);
        }
        return freq;
    }

    @Override
    public long queryMaxFreq() {
        long max = -1L;
        String freqLevels = sysctlString("dev.cpu.0.freq_levels", "");
        // MHz/Watts pairs like: 2501/32000 2187/27125 2000/24000
        for (String s : ParseUtil.whitespaces.split(freqLevels, -1)) {
            long freq = ParseUtil.parseLongOrDefault(s.split("/", -1)[0], -1L);
            if (max < freq) {
                max = freq;
            }
        }
        if (max > 0) {
            // If success, value is in MHz
            max *= 1_000_000;
        } else {
            max = sysctlLong("machdep.tsc_freq", -1L);
        }
        return max;
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root permissions), otherwise uses the values from
     * /var/run/dmesg.boot
     *
     * @param processorID The processorID as a long
     * @return The ProcessorID string
     */
    protected static String getProcessorIDfromDmiDecode(long processorID) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t processor")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return ParseUtil.getTextAfterString(checkLine, marker).trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Used the passed-in values
        return String.format(Locale.ROOT, "%016X", processorID);
    }
}
