/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.BsdCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * NetBSD Central Processor implementation. Shares the {@code dmesg}-based topology and cache parsing with the other
 * BSDs via {@link BsdCentralProcessor}; the processor identification, frequency, CPU ticks, and load average are read
 * from command output and name-based sysctls specific to NetBSD.
 */
@ThreadSafe
public class NetBsdCentralProcessor extends BsdCentralProcessor {
    /** The first 64-bit member of a hex dump. */
    private static final Pattern CPU_ID_FIRST_LONG = Pattern.compile("\\p{XDigit}{16}");
    /** The x86 model line, which ends with the CPUID signature: {@code "cpu0: AMD EPYC 7763 ..., id 0xa00f11"}. */
    private static final Pattern CPU_SIGNATURE = Pattern.compile("cpu0: .*, id 0x(\\p{XDigit}++)");

    private static final int CPUSTATES = 5;
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;

    @Override
    protected int sysctl(String name, int def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        // The architecture of the processor, where hw.machine is the port: evbarm rather than aarch64
        String machineArch = BsdSysctlUtil.sysctl("hw.machine_arch", "");
        boolean cpu64bit = machineArch.contains("64");
        List<String> dmesg = queryDmesg();
        String cpuName = queryCpuName();
        long cpuFreq = queryFreq(cpuName);
        // Off x86 the brand is absent and hw.model names the board, so take the processor's own line
        String dmesgName = parseDmesgCpuName(dmesg);
        if (BsdSysctlUtil.sysctl("machdep.cpu_brand", "").isEmpty() && !dmesgName.isEmpty()) {
            cpuName = dmesgName;
        }
        if (machineArch.startsWith("aarch64")) {
            long midr = parseMidr(ExecutingCommand.getFirstAnswer("sysctl -nx machdep.cpu0.cpu_id"),
                    machineArch.endsWith("eb"));
            if (midr >= 0) {
                return createArmIdentifier(midr, cpuName, cpuFreq);
            }
        }
        String cpuVendor = ExecutingCommand.getFirstAnswer("sysctl -n machdep.dmi.processor-vendor").trim();
        String[] fms = parseFamilyModelStepping(dmesg);
        return new ProcessorIdentifier(cpuVendor, cpuName, fms[0], fms[1], fms[2], "", cpu64bit, cpuFreq);
    }

    /**
     * Reads the Main ID Register from the hex dump of {@code machdep.cpu0.cpu_id}, a {@code struct
     * aarch64_sysctl_cpu_id} whose first member is the register as a 64-bit integer.
     *
     * @param cpuId     the output of {@code sysctl -nx machdep.cpu0.cpu_id}
     * @param bigEndian whether the kernel is big-endian ({@code aarch64eb})
     * @return the register, or -1 if the output does not carry one
     */
    static long parseMidr(String cpuId, boolean bigEndian) {
        Matcher m = CPU_ID_FIRST_LONG.matcher(cpuId.trim());
        if (!m.lookingAt()) {
            return -1L;
        }
        // The dump is in memory order, so a little-endian kernel's register reads byte-reversed
        long first = ParseUtil.hexStringToLong(m.group(), 0L);
        return (bigEndian ? first : Long.reverseBytes(first)) & 0xffff_ffffL;
    }

    /**
     * Creates the identifier of an ARM processor from its Main ID Register, in the form Linux reports it: the
     * implementer as the vendor, the part number as the model, and the variant and revision as the stepping.
     *
     * @param midr    the Main ID Register
     * @param cpuName the processor name
     * @param cpuFreq the processor frequency in Hz, or -1 if unknown
     * @return the processor identifier
     */
    static ProcessorIdentifier createArmIdentifier(long midr, String cpuName, long cpuFreq) {
        String vendor = String.format(Locale.ROOT, "0x%02x", (midr >> 24) & 0xff);
        String model = String.format(Locale.ROOT, "0x%03x", (midr >> 4) & 0xfff);
        String stepping = "r" + ((midr >> 20) & 0xf) + "p" + (midr & 0xf);
        String processorID = String.format(Locale.ROOT, "%08X00000000", midr);
        // Every AArch64 processor reports architecture 0xF, "defined by the ID registers"; Linux shows 8
        return new ProcessorIdentifier(vendor, cpuName, "8", model, stepping, processorID, true, cpuFreq);
    }

    @Override
    protected long[] queryCurrentFreq() {
        return new long[] { queryFreq(queryCpuName()) };
    }

    private static String queryCpuName() {
        String cpuName = BsdSysctlUtil.sysctl("machdep.cpu_brand", "");
        if (cpuName.isEmpty()) {
            cpuName = BsdSysctlUtil.sysctl("hw.model", "");
        }
        return cpuName;
    }

    private static long queryFreq(String cpuName) {
        // machdep.tsc_freq gives frequency in Hz on x86
        long freq = BsdSysctlUtil.sysctl("machdep.tsc_freq", 0L);
        if (freq > 0L) {
            return freq;
        }
        return parseFreq(cpuName, BsdSysctlUtil.sysctl("machdep.dmi.processor-frequency", ""));
    }

    /**
     * Parses the processor frequency from the processor name, e.g. {@code "Intel ... @ 2.10GHz"}, or failing that from
     * the SMBIOS processor frequency, e.g. {@code "2000 MHz"}, which is the only source on non-x86 machines.
     *
     * @param cpuName      the processor name
     * @param dmiFrequency the value of {@code machdep.dmi.processor-frequency}
     * @return the frequency in Hz, or -1 if neither string carries one
     */
    static long parseFreq(String cpuName, String dmiFrequency) {
        long freq = ParseUtil.parseHertz(cpuName);
        return freq > 0L ? freq : ParseUtil.parseHertz(dmiFrequency);
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
                contextSwitches = ParseUtil.parseLongOrDefault(ParseUtil.whitespaces.split(line.trim(), -1)[0], 0L);
            } else if (line.endsWith("device interrupts")) {
                interrupts = ParseUtil.parseLongOrDefault(ParseUtil.whitespaces.split(line.trim(), -1)[0], 0L);
            }
        }
        return new Pair<>(contextSwitches, interrupts);
    }

    /**
     * Parses family, model, and stepping from the CPUID signature at the end of the x86 model line in {@code dmesg},
     * {@code "cpu0: AMD EPYC 7763 64-Core Processor, id 0xa00f11"}.
     *
     * @param dmesg the lines emitted by {@code dmesg}, without timestamps
     * @return a 3-element array of [family, model, stepping] in decimal, each empty string if not found
     */
    static String[] parseFamilyModelStepping(List<String> dmesg) {
        for (String line : dmesg) {
            Matcher m = CPU_SIGNATURE.matcher(line);
            if (m.matches()) {
                Triplet<Integer, Integer, Integer> fms = cpuidToFamilyModelStepping(
                        ParseUtil.hexStringToInt(m.group(1), 0));
                return new String[] { fms.getA().toString(), fms.getB().toString(), fms.getC().toString() };
            }
        }
        return new String[] { "", "", "" };
    }

    @Override
    protected long[] querySystemCpuLoadTicks() {
        return cpTimeToTicks(queryCpTime(-1));
    }

    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][];
        for (int cpu = 0; cpu < ticks.length; cpu++) {
            ticks[cpu] = cpTimeToTicks(queryCpTime(cpu));
        }
        return ticks;
    }

    /**
     * Queries the {@code kern.cp_time} counters, for the whole system or for one processor.
     *
     * @param cpu The processor number, or a negative value for the system total
     * @return The counters indexed by the {@code CP_*} states, {@code [user, nice, sys, intr, idle]}; shorter than
     *         {@code CPUSTATES} if they could not be read
     */
    protected long[] queryCpTime(int cpu) {
        // "kern.cp_time: user = N, nice = N, sys = N, intr = N, idle = N", or "kern.cp_time.N: ..." per processor
        return parseCpTime(
                ExecutingCommand.getFirstAnswer(cpu < 0 ? "sysctl kern.cp_time" : "sysctl kern.cp_time." + cpu));
    }

    /**
     * Maps the {@code kern.cp_time} counters onto the {@link TickType} indices.
     *
     * @param cpTime The counters indexed by the {@code CP_*} states
     * @return The ticks, all zero if {@code cpTime} is shorter than {@code CPUSTATES}
     */
    static long[] cpTimeToTicks(long[] cpTime) {
        long[] ticks = new long[TickType.values().length];
        if (cpTime.length >= CPUSTATES) {
            ticks[TickType.USER.getIndex()] = cpTime[CP_USER];
            ticks[TickType.NICE.getIndex()] = cpTime[CP_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpTime[CP_SYS];
            ticks[TickType.IRQ.getIndex()] = cpTime[CP_INTR];
            ticks[TickType.IDLE.getIndex()] = cpTime[CP_IDLE];
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
    static long[] parseCpTime(@Nullable String cpTimeStr) {
        long[] ticks = new long[CPUSTATES];
        if (cpTimeStr == null || cpTimeStr.isEmpty()) {
            return ticks;
        }
        // Extract the values after the colon
        int colonIdx = cpTimeStr.indexOf(':');
        if (colonIdx < 0) {
            return ticks;
        }
        String[] pairs = cpTimeStr.substring(colonIdx + 1).split(",", -1);
        for (String pair : pairs) {
            // Both halves are trimmed below, so a plain "=" split is equivalent to matching the spaces here
            String[] kv = pair.split("=", -1);
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
        return queryLoadAverage(nelem);
    }

    /**
     * Queries the system load average.
     *
     * @param nelem Number of elements to return, already validated to be from one to three
     * @return the load averages for 1, 5, and 15 minutes, truncated to {@code nelem}; -1 for any not available
     */
    protected double[] queryLoadAverage(int nelem) {
        double[] average = new double[nelem];
        Arrays.fill(average, -1d);
        // Parse "vm.loadavg: 1.59 0.47 0.18"
        String loadavg = ExecutingCommand.getFirstAnswer("sysctl -n vm.loadavg");
        if (!loadavg.isEmpty()) {
            String[] loads = ParseUtil.whitespaces.split(loadavg.trim(), -1);
            for (int i = 0; i < nelem && i < loads.length; i++) {
                average[i] = ParseUtil.parseDoubleOrDefault(loads[i], -1d);
            }
        }
        return average;
    }
}
