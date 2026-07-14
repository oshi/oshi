/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import oshi.hardware.common.platform.unix.BsdCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Shared OpenBSD CentralProcessor logic. The processor identification, frequency, and {@code vmstat} parsing are common
 * to the JNA and FFM backends; subclasses supply only the sysctl reads (mib- and name-based) and the native CPU-tick
 * and load-average calls.
 */
public abstract class OpenBsdCentralProcessor extends BsdCentralProcessor {

    // OpenBSD sysctl(3) ABI identifiers, stable across releases (mirrored from the JNA/FFM libc backends).
    private static final int CTL_HW = 6;
    private static final int HW_MACHINE = 1;
    private static final int HW_MODEL = 2;
    private static final int HW_CPUSPEED = 12;

    // OpenBSD CPU state indices into the kern.cptime / kern.cptime2 arrays
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;
    private static final int CPUSTATES = 5;

    /**
     * Reads a string sysctl value by name.
     *
     * @param name the sysctl name
     * @param def  the default value
     * @return the sysctl string value or the default
     */
    protected abstract String sysctl(String name, String def);

    /**
     * Reads a string sysctl value addressed by a Management Information Base (MIB) array.
     *
     * @param mib the sysctl MIB
     * @param def the default value
     * @return the sysctl string value or the default
     */
    protected abstract String sysctl(int[] mib, String def);

    /**
     * Reads an integer sysctl value addressed by a Management Information Base (MIB) array.
     *
     * @param mib the sysctl MIB
     * @param def the default value
     * @return the sysctl integer value or the default
     */
    protected abstract int sysctl(int[] mib, int def);

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = sysctl("machdep.cpuvendor", "");
        int[] mib = { CTL_HW, HW_MODEL };
        String cpuName = sysctl(mib, "");
        int cpuid = ParseUtil.hexStringToInt(sysctl("machdep.cpuid", ""), 0);
        int cpufeature = ParseUtil.hexStringToInt(sysctl("machdep.cpufeature", ""), 0);
        Triplet<Integer, Integer, Integer> cpu = cpuidToFamilyModelStepping(cpuid);
        String cpuFamily = cpu.getA().toString();
        String cpuModel = cpu.getB().toString();
        String cpuStepping = cpu.getC().toString();
        long cpuFreq = ParseUtil.parseHertz(cpuName);
        if (cpuFreq < 0) {
            cpuFreq = queryMaxFreq();
        }
        mib = new int[] { CTL_HW, HW_MACHINE };
        String machine = sysctl(mib, "");
        boolean cpu64bit = machine != null && machine.contains("64")
                || ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
        String processorID = String.format(Locale.ROOT, "%08x%08x", cpufeature, cpuid);

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    private static Triplet<Integer, Integer, Integer> cpuidToFamilyModelStepping(int cpuid) {
        // family is bits 27:20 | 11:8
        int family = cpuid >> 16 & 0xff0 | cpuid >> 8 & 0xf;
        // model is bits 19:16 | 7:4
        int model = cpuid >> 12 & 0xf0 | cpuid >> 4 & 0xf;
        // stepping is bits 3:0
        int stepping = cpuid & 0xf;
        return new Triplet<>(family, model, stepping);
    }

    @Override
    protected long[] queryCurrentFreq() {
        long[] freq = new long[1];
        int[] mib = { CTL_HW, HW_CPUSPEED };
        freq[0] = sysctl(mib, 0) * 1_000_000L;
        return freq;
    }

    @Override
    protected Pair<Long, Long> queryVmStats() {
        long contextSwitches = 0L;
        long interrupts = 0L;
        List<String> vmstat = ExecutingCommand.runNative("vmstat -s");
        for (String line : vmstat) {
            if (line.endsWith("cpu context switches")) {
                contextSwitches = ParseUtil.parseLongOrDefault(line.trim().split("\\s+")[0], 0L);
            } else if (line.endsWith("interrupts")) {
                interrupts = ParseUtil.parseLongOrDefault(line.trim().split("\\s+")[0], 0L);
            }
        }
        return new Pair<>(contextSwitches, interrupts);
    }

    /**
     * Reads the system-wide CPU time counters ({@code kern.cptime}) as an array of 5 or 6 {@code long} values.
     *
     * @return the CPU state ticks, or a shorter/empty array if unavailable
     */
    protected abstract long[] querySystemCpTime();

    /**
     * Reads a single processor's CPU time counters ({@code kern.cptime2}) as an array of 5 or 6 {@code long} values.
     *
     * @param cpu the logical processor index
     * @return the CPU state ticks, or a shorter/empty array if unavailable
     */
    protected abstract long[] queryProcessorCpTime(int cpu);

    /**
     * Native {@code getloadavg(3)} call, filling {@code loadavg} with up to {@code nelem} samples.
     *
     * @param loadavg the array to populate
     * @param nelem   the number of elements requested
     * @return the number of samples retrieved
     */
    protected abstract int getloadavgNative(double[] loadavg, int nelem);

    /**
     * Maps a 5- or 6-element OpenBSD CPU-state array to the corresponding {@link TickType} slots of {@code ticks}.
     * OpenBSD 6.4+ adds a spin-time element, giving a 6-element array; the extra element shifts IRQ and IDLE by one.
     *
     * @param ticks   the destination tick array
     * @param cpTicks the source CPU-state values (5 or 6 longs)
     */
    private static void fillTicks(long[] ticks, long[] cpTicks) {
        if (cpTicks.length < CPUSTATES) {
            return;
        }
        ticks[TickType.USER.getIndex()] = cpTicks[CP_USER];
        ticks[TickType.NICE.getIndex()] = cpTicks[CP_NICE];
        ticks[TickType.SYSTEM.getIndex()] = cpTicks[CP_SYS];
        int offset = cpTicks.length > CPUSTATES ? 1 : 0;
        ticks[TickType.IRQ.getIndex()] = cpTicks[CP_INTR + offset];
        ticks[TickType.IDLE.getIndex()] = cpTicks[CP_IDLE + offset];
    }

    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        fillTicks(ticks, querySystemCpTime());
        return ticks;
    }

    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            fillTicks(ticks[cpu], queryProcessorCpTime(cpu));
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
            Arrays.fill(average, -1d);
        }
        return average;
    }
}
