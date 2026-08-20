/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.ExceptionUtil.getLongOrDefault;
import static oshi.util.ExceptionUtil.getOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorPerformanceProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.SystemTickCountProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.LogicalProcessorInformationFFM;
import oshi.driver.windows.perfmon.LoadAverageFFM;
import oshi.driver.windows.perfmon.ProcessorInformationFFM;
import oshi.driver.windows.perfmon.SystemInformationFFM;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.windows.Advapi32FFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.WinNTFFM;
import oshi.ffm.platform.windows.WinRegFFM;
import oshi.ffm.platform.windows.WindowsForeignFunctions;
import oshi.hardware.common.platform.windows.WindowsCentralProcessor;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

/**
 * FFM-based Windows Central Processor implementation.
 */
// Not final so tests can subclass it to force the useLegacySystemCounters() gate and exercise the legacy GetSystemTimes
// path against the real system.
@ThreadSafe
class WindowsCentralProcessorFFM extends WindowsCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessorFFM.class);

    private static final MemorySegment HKLM = MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE);

    // ProcessorInformation power level for CallNtPowerInformation
    private static final int PROCESSOR_INFORMATION_LEVEL = 11;

    // ProcessorPowerInformation structure size: 6 ints = 24 bytes
    private static final int PPI_SIZE = 24;

    static {
        if (USE_LOAD_AVERAGE) {
            LoadAverageFFM.getInstance().startDaemon();
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
                try (NativeHandle key = NativeHandle.of(hKey.get(ValueLayout.ADDRESS, 0), Advapi32FFM::RegCloseKey)) {
                    // Get first subkey name
                    MemorySegment nameBuffer = arena.allocate(256 * 2L); // WCHAR[256]
                    MemorySegment nameLen = arena.allocate(ValueLayout.JAVA_INT);
                    nameLen.set(ValueLayout.JAVA_INT, 0, 256);
                    if (Advapi32FFM.RegEnumKeyEx(key.get(), 0, nameBuffer, nameLen, MemorySegment.NULL,
                            MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL) == 0) {
                        String firstKey = WindowsForeignFunctions.readWideString(nameBuffer);
                        String cpuRegistryPath = cpuRegistryRoot + firstKey;
                        cpuVendor = registryGetString(arena, cpuRegistryPath, "VendorIdentifier");
                        cpuName = registryGetString(arena, cpuRegistryPath, "ProcessorNameString");
                        cpuIdentifier = registryGetString(arena, cpuRegistryPath, "Identifier");
                        cpuVendorFreq = registryGetDword(arena, cpuRegistryPath, "~MHz") * 1_000_000L;
                    }
                }
            }
        } catch (Throwable t) {
            LOG.debug("Failed to read processor registry info", t);
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
            processorID = WmiUtil.getString(processorIdResult, ProcessorIdProperty.PROCESSORID, 0);
        } else {
            processorID = createProcessorID(cpuStepping, cpuModel, cpuFamily,
                    cpu64bit ? new String[] { "ia64" } : new String[0]);
        }
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuVendorFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>, List<String>> initProcessorCounts() {
        Triplet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>> lpi = LogicalProcessorInformationFFM
                .getLogicalProcessorInformationEx();
        buildNumaNodeProcMap(lpi.getA());

        List<String> featureFlags = queryFeatureFlags(Kernel32FFM::IsProcessorFeaturePresent);
        return new Quartet<>(lpi.getA(), lpi.getB(), lpi.getC(), featureFlags);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        if (useLegacySystemCounters()) {
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
    protected Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> queryProcessorPerformanceCounters() {
        return ProcessorInformationFFM.queryProcessorPerformanceCounters();
    }

    @Override
    protected Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        return ProcessorInformationFFM.queryFrequencyCounters();
    }

    @Override
    protected long[] queryNTPower(int fieldIndex) {
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
                long offset = i * (long) PPI_SIZE;
                // ProcessorPowerInformation: number(4), maxMhz(4), currentMhz(4), mhzLimit(4), maxIdleState(4),
                // currentIdleState(4)
                freqs[i] = switch (fieldIndex) {
                    case 1 -> Integer.toUnsignedLong(buffer.get(ValueLayout.JAVA_INT, offset + 4)) * 1_000_000L;
                    case 2 -> Integer.toUnsignedLong(buffer.get(ValueLayout.JAVA_INT, offset + 8)) * 1_000_000L;
                    default -> -1L;
                };
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
    protected Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
        return ProcessorInformationFFM.queryProcessorCapacityCounters();
    }

    @Override
    protected Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters() {
        return ProcessorInformationFFM.queryProcessorCounters();
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
        return getOrDefault(() -> {
            MemorySegment hKey = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment subKey = WindowsForeignFunctions.toWideString(arena, path);
            if (Advapi32FFM.RegOpenKeyEx(HKLM, subKey, 0, WinNTFFM.KEY_READ, hKey) == 0) {
                try (NativeHandle key = NativeHandle.of(hKey.get(ValueLayout.ADDRESS, 0), Advapi32FFM::RegCloseKey)) {
                    MemorySegment valueNameSeg = WindowsForeignFunctions.toWideString(arena, valueName);
                    MemorySegment dataSize = arena.allocate(ValueLayout.JAVA_INT);
                    dataSize.set(ValueLayout.JAVA_INT, 0, 512);
                    MemorySegment data = arena.allocate(512);
                    if (Advapi32FFM.RegQueryValueEx(key.get(), valueNameSeg, MemorySegment.NULL, data, dataSize) == 0) {
                        return WindowsForeignFunctions.readWideString(data);
                    }
                }
            }
            return "";
        }, "", LOG, "Failed to read registry string {}/{}", path, valueName);
    }

    private static long registryGetDword(Arena arena, String path, String valueName) {
        return getLongOrDefault(() -> {
            MemorySegment hKey = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment subKey = WindowsForeignFunctions.toWideString(arena, path);
            if (Advapi32FFM.RegOpenKeyEx(HKLM, subKey, 0, WinNTFFM.KEY_READ, hKey) == 0) {
                try (NativeHandle key = NativeHandle.of(hKey.get(ValueLayout.ADDRESS, 0), Advapi32FFM::RegCloseKey)) {
                    MemorySegment valueNameSeg = WindowsForeignFunctions.toWideString(arena, valueName);
                    MemorySegment dataSize = arena.allocate(ValueLayout.JAVA_INT);
                    dataSize.set(ValueLayout.JAVA_INT, 0, 4);
                    MemorySegment data = arena.allocate(4);
                    if (Advapi32FFM.RegQueryValueEx(key.get(), valueNameSeg, MemorySegment.NULL, data, dataSize) == 0) {
                        return Integer.toUnsignedLong(data.get(ValueLayout.JAVA_INT, 0));
                    }
                }
            }
            return 0L;
        }, 0L, LOG, "Failed to read registry DWORD {}/{}", path, valueName);
    }
}
