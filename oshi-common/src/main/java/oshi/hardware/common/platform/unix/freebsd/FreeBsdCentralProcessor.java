/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Shared FreeBSD CentralProcessor logic. Subclasses provide the sysctl and native-call backends (JNA or FFM).
 */
public abstract class FreeBsdCentralProcessor extends AbstractCentralProcessor {

    // Capture the CSV of hex values as group(1), clients should split on ','
    private static final Pattern CPUMASK = Pattern
            .compile(".*<cpu\\s.*mask=\"(\\p{XDigit}+(,\\p{XDigit}+)*)\".*>.*</cpu>.*");

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
    protected abstract long[] queryCpTimes(String name);

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
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        long[] cpTimes = queryCpTimes("kern.cp_times");
        if (cpTimes == null) {
            return ticks;
        }
        int cpus = Math.min(getLogicalProcessorCount(), cpTimes.length / CPUSTATES);
        for (int cpu = 0; cpu < cpus; cpu++) {
            fillTicks(ticks[cpu], cpTimes, cpu * CPUSTATES);
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
        return ParseUtil.unsignedIntToLong(sysctlInt("vm.stats.sys.v_swtch", 0));
    }

    @Override
    public long queryInterrupts() {
        return ParseUtil.unsignedIntToLong(sysctlInt("vm.stats.sys.v_intr", 0));
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
     *
     * @param dmesg       the lines of {@code dmesg.boot}
     * @param initialName the CPU name from {@code sysctl hw.model}
     * @return the parsed identifier fields
     */
    static DmesgProcessorId parseProcessorIdFromDmesg(List<String> dmesg, String initialName) {
        final Pattern identifierPattern = Pattern
                .compile("Origin=\"([^\"]*)\".*Id=(\\S+).*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");
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
            } else if (line.startsWith("Origin=")) {
                Matcher m = identifierPattern.matcher(line);
                if (m.matches()) {
                    cpuVendor = m.group(1);
                    processorIdBits |= ParseUtil.decodeLongOrDefault(m.group(2), 0L);
                    cpuFamily = String.valueOf(ParseUtil.decodeIntOrDefault(m.group(3), 0));
                    cpuModel = String.valueOf(ParseUtil.decodeIntOrDefault(m.group(4), 0));
                    cpuStepping = String.valueOf(ParseUtil.decodeIntOrDefault(m.group(5), 0));
                }
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
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
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
            if (s.contains("Origin=")) {
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
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), Type.DATA));
            } else if (checkLine.contains("L1i cache:")) {
                caches.add(new ProcessorCache(1, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), Type.INSTRUCTION));
            } else if (checkLine.contains("L2 cache:")) {
                caches.add(new ProcessorCache(2, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), Type.UNIFIED));
            } else if (checkLine.contains("L3 cache:")) {
                caches.add(new ProcessorCache(3, 0, 0,
                        ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), Type.UNIFIED));
            }
        }
        return orderedProcCaches(caches);
    }

    private List<LogicalProcessor> parseTopology() {
        String[] topology = sysctlString("kern.sched.topology_spec", "").split("[\\n\\r]");
        /*-
         * Sample output:
         *
        <groups>
        <group level="1" cache-level="0">
         <cpu count="24" mask="ffffff">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23</cpu>
         <children>
          <group level="2" cache-level="2">
           <cpu count="12" mask="fff">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11</cpu>
           <children>
            <group level="3" cache-level="1">
             <cpu count="2" mask="3">0, 1</cpu>
             <flags><flag name="THREAD">THREAD group</flag><flag name="SMT">SMT group</flag></flags>
            </group>
            ...
        * On FreeBSD 13.1, the output may contain a csv value for the mask:
        <groups>
         <group level="1" cache-level="3">
          <cpu count="8" mask="ff,0,0,0">0, 1, 2, 3, 4, 5, 6, 7</cpu>
          <children>
           <group level="2" cache-level="2">
            <cpu count="2" mask="3,0,0,0">0, 1</cpu>
            ...
        *
        * Opens with <groups>
        * <group> level 1 identifies all the processors via bitmask, should only be one
        * <group> level 2 separates by physical package
        * <group> level 3 puts hyperthreads together: if THREAD or SMT or HTT all the CPUs are one physical
        * If there is no level 3, then all logical processors are physical
        */
        // Create lists of the group bitmasks
        long group1 = 1L;
        List<Long> group2 = new ArrayList<>();
        List<Long> group3 = new ArrayList<>();
        int groupLevel = 0;
        for (String topo : topology) {
            if (topo.contains("<group level=")) {
                groupLevel++;
            } else if (topo.contains("</group>")) {
                groupLevel--;
            } else if (topo.contains("<cpu")) {
                // Find <cpu> tag and extract bits
                Matcher m = CPUMASK.matcher(topo);
                if (m.matches()) {
                    // If csv of hex values like "f,0,0,0", parse the first value
                    String csvMatch = m.group(1);
                    String[] csvTokens = csvMatch.split(",");
                    String firstVal = csvTokens[0];

                    // Regex guarantees parsing digits so we won't get a
                    // NumberFormatException
                    long parsedVal = ParseUtil.hexStringToLong(firstVal, 0);
                    switch (groupLevel) {
                        case 1:
                            group1 = parsedVal;
                            break;
                        case 2:
                            group2.add(parsedVal);
                            break;
                        case 3:
                            group3.add(parsedVal);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return matchBitmasks(group1, group2, group3);
    }

    private static List<LogicalProcessor> matchBitmasks(long group1, List<Long> group2, List<Long> group3) {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        // Lowest and Highest set bits, indexing from 0
        int lowBit = Long.numberOfTrailingZeros(group1);
        int hiBit = 63 - Long.numberOfLeadingZeros(group1);
        // Create logical processors for this core
        for (int i = lowBit; i <= hiBit; i++) {
            if ((group1 & (1L << i)) > 0) {
                int numaNode = 0;
                LogicalProcessor logProc = new LogicalProcessor(i, getMatchingBitmask(group3, i),
                        getMatchingBitmask(group2, i), numaNode);
                logProcs.add(logProc);
            }
        }
        return logProcs;
    }

    private static int getMatchingBitmask(List<Long> bitmasks, int lp) {
        for (int j = 0; j < bitmasks.size(); j++) {
            if ((bitmasks.get(j).longValue() & (1L << lp)) != 0) {
                return j;
            }
        }
        return 0;
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
        for (String s : ParseUtil.whitespaces.split(freqLevels)) {
            long freq = ParseUtil.parseLongOrDefault(s.split("/")[0], -1L);
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
                String[] parts = checkLine.split(marker);
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
        }
        // If we've gotten this far, dmidecode failed. Used the passed-in values
        return String.format(Locale.ROOT, "%016X", processorID);
    }
}
