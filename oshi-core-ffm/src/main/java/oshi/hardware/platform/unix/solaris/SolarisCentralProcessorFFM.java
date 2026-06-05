/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.unix.solaris.SolarisLibcFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

@ThreadSafe
final class SolarisCentralProcessorFFM extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisCentralProcessorFFM.class);

    private static final String CPU_INFO = "cpu_info";

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        boolean cpu64bit = "64".equals(ExecutingCommand.getFirstAnswer("isainfo -b").trim());
        String cpuVendor = "";
        String cpuName = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long cpuFreq = 0L;

        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup(CPU_INFO, -1, null);
            if (ksp.address() != 0L && kc.read(ksp)) {
                cpuVendor = KstatUtilFFM.dataLookupString(ksp, "vendor_id");
                cpuName = KstatUtilFFM.dataLookupString(ksp, "brand");
                cpuFamily = KstatUtilFFM.dataLookupString(ksp, "family");
                cpuModel = KstatUtilFFM.dataLookupString(ksp, "model");
                cpuStepping = KstatUtilFFM.dataLookupString(ksp, "stepping");
                cpuFreq = KstatUtilFFM.dataLookupLong(ksp, "clock_MHz") * 1_000_000L;
            }
        }
        String processorID = getProcessorID(cpuStepping, cpuModel, cpuFamily);
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        Map<Integer, Integer> numaNodeMap = mapNumaNodes();
        List<LogicalProcessor> logProcs = new ArrayList<>();
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            List<MemorySegment> kstats = kc.lookupAll(CPU_INFO, -1, null);
            for (MemorySegment ksp : kstats) {
                if (ksp.address() != 0L && kc.read(ksp)) {
                    int procId = logProcs.size();
                    String chipId = KstatUtilFFM.dataLookupString(ksp, "chip_id");
                    String coreId = KstatUtilFFM.dataLookupString(ksp, "core_id");
                    logProcs.add(new LogicalProcessor(procId, ParseUtil.parseIntOrDefault(coreId, 0),
                            ParseUtil.parseIntOrDefault(chipId, 0), numaNodeMap.getOrDefault(procId, 0)));
                }
            }
        }
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        Map<Integer, String> dmesg = new HashMap<>();
        Pattern p = Pattern.compile(".* cpu(\\d+): ((ARM|AMD|Intel).+)");
        for (String s : ExecutingCommand.runNative("dmesg")) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
                int coreId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                dmesg.put(coreId, m.group(2).trim());
            }
        }
        if (dmesg.isEmpty()) {
            return new Quartet<>(logProcs, null, null, Collections.emptyList());
        }
        List<String> featureFlags = ExecutingCommand.runNative("isainfo -x");
        return new Quartet<>(logProcs, createProcListFromDmesg(logProcs, dmesg), null, featureFlags);
    }

    private static Map<Integer, Integer> mapNumaNodes() {
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        int lgroup = 0;
        for (String line : ExecutingCommand.runNative("lgrpinfo -c leaves")) {
            if (line.startsWith("lgroup")) {
                lgroup = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("CPUs:") || line.contains("CPU:")) {
                for (Integer cpu : ParseUtil.parseHyphenatedIntList(line.split(":")[1])) {
                    numaNodeMap.put(cpu, lgroup);
                }
            }
        }
        return numaNodeMap;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
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
    public long[] queryCurrentFreq() {
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, -1);
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            for (int i = 0; i < freqs.length; i++) {
                for (MemorySegment ksp : kc.lookupAll(CPU_INFO, i, null)) {
                    if (ksp.address() != 0L && kc.read(ksp)) {
                        freqs[i] = KstatUtilFFM.dataLookupLong(ksp, "current_clock_Hz");
                    }
                }
            }
        }
        return freqs;
    }

    @Override
    public long queryMaxFreq() {
        long max = -1L;
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            for (MemorySegment ksp : kc.lookupAll(CPU_INFO, 0, null)) {
                if (kc.read(ksp)) {
                    String suppFreq = KstatUtilFFM.dataLookupString(ksp, "supported_frequencies_Hz");
                    if (!suppFreq.isEmpty()) {
                        for (String s : suppFreq.split(":")) {
                            long freq = ParseUtil.parseLongOrDefault(s, -1L);
                            if (max < freq) {
                                max = freq;
                            }
                        }
                    }
                }
            }
        }
        return max;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(nelem * Double.BYTES);
            int retval = SolarisLibcFunctions.getloadavg(buf, nelem);
            int validCount = Math.max(retval, 0);
            for (int i = 0; i < validCount && i < nelem; i++) {
                average[i] = buf.get(ValueLayout.JAVA_DOUBLE, i * Double.BYTES);
            }
            for (int i = validCount; i < nelem; i++) {
                average[i] = -1d;
            }
        } catch (Throwable t) {
            LOG.warn("getloadavg failed", t);
            Arrays.fill(average, -1d);
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        int cpu = -1;
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            List<MemorySegment> kstats = kc.lookupAll("cpu", -1, "sys");
            for (MemorySegment ksp : kstats) {
                if (++cpu >= ticks.length) {
                    break;
                }
                if (kc.read(ksp)) {
                    ticks[cpu][TickType.IDLE.getIndex()] = KstatUtilFFM.dataLookupLong(ksp, "cpu_ticks_idle");
                    ticks[cpu][TickType.SYSTEM.getIndex()] = KstatUtilFFM.dataLookupLong(ksp, "cpu_ticks_kernel");
                    ticks[cpu][TickType.USER.getIndex()] = KstatUtilFFM.dataLookupLong(ksp, "cpu_ticks_user");
                }
            }
        }
        return ticks;
    }

    private static String getProcessorID(String stepping, String model, String family) {
        List<String> isainfo = ExecutingCommand.runNative("isainfo -v");
        StringBuilder flags = new StringBuilder();
        for (String line : isainfo) {
            if (line.startsWith("32-bit")) {
                break;
            } else if (!line.startsWith("64-bit")) {
                flags.append(' ').append(line.trim());
            }
        }
        return createProcessorID(stepping, model, family,
                ParseUtil.whitespaces.split(flags.toString().toLowerCase(Locale.ROOT)));
    }

    @Override
    public long queryContextSwitches() {
        long swtch = 0;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/pswitch\\|inv_swtch/");
        for (String s : kstat) {
            swtch += ParseUtil.parseLastLong(s, 0L);
        }
        return swtch;
    }

    @Override
    public long queryInterrupts() {
        long intr = 0;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/intr/");
        for (String s : kstat) {
            intr += ParseUtil.parseLastLong(s, 0L);
        }
        return intr;
    }
}
