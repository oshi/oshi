/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

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
                contextSwitches = ParseUtil.getFirstIntValue(line);
            } else if (line.endsWith("interrupts")) {
                interrupts = ParseUtil.getFirstIntValue(line);
            }
        }
        return new Pair<>(contextSwitches, interrupts);
    }
}
