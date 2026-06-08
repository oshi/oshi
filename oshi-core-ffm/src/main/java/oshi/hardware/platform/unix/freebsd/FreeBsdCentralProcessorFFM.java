/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * FFM-backed FreeBSD central processor.
 */
@ThreadSafe
public class FreeBsdCentralProcessorFFM extends FreeBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessorFFM.class);

    // Capture the CSV of hex values as group(1), clients should split on ','
    private static final Pattern CPUMASK = Pattern
            .compile(".*<cpu\\s.*mask=\"(\\p{XDigit}+(,\\p{XDigit}+)*)\".*>.*</cpu>.*");

    // FreeBSD CPU state indices into kern.cp_time / kern.cp_times arrays
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;
    private static final int CPUSTATES = 5;
    private static final long UINT64_SIZE = Long.BYTES;
    private static final long CPTIME_SIZE = CPUSTATES * UINT64_SIZE;

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        final Pattern identifierPattern = Pattern
                .compile("Origin=\"([^\"]*)\".*Id=(\\S+).*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");
        final Pattern featuresPattern = Pattern.compile("Features=(\\S+)<.*");

        String cpuVendor = "";
        String cpuName = BsdSysctlUtilFFM.sysctl("hw.model", "");
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        String processorID;
        long cpuFreq = BsdSysctlUtilFFM.sysctl("hw.clockrate", 0L) * 1_000_000L;

        boolean cpu64bit;

        // Parsing dmesg.boot is apparently the only reliable source for processor
        // identification in FreeBSD
        long processorIdBits = 0L;
        List<String> cpuInfo = FileUtil.readFile("/var/run/dmesg.boot");
        for (String line : cpuInfo) {
            line = line.trim();
            // Prefer hw.model to this one
            if (line.startsWith("CPU:") && cpuName.isEmpty()) {
                cpuName = line.replace("CPU:", "").trim();
            } else if (line.startsWith("Origin=")) {
                Matcher m = identifierPattern.matcher(line);
                if (m.matches()) {
                    cpuVendor = m.group(1);
                    processorIdBits |= Long.decode(m.group(2));
                    cpuFamily = Integer.decode(m.group(3)).toString();
                    cpuModel = Integer.decode(m.group(4)).toString();
                    cpuStepping = Integer.decode(m.group(5)).toString();
                }
            } else if (line.startsWith("Features=")) {
                Matcher m = featuresPattern.matcher(line);
                if (m.matches()) {
                    processorIdBits |= Long.decode(m.group(1)) << 32;
                }
                // No further interest in this file
                break;
            }
        }
        cpu64bit = ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
        processorID = getProcessorIDfromDmiDecode(processorIdBits);

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        List<LogicalProcessor> logProcs = parseTopology();
        // Force at least one processor
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        Map<Integer, String> dmesg = new HashMap<>();
        // cpu0: <Open Firmware CPU> on cpulist0
        Pattern normal = Pattern.compile("cpu(\\d+): (.+) on .*");
        // CPU 0: ARM Cortex-A53 r0p4 affinity: 0 0
        Pattern hybrid = Pattern.compile("CPU\\s*(\\d+): (.+) affinity:.*");
        List<String> featureFlags = new ArrayList<>();
        boolean readingFlags = false;
        for (String s : FileUtil.readFile("/var/run/dmesg.boot")) {
            Matcher h = hybrid.matcher(s);
            if (h.matches()) {
                int coreId = ParseUtil.parseIntOrDefault(h.group(1), 0);
                // This always takes priority, overwrite if needed
                dmesg.put(coreId, h.group(2).trim());
            } else {
                Matcher n = normal.matcher(s);
                if (n.matches()) {
                    int coreId = ParseUtil.parseIntOrDefault(n.group(1), 0);
                    // Don't overwrite if h matched earlier
                    dmesg.putIfAbsent(coreId, n.group(2).trim());
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
        List<PhysicalProcessor> physProcs = dmesg.isEmpty() ? null : createProcListFromDmesg(logProcs, dmesg);
        List<ProcessorCache> caches = getCacheInfoFromLscpu();
        return new Quartet<>(logProcs, physProcs, caches, featureFlags);
    }

    private List<ProcessorCache> getCacheInfoFromLscpu() {
        Set<ProcessorCache> caches = new HashSet<>();
        for (String checkLine : ExecutingCommand.runNative("lscpu")) {
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

    private static List<LogicalProcessor> parseTopology() {
        String[] topology = BsdSysctlUtilFFM.sysctl("kern.sched.topology_spec", "").split("[\\n\\r]");
        // See FreeBsdCentralProcessorJNA for sample output and the layered <group> semantics.
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
                Matcher m = CPUMASK.matcher(topo);
                if (m.matches()) {
                    String csvMatch = m.group(1);
                    String[] csvTokens = csvMatch.split(",");
                    String firstVal = csvTokens[0];
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
        int lowBit = Long.numberOfTrailingZeros(group1);
        int hiBit = 63 - Long.numberOfLeadingZeros(group1);
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
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cpTime = arena.allocate(CPTIME_SIZE);
            if (BsdSysctlUtilFFM.sysctl("kern.cp_time", cpTime)) {
                ticks[TickType.USER.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_USER);
                ticks[TickType.NICE.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_NICE);
                ticks[TickType.SYSTEM.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_SYS);
                ticks[TickType.IRQ.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_INTR);
                ticks[TickType.IDLE.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_IDLE);
            }
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freq = new long[1];
        freq[0] = BsdSysctlUtilFFM.sysctl("dev.cpu.0.freq", -1L);
        if (freq[0] > 0) {
            // If success, value is in MHz
            freq[0] *= 1_000_000L;
        } else {
            freq[0] = BsdSysctlUtilFFM.sysctl("machdep.tsc_freq", -1L);
        }
        return freq;
    }

    @Override
    public long queryMaxFreq() {
        long max = -1L;
        String freqLevels = BsdSysctlUtilFFM.sysctl("dev.cpu.0.freq_levels", "");
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
            max = BsdSysctlUtilFFM.sysctl("machdep.tsc_freq", -1L);
        }
        return max;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        final int n = nelem;
        return callInArenaOrDefault(arena -> {
            MemorySegment avg = arena.allocate(JAVA_DOUBLE, n);
            int retval = FreeBsdLibcFunctions.getloadavg(avg, n);
            double[] result = new double[n];
            for (int i = 0; i < n; i++) {
                result[i] = (i < retval) ? avg.getAtIndex(JAVA_DOUBLE, i) : -1d;
            }
            return result;
        }, LOG, Level.WARN, "Failed to read load average", fillNegative(nelem));
    }

    private static double[] fillNegative(int nelem) {
        double[] arr = new double[nelem];
        for (int i = 0; i < nelem; i++) {
            arr[i] = -1d;
        }
        return arr;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        int cpus = getLogicalProcessorCount();
        long[][] ticks = new long[cpus][TickType.values().length];
        MemorySegment buf = BsdSysctlUtilFFM.sysctl("kern.cp_times");
        if (buf == null) {
            return ticks;
        }
        for (int cpu = 0; cpu < cpus; cpu++) {
            long base = CPTIME_SIZE * cpu;
            ticks[cpu][TickType.USER.getIndex()] = buf.get(JAVA_LONG, base + CP_USER * UINT64_SIZE);
            ticks[cpu][TickType.NICE.getIndex()] = buf.get(JAVA_LONG, base + CP_NICE * UINT64_SIZE);
            ticks[cpu][TickType.SYSTEM.getIndex()] = buf.get(JAVA_LONG, base + CP_SYS * UINT64_SIZE);
            ticks[cpu][TickType.IRQ.getIndex()] = buf.get(JAVA_LONG, base + CP_INTR * UINT64_SIZE);
            ticks[cpu][TickType.IDLE.getIndex()] = buf.get(JAVA_LONG, base + CP_IDLE * UINT64_SIZE);
        }
        return ticks;
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root permissions), otherwise uses the values from
     * /var/run/dmesg.boot
     *
     * @param processorID The processorID as a long
     * @return The ProcessorID string
     */
    private static String getProcessorIDfromDmiDecode(long processorID) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t processor")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Used the passed-in values
        return String.format(Locale.ROOT, "%016X", processorID);
    }

    @Override
    public long queryContextSwitches() {
        return ParseUtil.unsignedIntToLong(BsdSysctlUtilFFM.sysctl("vm.stats.sys.v_swtch", 0));
    }

    @Override
    public long queryInterrupts() {
        return ParseUtil.unsignedIntToLong(BsdSysctlUtilFFM.sysctl("vm.stats.sys.v_intr", 0));
    }
}
