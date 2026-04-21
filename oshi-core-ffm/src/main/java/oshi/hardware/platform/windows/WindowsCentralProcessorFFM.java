/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.SystemTickCountProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.windows.LogicalProcessorInformationFFM;
import oshi.driver.windows.perfmon.LoadAverageFFM;
import oshi.driver.windows.perfmon.ProcessorInformationFFM;
import oshi.driver.windows.perfmon.SystemInformationFFM;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.ffm.windows.WinNTFFM;
import oshi.ffm.windows.WinRegFFM;
import oshi.ffm.windows.WindowsForeignFunctions;
import oshi.hardware.common.platform.windows.WindowsCentralProcessor;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtilFFM;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

/**
 * FFM-based Windows Central Processor implementation.
 */
@ThreadSafe
final class WindowsCentralProcessorFFM extends WindowsCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessorFFM.class);

    private static final MemorySegment HKLM = MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE);

    // ProcessorInformation power level for CallNtPowerInformation
    private static final int PROCESSOR_INFORMATION_LEVEL = 11;

    // ProcessorPowerInformation structure size: 6 ints = 24 bytes
    private static final int PPI_SIZE = 24;

    // ProcessorFeature enum values matching JNA's Kernel32.ProcessorFeature
    private static final int[] PROCESSOR_FEATURES = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 36, 37, 38, 39, 40, 41, 43, 44, 45 };
    private static final String[] PROCESSOR_FEATURE_NAMES = { "PF_FLOATING_POINT_PRECISION_ERRATA",
            "PF_FLOATING_POINT_EMULATED", "PF_COMPARE_EXCHANGE_DOUBLE", "PF_MMX_INSTRUCTIONS_AVAILABLE",
            "PF_PPC_MOVEMEM_64BIT_OK", "PF_ALPHA_BYTE_INSTRUCTIONS", "PF_XMMI_INSTRUCTIONS_AVAILABLE",
            "PF_3DNOW_INSTRUCTIONS_AVAILABLE", "PF_RDTSC_INSTRUCTION_AVAILABLE", "PF_PAE_ENABLED",
            "PF_XMMI64_INSTRUCTIONS_AVAILABLE", "PF_SSE_DAZ_MODE_AVAILABLE", "PF_NX_ENABLED",
            "PF_SSE3_INSTRUCTIONS_AVAILABLE", "PF_COMPARE_EXCHANGE128", "PF_COMPARE64_EXCHANGE128",
            "PF_CHANNELS_ENABLED", "PF_XSAVE_ENABLED", "PF_ARM_VFP_32_REGISTERS_AVAILABLE",
            "PF_ARM_NEON_INSTRUCTIONS_AVAILABLE", "PF_SECOND_LEVEL_ADDRESS_TRANSLATION", "PF_VIRT_FIRMWARE_ENABLED",
            "PF_RDWRFSGSBASE_AVAILABLE", "PF_FASTFAIL_AVAILABLE", "PF_ARM_DIVIDE_INSTRUCTION_AVAILABLE",
            "PF_ARM_64BIT_LOADSTORE_ATOMIC", "PF_ARM_EXTERNAL_CACHE_AVAILABLE", "PF_ARM_FMAC_INSTRUCTIONS_AVAILABLE",
            "PF_RDRAND_INSTRUCTION_AVAILABLE", "PF_ARM_V8_INSTRUCTIONS_AVAILABLE",
            "PF_ARM_V8_CRYPTO_INSTRUCTIONS_AVAILABLE", "PF_ARM_V8_CRC32_INSTRUCTIONS_AVAILABLE",
            "PF_RDTSCP_INSTRUCTION_AVAILABLE", "PF_RDPID_INSTRUCTION_AVAILABLE",
            "PF_ARM_V81_ATOMIC_INSTRUCTIONS_AVAILABLE", "PF_SSSE3_INSTRUCTIONS_AVAILABLE",
            "PF_SSE4_1_INSTRUCTIONS_AVAILABLE", "PF_SSE4_2_INSTRUCTIONS_AVAILABLE", "PF_AVX_INSTRUCTIONS_AVAILABLE",
            "PF_AVX2_INSTRUCTIONS_AVAILABLE", "PF_AVX512F_INSTRUCTIONS_AVAILABLE",
            "PF_ARM_V82_DP_INSTRUCTIONS_AVAILABLE", "PF_ARM_V83_JSCVT_INSTRUCTIONS_AVAILABLE",
            "PF_ARM_V83_LRCPC_INSTRUCTIONS_AVAILABLE" };

    static {
        if (USE_LOAD_AVERAGE) {
            LoadAverageFFM.getInstance().startDaemon();
        }
    }

    // This tick query is memoized to enforce a minimum elapsed time for determining
    // the capacity base multiplier
    private final Supplier<Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>>> processorUtilityCounters = USE_CPU_UTILITY
            ? memoize(ProcessorInformationFFM::queryProcessorCapacityCounters, TimeUnit.MILLISECONDS.toNanos(300L))
            : null;

    {
        if (USE_CPU_UTILITY) {
            setInitialUtilityCounters(processorUtilityCounters.get().getB());
        }
    }

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
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hKey = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment subKey = WindowsForeignFunctions.toWideString(arena, cpuRegistryRoot);
            if (Advapi32FFM.RegOpenKeyEx(HKLM, subKey, 0, WinNTFFM.KEY_READ, hKey) == 0) {
                MemorySegment openedKey = hKey.get(ValueLayout.ADDRESS, 0);
                try {
                    // Get first subkey name
                    MemorySegment nameBuffer = arena.allocate(256 * 2); // WCHAR[256]
                    MemorySegment nameLen = arena.allocate(ValueLayout.JAVA_INT);
                    nameLen.set(ValueLayout.JAVA_INT, 0, 256);
                    if (Advapi32FFM.RegEnumKeyEx(openedKey, 0, nameBuffer, nameLen, MemorySegment.NULL,
                            MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL) == 0) {
                        String firstKey = WindowsForeignFunctions.readWideString(nameBuffer);
                        String cpuRegistryPath = cpuRegistryRoot + firstKey;
                        cpuVendor = registryGetString(arena, cpuRegistryPath, "VendorIdentifier");
                        cpuName = registryGetString(arena, cpuRegistryPath, "ProcessorNameString");
                        cpuIdentifier = registryGetString(arena, cpuRegistryPath, "Identifier");
                        cpuVendorFreq = registryGetDword(arena, cpuRegistryPath, "~MHz") * 1_000_000L;
                    }
                } finally {
                    Advapi32FFM.RegCloseKey(openedKey);
                }
            }
        } catch (Throwable t) {
            LOG.debug("Failed to read processor registry info: {}", t.getMessage());
        }

        if (!cpuIdentifier.isEmpty()) {
            cpuFamily = parseIdentifier(cpuIdentifier, "Family");
            cpuModel = parseIdentifier(cpuIdentifier, "Model");
            cpuStepping = parseIdentifier(cpuIdentifier, "Stepping");
        }

        // GetNativeSystemInfo to determine 64-bit
        try (Arena arena = Arena.ofConfined()) {
            // SYSTEM_INFO is 48 bytes on x64
            MemorySegment sysInfo = arena.allocate(48);
            if (Kernel32FFM.GetNativeSystemInfo(sysInfo)) {
                // wProcessorArchitecture is at offset 0 (WORD in union)
                int arch = Short.toUnsignedInt(sysInfo.get(ValueLayout.JAVA_SHORT, 0));
                if (arch == 9 || arch == 12 || arch == 6) { // AMD64, ARM64, IA64
                    cpu64bit = true;
                }
            }
        }

        var processorIdResult = Win32ProcessorFFM.queryProcessorId();
        if (processorIdResult.getResultCount() > 0) {
            processorID = WmiUtilFFM.getString(processorIdResult, ProcessorIdProperty.PROCESSORID, 0);
        } else {
            processorID = createProcessorID(cpuStepping, cpuModel, cpuFamily,
                    cpu64bit ? new String[] { "ia64" } : new String[0]);
        }
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuVendorFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> lpi = LogicalProcessorInformationFFM
                .getLogicalProcessorInformationEx();
        buildNumaNodeProcMap(lpi.getA());

        List<String> featureFlags = IntStream.range(0, PROCESSOR_FEATURES.length)
                .filter(i -> Kernel32FFM.IsProcessorFeaturePresent(PROCESSOR_FEATURES[i]))
                .mapToObj(i -> PROCESSOR_FEATURE_NAMES[i]).collect(Collectors.toList());
        return new Quartet<>(lpi.getA(), lpi.getB(), lpi.getC(), featureFlags);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        if (USE_LEGACY_SYSTEM_COUNTERS) {
            try (Arena arena = Arena.ofConfined()) {
                // FILETIME is 8 bytes (dwLowDateTime + dwHighDateTime)
                MemorySegment lpIdleTime = arena.allocate(8);
                MemorySegment lpKernelTime = arena.allocate(8);
                MemorySegment lpUserTime = arena.allocate(8);
                if (!Kernel32FFM.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
                    LOG.error("Failed to update system idle/kernel/user times. Error code: {}",
                            Kernel32FFM.GetLastError().orElse(-1));
                    return ticks;
                }

                Map<SystemTickCountProperty, Long> valueMap = ProcessorInformationFFM.querySystemCounters();
                ticks[TickType.IRQ.getIndex()] = valueMap.getOrDefault(SystemTickCountProperty.PERCENTINTERRUPTTIME, 0L)
                        / 10_000L;
                ticks[TickType.SOFTIRQ.getIndex()] = valueMap.getOrDefault(SystemTickCountProperty.PERCENTDPCTIME, 0L)
                        / 10_000L;

                ticks[TickType.IDLE.getIndex()] = lpIdleTime.get(ValueLayout.JAVA_LONG, 0) / 10_000L;
                ticks[TickType.SYSTEM.getIndex()] = lpKernelTime.get(ValueLayout.JAVA_LONG, 0) / 10_000L
                        - ticks[TickType.IDLE.getIndex()];
                ticks[TickType.USER.getIndex()] = lpUserTime.get(ValueLayout.JAVA_LONG, 0) / 10_000L;
                ticks[TickType.SYSTEM.getIndex()] -= ticks[TickType.IRQ.getIndex()]
                        + ticks[TickType.SOFTIRQ.getIndex()];
            }
            return ticks;
        }
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
        if (VersionHelpersFFM.IsWindows7OrGreater()) {
            Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> instanceValuePair = ProcessorInformationFFM
                    .queryFrequencyCounters();
            List<String> instances = instanceValuePair.getA();
            Map<ProcessorFrequencyProperty, List<Long>> valueMap = instanceValuePair.getB();
            List<Long> percentMaxList = valueMap.get(ProcessorFrequencyProperty.PERCENTOFMAXIMUMFREQUENCY);
            if (!instances.isEmpty() && percentMaxList != null) {
                long maxFreq = this.getMaxFreq();
                if (maxFreq > 0) {
                    long[] freqs = new long[getLogicalProcessorCount()];
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
                        if (cpu < 0 || cpu >= getLogicalProcessorCount() || i >= percentMaxList.size()) {
                            continue;
                        }
                        freqs[cpu] = percentMaxList.get(i) * maxFreq / 100L;
                    }
                    return freqs;
                }
            }
        }
        return queryNTPower(2);
    }

    @Override
    public long queryMaxFreq() {
        long[] freqs = queryNTPower(1);
        return Arrays.stream(freqs).max().orElse(-1L);
    }

    private long[] queryNTPower(int fieldIndex) {
        long[] freqs = new long[getLogicalProcessorCount()];
        int totalSize = PPI_SIZE * freqs.length;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(totalSize);
            int status = Kernel32FFM.CallNtPowerInformation(PROCESSOR_INFORMATION_LEVEL, MemorySegment.NULL, 0, buffer,
                    totalSize);
            if (status != 0) {
                LOG.error("Unable to get Processor Information, status: {}", status);
                Arrays.fill(freqs, -1L);
                return freqs;
            }
            for (int i = 0; i < freqs.length; i++) {
                int offset = i * PPI_SIZE;
                // ProcessorPowerInformation: number(4), maxMhz(4), currentMhz(4), mhzLimit(4), maxIdleState(4),
                // currentIdleState(4)
                if (fieldIndex == 1) {
                    freqs[i] = Integer.toUnsignedLong(buffer.get(ValueLayout.JAVA_INT, offset + 4)) * 1_000_000L;
                } else if (fieldIndex == 2) {
                    freqs[i] = Integer.toUnsignedLong(buffer.get(ValueLayout.JAVA_INT, offset + 8)) * 1_000_000L;
                } else {
                    freqs[i] = -1L;
                }
                if (freqs[i] == 0) {
                    freqs[i] = getProcessorIdentifier().getVendorFreq();
                }
            }
        }
        return freqs;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        return LoadAverageFFM.getInstance().queryLoadAverage(nelem);
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        List<String> instances;
        List<Long> systemList;
        List<Long> userList;
        List<Long> irqList;
        List<Long> softIrqList;
        List<Long> idleList;
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
            idleList = valueMap.get(ProcessorUtilityTickCountProperty.PERCENTPROCESSORTIME);
            baseList = valueMap.get(ProcessorUtilityTickCountProperty.TIMESTAMP_SYS100NS);
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
            Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> instanceValuePair = ProcessorInformationFFM
                    .queryProcessorCounters();
            instances = instanceValuePair.getA();
            Map<ProcessorTickCountProperty, List<Long>> valueMap = instanceValuePair.getB();
            systemList = valueMap.get(ProcessorTickCountProperty.PERCENTPRIVILEGEDTIME);
            userList = valueMap.get(ProcessorTickCountProperty.PERCENTUSERTIME);
            irqList = valueMap.get(ProcessorTickCountProperty.PERCENTINTERRUPTTIME);
            softIrqList = valueMap.get(ProcessorTickCountProperty.PERCENTDPCTIME);
            idleList = valueMap.get(ProcessorTickCountProperty.PERCENTPROCESSORTIME);
        }

        return processTickData(instances, systemList, userList, irqList, softIrqList, idleList, baseList, systemUtility,
                processorUtility, processorUtilityBase, initSystemList, initUserList, initBase, initSystemUtility,
                initProcessorUtility, initProcessorUtilityBase);
    }

    @Override
    protected Map<ProcessorUtilityTickCountProperty, List<Long>> queryProcessorUtilityCounters() {
        return ProcessorInformationFFM.queryProcessorCapacityCounters().getB();
    }

    @Override
    public long queryContextSwitches() {
        return SystemInformationFFM.queryContextSwitchCounters()
                .getOrDefault(ContextSwitchProperty.CONTEXTSWITCHESPERSEC, 0L);
    }

    @Override
    public long queryInterrupts() {
        return ProcessorInformationFFM.queryInterruptCounters().getOrDefault(InterruptsProperty.INTERRUPTSPERSEC, 0L);
    }

    private static String registryGetString(Arena arena, String path, String valueName) {
        try {
            MemorySegment hKey = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment subKey = WindowsForeignFunctions.toWideString(arena, path);
            if (Advapi32FFM.RegOpenKeyEx(HKLM, subKey, 0, WinNTFFM.KEY_READ, hKey) == 0) {
                MemorySegment openedKey = hKey.get(ValueLayout.ADDRESS, 0);
                try {
                    MemorySegment valueNameSeg = WindowsForeignFunctions.toWideString(arena, valueName);
                    MemorySegment dataSize = arena.allocate(ValueLayout.JAVA_INT);
                    dataSize.set(ValueLayout.JAVA_INT, 0, 512);
                    MemorySegment data = arena.allocate(512);
                    if (Advapi32FFM.RegQueryValueEx(openedKey, valueNameSeg, 0, MemorySegment.NULL, data,
                            dataSize) == 0) {
                        return WindowsForeignFunctions.readWideString(data);
                    }
                } finally {
                    Advapi32FFM.RegCloseKey(openedKey);
                }
            }
        } catch (Throwable t) {
            LOG.debug("Failed to read registry string {}/{}: {}", path, valueName, t.getMessage());
        }
        return "";
    }

    private static long registryGetDword(Arena arena, String path, String valueName) {
        try {
            MemorySegment hKey = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment subKey = WindowsForeignFunctions.toWideString(arena, path);
            if (Advapi32FFM.RegOpenKeyEx(HKLM, subKey, 0, WinNTFFM.KEY_READ, hKey) == 0) {
                MemorySegment openedKey = hKey.get(ValueLayout.ADDRESS, 0);
                try {
                    MemorySegment valueNameSeg = WindowsForeignFunctions.toWideString(arena, valueName);
                    MemorySegment dataSize = arena.allocate(ValueLayout.JAVA_INT);
                    dataSize.set(ValueLayout.JAVA_INT, 0, 4);
                    MemorySegment data = arena.allocate(4);
                    if (Advapi32FFM.RegQueryValueEx(openedKey, valueNameSeg, 0, MemorySegment.NULL, data,
                            dataSize) == 0) {
                        return Integer.toUnsignedLong(data.get(ValueLayout.JAVA_INT, 0));
                    }
                } finally {
                    Advapi32FFM.RegCloseKey(openedKey);
                }
            }
        } catch (Throwable t) {
            LOG.debug("Failed to read registry DWORD {}/{}: {}", path, valueName, t.getMessage());
        }
        return 0L;
    }
}
