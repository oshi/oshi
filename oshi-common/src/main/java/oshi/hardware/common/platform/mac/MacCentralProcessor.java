/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

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
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Base class for macOS CentralProcessor implementations. Subclasses provide platform-specific sysctl, IOKit, and Mach
 * kernel calls.
 */
@ThreadSafe
public abstract class MacCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessor.class);

    // Indices into the Mach cpu_ticks arrays (host_cpu_load_info / processor_cpu_load_info)
    private static final int CPU_STATE_MAX = 4;
    private static final int CPU_STATE_USER = 0;
    private static final int CPU_STATE_SYSTEM = 1;
    private static final int CPU_STATE_IDLE = 2;
    private static final int CPU_STATE_NICE = 3;

    /**
     * Default constructor.
     */
    protected MacCentralProcessor() {
    }

    private static final Set<String> ARM_P_CORES = Stream
            .of("apple,firestorm arm,v8", "apple,avalanche arm,v8", "apple,everest arm,v8", "apple,donan arm,v8")
            .collect(Collectors.toSet());

    private static final Pattern CPU_N = Pattern.compile("^cpu(\\d+)");

    /** ARM CPU type constant. */
    protected static final int ARM_CPUTYPE = 0x0100000C;
    /** M1 CPU family constant. */
    protected static final int M1_CPUFAMILY = 0x1b588bb3;
    /** M2 CPU family constant. */
    protected static final int M2_CPUFAMILY = 0xda33d83d;
    /** M3 CPU family constant. */
    protected static final int M3_CPUFAMILY = 0x8765edea;
    /** M3 Pro/Max CPU family constant. */
    protected static final int M3_PRO_CPUFAMILY = 0x5f4dea93;
    /** M4 CPU family constant. */
    protected static final int M4_CPUFAMILY = 0x6f5129ac;
    /** Default frequency in Hz. */
    protected static final long DEFAULT_FREQUENCY = 2_400_000_000L;

    private final Supplier<String> vendor = memoize(this::platformExpert);
    private final boolean isArmCpu = isArmCpu();

    private long performanceCoreFrequency = DEFAULT_FREQUENCY;
    private long efficiencyCoreFrequency = DEFAULT_FREQUENCY;

    /**
     * Returns the sysctl provider for this implementation.
     *
     * @return the sysctl provider
     */
    protected abstract SysctlProvider sysctlProvider();

    /**
     * Queries a sysctl integer value.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    protected int sysctlInt(String name, int defaultValue) {
        return sysctlProvider().sysctlInt(name, defaultValue);
    }

    /**
     * Queries a sysctl integer value without logging warnings.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    protected int sysctlIntNoWarn(String name, int defaultValue) {
        return sysctlProvider().sysctlIntNoWarn(name, defaultValue);
    }

    /**
     * Queries a sysctl long value.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    protected long sysctlLong(String name, long defaultValue) {
        return sysctlProvider().sysctlLong(name, defaultValue);
    }

    /**
     * Queries a sysctl string value.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    protected String sysctlString(String name, String defaultValue) {
        return sysctlProvider().sysctlString(name, defaultValue);
    }

    /**
     * Queries a sysctl string value without logging warnings.
     *
     * @param name         the sysctl name
     * @param defaultValue the default value if not found
     * @return the sysctl value
     */
    protected String sysctlStringNoWarn(String name, String defaultValue) {
        return sysctlProvider().sysctlStringNoWarn(name, defaultValue);
    }

    /**
     * Returns the IOKit provider for this implementation.
     *
     * @return the IOKit provider
     */
    protected abstract IOKitProvider ioKitProvider();

    /**
     * Queries the platform expert vendor string.
     *
     * @return the vendor string
     */
    protected String platformExpert() {
        String manufacturer = ioKitProvider().withMatchingService("IOPlatformExpertDevice", entry -> {
            byte[] data = entry.getByteArrayProperty("manufacturer");
            return data != null ? new String(data, StandardCharsets.UTF_8).replace("\0", "").trim() : null;
        });
        return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
    }

    /**
     * Queries compatible strings for CPU identification.
     *
     * @return a map of physical processor numbers to compatible strings
     */
    protected Map<Integer, String> queryCompatibleStrings() {
        Map<Integer, String> compatibleStrMap = new HashMap<>();
        ioKitProvider().forEachMatchingService("IOPlatformDevice", entry -> {
            String name = entry.getName();
            if (name != null) {
                Matcher m = CPU_N.matcher(name.toLowerCase(Locale.ROOT));
                if (m.matches()) {
                    int procId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                    byte[] data = entry.getByteArrayProperty("compatible");
                    if (data != null) {
                        compatibleStrMap.put(procId,
                                new String(data, StandardCharsets.UTF_8).replace('\0', ' ').trim());
                    }
                }
            }
        });
        return compatibleStrMap;
    }

    /**
     * Calculates nominal frequencies for performance and efficiency cores.
     */
    protected void calculateNominalFrequencies() {
        ioKitProvider().forEachMatchingServiceUntil("AppleARMIODevice", entry -> {
            if ("pmgr".equalsIgnoreCase(entry.getName())) {
                setPerformanceCoreFrequency(
                        getMaxFreqFromByteArray(entry.getByteArrayProperty("voltage-states5-sram")));
                setEfficiencyCoreFrequency(getMaxFreqFromByteArray(entry.getByteArrayProperty("voltage-states1-sram")));
                return true;
            }
            return false;
        });
    }

    /**
     * Gets the performance core frequency.
     *
     * @return the performance core frequency in Hz
     */
    protected long getPerformanceCoreFrequency() {
        return performanceCoreFrequency;
    }

    /**
     * Sets the performance core frequency.
     *
     * @param freq the frequency in Hz
     */
    protected void setPerformanceCoreFrequency(long freq) {
        this.performanceCoreFrequency = freq;
    }

    /**
     * Gets the efficiency core frequency.
     *
     * @return the efficiency core frequency in Hz
     */
    protected long getEfficiencyCoreFrequency() {
        return efficiencyCoreFrequency;
    }

    /**
     * Sets the efficiency core frequency.
     *
     * @param freq the frequency in Hz
     */
    protected void setEfficiencyCoreFrequency(long freq) {
        this.efficiencyCoreFrequency = freq;
    }

    /**
     * Checks if this is an ARM CPU.
     *
     * @return true if ARM
     */
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
                        case 4:
                            family = M4_CPUFAMILY;
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
        List<String> x86Features = parseX86FeatureFlags();
        return x86Features.isEmpty() ? ExecutingCommand.runNative("sysctl -a hw.optional") : x86Features;
    }

    List<String> parseX86FeatureFlags() {
        return Stream.of("features", "extfeatures", "leaf7_features").map(f -> {
            String key = "machdep.cpu." + f;
            String features = sysctlStringNoWarn(key, "");
            return Util.isBlank(features) ? null : (key + ": " + features);
        }).filter(Objects::nonNull).collect(Collectors.toList());
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

    /**
     * Extracts the maximum frequency from a byte array property.
     *
     * @param data the byte array from IOKit
     * @return the frequency in Hz, or DEFAULT_FREQUENCY if unavailable
     */
    protected long getMaxFreqFromByteArray(byte[] data) {
        if (data != null && data.length >= 8) {
            byte[] freqData = Arrays.copyOfRange(data, data.length - 8, data.length - 4);
            return ParseUtil.byteArrayToLong(freqData, 4, false);
        }
        return DEFAULT_FREQUENCY;
    }

    /**
     * Reads the system-wide {@code host_cpu_load_info} CPU-tick counters.
     *
     * @return the {@code CPU_STATE_MAX} raw tick values, or a shorter/empty array on failure
     */
    protected abstract int[] queryHostCpuLoadTicks();

    /**
     * Reads the per-processor {@code processor_cpu_load_info} CPU-tick counters as a flat array of
     * {@code processorCount * CPU_STATE_MAX} values. Implementations copy the values out of native memory and release
     * the kernel-allocated buffer before returning.
     *
     * @return the flat per-processor tick values, or an empty array on failure
     */
    protected abstract int[] queryProcessorCpuTicks();

    /**
     * Native {@code getloadavg(3)} call, filling {@code loadavg} with up to {@code nelem} samples.
     *
     * @param loadavg the array to populate
     * @param nelem   the number of elements requested
     * @return the number of samples retrieved, or a negative value on failure
     */
    protected abstract int getloadavgNative(double[] loadavg, int nelem);

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int[] cpuTicks = queryHostCpuLoadTicks();
        if (cpuTicks.length < CPU_STATE_MAX) {
            return ticks;
        }
        ticks[TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[CPU_STATE_USER]);
        ticks[TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[CPU_STATE_NICE]);
        ticks[TickType.SYSTEM.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[CPU_STATE_SYSTEM]);
        ticks[TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[CPU_STATE_IDLE]);
        return ticks;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        int[] cpuTicks = queryProcessorCpuTicks();
        int procCount = cpuTicks.length / CPU_STATE_MAX;
        if (procCount > ticks.length) {
            LOG.warn("processor_cpu_load_info returned {} CPUs but expected {}; capping iteration", procCount,
                    ticks.length);
        }
        int cpuLimit = Math.min(procCount, ticks.length);
        for (int cpu = 0; cpu < cpuLimit; cpu++) {
            int offset = cpu * CPU_STATE_MAX;
            ticks[cpu][TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + CPU_STATE_USER]);
            ticks[cpu][TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + CPU_STATE_NICE]);
            ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + CPU_STATE_SYSTEM]);
            ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + CPU_STATE_IDLE]);
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = getloadavgNative(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }
}
