/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CPUSTATES;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_IDLE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_INTR;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_NICE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_SYS;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_USER;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_HW;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.HW_CPUSPEED;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.HW_MACHINE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.HW_MODEL;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_CPTIME;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_CPTIME2;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

@ThreadSafe
public class OpenBsdCentralProcessorFFM extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdCentralProcessorFFM.class);

    private final Supplier<Pair<Long, Long>> vmStats = memoize(OpenBsdCentralProcessorFFM::queryVmStats,
            defaultExpiration());
    private static final Pattern DMESG_CPU = Pattern.compile("cpu(\\d+): smt (\\d+), core (\\d+), package (\\d+)");

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = OpenBsdSysctlUtilFFM.sysctl("machdep.cpuvendor", "");
        int[] mib = { CTL_HW, HW_MODEL };
        String cpuName = OpenBsdSysctlUtilFFM.sysctl(mib, "");
        int cpuid = ParseUtil.hexStringToInt(OpenBsdSysctlUtilFFM.sysctl("machdep.cpuid", ""), 0);
        int cpufeature = ParseUtil.hexStringToInt(OpenBsdSysctlUtilFFM.sysctl("machdep.cpufeature", ""), 0);
        Triplet<Integer, Integer, Integer> cpu = cpuidToFamilyModelStepping(cpuid);
        String cpuFamily = cpu.getA().toString();
        String cpuModel = cpu.getB().toString();
        String cpuStepping = cpu.getC().toString();
        long cpuFreq = ParseUtil.parseHertz(cpuName);
        if (cpuFreq < 0) {
            cpuFreq = queryMaxFreq();
        }
        mib = new int[] { CTL_HW, HW_MACHINE };
        String machine = OpenBsdSysctlUtilFFM.sysctl(mib, "");
        boolean cpu64bit = machine != null && machine.contains("64")
                || ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
        String processorID = String.format(Locale.ROOT, "%08x%08x", cpufeature, cpuid);

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    private static Triplet<Integer, Integer, Integer> cpuidToFamilyModelStepping(int cpuid) {
        int family = cpuid >> 16 & 0xff0 | cpuid >> 8 & 0xf;
        int model = cpuid >> 12 & 0xf0 | cpuid >> 4 & 0xf;
        int stepping = cpuid & 0xf;
        return new Triplet<>(family, model, stepping);
    }

    @Override
    protected long[] queryCurrentFreq() {
        long[] freq = new long[1];
        int[] mib = { CTL_HW, HW_CPUSPEED };
        freq[0] = OpenBsdSysctlUtilFFM.sysctl(mib, 0) * 1_000_000L;
        return freq;
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
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
        int logicalProcessorCount = OpenBsdSysctlUtilFFM.sysctl("hw.ncpuonline", 1);
        if (logicalProcessorCount < coreMap.keySet().size()) {
            logicalProcessorCount = coreMap.keySet().size();
        }
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        for (int i = 0; i < logicalProcessorCount; i++) {
            logProcs.add(new LogicalProcessor(i, coreMap.getOrDefault(i, 0), packageMap.getOrDefault(i, 0)));
        }
        Map<Integer, String> cpuMap = new HashMap<>();
        Pattern p = Pattern.compile("cpu(\\d+).*: ((ARM|AMD|Intel|Apple).+)");

        Set<ProcessorCache> caches = new HashSet<>();
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

    @Override
    protected long queryContextSwitches() {
        return vmStats.get().getA();
    }

    @Override
    protected long queryInterrupts() {
        return vmStats.get().getB();
    }

    private static Pair<Long, Long> queryVmStats() {
        long contextSwitches = 0L;
        long interrupts = 0L;
        for (String line : ExecutingCommand.runNative("vmstat -s")) {
            if (line.endsWith("cpu context switches")) {
                contextSwitches = ParseUtil.getFirstIntValue(line);
            } else if (line.endsWith("interrupts")) {
                interrupts = ParseUtil.getFirstIntValue(line);
            }
        }
        return new Pair<>(contextSwitches, interrupts);
    }

    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int[] mib = { CTL_KERN, KERN_CPTIME };
        MemorySegment buf = OpenBsdSysctlUtilFFM.sysctl(mib);
        if (buf != null) {
            int arraySize = (int) (buf.byteSize() / JAVA_LONG.byteSize());
            if (arraySize >= CPUSTATES) {
                ticks[TickType.USER.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_USER);
                ticks[TickType.NICE.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_NICE);
                ticks[TickType.SYSTEM.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_SYS);
                int offset = arraySize > CPUSTATES ? 1 : 0;
                ticks[TickType.IRQ.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_INTR + offset);
                ticks[TickType.IDLE.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_IDLE + offset);
            }
        }
        return ticks;
    }

    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            int[] mib = { CTL_KERN, KERN_CPTIME2, cpu };
            MemorySegment buf = OpenBsdSysctlUtilFFM.sysctl(mib);
            if (buf != null) {
                int arraySize = (int) (buf.byteSize() / JAVA_LONG.byteSize());
                if (arraySize >= CPUSTATES) {
                    ticks[cpu][TickType.USER.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_USER);
                    ticks[cpu][TickType.NICE.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_NICE);
                    ticks[cpu][TickType.SYSTEM.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_SYS);
                    int offset = arraySize > CPUSTATES ? 1 : 0;
                    ticks[cpu][TickType.IRQ.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_INTR + offset);
                    ticks[cpu][TickType.IDLE.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_IDLE + offset);
                }
            }
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        Arrays.fill(average, -1d);
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            MemorySegment avg = arena.allocate(JAVA_DOUBLE, nelem);
            int retval = OpenBsdLibcFunctions.getloadavg(avg, nelem);
            double[] result = new double[nelem];
            for (int i = 0; i < nelem; i++) {
                result[i] = (i < retval) ? avg.getAtIndex(JAVA_DOUBLE, i) : -1d;
            }
            return result;
        }, LOG, Level.WARN, "Failed to read load average", average);
    }
}
