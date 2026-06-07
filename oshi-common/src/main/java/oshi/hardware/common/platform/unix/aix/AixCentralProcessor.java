/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.Lssrad;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Abstract base for AIX CentralProcessor. The ProcessorIdentifier parsing, cache table, load-average math, and tick
 * conversion live here; concrete subclasses (JNA/FFM) provide the partition config and per-CPU/total tick rows from
 * their respective perfstat data sources.
 */
@ThreadSafe
public abstract class AixCentralProcessor extends AbstractCentralProcessor {

    /** Jiffies per second, used for process time counters. */
    protected static final long USER_HZ = ParseUtil
            .parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"), 100L);

    private static final int SBITS = querySbits();

    /** Tick values OSHI consumes from {@code perfstat_cpu_total_t} and {@code perfstat_cpu_t}. */
    public static class CpuTickRow {
        public long user;
        public long sys;
        public long idle;
        public long wait;
        public long devintrs;
        public long softintrs;
        public long idle_stolen_purr;
        public long busy_stolen_purr;
    }

    /** Aggregate {@code perfstat_cpu_total_t} values OSHI consumes. */
    public static final class CpuTotalRow extends CpuTickRow {
        public int ncpus;
        public long processorHZ;
        public long pswitch;
        public long[] loadavg = new long[3];
    }

    /** Partition fields the CPU init path needs from {@code perfstat_partition_config_t}. */
    public static final class PartitionInfo {
        public long vcpusMax;
        public int smtthreads;
        public String machineID = "";
        public double processorMHz;
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        PartitionInfo info = queryPartitionInfo();
        String cpuVendor = Constants.UNKNOWN;
        String cpuName = "";
        String cpuFamily = "";
        boolean cpu64bit = false;

        final String nameMarker = "Processor Type:";
        final String familyMarker = "Processor Version:";
        final String bitnessMarker = "CPU Type:";
        for (final String checkLine : ExecutingCommand.runNative("prtconf")) {
            if (checkLine.startsWith(nameMarker)) {
                cpuName = checkLine.split(nameMarker)[1].trim();
                if (cpuName.startsWith("P")) {
                    cpuVendor = "IBM";
                } else if (cpuName.startsWith("I")) {
                    cpuVendor = "Intel";
                }
            } else if (checkLine.startsWith(familyMarker)) {
                cpuFamily = checkLine.split(familyMarker)[1].trim();
            } else if (checkLine.startsWith(bitnessMarker)) {
                cpu64bit = checkLine.split(bitnessMarker)[1].contains("64");
            }
        }

        String cpuModel = "";
        String cpuStepping = "";
        String machineId = info.machineID;
        if (machineId.isEmpty()) {
            machineId = ExecutingCommand.getFirstAnswer("uname -m");
        }
        if (machineId.length() > 10) {
            int m = machineId.length() - 4;
            int s = machineId.length() - 2;
            cpuModel = machineId.substring(m, s);
            cpuStepping = machineId.substring(s);
        }

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, machineId, cpu64bit,
                (long) (info.processorMHz * 1_000_000L));
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        PartitionInfo info = queryPartitionInfo();
        int physProcs = (int) info.vcpusMax;
        if (physProcs < 1) {
            physProcs = 1;
        }
        int smtThreads = info.smtthreads > 0 ? info.smtthreads : 1;
        int lcpus = physProcs * smtThreads;
        int lpPerPp = lcpus / physProcs;
        Map<Integer, Pair<Integer, Integer>> nodePkgMap = Lssrad.queryNodesPackages();
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (int proc = 0; proc < lcpus; proc++) {
            Pair<Integer, Integer> nodePkg = nodePkgMap.get(proc);
            int physProc = proc / lpPerPp;
            logProcs.add(new LogicalProcessor(proc, physProc, nodePkg == null ? 0 : nodePkg.getB(),
                    nodePkg == null ? 0 : nodePkg.getA()));
        }
        return new Quartet<>(logProcs, null, getCachesForModel(physProcs), Collections.emptyList());
    }

    private static List<ProcessorCache> getCachesForModel(int cores) {
        List<ProcessorCache> caches = new ArrayList<>();
        int powerVersion = ParseUtil.getFirstIntValue(ExecutingCommand.getFirstAnswer("uname -n"));
        switch (powerVersion) {
            case 7:
                caches.add(new ProcessorCache(3, 8, 128, (2 * 32) << 20, Type.UNIFIED));
                caches.add(new ProcessorCache(2, 8, 128, 256 << 10, Type.UNIFIED));
                caches.add(new ProcessorCache(1, 8, 128, 32 << 10, Type.DATA));
                caches.add(new ProcessorCache(1, 4, 128, 32 << 10, Type.INSTRUCTION));
                break;
            case 8:
                caches.add(new ProcessorCache(4, 8, 128, (16 * 16) << 20, Type.UNIFIED));
                caches.add(new ProcessorCache(3, 8, 128, 40 << 20, Type.UNIFIED));
                caches.add(new ProcessorCache(2, 8, 128, 512 << 10, Type.UNIFIED));
                caches.add(new ProcessorCache(1, 8, 128, 64 << 10, Type.DATA));
                caches.add(new ProcessorCache(1, 8, 128, 32 << 10, Type.INSTRUCTION));
                break;
            case 9:
                caches.add(new ProcessorCache(3, 20, 128, (cores * 10) << 20, Type.UNIFIED));
                caches.add(new ProcessorCache(2, 8, 128, 512 << 10, Type.UNIFIED));
                caches.add(new ProcessorCache(1, 8, 128, 32 << 10, Type.DATA));
                caches.add(new ProcessorCache(1, 8, 128, 32 << 10, Type.INSTRUCTION));
                break;
            default:
                // Don't guess
        }
        return caches;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        CpuTotalRow row = queryCpuTotal();
        return ticksFromRow(row);
    }

    @Override
    public long[] queryCurrentFreq() {
        // pmcycles may require root; this is best-effort.
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, -1);
        String freqMarker = "runs at";
        int idx = 0;
        for (final String checkLine : ExecutingCommand.runNative("pmcycles -m")) {
            if (checkLine.contains(freqMarker)) {
                freqs[idx++] = ParseUtil.parseHertz(checkLine.split(freqMarker)[1].trim());
                if (idx >= freqs.length) {
                    break;
                }
            }
        }
        return freqs;
    }

    @Override
    protected long queryMaxFreq() {
        return queryCpuTotal().processorHZ;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        long[] loadavg = queryCpuTotal().loadavg;
        for (int i = 0; i < nelem; i++) {
            average[i] = loadavg[i] / (double) (1L << SBITS);
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        CpuTickRow[] cpu = queryPerCpuTicks();
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        for (int i = 0; i < cpu.length && i < ticks.length; i++) {
            ticks[i] = ticksFromRow(cpu[i]);
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        return queryCpuTotal().pswitch;
    }

    @Override
    public long queryInterrupts() {
        CpuTotalRow row = queryCpuTotal();
        return row.devintrs + row.softintrs;
    }

    private static long[] ticksFromRow(CpuTickRow row) {
        long[] ticks = new long[TickType.values().length];
        ticks[TickType.USER.ordinal()] = row.user * 1000L / USER_HZ;
        // Skip NICE
        ticks[TickType.SYSTEM.ordinal()] = row.sys * 1000L / USER_HZ;
        ticks[TickType.IDLE.ordinal()] = row.idle * 1000L / USER_HZ;
        ticks[TickType.IOWAIT.ordinal()] = row.wait * 1000L / USER_HZ;
        ticks[TickType.IRQ.ordinal()] = row.devintrs * 1000L / USER_HZ;
        ticks[TickType.SOFTIRQ.ordinal()] = row.softintrs * 1000L / USER_HZ;
        ticks[TickType.STEAL.ordinal()] = (row.idle_stolen_purr + row.busy_stolen_purr) * 1000L / USER_HZ;
        return ticks;
    }

    private static int querySbits() {
        for (String s : FileUtil.readFile("/usr/include/sys/proc.h")) {
            if (s.contains("SBITS") && s.contains("#define")) {
                return ParseUtil.parseLastInt(s, 16);
            }
        }
        return 16;
    }

    /**
     * Queries the AIX partition config.
     *
     * @return populated partition config (vcpus.max, smtthreads, machineID, processorMHz)
     */
    protected abstract PartitionInfo queryPartitionInfo();

    /**
     * Queries the aggregate CPU statistics.
     *
     * @return aggregate CPU ticks + load avg + freq
     */
    protected abstract CpuTotalRow queryCpuTotal();

    /**
     * Queries per-CPU statistics.
     *
     * @return per-CPU tick rows
     */
    protected abstract CpuTickRow[] queryPerCpuTicks();
}
