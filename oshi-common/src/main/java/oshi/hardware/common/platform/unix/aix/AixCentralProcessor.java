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
        Quartet<String, String, String, Boolean> id = parseProcessorId(ExecutingCommand.runNative("prtconf"));
        String cpuVendor = id.getA();
        String cpuName = id.getB();
        String cpuFamily = id.getC();
        boolean cpu64bit = id.getD();

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

    /**
     * Parses {@code prtconf} output for the processor vendor, name, family, and 64-bit flag.
     *
     * @param prtconf the lines of {@code prtconf} output
     * @return a {@link Quartet} of vendor, name, family, and 64-bit flag (vendor {@link Constants#UNKNOWN} if the
     *         processor type is not recognized)
     */
    static Quartet<String, String, String, Boolean> parseProcessorId(List<String> prtconf) {
        String cpuVendor = Constants.UNKNOWN;
        String cpuName = "";
        String cpuFamily = "";
        boolean cpu64bit = false;

        final String nameMarker = "Processor Type:";
        final String familyMarker = "Processor Version:";
        final String bitnessMarker = "CPU Type:";
        for (final String checkLine : prtconf) {
            // Each branch already confirmed the marker is a prefix, so substring is safe even when the value is empty
            if (checkLine.startsWith(nameMarker)) {
                cpuName = checkLine.substring(nameMarker.length()).trim();
                if (cpuName.startsWith("P")) {
                    cpuVendor = "IBM";
                } else if (cpuName.startsWith("I")) {
                    cpuVendor = "Intel";
                }
            } else if (checkLine.startsWith(familyMarker)) {
                cpuFamily = checkLine.substring(familyMarker.length()).trim();
            } else if (checkLine.startsWith(bitnessMarker)) {
                cpu64bit = checkLine.substring(bitnessMarker.length()).contains("64");
            }
        }
        return new Quartet<>(cpuVendor, cpuName, cpuFamily, cpu64bit);
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
        return cachesForPowerVersion(parsePowerVersion(ExecutingCommand.runNative("prtconf")), cores);
    }

    /**
     * Parses the POWER generation from {@code prtconf}'s {@code Processor Version} line (e.g. {@code PV_8_Compat} -&gt;
     * 8). This is the source used to select the cache topology; {@code uname -n} is the hostname and yields a
     * meaningless number.
     *
     * @param prtconf the lines of {@code prtconf} output
     * @return the POWER generation, or 0 if no {@code Processor Version} line is present
     */
    static int parsePowerVersion(List<String> prtconf) {
        final String versionMarker = "Processor Version:";
        for (String line : prtconf) {
            if (line.startsWith(versionMarker)) {
                return ParseUtil.getFirstIntValue(line.substring(versionMarker.length()));
            }
        }
        return 0;
    }

    /**
     * Returns the known cache topology for a given POWER version.
     *
     * @param powerVersion the POWER generation (e.g. 7, 8, 9), as parsed from {@code prtconf}'s {@code Processor
     *                     Version} field
     * @param cores        the number of physical cores, used to size the shared L3 on POWER9
     * @return the processor caches for that generation, or an empty list if the version is unrecognized
     */
    static List<ProcessorCache> cachesForPowerVersion(int powerVersion, int cores) {
        List<ProcessorCache> caches = new ArrayList<>();
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
        return parseCurrentFreq(ExecutingCommand.runNative("pmcycles -m"), getLogicalProcessorCount());
    }

    /**
     * Parses {@code pmcycles -m} output into a per-logical-processor frequency array.
     *
     * @param pmcycles the lines of {@code pmcycles -m} output
     * @param count    the number of logical processors (array length)
     * @return an array of frequencies in Hz; entries with no corresponding output line remain -1
     */
    static long[] parseCurrentFreq(List<String> pmcycles, int count) {
        long[] freqs = new long[count];
        Arrays.fill(freqs, -1);
        String freqMarker = "runs at";
        int idx = 0;
        for (final String checkLine : pmcycles) {
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
        return parseSbits(FileUtil.readFile("/usr/include/sys/proc.h"));
    }

    /**
     * Parses the {@code SBITS} {@code #define} value from {@code /usr/include/sys/proc.h}, used to scale load averages.
     *
     * @param procH the lines of {@code /usr/include/sys/proc.h}
     * @return the SBITS value, or 16 if not found
     */
    static int parseSbits(List<String> procH) {
        for (String s : procH) {
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
