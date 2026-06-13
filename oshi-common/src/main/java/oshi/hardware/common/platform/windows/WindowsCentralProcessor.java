/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
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
    }

    // populated by initProcessorCounts called by the parent constructor
    private Map<String, Integer> numaNodeProcToLogicalProcMap;

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
    private Long utilityBaseMultiplier;

    // This tick query is memoized to enforce a minimum elapsed time for determining the capacity base multiplier
    private final Supplier<Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>>> processorUtilityCounters = USE_CPU_UTILITY
            ? memoize(this::queryProcessorCapacityCounters, TimeUnit.MILLISECONDS.toNanos(300L))
            : null;

    {
        // Store the initial query and start the memoizer expiration
        if (USE_CPU_UTILITY) {
            setInitialUtilityCounters(processorUtilityCounters.get().getB());
        }
    }

    /**
     * Checks whether the OS version is Windows 8 or greater using system property 'os.version'.
     *
     * @return true if Windows 8 or greater
     */
    private static boolean isWindows8OrGreater() {
        // Use system property check that works without native access
        String osVersion = System.getProperty("os.version", "");
        String[] parts = osVersion.split("\\.");
        if (parts.length >= 2) {
            int major = ParseUtil.parseIntOrDefault(parts[0], 0);
            int minor = ParseUtil.parseIntOrDefault(parts[1], 0);
            return major > 6 || (major == 6 && minor >= 2);
        }
        return false;
    }

    /**
     * Gets the numaNodeProcToLogicalProcMap.
     *
     * @return the map
     */
    protected Map<String, Integer> getNumaNodeProcToLogicalProcMap() {
        return this.numaNodeProcToLogicalProcMap;
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
        this.numaNodeProcToLogicalProcMap = new HashMap<>();
        for (LogicalProcessor logProc : logProcs) {
            int node = logProc.getNumaNode();
            int procNum = nextProcIndexByNode.getOrDefault(node, 0);
            numaNodeProcToLogicalProcMap.put(String.format(Locale.ROOT, "%d,%d", node, procNum), lp++);
            nextProcIndexByNode.put(node, procNum + 1);
        }
    }

    /**
     * Parses identifier string
     *
     * @param identifier the full identifier string
     * @param key        the key to retrieve
     * @return the string following id
     */
    protected static String parseIdentifier(String identifier, String key) {
        String[] idSplit = ParseUtil.whitespaces.split(identifier);
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
        if (USE_CPU_UTILITY) {
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
    protected long[][] processTickData(List<String> instances, List<Long> systemList, List<Long> userList,
            List<Long> irqList, List<Long> softIrqList, List<Long> idleList, List<Long> baseList,
            List<Long> systemUtility, List<Long> processorUtility, List<Long> processorUtilityBase,
            List<Long> initSystemList, List<Long> initUserList, List<Long> initBase, List<Long> initSystemUtility,
            List<Long> initProcessorUtility, List<Long> initProcessorUtilityBase) {

        int ncpu = getLogicalProcessorCount();
        long[][] ticks = new long[ncpu][TickType.values().length];
        if (instances.isEmpty() || systemList == null || userList == null || irqList == null || softIrqList == null
                || idleList == null
                || (USE_CPU_UTILITY && (baseList == null || systemUtility == null || processorUtility == null
                        || processorUtilityBase == null || initSystemList == null || initUserList == null
                        || initBase == null || initSystemUtility == null || initProcessorUtility == null
                        || initProcessorUtilityBase == null))) {
            return ticks;
        }
        int size = instances.size();
        if (systemList.size() < size || userList.size() < size || irqList.size() < size || softIrqList.size() < size
                || idleList.size() < size) {
            return ticks;
        }
        if (USE_CPU_UTILITY && (baseList.size() < size || systemUtility.size() < size || processorUtility.size() < size
                || processorUtilityBase.size() < size || initSystemList.size() < size || initUserList.size() < size
                || initBase.size() < size || initSystemUtility.size() < size || initProcessorUtility.size() < size
                || initProcessorUtilityBase.size() < size)) {
            return ticks;
        }
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

            if (USE_CPU_UTILITY) {
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
}
