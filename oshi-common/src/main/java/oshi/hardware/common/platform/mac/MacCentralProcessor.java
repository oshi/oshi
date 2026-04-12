/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Base class for macOS CentralProcessor implementations. Subclasses provide platform-specific sysctl, IOKit, and Mach
 * kernel calls.
 */
@ThreadSafe
public abstract class MacCentralProcessor extends AbstractCentralProcessor {

    private static final Set<String> ARM_P_CORES = Stream
            .of("apple,firestorm arm,v8", "apple,avalanche arm,v8", "apple,everest arm,v8").collect(Collectors.toSet());

    protected static final int ARM_CPUTYPE = 0x0100000C;
    protected static final int M1_CPUFAMILY = 0x1b588bb3;
    protected static final int M2_CPUFAMILY = 0xda33d83d;
    protected static final int M3_CPUFAMILY = 0x8765edea;
    protected static final long DEFAULT_FREQUENCY = 2_400_000_000L;

    private final Supplier<String> vendor = memoize(this::platformExpert);
    private final boolean isArmCpu = isArmCpu();

    private long performanceCoreFrequency = DEFAULT_FREQUENCY;
    private long efficiencyCoreFrequency = DEFAULT_FREQUENCY;

    protected abstract int sysctlInt(String name, int defaultValue);

    protected abstract int sysctlIntNoWarn(String name, int defaultValue);

    protected abstract long sysctlLong(String name, long defaultValue);

    protected abstract String sysctlString(String name, String defaultValue);

    protected abstract String sysctlStringNoWarn(String name, String defaultValue);

    protected abstract String platformExpert();

    protected abstract Map<Integer, String> queryCompatibleStrings();

    protected abstract void calculateNominalFrequencies();

    protected long getPerformanceCoreFrequency() {
        return performanceCoreFrequency;
    }

    protected void setPerformanceCoreFrequency(long freq) {
        this.performanceCoreFrequency = freq;
    }

    protected long getEfficiencyCoreFrequency() {
        return efficiencyCoreFrequency;
    }

    protected void setEfficiencyCoreFrequency(long freq) {
        this.efficiencyCoreFrequency = freq;
    }

    protected boolean isArm() {
        return isArmCpu;
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuName = sysctlString("machdep.cpu.brand_string", "");
        String cpuVendor;
        String cpuStepping;
        String cpuModel;
        String cpuFamily;
        String processorID;
        if (cpuName.startsWith("Apple")) {
            cpuVendor = vendor.get();
            cpuStepping = "0";
            cpuModel = "0";
            int type;
            int family;
            if (isArmCpu) {
                type = ARM_CPUTYPE;
                family = sysctlInt("hw.cpufamily", 0);
                if (family == 0) {
                    int mSeries = ParseUtil.getFirstIntValue(cpuName);
                    switch (mSeries) {
                        case 2:
                            family = M2_CPUFAMILY;
                            break;
                        case 3:
                            family = M3_CPUFAMILY;
                            break;
                        default:
                            family = M1_CPUFAMILY;
                    }
                }
            } else {
                type = sysctlInt("hw.cputype", 0);
                family = sysctlInt("hw.cpufamily", 0);
            }
            cpuFamily = String.format(Locale.ROOT, "0x%08x", family);
            processorID = String.format(Locale.ROOT, "%08x%08x", type, family);
        } else {
            cpuVendor = sysctlString("machdep.cpu.vendor", "");
            int i = sysctlInt("machdep.cpu.stepping", -1);
            cpuStepping = i < 0 ? "" : Integer.toString(i);
            i = sysctlInt("machdep.cpu.model", -1);
            cpuModel = i < 0 ? "" : Integer.toString(i);
            i = sysctlInt("machdep.cpu.family", -1);
            cpuFamily = i < 0 ? "" : Integer.toString(i);
            long processorIdBits = 0L;
            processorIdBits |= sysctlInt("machdep.cpu.signature", 0);
            processorIdBits |= (sysctlLong("machdep.cpu.feature_bits", 0L) & 0xffffffff) << 32;
            processorID = String.format(Locale.ROOT, "%016x", processorIdBits);
        }
        if (isArmCpu) {
            calculateNominalFrequencies();
        }
        long cpuFreq = isArmCpu ? performanceCoreFrequency : sysctlLong("hw.cpufrequency", 0L);
        boolean cpu64bit = sysctlInt("hw.cpu64bit_capable", 0) != 0;

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        int logicalProcessorCount = sysctlInt("hw.logicalcpu", 1);
        int physicalProcessorCount = sysctlInt("hw.physicalcpu", 1);
        int physicalPackageCount = sysctlInt("hw.packages", 1);
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        Set<Integer> pkgCoreKeys = new HashSet<>();
        for (int i = 0; i < logicalProcessorCount; i++) {
            int coreId = i * physicalProcessorCount / logicalProcessorCount;
            int pkgId = i * physicalPackageCount / logicalProcessorCount;
            logProcs.add(new LogicalProcessor(i, coreId, pkgId));
            pkgCoreKeys.add((pkgId << 16) + coreId);
        }
        Map<Integer, String> compatMap = queryCompatibleStrings();
        int perflevels = sysctlIntNoWarn("hw.nperflevels", 1);
        List<PhysicalProcessor> physProcs = pkgCoreKeys.stream().sorted().map(k -> {
            String compat = compatMap.getOrDefault(k, "").toLowerCase(Locale.ROOT);
            int efficiency = ARM_P_CORES.contains(compat) ? 1 : 0;
            return new PhysicalProcessor(k >> 16, k & 0xffff, efficiency, compat);
        }).collect(Collectors.toList());
        List<ProcessorCache> caches = orderedProcCaches(getCacheValues(perflevels));
        List<String> featureFlags = getFeatureFlagsFromSysctl();
        return new Quartet<>(logProcs, physProcs, caches, featureFlags);
    }

    private Set<ProcessorCache> getCacheValues(int perflevels) {
        int linesize = (int) sysctlLong("hw.cachelinesize", 0L);
        int l1associativity = sysctlIntNoWarn("machdep.cpu.cache.L1_associativity", 0);
        int l2associativity = sysctlIntNoWarn("machdep.cpu.cache.L2_associativity", 0);
        Set<ProcessorCache> caches = new HashSet<>();
        for (int i = 0; i < perflevels; i++) {
            int size = sysctlIntNoWarn("hw.perflevel" + i + ".l1icachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(1, l1associativity, linesize, size, Type.INSTRUCTION));
            }
            size = sysctlIntNoWarn("hw.perflevel" + i + ".l1dcachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(1, l1associativity, linesize, size, Type.DATA));
            }
            size = sysctlIntNoWarn("hw.perflevel" + i + ".l2cachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(2, l2associativity, linesize, size, Type.UNIFIED));
            }
            size = sysctlIntNoWarn("hw.perflevel" + i + ".l3cachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(3, 0, linesize, size, Type.UNIFIED));
            }
        }
        return caches;
    }

    private List<String> getFeatureFlagsFromSysctl() {
        List<String> x86Features = Stream.of("features", "extfeatures", "leaf7_features").map(f -> {
            String key = "machdep.cpu." + f;
            String features = sysctlStringNoWarn(key, "");
            return Util.isBlank(features) ? null : (key + ": " + features);
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return x86Features.isEmpty() ? ExecutingCommand.runNative("sysctl -a hw.optional") : x86Features;
    }

    @Override
    public long[] queryCurrentFreq() {
        if (isArmCpu) {
            Map<Integer, Long> physFreqMap = new HashMap<>();
            getPhysicalProcessors().forEach(p -> physFreqMap.put(p.getPhysicalProcessorNumber(),
                    p.getEfficiency() > 0 ? performanceCoreFrequency : efficiencyCoreFrequency));
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
        return sysctlLong("hw.cpufrequency_max", getProcessorIdentifier().getVendorFreq());
    }

    @Override
    public long queryContextSwitches() {
        return 0L;
    }

    @Override
    public long queryInterrupts() {
        return 0L;
    }

    private boolean isArmCpu() {
        return getPhysicalProcessors().stream().map(PhysicalProcessor::getIdString).anyMatch(id -> id.contains("arm"));
    }

    protected long getMaxFreqFromByteArray(byte[] data) {
        if (data != null && data.length >= 8) {
            byte[] freqData = Arrays.copyOfRange(data, data.length - 8, data.length - 4);
            return ParseUtil.byteArrayToLong(freqData, 4, false);
        }
        return DEFAULT_FREQUENCY;
    }
}
