/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.jna.platform.unix.FreeBsdLibc.CpTime;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Triplet;

/**
 * A CPU
 */
@ThreadSafe
final class FreeBsdCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessor.class);

    // Capture the CSV of hex values as group(1), clients should split on ','
    private static final Pattern CPUMASK = Pattern
            .compile(".*<cpu\\s.*mask=\"(\\p{XDigit}+(,\\p{XDigit}+)*)\".*>.*</cpu>.*");

    private static final long CPTIME_SIZE;
    static {
        try (CpTime cpTime = new CpTime()) {
            CPTIME_SIZE = cpTime.size();
        }
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        final Pattern identifierPattern = Pattern
                .compile("Origin=\"([^\"]*)\".*Id=(\\S+).*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");
        final Pattern featuresPattern = Pattern.compile("Features=(\\S+)<.*");

        String cpuVendor = "";
        String cpuName = BsdSysctlUtil.sysctl("hw.model", "");
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        String processorID;
        long cpuFreq = BsdSysctlUtil.sysctl("hw.clockrate", 0L) * 1_000_000L;

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
    protected Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> initProcessorCounts() {
        List<LogicalProcessor> logProcs = parseTopology();
        // Force at least one processor
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        Map<Integer, String> dmesg = new HashMap<>();
        // cpu0: <Open Firmware CPU> on cpulist0
        Pattern normal = Pattern.compile("cpu(\\\\d+): (.+) on .*");
        // CPU 0: ARM Cortex-A53 r0p4 affinity: 0 0
        Pattern hybrid = Pattern.compile("CPU\\\\s*(\\\\d+): (.+) affinity:.*");
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
        }
        List<PhysicalProcessor> physProcs = dmesg.isEmpty() ? null : createProcListFromDmesg(logProcs, dmesg);
        List<ProcessorCache> caches = getCacheInfoFromLscpu();
        return new Triplet<>(logProcs, physProcs, caches);
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
        String[] topology = BsdSysctlUtil.sysctl("kern.sched.topology_spec", "").split("[\\n\\r]");
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
                    switch (groupLevel) {
                    case 1:
                        group1 = Long.parseLong(firstVal, 16);
                        break;
                    case 2:
                        group2.add(Long.parseLong(firstVal, 16));
                        break;
                    case 3:
                        group3.add(Long.parseLong(firstVal, 16));
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
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        try (CpTime cpTime = new CpTime()) {
            BsdSysctlUtil.sysctl("kern.cp_time", cpTime);
            ticks[TickType.USER.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_USER];
            ticks[TickType.NICE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_SYS];
            ticks[TickType.IRQ.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_INTR];
            ticks[TickType.IDLE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_IDLE];
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freq = new long[1];
        freq[0] = BsdSysctlUtil.sysctl("dev.cpu.0.freq", -1L);
        if (freq[0] > 0) {
            // If success, value is in MHz
            freq[0] *= 1_000_000L;
        } else {
            freq[0] = BsdSysctlUtil.sysctl("machdep.tsc_freq", -1L);
        }
        return freq;
    }

    @Override
    public long queryMaxFreq() {
        long max = -1L;
        String freqLevels = BsdSysctlUtil.sysctl("dev.cpu.0.freq_levels", "");
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
            max = BsdSysctlUtil.sysctl("machdep.tsc_freq", -1L);
        }
        return max;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = FreeBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];

        // Allocate memory for array of CPTime
        long arraySize = CPTIME_SIZE * getLogicalProcessorCount();
        try (Memory p = new Memory(arraySize);
                CloseableSizeTByReference oldlenp = new CloseableSizeTByReference(arraySize)) {
            String name = "kern.cp_times";
            // Fetch
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, oldlenp, null, size_t.ZERO)) {
                LOG.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                return ticks;
            }
            // p now points to the data; need to copy each element
            for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                ticks[cpu][TickType.USER.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_USER * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.NICE.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_NICE * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.SYSTEM.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_SYS * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.IRQ.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_INTR * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.IDLE.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_IDLE * FreeBsdLibc.UINT64_SIZE); // lgtm
            }
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
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Used the passed-in values
        return String.format("%016X", processorID);
    }

    @Override
    public long queryContextSwitches() {
        String name = "vm.stats.sys.v_swtch";
        size_t.ByReference size = new size_t.ByReference(new size_t(FreeBsdLibc.INT_SIZE));
        try (Memory p = new Memory(size.longValue())) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                return 0L;
            }
            return ParseUtil.unsignedIntToLong(p.getInt(0));
        }
    }

    @Override
    public long queryInterrupts() {
        String name = "vm.stats.sys.v_intr";
        size_t.ByReference size = new size_t.ByReference(new size_t(FreeBsdLibc.INT_SIZE));
        try (Memory p = new Memory(size.longValue())) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                return 0L;
            }
            return ParseUtil.unsignedIntToLong(p.getInt(0));
        }
    }
}
