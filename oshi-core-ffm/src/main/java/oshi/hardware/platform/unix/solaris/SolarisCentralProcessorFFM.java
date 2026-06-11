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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.solaris.SolarisLibcFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.common.platform.unix.solaris.SolarisCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * FFM-backed Solaris CPU. Uses the legacy {@code kstat} chain only; Kstat2 exists only on the JDK 17-capped latest
 * Solaris, so FFM (JDK 25) never needs it.
 */
@ThreadSafe
final class SolarisCentralProcessorFFM extends SolarisCentralProcessor {

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
    protected List<LogicalProcessor> queryLogicalProcessors() {
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
        return logProcs;
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
            MemorySegment buf = arena.allocate((long) nelem * Double.BYTES);
            int retval = SolarisLibcFunctions.getloadavg(buf, nelem);
            int validCount = Math.max(retval, 0);
            for (int i = 0; i < validCount && i < nelem; i++) {
                average[i] = buf.get(ValueLayout.JAVA_DOUBLE, (long) i * Double.BYTES);
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
}
