/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.PowrProf.POWER_INFORMATION_LEVEL;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.SystemTickCountProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.windows.LogicalProcessorInformation;
import oshi.driver.windows.perfmon.LoadAverageJNA;
import oshi.driver.windows.perfmon.ProcessorInformationJNA;
import oshi.driver.windows.perfmon.SystemInformationJNA;
import oshi.driver.windows.wmi.Win32ProcessorJNA;
import oshi.hardware.common.platform.windows.WindowsCentralProcessor;
import oshi.jna.Struct.CloseableSystemInfo;
import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Kernel32.ProcessorFeature;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.ProcessorPowerInformation;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

/**
 * A CPU, representing all of a system's processors. It may contain multiple individual Physical and Logical processors.
 */
@ThreadSafe
final class WindowsCentralProcessorJNA extends WindowsCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessorJNA.class);

    static {
        if (USE_LOAD_AVERAGE) {
            LoadAverageJNA.getInstance().startDaemon();
        }
    }

    // This tick query is memoized to enforce a minimum elapsed time for determining
    // the capacity base multiplier
    private final Supplier<Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>>> processorUtilityCounters = USE_CPU_UTILITY
            ? memoize(WindowsCentralProcessorJNA::queryProcessorUtilityCountersStatic,
                    TimeUnit.MILLISECONDS.toNanos(300L))
            : null;

    {
        // Store the initial query and start the memoizer expiration
        if (USE_CPU_UTILITY) {
            setInitialUtilityCounters(processorUtilityCounters.get().getB());
        }
    }

    /**
     * Initializes Class variables
     */
    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = "";
        String cpuName = "";
        String cpuIdentifier = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long cpuVendorFreq = 0L;
        String processorID;
        boolean cpu64bit = false;

        final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\";
        String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
        if (processorIds.length > 0) {
            String cpuRegistryPath = cpuRegistryRoot + processorIds[0];
            cpuVendor = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "VendorIdentifier");
            cpuName = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "ProcessorNameString");
            cpuIdentifier = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "Identifier");
            try {
                cpuVendorFreq = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "~MHz")
                        * 1_000_000L;
            } catch (Win32Exception e) {
                // Leave as 0, parse the identifier as backup
            }
        }
        if (!cpuIdentifier.isEmpty()) {
            cpuFamily = parseIdentifier(cpuIdentifier, "Family");
            cpuModel = parseIdentifier(cpuIdentifier, "Model");
            cpuStepping = parseIdentifier(cpuIdentifier, "Stepping");
        }
        try (CloseableSystemInfo sysinfo = new CloseableSystemInfo()) {
            Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
            int processorArchitecture = sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue();
            if (processorArchitecture == 9 // PROCESSOR_ARCHITECTURE_AMD64
                    || processorArchitecture == 12 // PROCESSOR_ARCHITECTURE_ARM64
                    || processorArchitecture == 6) { // PROCESSOR_ARCHITECTURE_IA64
                cpu64bit = true;
            }
        }
        WmiResult<ProcessorIdProperty> processorId = Win32ProcessorJNA.queryProcessorId();
        if (processorId.getResultCount() > 0) {
            processorID = WmiUtil.getString(processorId, ProcessorIdProperty.PROCESSORID, 0);
        } else {
            processorID = createProcessorID(cpuStepping, cpuModel, cpuFamily,
                    cpu64bit ? new String[] { "ia64" } : new String[0]);
        }
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuVendorFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> lpi;
        if (VersionHelpers.IsWindows7OrGreater()) {
            lpi = LogicalProcessorInformation.getLogicalProcessorInformationEx();
            buildNumaNodeProcMap(lpi.getA());
        } else {
            lpi = LogicalProcessorInformation.getLogicalProcessorInformation();
        }
        List<String> featureFlags = Arrays.stream(ProcessorFeature.values())
                .filter(f -> Kernel32.INSTANCE.IsProcessorFeaturePresent(f.value())).map(ProcessorFeature::name)
                .collect(Collectors.toList());
        return new Quartet<>(lpi.getA(), lpi.getB(), lpi.getC(), featureFlags);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        if (USE_LEGACY_SYSTEM_COUNTERS) {
            WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
            WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
            WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
            if (!Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
                LOG.error("Failed to update system idle/kernel/user times. Error code: {}", Native.getLastError());
                return ticks;
            }
            // IOwait:
            // Windows does not measure IOWait.

            // IRQ and ticks:
            // Percent time raw value is cumulative 100NS-ticks
            // Divide by 10_000 to get milliseconds
            Map<SystemTickCountProperty, Long> valueMap = ProcessorInformationJNA.querySystemCounters();
            ticks[TickType.IRQ.getIndex()] = valueMap.getOrDefault(SystemTickCountProperty.PERCENTINTERRUPTTIME, 0L)
                    / 10_000L;
            ticks[TickType.SOFTIRQ.getIndex()] = valueMap.getOrDefault(SystemTickCountProperty.PERCENTDPCTIME, 0L)
                    / 10_000L;

            ticks[TickType.IDLE.getIndex()] = lpIdleTime.toDWordLong().longValue() / 10_000L;
            ticks[TickType.SYSTEM.getIndex()] = lpKernelTime.toDWordLong().longValue() / 10_000L
                    - ticks[TickType.IDLE.getIndex()];
            ticks[TickType.USER.getIndex()] = lpUserTime.toDWordLong().longValue() / 10_000L;
            // Additional decrement to avoid double counting in the total array
            ticks[TickType.SYSTEM.getIndex()] -= ticks[TickType.IRQ.getIndex()] + ticks[TickType.SOFTIRQ.getIndex()];
            return ticks;
        }
        // To get load in processor group scenario, we need perfmon counters, but the
        // _Total instance is an average rather than total (scaled) number of ticks
        // which matches GetSystemTimes() results. We can just query the per-processor
        // ticks and add them up. Calling the get() method gains the benefit of
        // synchronizing this output with the memoized result of per-processor ticks as
        // well.
        // Sum processor ticks
        long[][] procTicks = getProcessorCpuLoadTicks();
        for (int i = 0; i < ticks.length; i++) {
            for (long[] procTick : procTicks) {
                ticks[i] += procTick[i];
            }
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        if (VersionHelpers.IsWindows7OrGreater()) {
            Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> instanceValuePair = ProcessorInformationJNA
                    .queryFrequencyCounters();
            List<String> instances = instanceValuePair.getA();
            Map<ProcessorFrequencyProperty, List<Long>> valueMap = instanceValuePair.getB();
            List<Long> percentMaxList = valueMap.get(ProcessorFrequencyProperty.PERCENTOFMAXIMUMFREQUENCY);
            if (!instances.isEmpty()) {
                long maxFreq = this.getMaxFreq();
                long[] freqs = new long[getLogicalProcessorCount()];
                for (String instance : instances) {
                    int cpu = instance.contains(",") ? getNumaNodeProcToLogicalProcMap().getOrDefault(instance, 0)
                            : ParseUtil.parseIntOrDefault(instance, 0);
                    if (cpu >= getLogicalProcessorCount()) {
                        continue;
                    }
                    freqs[cpu] = percentMaxList.get(cpu) * maxFreq / 100L;
                }
                return freqs;
            }
        }
        // If <Win7 or anything failed in PDH/WMI, use the native call
        return queryNTPower(2); // Current is field index 2
    }

    @Override
    public long queryMaxFreq() {
        long[] freqs = queryNTPower(1); // Max is field index 1
        return Arrays.stream(freqs).max().orElse(-1L);
    }

    /**
     * Call CallNTPowerInformation for Processor information and return an array of the specified index
     *
     * @param fieldIndex The field, in order as defined in the {@link PowrProf#PROCESSOR_INFORMATION} structure.
     * @return The array of values.
     */
    private long[] queryNTPower(int fieldIndex) {
        ProcessorPowerInformation ppi = new ProcessorPowerInformation();
        ProcessorPowerInformation[] ppiArray = (ProcessorPowerInformation[]) ppi.toArray(getLogicalProcessorCount());
        long[] freqs = new long[getLogicalProcessorCount()];
        if (0 != PowrProf.INSTANCE.CallNtPowerInformation(POWER_INFORMATION_LEVEL.ProcessorInformation, null, 0,
                ppiArray[0].getPointer(), ppi.size() * ppiArray.length)) {
            LOG.error("Unable to get Processor Information");
            Arrays.fill(freqs, -1L);
            return freqs;
        }
        for (int i = 0; i < freqs.length; i++) {
            if (fieldIndex == 1) { // Max
                freqs[i] = ppiArray[i].maxMhz * 1_000_000L;
            } else if (fieldIndex == 2) { // Current
                freqs[i] = ppiArray[i].currentMhz * 1_000_000L;
            } else {
                freqs[i] = -1L;
            }
            // In Win11 23H2 CallNtPowerInformation returns all 0's so use vendor freq
            if (freqs[i] == 0) {
                freqs[i] = getProcessorIdentifier().getVendorFreq();
            }
        }
        return freqs;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        return LoadAverageJNA.getInstance().queryLoadAverage(nelem);
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
            Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> instanceValuePair = ProcessorInformationJNA
                    .queryProcessorCounters();
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

    @Override
    protected Map<ProcessorUtilityTickCountProperty, List<Long>> queryProcessorUtilityCounters() {
        return ProcessorInformationJNA.queryProcessorCapacityCounters().getB();
    }

    private static Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorUtilityCountersStatic() {
        return ProcessorInformationJNA.queryProcessorCapacityCounters();
    }

    @Override
    public long queryContextSwitches() {
        return SystemInformationJNA.queryContextSwitchCounters()
                .getOrDefault(ContextSwitchProperty.CONTEXTSWITCHESPERSEC, 0L);
    }

    @Override
    public long queryInterrupts() {
        return ProcessorInformationJNA.queryInterruptCounters().getOrDefault(InterruptsProperty.INTERRUPTSPERSEC, 0L);
    }
}
