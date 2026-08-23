/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorPerformanceProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Common non-native logic for Windows Central Processor implementations.
 */
@ThreadSafe
public abstract class WindowsCentralProcessor extends AbstractCentralProcessor {

    /** Default constructor. */
    protected WindowsCentralProcessor() {
        // Store the initial query and start the memoizer expiration
        if (USE_CPU_UTILITY && processorUtilityCounters != null) {
            setInitialUtilityCounters(processorUtilityCounters.get().getB());
        }
    }

    // Populated by initProcessorCounts, which the parent constructor calls before this class's field initializers
    // run; that ordering rules out an AtomicReference holder (its initializer would not have run yet), so this stays
    // a volatile reference to a swap-published immutable snapshot, for which volatile publication is sufficient.
    // Assigned by buildNumaNodeProcMap during initProcessorCounts, which the superclass constructor calls before
    // this class's field initializers would run, so it must not be given one. Read through the getter.
    private volatile @Nullable Map<String, Integer> numaNodeProcToLogicalProcMap; // NOSONAR java:S3077

    /** Whether to use legacy Processor counters rather than Processor Information counters. */
    protected static final boolean USE_LEGACY_SYSTEM_COUNTERS = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_LEGACY_SYSTEM_COUNTERS, false);

    /** Whether to start a daemon thread to calculate load average. */
    protected static final boolean USE_LOAD_AVERAGE = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, false);

    /** Whether to match task manager using Processor Utility ticks. */
    protected static final boolean USE_CPU_UTILITY = isWindows8OrGreater()
            && GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, false);

    // Previous sample for utility base multiplier calculation
    private final AtomicReference<Map<ProcessorUtilityTickCountProperty, List<Long>>> initialUtilityCounters = new AtomicReference<>();
    // Lazily initialized
    private @Nullable Long utilityBaseMultiplier;

    // This tick query is memoized to enforce a minimum elapsed time for determining the capacity base multiplier
    private final @Nullable Supplier<Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>>> processorUtilityCounters = USE_CPU_UTILITY
            ? memoize(this::queryProcessorCapacityCounters, TimeUnit.MILLISECONDS.toNanos(300L))
            : null;

    /**
     * Checks whether the OS version is at least the given Windows {@code major.minor} using system property
     * 'os.version', without requiring native access.
     *
     * @param major the minimum major version
     * @param minor the minimum minor version
     * @return true if the OS version is at least {@code major.minor}
     */
    private static boolean isWindowsVersionOrGreater(int major, int minor) {
        String[] parts = System.getProperty("os.version", "").split("\\.", -1);
        if (parts.length >= 2) {
            int osMajor = ParseUtil.parseIntOrDefault(parts[0], 0);
            int osMinor = ParseUtil.parseIntOrDefault(parts[1], 0);
            return osMajor > major || (osMajor == major && osMinor >= minor);
        }
        return false;
    }

    /**
     * Checks whether the OS version is Windows 8 or greater.
     *
     * @return true if Windows 8 or greater
     */
    private static boolean isWindows8OrGreater() {
        return isWindowsVersionOrGreater(6, 2);
    }

    /**
     * Whether the legacy {@code GetSystemTimes} path is used for system CPU load ticks rather than the per-processor
     * perfmon sum. Overridable so tests can force the legacy path independently of the
     * {@link #USE_LEGACY_SYSTEM_COUNTERS} static capture.
     *
     * @return true to use the legacy {@code GetSystemTimes} path
     */
    protected boolean useLegacySystemCounters() {
        return USE_LEGACY_SYSTEM_COUNTERS;
    }

    /**
     * Gets the numaNodeProcToLogicalProcMap.
     *
     * @return the map
     */
    protected Map<String, Integer> getNumaNodeProcToLogicalProcMap() {
        Map<String, Integer> map = this.numaNodeProcToLogicalProcMap;
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Gets the initial utility counters.
     *
     * @return the initial utility counters
     */
    protected Map<ProcessorUtilityTickCountProperty, List<Long>> getInitialUtilityCounters() {
        return this.initialUtilityCounters.get();
    }

    /**
     * Sets the initial utility counters.
     *
     * @param counters the counters to set
     */
    protected void setInitialUtilityCounters(Map<ProcessorUtilityTickCountProperty, List<Long>> counters) {
        this.initialUtilityCounters.set(counters);
    }

    /**
     * Builds the numaNodeProcToLogicalProcMap from the logical processor list.
     *
     * @param logProcs the list of logical processors
     */
    protected void buildNumaNodeProcMap(List<LogicalProcessor> logProcs) {
        Map<Integer, Integer> nextProcIndexByNode = new HashMap<>();
        int lp = 0;
        Map<String, Integer> map = new HashMap<>();
        for (LogicalProcessor logProc : logProcs) {
            int node = logProc.getNumaNode();
            int procNum = nextProcIndexByNode.getOrDefault(node, 0);
            map.put(String.format(Locale.ROOT, "%d,%d", node, procNum), lp++);
            nextProcIndexByNode.put(node, procNum + 1);
        }
        // Publish the fully-built map as the final step so readers never observe a partial map
        this.numaNodeProcToLogicalProcMap = map;
    }

    /**
     * Parses identifier string
     *
     * @param identifier the full identifier string
     * @param key        the key to retrieve
     * @return the string following id
     */
    protected static String parseIdentifier(String identifier, String key) {
        String[] idSplit = ParseUtil.whitespaces.split(identifier, -1);
        boolean found = false;
        for (String s : idSplit) {
            if (found) {
                return s;
            }
            found = s.equals(key);
        }
        return "";
    }

    /**
     * Lazily calculate the capacity tick multiplier once.
     *
     * @param deltaBase The difference in base ticks.
     * @param deltaT    The difference in elapsed 100NS time
     * @return The ratio of elapsed time to base ticks
     */
    protected synchronized long lazilyCalculateMultiplier(long deltaBase, long deltaT) {
        if (utilityBaseMultiplier == null) {
            // If too much time has elapsed from class instantiation, re-initialize the
            // ticks and return without calculating. Approx 7 minutes for 100NS counter to
            // exceed max unsigned int.
            if (deltaT >> 32 > 0) {
                setInitialUtilityCounters(queryProcessorUtilityCounters());
                return 0L;
            }
            // Base counter wraps approximately every 115 minutes
            // If deltaBase is nonpositive assume it has wrapped
            if (deltaBase <= 0) {
                deltaBase += 1L << 32;
            }
            long multiplier = Math.round((double) deltaT / deltaBase);
            // If not enough time has elapsed, return the value this one time but don't
            // persist. 5000 ms = 50 million 100NS ticks
            if (deltaT < 50_000_000L) {
                return multiplier;
            }
            utilityBaseMultiplier = multiplier;
        }
        return utilityBaseMultiplier;
    }

    /**
     * Provides the current utility counters for re-initialization.
     *
     * @return the current processor utility counter values
     */
    protected Map<ProcessorUtilityTickCountProperty, List<Long>> queryProcessorUtilityCounters() {
        return queryProcessorCapacityCounters().getB();
    }

    /**
     * Subclasses query the perfmon Processor Information capacity (utility) counters via their JNA or FFM driver.
     *
     * @return the instance names and processor capacity/utility counter values
     */
    protected abstract Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters();

    /**
     * Subclasses query the perfmon Processor Information tick counters via their JNA or FFM driver.
     *
     * @return the instance names and processor tick counter values
     */
    protected abstract Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters();

    /**
     * Subclasses query the Processor Performance (% Processor Performance) counters via their JNA or FFM driver.
     *
     * @return the instance names and processor performance counter values
     */
    protected abstract Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> queryProcessorPerformanceCounters();

    /**
     * Subclasses query the Processor Frequency (% of Maximum Frequency) counters via their JNA or FFM driver.
     *
     * @return the instance names and processor frequency counter values
     */
    protected abstract Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters();

    /**
     * Subclasses call {@code CallNtPowerInformation} for Processor information, returning the requested field for each
     * logical processor.
     *
     * @param fieldIndex the field index (1 = max MHz, 2 = current MHz)
     * @return the array of frequency values, in Hz
     */
    protected abstract long[] queryNTPower(int fieldIndex);

    /**
     * Checks whether the OS version is Windows 7 or greater.
     *
     * @return true if Windows 7 or greater
     */
    private static boolean isWindows7OrGreater() {
        return isWindowsVersionOrGreater(6, 1);
    }

    @Override
    public long[] queryCurrentFreq() {
        if (isWindows7OrGreater()) {
            long maxFreq = this.getMaxFreq();
            if (maxFreq > 0) {
                // Prefer % Processor Performance from WMI formatted table (Win8+, reports >100% with turbo)
                Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> perfPair = queryProcessorPerformanceCounters();
                List<Long> perfList = perfPair.getB().get(ProcessorPerformanceProperty.PERCENTPROCESSORPERFORMANCE);
                long[] freqs = mapPercentToFreqs(perfPair.getA(), perfList, maxFreq);
                if (freqs != null) {
                    return freqs;
                }
                // Fall back to % of Maximum Frequency (Win7, caps at 100%)
                Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> freqPair = queryFrequencyCounters();
                List<Long> percentMaxList = freqPair.getB().get(ProcessorFrequencyProperty.PERCENTOFMAXIMUMFREQUENCY);
                freqs = mapPercentToFreqs(freqPair.getA(), percentMaxList, maxFreq);
                if (freqs != null) {
                    return freqs;
                }
            }
        }
        // If <Win7 or anything failed in PDH/WMI, use the native call
        return queryNTPower(2); // Current is field index 2
    }

    private long @Nullable [] mapPercentToFreqs(List<String> instances, @Nullable List<Long> percentList,
            long maxFreq) {
        if (instances.isEmpty() || percentList == null) {
            return null;
        }
        long[] freqs = new long[getLogicalProcessorCount()];
        boolean populated = false;
        // instances and percentList are parallel lists; read by position i and map the instance name to the logical
        // processor index the same way processTickData does (numa "group,proc" via the map, else the plain number).
        for (int i = 0; i < instances.size(); i++) {
            String instance = instances.get(i);
            int cpu;
            if (instance.contains(",")) {
                if (!getNumaNodeProcToLogicalProcMap().containsKey(instance)) {
                    continue;
                }
                cpu = getNumaNodeProcToLogicalProcMap().get(instance);
            } else {
                cpu = ParseUtil.parseIntOrDefault(instance, -1);
            }
            if (cpu < 0 || cpu >= getLogicalProcessorCount() || i >= percentList.size()) {
                continue;
            }
            freqs[cpu] = percentList.get(i) * maxFreq / 100L;
            if (freqs[cpu] > 0) {
                populated = true;
            }
        }
        return populated ? freqs : null;
    }

    @Override
    public long queryMaxFreq() {
        long[] freqs = queryNTPower(1); // Max is field index 1
        return Arrays.stream(freqs).max().orElse(-1L);
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        // These are used in all cases
        List<String> instances;
        List<Long> systemList;
        List<Long> userList;
        List<Long> irqList;
        List<Long> softIrqList;
        List<Long> idleList;
        // These are only used with USE_CPU_UTILITY
        List<Long> baseList = null;
        List<Long> systemUtility = null;
        List<Long> processorUtility = null;
        List<Long> processorUtilityBase = null;
        List<Long> initSystemList = null;
        List<Long> initUserList = null;
        List<Long> initBase = null;
        List<Long> initSystemUtility = null;
        List<Long> initProcessorUtility = null;
        List<Long> initProcessorUtilityBase = null;
        if (USE_CPU_UTILITY && processorUtilityCounters != null) {
            Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> instanceValuePair = processorUtilityCounters
                    .get();
            instances = instanceValuePair.getA();
            Map<ProcessorUtilityTickCountProperty, List<Long>> valueMap = instanceValuePair.getB();
            systemList = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDTIME);
            userList = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTUSERTIME);
            irqList = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTINTERRUPTTIME);
            softIrqList = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTDPCTIME);
            // % Processor Time is actually Idle time
            idleList = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTPROCESSORTIME);
            baseList = valueMap.get(ProcessorUtilityTickCountProperty.TIMESTAMP_SYS100NS);
            // Utility ticks, if configured
            systemUtility = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDUTILITY);
            processorUtility = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY);
            processorUtilityBase = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY_BASE);

            initSystemList = getInitialUtilityCounters().get(ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDTIME);
            initUserList = getInitialUtilityCounters().get(ProcessorUtilityTickCountProperty.PERCENTUSERTIME);
            initBase = getInitialUtilityCounters().get(ProcessorUtilityTickCountProperty.TIMESTAMP_SYS100NS);
            initSystemUtility = getInitialUtilityCounters()
                    .get(ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDUTILITY);
            initProcessorUtility = getInitialUtilityCounters()
                    .get(ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY);
            initProcessorUtilityBase = getInitialUtilityCounters()
                    .get(ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY_BASE);
        } else {
            Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> instanceValuePair = queryProcessorCounters();
            instances = instanceValuePair.getA();
            Map<ProcessorTickCountProperty, List<Long>> valueMap = instanceValuePair.getB();
            systemList = valueMap.get(ProcessorTickCountProperty.PERCENTPRIVILEGEDTIME);
            userList = valueMap.get(ProcessorTickCountProperty.PERCENTUSERTIME);
            irqList = valueMap.get(ProcessorTickCountProperty.PERCENTINTERRUPTTIME);
            softIrqList = valueMap.get(ProcessorTickCountProperty.PERCENTDPCTIME);
            // % Processor Time is actually Idle time
            idleList = valueMap.get(ProcessorTickCountProperty.PERCENTPROCESSORTIME);
        }

        return processTickData(instances, systemList, userList, irqList, softIrqList, idleList, baseList, systemUtility,
                processorUtility, processorUtilityBase, initSystemList, initUserList, initBase, initSystemUtility,
                initProcessorUtility, initProcessorUtilityBase);
    }

    /**
     * Processes raw perfmon tick data into the standard tick array format. Handles both legacy and utility-based
     * approaches.
     *
     * @param instances                the perfmon instance names
     * @param systemList               system (privileged) time ticks
     * @param userList                 user time ticks
     * @param irqList                  IRQ time ticks
     * @param softIrqList              soft IRQ (DPC) time ticks
     * @param idleList                 idle time ticks
     * @param baseList                 timestamp base (for utility mode), may be null
     * @param systemUtility            system utility ticks, may be null
     * @param processorUtility         processor utility ticks, may be null
     * @param processorUtilityBase     processor utility base, may be null
     * @param initSystemList           initial system ticks (for utility mode), may be null
     * @param initUserList             initial user ticks (for utility mode), may be null
     * @param initBase                 initial base (for utility mode), may be null
     * @param initSystemUtility        initial system utility, may be null
     * @param initProcessorUtility     initial processor utility, may be null
     * @param initProcessorUtilityBase initial processor utility base, may be null
     * @return the processed tick array
     */
    protected long[][] processTickData(List<String> instances, @Nullable List<Long> systemList,
            @Nullable List<Long> userList, @Nullable List<Long> irqList, @Nullable List<Long> softIrqList,
            @Nullable List<Long> idleList, @Nullable List<Long> baseList, @Nullable List<Long> systemUtility,
            @Nullable List<Long> processorUtility, @Nullable List<Long> processorUtilityBase,
            @Nullable List<Long> initSystemList, @Nullable List<Long> initUserList, @Nullable List<Long> initBase,
            @Nullable List<Long> initSystemUtility, @Nullable List<Long> initProcessorUtility,
            @Nullable List<Long> initProcessorUtilityBase) {

        int ncpu = getLogicalProcessorCount();
        long[][] ticks = new long[ncpu][TickType.values().length];
        if (instances.isEmpty() || systemList == null || userList == null || irqList == null || softIrqList == null
                || idleList == null) {
            return ticks;
        }
        int size = instances.size();
        if (systemList.size() < size || userList.size() < size || irqList.size() < size || softIrqList.size() < size
                || idleList.size() < size) {
            return ticks;
        }
        // The utility counters only refine the ticks read above. If any of them is missing or short, report the
        // raw ticks rather than nothing; previously an absent utility counter discarded the whole sample.
        boolean utilityAvailable = USE_CPU_UTILITY
                && hasAll(size, baseList, systemUtility, processorUtility, processorUtilityBase, initSystemList,
                        initUserList, initBase, initSystemUtility, initProcessorUtility, initProcessorUtilityBase);
        for (int i = 0; i < instances.size(); i++) {
            String instance = instances.get(i);
            int cpu;
            if (instance.contains(",")) {
                if (!getNumaNodeProcToLogicalProcMap().containsKey(instance)) {
                    continue;
                }
                cpu = getNumaNodeProcToLogicalProcMap().get(instance);
            } else {
                cpu = ParseUtil.parseIntOrDefault(instance, -1);
            }
            if (cpu < 0 || cpu >= ncpu) {
                continue;
            }
            ticks[cpu][TickType.SYSTEM.getIndex()] = systemList.get(i);
            ticks[cpu][TickType.USER.getIndex()] = userList.get(i);
            ticks[cpu][TickType.IRQ.getIndex()] = irqList.get(i);
            ticks[cpu][TickType.SOFTIRQ.getIndex()] = softIrqList.get(i);
            ticks[cpu][TickType.IDLE.getIndex()] = idleList.get(i);

            if (utilityAvailable && baseList != null && initBase != null && processorUtilityBase != null
                    && initProcessorUtilityBase != null && processorUtility != null && initProcessorUtility != null
                    && systemUtility != null && initSystemUtility != null && initUserList != null
                    && initSystemList != null) {
                long deltaT = baseList.get(i) - initBase.get(i);
                if (deltaT > 0) {
                    long deltaBase = processorUtilityBase.get(i) - initProcessorUtilityBase.get(i);
                    long multiplier = lazilyCalculateMultiplier(deltaBase, deltaT);
                    if (multiplier > 0) {
                        long deltaProc = processorUtility.get(i) - initProcessorUtility.get(i);
                        long deltaSys = systemUtility.get(i) - initSystemUtility.get(i);
                        long newUser = initUserList.get(i) + multiplier * (deltaProc - deltaSys) / 100;
                        long newSystem = initSystemList.get(i) + multiplier * deltaSys / 100;
                        long delta = newUser - ticks[cpu][TickType.USER.getIndex()];
                        ticks[cpu][TickType.USER.getIndex()] = newUser;
                        delta += newSystem - ticks[cpu][TickType.SYSTEM.getIndex()];
                        ticks[cpu][TickType.SYSTEM.getIndex()] = newSystem;
                        ticks[cpu][TickType.IDLE.getIndex()] -= delta;
                    }
                }
            }

            // Decrement IRQ from system to avoid double counting in the total array
            ticks[cpu][TickType.SYSTEM.getIndex()] -= ticks[cpu][TickType.IRQ.getIndex()]
                    + ticks[cpu][TickType.SOFTIRQ.getIndex()];

            // Raw value is cumulative 100NS-ticks
            // Divide by 10_000 to get milliseconds
            ticks[cpu][TickType.SYSTEM.getIndex()] /= 10_000L;
            ticks[cpu][TickType.USER.getIndex()] /= 10_000L;
            ticks[cpu][TickType.IRQ.getIndex()] /= 10_000L;
            ticks[cpu][TickType.SOFTIRQ.getIndex()] /= 10_000L;
            ticks[cpu][TickType.IDLE.getIndex()] /= 10_000L;
        }
        return ticks;
    }

    /**
     * Reports whether every one of the given counter lists was read and holds at least {@code size} values.
     *
     * @param size  the number of processor instances to be indexed
     * @param lists the counter lists to check
     * @return true if all are present and long enough
     */
    // Only iterates the array and reads from it; nothing is stored into it and it does not escape, so the
    // generic varargs array cannot be polluted.
    @SafeVarargs
    private static boolean hasAll(int size, @Nullable List<Long>... lists) {
        for (List<Long> list : lists) {
            if (list == null || list.size() < size) {
                return false;
            }
        }
        return true;
    }

    /**
     * Every processor feature {@code IsProcessorFeaturePresent()} accepts, mirroring the {@code PF_} defines in
     * {@code winnt.h}. Windows returns false for a feature its build does not know, so the whole set can be queried on
     * any version.
     */
    private enum ProcessorFeature {
        PF_FLOATING_POINT_PRECISION_ERRATA(0), PF_FLOATING_POINT_EMULATED(1), PF_COMPARE_EXCHANGE_DOUBLE(2),
        PF_MMX_INSTRUCTIONS_AVAILABLE(3), PF_PPC_MOVEMEM_64BIT_OK(4), PF_ALPHA_BYTE_INSTRUCTIONS(5),
        PF_XMMI_INSTRUCTIONS_AVAILABLE(6), PF_3DNOW_INSTRUCTIONS_AVAILABLE(7), PF_RDTSC_INSTRUCTION_AVAILABLE(8),
        PF_PAE_ENABLED(9), PF_XMMI64_INSTRUCTIONS_AVAILABLE(10), PF_SSE_DAZ_MODE_AVAILABLE(11), PF_NX_ENABLED(12),
        PF_SSE3_INSTRUCTIONS_AVAILABLE(13), PF_COMPARE_EXCHANGE128(14), PF_COMPARE64_EXCHANGE128(15),
        PF_CHANNELS_ENABLED(16), PF_XSAVE_ENABLED(17), PF_ARM_VFP_32_REGISTERS_AVAILABLE(18),
        PF_ARM_NEON_INSTRUCTIONS_AVAILABLE(19), PF_SECOND_LEVEL_ADDRESS_TRANSLATION(20), PF_VIRT_FIRMWARE_ENABLED(21),
        PF_RDWRFSGSBASE_AVAILABLE(22), PF_FASTFAIL_AVAILABLE(23), PF_ARM_DIVIDE_INSTRUCTION_AVAILABLE(24),
        PF_ARM_64BIT_LOADSTORE_ATOMIC(25), PF_ARM_EXTERNAL_CACHE_AVAILABLE(26), PF_ARM_FMAC_INSTRUCTIONS_AVAILABLE(27),
        PF_RDRAND_INSTRUCTION_AVAILABLE(28), PF_ARM_V8_INSTRUCTIONS_AVAILABLE(29),
        PF_ARM_V8_CRYPTO_INSTRUCTIONS_AVAILABLE(30), PF_ARM_V8_CRC32_INSTRUCTIONS_AVAILABLE(31),
        PF_RDTSCP_INSTRUCTION_AVAILABLE(32), PF_RDPID_INSTRUCTION_AVAILABLE(33),
        PF_ARM_V81_ATOMIC_INSTRUCTIONS_AVAILABLE(34), PF_MONITORX_INSTRUCTION_AVAILABLE(35),
        PF_SSSE3_INSTRUCTIONS_AVAILABLE(36), PF_SSE4_1_INSTRUCTIONS_AVAILABLE(37), PF_SSE4_2_INSTRUCTIONS_AVAILABLE(38),
        PF_AVX_INSTRUCTIONS_AVAILABLE(39), PF_AVX2_INSTRUCTIONS_AVAILABLE(40), PF_AVX512F_INSTRUCTIONS_AVAILABLE(41),
        PF_ERMS_AVAILABLE(42), PF_ARM_V82_DP_INSTRUCTIONS_AVAILABLE(43), PF_ARM_V83_JSCVT_INSTRUCTIONS_AVAILABLE(44),
        PF_ARM_V83_LRCPC_INSTRUCTIONS_AVAILABLE(45), PF_ARM_SVE_INSTRUCTIONS_AVAILABLE(46),
        PF_ARM_SVE2_INSTRUCTIONS_AVAILABLE(47), PF_ARM_SVE2_1_INSTRUCTIONS_AVAILABLE(48),
        PF_ARM_SVE_AES_INSTRUCTIONS_AVAILABLE(49), PF_ARM_SVE_PMULL128_INSTRUCTIONS_AVAILABLE(50),
        PF_ARM_SVE_BITPERM_INSTRUCTIONS_AVAILABLE(51), PF_ARM_SVE_BF16_INSTRUCTIONS_AVAILABLE(52),
        PF_ARM_SVE_EBF16_INSTRUCTIONS_AVAILABLE(53), PF_ARM_SVE_B16B16_INSTRUCTIONS_AVAILABLE(54),
        PF_ARM_SVE_SHA3_INSTRUCTIONS_AVAILABLE(55), PF_ARM_SVE_SM4_INSTRUCTIONS_AVAILABLE(56),
        PF_ARM_SVE_I8MM_INSTRUCTIONS_AVAILABLE(57), PF_ARM_SVE_F32MM_INSTRUCTIONS_AVAILABLE(58),
        PF_ARM_SVE_F64MM_INSTRUCTIONS_AVAILABLE(59), PF_BMI2_INSTRUCTIONS_AVAILABLE(60),
        PF_MOVDIR64B_INSTRUCTION_AVAILABLE(61), PF_ARM_LSE2_AVAILABLE(62), PF_RESERVED_FEATURE(63),
        PF_ARM_SHA3_INSTRUCTIONS_AVAILABLE(64), PF_ARM_SHA512_INSTRUCTIONS_AVAILABLE(65),
        PF_ARM_V82_I8MM_INSTRUCTIONS_AVAILABLE(66), PF_ARM_V82_FP16_INSTRUCTIONS_AVAILABLE(67),
        PF_ARM_V86_BF16_INSTRUCTIONS_AVAILABLE(68), PF_ARM_V86_EBF16_INSTRUCTIONS_AVAILABLE(69),
        PF_ARM_SME_INSTRUCTIONS_AVAILABLE(70), PF_ARM_SME2_INSTRUCTIONS_AVAILABLE(71),
        PF_ARM_SME2_1_INSTRUCTIONS_AVAILABLE(72), PF_ARM_SME2_2_INSTRUCTIONS_AVAILABLE(73),
        PF_ARM_SME_AES_INSTRUCTIONS_AVAILABLE(74), PF_ARM_SME_SBITPERM_INSTRUCTIONS_AVAILABLE(75),
        PF_ARM_SME_SF8MM4_INSTRUCTIONS_AVAILABLE(76), PF_ARM_SME_SF8MM8_INSTRUCTIONS_AVAILABLE(77),
        PF_ARM_SME_SF8DP2_INSTRUCTIONS_AVAILABLE(78), PF_ARM_SME_SF8DP4_INSTRUCTIONS_AVAILABLE(79),
        PF_ARM_SME_SF8FMA_INSTRUCTIONS_AVAILABLE(80), PF_ARM_SME_F8F32_INSTRUCTIONS_AVAILABLE(81),
        PF_ARM_SME_F8F16_INSTRUCTIONS_AVAILABLE(82), PF_ARM_SME_F16F16_INSTRUCTIONS_AVAILABLE(83),
        PF_ARM_SME_B16B16_INSTRUCTIONS_AVAILABLE(84), PF_ARM_SME_F64F64_INSTRUCTIONS_AVAILABLE(85),
        PF_ARM_SME_I16I64_INSTRUCTIONS_AVAILABLE(86), PF_ARM_SME_LUTv2_INSTRUCTIONS_AVAILABLE(87),
        PF_ARM_SME_FA64_INSTRUCTIONS_AVAILABLE(88), PF_UMONITOR_INSTRUCTION_AVAILABLE(89);

        private final int value;

        ProcessorFeature(int value) {
            this.value = value;
        }
    }

    /**
     * Collects the processor features reported as present.
     *
     * @param featurePresent Tests one feature value, normally the platform's {@code IsProcessorFeaturePresent()}
     * @return The names of the features present, in ascending feature order
     */
    protected static List<String> queryFeatureFlags(IntPredicate featurePresent) {
        List<String> featureFlags = new ArrayList<>();
        for (ProcessorFeature feature : ProcessorFeature.values()) {
            if (featurePresent.test(feature.value)) {
                featureFlags.add(feature.name());
            }
        }
        return featureFlags;
    }
}
