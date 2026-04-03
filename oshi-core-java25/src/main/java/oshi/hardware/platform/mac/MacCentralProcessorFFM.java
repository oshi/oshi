/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystem;
import oshi.ffm.mac.MacSystemFunctions;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.IOKitUtilFFM;
import oshi.util.platform.mac.SysctlUtilFFM;
import oshi.util.tuples.Quartet;

/**
 * A CPU.
 */
@ThreadSafe
final class MacCentralProcessorFFM extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessorFFM.class);

    private static final Set<String> ARM_P_CORES = Stream
            .of("apple,firestorm arm,v8", "apple,avalanche arm,v8", "apple,everest arm,v8").collect(Collectors.toSet());

    private static final int ARM_CPUTYPE = 0x0100000C;
    private static final int M1_CPUFAMILY = 0x1b588bb3;
    private static final int M2_CPUFAMILY = 0xda33d83d;
    private static final int M3_CPUFAMILY = 0x8765edea;
    private static final long DEFAULT_FREQUENCY = 2_400_000_000L;
    private static final Pattern CPU_N = Pattern.compile("^cpu(\\d+)");

    private final Supplier<String> vendor = memoize(MacCentralProcessorFFM::platformExpert);
    private final boolean isArmCpu = isArmCpu();

    // Equivalents of hw.cpufrequency on Apple Silicon, defaulting to Rosetta value
    // Will update during initialization
    private long performanceCoreFrequency = DEFAULT_FREQUENCY;
    private long efficiencyCoreFrequency = DEFAULT_FREQUENCY;

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuName = SysctlUtilFFM.sysctl("machdep.cpu.brand_string", "");
        String cpuVendor;
        String cpuStepping;
        String cpuModel;
        String cpuFamily;
        String processorID;
        // Initial M1 chips said "Apple Processor". Later branding includes M1, M1 Pro,
        // M1 Max, M2, etc. So if it starts with Apple it's M-something.
        if (cpuName.startsWith("Apple")) {
            // Processing an M1 chip
            cpuVendor = vendor.get();
            cpuStepping = "0"; // No correlation yet
            cpuModel = "0"; // No correlation yet
            int type;
            int family;
            if (isArmCpu) {
                type = ARM_CPUTYPE;
                int mSeries = ParseUtil.getFirstIntValue(cpuName);
                switch (mSeries) {
                    case 2:
                        family = M2_CPUFAMILY;
                        break;
                    case 3:
                        family = M3_CPUFAMILY;
                        break;
                    default:
                        // Some M1 did not brand as such
                        family = M1_CPUFAMILY;
                }
            } else {
                type = SysctlUtilFFM.sysctl("hw.cputype", 0);
                family = SysctlUtilFFM.sysctl("hw.cpufamily", 0);
            }
            // Translate to output
            cpuFamily = String.format(Locale.ROOT, "0x%08x", family);
            // Processor ID is an intel concept but CPU type + family conveys same info
            processorID = String.format(Locale.ROOT, "%08x%08x", type, family);
        } else {
            // Processing an Intel chip
            cpuVendor = SysctlUtilFFM.sysctl("machdep.cpu.vendor", "");
            int i = SysctlUtilFFM.sysctl("machdep.cpu.stepping", -1);
            cpuStepping = i < 0 ? "" : Integer.toString(i);
            i = SysctlUtilFFM.sysctl("machdep.cpu.model", -1);
            cpuModel = i < 0 ? "" : Integer.toString(i);
            i = SysctlUtilFFM.sysctl("machdep.cpu.family", -1);
            cpuFamily = i < 0 ? "" : Integer.toString(i);
            long processorIdBits = 0L;
            processorIdBits |= SysctlUtilFFM.sysctl("machdep.cpu.signature", 0);
            processorIdBits |= (SysctlUtilFFM.sysctl("machdep.cpu.feature_bits", 0L) & 0xffffffff) << 32;
            processorID = String.format(Locale.ROOT, "%016x", processorIdBits);
        }
        if (isArmCpu) {
            calculateNominalFrequencies();
        }
        long cpuFreq = isArmCpu ? performanceCoreFrequency : SysctlUtilFFM.sysctl("hw.cpufrequency", 0L);
        boolean cpu64bit = SysctlUtilFFM.sysctl("hw.cpu64bit_capable", 0) != 0;

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        int logicalProcessorCount = SysctlUtilFFM.sysctl("hw.logicalcpu", 1);
        int physicalProcessorCount = SysctlUtilFFM.sysctl("hw.physicalcpu", 1);
        int physicalPackageCount = SysctlUtilFFM.sysctl("hw.packages", 1);
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        Set<Integer> pkgCoreKeys = new HashSet<>();
        for (int i = 0; i < logicalProcessorCount; i++) {
            int coreId = i * physicalProcessorCount / logicalProcessorCount;
            int pkgId = i * physicalPackageCount / logicalProcessorCount;
            logProcs.add(new LogicalProcessor(i, coreId, pkgId));
            pkgCoreKeys.add((pkgId << 16) + coreId);
        }
        Map<Integer, String> compatMap = queryCompatibleStrings();
        int perflevels = SysctlUtilFFM.sysctl("hw.nperflevels", 1, false);
        List<PhysicalProcessor> physProcs = pkgCoreKeys.stream().sorted().map(k -> {
            String compat = compatMap.getOrDefault(k, "").toLowerCase(Locale.ROOT);
            // This is brittle. A better long term solution is to use sysctls
            // hw.perflevel1.physicalcpu: 2
            // hw.perflevel0.physicalcpu: 8
            // Note the 1 and 0 values are reversed from OSHI API definition
            int efficiency = ARM_P_CORES.contains(compat) ? 1 : 0;
            return new PhysicalProcessor(k >> 16, k & 0xffff, efficiency, compat);
        }).collect(Collectors.toList());
        List<ProcessorCache> caches = orderedProcCaches(getCacheValues(perflevels));
        List<String> featureFlags = getFeatureFlagsFromSysctl();
        return new Quartet<>(logProcs, physProcs, caches, featureFlags);
    }

    private Set<ProcessorCache> getCacheValues(int perflevels) {
        int linesize = (int) SysctlUtilFFM.sysctl("hw.cachelinesize", 0L);
        int l1associativity = SysctlUtilFFM.sysctl("machdep.cpu.cache.L1_associativity", 0, false);
        int l2associativity = SysctlUtilFFM.sysctl("machdep.cpu.cache.L2_associativity", 0, false);
        Set<ProcessorCache> caches = new HashSet<>();
        for (int i = 0; i < perflevels; i++) {
            int size = SysctlUtilFFM.sysctl("hw.perflevel" + i + ".l1icachesize", 0, false);
            if (size > 0) {
                caches.add(new ProcessorCache(1, l1associativity, linesize, size, Type.INSTRUCTION));
            }
            size = SysctlUtilFFM.sysctl("hw.perflevel" + i + ".l1dcachesize", 0, false);
            if (size > 0) {
                caches.add(new ProcessorCache(1, l1associativity, linesize, size, Type.DATA));
            }
            size = SysctlUtilFFM.sysctl("hw.perflevel" + i + ".l2cachesize", 0, false);
            if (size > 0) {
                caches.add(new ProcessorCache(2, l2associativity, linesize, size, Type.UNIFIED));
            }
            size = SysctlUtilFFM.sysctl("hw.perflevel" + i + ".l3cachesize", 0, false);
            if (size > 0) {
                caches.add(new ProcessorCache(3, 0, linesize, size, Type.UNIFIED));
            }
        }
        return caches;
    }

    private List<String> getFeatureFlagsFromSysctl() {
        List<String> x86Features = Stream.of("features", "extfeatures", "leaf7_features").map(f -> {
            String key = "machdep.cpu." + f;
            String features = SysctlUtilFFM.sysctl(key, "", false);
            return Util.isBlank(features) ? null : (key + ": " + features);
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return x86Features.isEmpty() ? ExecutingCommand.runNative("sysctl -a hw.optional") : x86Features;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            MemorySegment cpuLoadInfo = arena.allocate(MacSystem.HOST_CPU_LOAD_INFO_DATA);
            MemorySegment count = arena.allocateFrom(ValueLayout.JAVA_INT,
                    (int) (MacSystem.HOST_CPU_LOAD_INFO_DATA.byteSize() / MacSystem.INT_SIZE));
            int machPort = MacSystemFunctions.mach_host_self();
            if (0 != MacSystemFunctions.host_statistics(callState, machPort, MacSystem.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                    count)) {
                LOG.error("Failed to get System CPU ticks. Error code: {} ", getErrno(callState));
                return ticks;
            }
            var cpuTicksHandle = MacSystem.HOST_CPU_LOAD_INFO_DATA.varHandle(MacSystem.CPU_TICKS,
                    MemoryLayout.PathElement.sequenceElement());
            ticks[TickType.USER.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_USER));
            ticks[TickType.NICE.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_NICE));
            ticks[TickType.SYSTEM.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_SYSTEM));
            ticks[TickType.IDLE.getIndex()] = Integer
                    .toUnsignedLong((int) cpuTicksHandle.get(cpuLoadInfo, 0L, (long) MacSystem.CPU_STATE_IDLE));
        } catch (Throwable e) {
            LOG.error("Failed to get System CPU ticks", e);
        }
        // Leave IOWait and IRQ values as 0
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        if (isArmCpu) {
            Map<Integer, Long> physFreqMap = getPhysicalProcessors().stream()
                    .collect(Collectors.toMap(PhysicalProcessor::getPhysicalProcessorNumber,
                            p -> p.getEfficiency() > 0 ? performanceCoreFrequency : efficiencyCoreFrequency));
            return getLogicalProcessors().stream().map(LogicalProcessor::getPhysicalProcessorNumber)
                    .map(p -> physFreqMap.getOrDefault(p, performanceCoreFrequency)).mapToLong(f -> f).toArray();
        }
        return new long[] { getProcessorIdentifier().getVendorFreq() };
    }

    @Override
    public long queryMaxFreq() {
        if (isArmCpu) {
            return performanceCoreFrequency;
        }
        return SysctlUtilFFM.sysctl("hw.cpufrequency_max", getProcessorIdentifier().getVendorFreq());
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment loadavgSeg = arena.allocate(ValueLayout.JAVA_DOUBLE, nelem);
            int retval = MacSystemFunctions.getloadavg(loadavgSeg, nelem);
            if (retval < nelem) {
                Arrays.fill(average, -1d);
            } else {
                for (int i = 0; i < nelem; i++) {
                    average[i] = loadavgSeg.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
                }
            }
        } catch (Throwable e) {
            Arrays.fill(average, -1d);
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            MemorySegment procCountSeg = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment procInfoPtrSeg = arena.allocate(ValueLayout.ADDRESS);
            MemorySegment procInfoCountSeg = arena.allocate(ValueLayout.JAVA_INT);
            int machPort = MacSystemFunctions.mach_host_self();
            if (0 != MacSystemFunctions.host_processor_info(callState, machPort, MacSystem.PROCESSOR_CPU_LOAD_INFO,
                    procCountSeg, procInfoPtrSeg, procInfoCountSeg)) {
                LOG.error("Failed to update CPU Load. Error code: {}", getErrno(callState));
                return ticks;
            }
            int procCount = procCountSeg.get(ValueLayout.JAVA_INT, 0);
            int procInfoCount = procInfoCountSeg.get(ValueLayout.JAVA_INT, 0);
            MemorySegment rawProcInfoPtr = procInfoPtrSeg.get(ValueLayout.ADDRESS, 0);
            MemorySegment procInfoPtr = rawProcInfoPtr.reinterpret((long) procInfoCount * MacSystem.INT_SIZE);
            try {
                if (procCount != ticks.length) {
                    LOG.warn("host_processor_info returned {} CPUs but expected {}; capping iteration", procCount,
                            ticks.length);
                }
                int cpuLimit = Math.min(procCount, ticks.length);
                for (int cpu = 0; cpu < cpuLimit; cpu++) {
                    int offset = cpu * MacSystem.CPU_STATE_MAX;
                    ticks[cpu][TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, offset + MacSystem.CPU_STATE_USER));
                    ticks[cpu][TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, offset + MacSystem.CPU_STATE_NICE));
                    ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, offset + MacSystem.CPU_STATE_SYSTEM));
                    ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(
                            procInfoPtr.getAtIndex(ValueLayout.JAVA_INT, offset + MacSystem.CPU_STATE_IDLE));
                }
            } finally {
                try {
                    MacSystemFunctions.vm_deallocate(MacSystemFunctions.mach_task_self(), rawProcInfoPtr.address(),
                            (long) procInfoCount * MacSystem.INT_SIZE);
                } catch (Throwable e) {
                    LOG.warn("Failed to vm_deallocate processor info buffer", e);
                }
            }
        } catch (Throwable e) {
            LOG.error("Failed to update CPU Load", e);
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        // Not available on macOS since at least 10.3.9. Early versions may have
        // provided access to the vmmeter structure using sysctl [CTL_VM, VM_METER] but
        // it now fails (ENOENT) and there is no other reference to it in source code
        return 0L;
    }

    @Override
    public long queryInterrupts() {
        // Not available on macOS since at least 10.3.9. Early versions may have
        // provided access to the vmmeter structure using sysctl [CTL_VM, VM_METER] but
        // it now fails (ENOENT) and there is no other reference to it in source code
        return 0L;
    }

    private static String platformExpert() {
        String manufacturer = null;
        IORegistryEntry platformExpert = IOKitUtilFFM.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            try {
                byte[] data = platformExpert.getByteArrayProperty("manufacturer");
                if (data != null) {
                    manufacturer = new String(data, StandardCharsets.UTF_8).replace("\0", "").trim();
                }
            } finally {
                platformExpert.release();
            }
        }
        return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
    }

    // Called by initProcessorCount in the constructor
    // These populate the physical processor id strings
    private static Map<Integer, String> queryCompatibleStrings() {
        Map<Integer, String> compatibleStrMap = new HashMap<>();
        // All CPUs are an IOPlatformDevice
        // Iterate each CPU and save frequency and "compatible" strings
        IOIterator iter = IOKitUtilFFM.getMatchingServices("IOPlatformDevice");
        if (iter != null) {
            try {
                IORegistryEntry cpu = iter.next();
                while (cpu != null) {
                    try {
                        String name = cpu.getName();
                        if (name != null) {
                            Matcher m = CPU_N.matcher(name.toLowerCase(Locale.ROOT));
                            if (m.matches()) {
                                int procId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                                // Compatible key is null-delimited C string array in byte array
                                byte[] data = cpu.getByteArrayProperty("compatible");
                                if (data != null) {
                                    // Byte array is null delimited
                                    // Example value for M2: "apple,blizzard", "ARM,v8"
                                    compatibleStrMap.put(procId,
                                            new String(data, StandardCharsets.UTF_8).replace('\0', ' ').trim());
                                }
                            }
                        }
                    } finally {
                        cpu.release();
                    }
                    cpu = iter.next();
                }
            } finally {
                iter.release();
            }
        }
        return compatibleStrMap;
    }

    // Called when initiating instance variables which occurs after constructor has
    // populated physical processors
    private boolean isArmCpu() {
        return getPhysicalProcessors().stream().map(PhysicalProcessor::getIdString).anyMatch(id -> id.contains("arm"));
    }

    private void calculateNominalFrequencies() {
        IOIterator iter = IOKitUtilFFM.getMatchingServices("AppleARMIODevice");
        if (iter != null) {
            try {
                IORegistryEntry device = iter.next();
                try {
                    while (device != null) {
                        if ("pmgr".equalsIgnoreCase(device.getName())) {
                            performanceCoreFrequency = getMaxFreqFromByteArray(
                                    device.getByteArrayProperty("voltage-states5-sram"));
                            efficiencyCoreFrequency = getMaxFreqFromByteArray(
                                    device.getByteArrayProperty("voltage-states1-sram"));
                            return;
                        }
                        device.release();
                        device = iter.next();
                    }
                } finally {
                    if (device != null) {
                        device.release();
                    }
                }
            } finally {
                iter.release();
            }
        }
    }

    private long getMaxFreqFromByteArray(byte[] data) {
        // Max freq is 8 bytes from the end of the array
        if (data != null && data.length >= 8) {
            byte[] freqData = Arrays.copyOfRange(data, data.length - 8, data.length - 4);
            return ParseUtil.byteArrayToLong(freqData, 4, false);
        }
        return DEFAULT_FREQUENCY;
    }
}
