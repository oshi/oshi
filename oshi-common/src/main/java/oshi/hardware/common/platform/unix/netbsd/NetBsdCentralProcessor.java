/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.BsdCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.netbsd.NetBsdSysctlUtil;
import oshi.util.tuples.Pair;

/**
 * NetBSD Central Processor implementation. Shares the {@code dmesg}-based topology and cache parsing with the other
 * BSDs via {@link BsdCentralProcessor}; the processor identification, frequency, CPU ticks, and load average are read
 * from command output and name-based sysctls specific to NetBSD.
 */
@ThreadSafe
public class NetBsdCentralProcessor extends BsdCentralProcessor {

    private static final int CPUSTATES = 5;
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;

    @Override
    protected int sysctl(String name, int def) {
        return NetBsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = NetBsdSysctlUtil.sysctl("machdep.cpu_vendor", "");
        if (cpuVendor.isEmpty()) {
            cpuVendor = ExecutingCommand.getFirstAnswer("sysctl -n machdep.dmi.processor-vendor").trim();
        }
        String cpuName = NetBsdSysctlUtil.sysctl("machdep.cpu_brand", "");
        if (cpuName.isEmpty()) {
            cpuName = NetBsdSysctlUtil.sysctl("hw.model", "");
        }
        String[] fms = parseFamilyModelStepping(ExecutingCommand.runNative("dmesg"));
        String cpuFamily = fms[0];
        String cpuModel = fms[1];
        String cpuStepping = fms[2];
        String processorID = "";
        long cpuFreq = NetBsdSysctlUtil.sysctl("machdep.tsc_freq", 0L);
        if (cpuFreq == 0L) {
            cpuFreq = ParseUtil.parseHertz(cpuName);
            if (cpuFreq < 0) {
                cpuFreq = queryMaxFreq();
            }
        }
        String machine = NetBsdSysctlUtil.sysctl("hw.machine", "");
        boolean cpu64bit = machine != null && machine.contains("64")
                || ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected long[] queryCurrentFreq() {
        long[] freq = new long[1];
        // machdep.tsc_freq gives frequency in Hz on x86
        freq[0] = NetBsdSysctlUtil.sysctl("machdep.tsc_freq", 0L);
        if (freq[0] == 0L) {
            // Fallback: parse from cpu name (e.g., "Intel ... @ 2.10GHz")
            freq[0] = queryMaxFreq();
        }
        return freq;
    }

    @Override
    protected Pair<Long, Long> queryVmStats() {
        return parseVmStats(ExecutingCommand.runNative("vmstat -s"));
    }

    /**
     * Parses the output of {@code vmstat -s} to extract context switch and interrupt counts.
     *
     * @param vmstat the lines emitted by {@code vmstat -s}
     * @return a {@link Pair} of (context switches, interrupts)
     */
    static Pair<Long, Long> parseVmStats(List<String> vmstat) {
        long contextSwitches = 0L;
        long interrupts = 0L;
        for (String line : vmstat) {
            if (line.endsWith("CPU context switches")) {
                contextSwitches = ParseUtil.parseLongOrDefault(line.trim().split("\\s+")[0], 0L);
            } else if (line.endsWith("device interrupts")) {
                interrupts = ParseUtil.parseLongOrDefault(line.trim().split("\\s+")[0], 0L);
            }
        }
        return new Pair<>(contextSwitches, interrupts);
    }

    /**
     * Parses family, model, and stepping from {@code dmesg} output. Looks for lines starting with {@code "cpu0:"} that
     * contain a hyphen-separated triple like {@code "06-7a-01"}.
     *
     * @param dmesg the lines emitted by {@code dmesg}
     * @return a 3-element array of [family, model, stepping], each empty string if not found
     */
    static String[] parseFamilyModelStepping(List<String> dmesg) {
        for (String line : dmesg) {
            if (line.startsWith("cpu0:") && line.matches(".*\\d+-[\\da-fA-F]+-[\\da-fA-F]+.*")) {
                String[] parts = line.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.matches("[\\da-fA-F]+-[\\da-fA-F]+-[\\da-fA-F]+")) {
                        return trimmed.split("-");
                    }
                }
            }
        }
        return new String[] { "", "", "" };
    }

    /**
     * Get the system CPU load ticks
     *
     * @return The system CPU load ticks
     */
    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // Parse "kern.cp_time: user = N, nice = N, sys = N, intr = N, idle = N"
        long[] cpuTicks = parseCpTime(ExecutingCommand.getFirstAnswer("sysctl kern.cp_time"));
        if (cpuTicks.length >= CPUSTATES) {
            ticks[TickType.USER.getIndex()] = cpuTicks[CP_USER];
            ticks[TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
            ticks[TickType.IRQ.getIndex()] = cpuTicks[CP_INTR];
            ticks[TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE];
        }
        return ticks;
    }

    /**
     * Get the processor CPU load ticks
     *
     * @return The processor CPU load ticks
     */
    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            // Per-CPU: "kern.cp_time.N: user = ..., nice = ..., sys = ..., intr = ..., idle = ..."
            long[] cpuTicks = parseCpTime(ExecutingCommand.getFirstAnswer("sysctl kern.cp_time." + cpu));
            if (cpuTicks.length >= CPUSTATES) {
                ticks[cpu][TickType.USER.getIndex()] = cpuTicks[CP_USER];
                ticks[cpu][TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                ticks[cpu][TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                ticks[cpu][TickType.IRQ.getIndex()] = cpuTicks[CP_INTR];
                ticks[cpu][TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE];
            }
        }
        return ticks;
    }

    /**
     * Parse sysctl kern.cp_time output to an array of tick values.
     * <p>
     * Input format: "kern.cp_time: user = 2930, nice = 42, sys = 1334, intr = 1877, idle = 46354"
     *
     * @param cpTimeStr the sysctl output string
     * @return array of [user, nice, sys, intr, idle] tick values
     */
    static long[] parseCpTime(String cpTimeStr) {
        long[] ticks = new long[CPUSTATES];
        if (cpTimeStr == null || cpTimeStr.isEmpty()) {
            return ticks;
        }
        // Extract the values after the colon
        int colonIdx = cpTimeStr.indexOf(':');
        if (colonIdx < 0) {
            return ticks;
        }
        String[] pairs = cpTimeStr.substring(colonIdx + 1).split(",");
        for (String pair : pairs) {
            String[] kv = pair.trim().split("\\s*=\\s*");
            if (kv.length == 2) {
                long val = ParseUtil.parseLongOrDefault(kv[1].trim(), 0L);
                switch (kv[0].trim()) {
                    case "user":
                        ticks[CP_USER] = val;
                        break;
                    case "nice":
                        ticks[CP_NICE] = val;
                        break;
                    case "sys":
                        ticks[CP_SYS] = val;
                        break;
                    case "intr":
                        ticks[CP_INTR] = val;
                        break;
                    case "idle":
                        ticks[CP_IDLE] = val;
                        break;
                    default:
                        break;
                }
            }
        }
        return ticks;
    }

    /**
     * Returns the system load average for the number of elements specified, up to 3, representing 1, 5, and 15 minutes.
     * The system load average is the sum of the number of runnable entities queued to the available processors and the
     * number of runnable entities running on the available processors averaged over a period of time. The way in which
     * the load average is calculated is operating system specific but is typically a damped time-dependent average. If
     * the load average is not available, a negative value is returned. This method is designed to provide a hint about
     * the system load and may be queried frequently.
     * <p>
     * The load average may be unavailable on some platforms (e.g., Windows) where it is expensive to implement this
     * method.
     *
     * @param nelem Number of elements to return.
     * @return an array of the system load averages for 1, 5, and 15 minutes with the size of the array specified by
     *         nelem; or negative values if not available.
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        Arrays.fill(average, -1d);
        // Parse "vm.loadavg: 1.59 0.47 0.18"
        String loadavg = ExecutingCommand.getFirstAnswer("sysctl -n vm.loadavg");
        if (!loadavg.isEmpty()) {
            String[] loads = ParseUtil.whitespaces.split(loadavg.trim());
            for (int i = 0; i < nelem && i < loads.length; i++) {
                average[i] = ParseUtil.parseDoubleOrDefault(loads[i], -1d);
            }
        }
        return average;
    }
}
