/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Abstract base for the Solaris CPU. The command-based pieces (NUMA mapping, processor-ID feature flags, the {@code
 * dmesg} physical-processor parsing, the averaged tick rollup, and the {@code kstat -p} context-switch/interrupt
 * fallbacks) are shared; the {@code kstat}-chain queries are native and implemented by the JNA and FFM subclasses.
 */
@ThreadSafe
public abstract class SolarisCentralProcessor extends AbstractCentralProcessor {

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        List<LogicalProcessor> logProcs = queryLogicalProcessors();
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        Map<Integer, String> dmesg = new HashMap<>();
        // Jan 9 14:04:28 solaris unix: [ID 950921 kern.info] cpu0: Intel(r) Celeron(r)
        // CPU J3455 @ 1.50GHz
        // but NOT: Jan 9 14:04:28 solaris unix: [ID 950921 kern.info] cpu0: x86 (chipid
        // 0x0 GenuineIntel 506C9 family 6 model 92 step 9 clock 1500 MHz)
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

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // Average processor ticks
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
    public long queryContextSwitches() {
        long swtch = 0;
        // Shell-escaped pipe: Java \\\\| -> shell \\| -> kstat regex \| -> matches literal pipe.
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/pswitch\\\\|inv_swtch/");
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

    /**
     * Enumerates the logical processors from the subclass's kstat data source.
     *
     * @return the logical processor list (may be empty; the caller substitutes a single default processor)
     */
    protected abstract List<LogicalProcessor> queryLogicalProcessors();

    /**
     * Maps each CPU id to its NUMA node (lgroup) via {@code lgrpinfo}.
     *
     * @return a map of processor id to NUMA node
     */
    protected static Map<Integer, Integer> mapNumaNodes() {
        // Get numa node info from lgrpinfo
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        int lgroup = 0;
        for (String line : ExecutingCommand.runNative("lgrpinfo -c leaves")) {
            // Format:
            // lgroup 0 (root):
            // CPUs: 0 1
            // CPUs: 0-7
            // CPUs: 0-3 6 7 12 13
            // CPU: 0
            // CPU: 1
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

    /**
     * Fetches the ProcessorID by encoding the stepping, model, family, and feature flags.
     *
     * @param stepping the stepping
     * @param model    the model
     * @param family   the family
     * @return the Processor ID string
     */
    protected static String getProcessorID(String stepping, String model, String family) {
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
}
