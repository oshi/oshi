/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;
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

    private static final Pattern CPU_N = Pattern.compile("^cpu(\\d+)");

    /** Extracts the core codename from an IORegistry {@code compatible} string, e.g. {@code apple,avalanche}. */
    private static final Pattern APPLE_CORE = Pattern.compile("apple,([a-z0-9_.-]+)");

    /** Prefix of the microarchitecture description, matching the format of the entries in the architecture table. */
    private static final String MICROARCH_PREFIX = "ARM64 SoC: ";

    /** ARM CPU type constant. */
    protected static final int ARM_CPUTYPE = 0x0100000C;
    /** Default frequency in Hz. */
    protected static final long DEFAULT_FREQUENCY = 2_400_000_000L;

    private final Supplier<String> vendor = memoize(this::platformExpert);
    private final boolean isArmCpu = isArmCpu();

    private volatile long performanceCoreFrequency = DEFAULT_FREQUENCY;
    private volatile long efficiencyCoreFrequency = DEFAULT_FREQUENCY;

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
    protected @Nullable String platformExpert() {
        String manufacturer = ioKitProvider().withMatchingService("IOPlatformExpertDevice", entry -> {
            byte[] data = entry.getByteArrayProperty("manufacturer");
            return data != null ? ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8) : null;
        });
        return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
    }

    /**
     * Queries the per-core IORegistry properties used to identify the CPU. Walks the {@code IOPlatformDevice} nodes
     * named {@code cpuN} once, reading both the {@code compatible} property (which names the core's microarchitecture,
     * e.g. {@code apple,avalanche arm,v8}) and the {@code cluster-type} property (a single letter, {@code P} for a
     * performance cluster or {@code E} for an efficiency cluster).
     *
     * @return a map of physical processor numbers to a pair of ({@code compatible}, {@code cluster-type}). Either
     *         element may be {@code null} if that property is absent from the node.
     */
    protected Map<Integer, Pair<@Nullable String, @Nullable String>> queryCoreProperties() {
        Map<Integer, Pair<@Nullable String, @Nullable String>> coreProperties = new HashMap<>();
        ioKitProvider().forEachMatchingService("IOPlatformDevice", entry -> {
            String name = entry.getName();
            if (name != null) {
                Matcher m = CPU_N.matcher(name.toLowerCase(Locale.ROOT));
                if (m.matches()) {
                    int procId = ParseUtil.parseIntOrDefault(m.group(1), 0);
                    coreProperties.put(procId, new Pair<>(ioRegString(entry.getByteArrayProperty("compatible")),
                            ioRegString(entry.getByteArrayProperty("cluster-type"))));
                }
            }
        });
        return coreProperties;
    }

    /**
     * Decodes an IORegistry byte-array property. These hold one or more null-terminated strings, so nulls become
     * separators.
     *
     * @param data the raw property value, possibly null
     * @return the decoded string, or null if {@code data} was null
     */
    private static @Nullable String ioRegString(byte @Nullable [] data) {
        return data == null ? null : new String(data, StandardCharsets.UTF_8).replace('\0', ' ').trim();
    }

    /**
     * Derives a performance/efficiency class for each physical core, so that hybrid Apple Silicon cores are classified
     * without a hardcoded table of core codenames. A higher value indicates a higher-performing core, matching
     * {@link PhysicalProcessor#getEfficiency()}.
     * <p>
     * The first of these strategies that classifies every core wins:
     * <ol>
     * <li>The {@code cluster-type} property read from each core: {@code P} is a performance cluster, {@code E} an
     * efficiency cluster. This is chip-independent and exact.</li>
     * <li>If only some cores report a recognized {@code cluster-type}, the mapping learned from those cores is applied
     * to the other cores sharing the same codename.</li>
     * <li>Grouping cores by codename and ordering the groups by their lowest core number, since macOS numbers
     * efficiency cores first. Where there are exactly two groups and {@code hw.perflevel0.physicalcpu} matches the size
     * of exactly one of them, that count decides which group is the performance group, overriding the ordering. This
     * applies only when {@code cluster-type} classified no core at all: a {@code cluster-type} reading is exact, so
     * where some cores reported one those values are kept and only the remainder falls to the default below.</li>
     * <li>Failing all of those, every core is class 0, which is the historical behavior for a chip whose cores were not
     * recognized, and for non-hybrid and Intel processors.</li>
     * </ol>
     * Note that {@code hw.nperflevels} is deliberately neither an input nor a gate here: it does not exist before macOS
     * 12, so requiring it would misclassify an M1 running macOS 11 as non-hybrid.
     *
     * @param coreKeys          the physical core keys, which sort in core number order
     * @param coreProperties    per-core ({@code compatible}, {@code cluster-type}) values from
     *                          {@link #queryCoreProperties()}; may be empty, and either element of any pair may be null
     * @param topPerfLevelCores the value of {@code hw.perflevel0.physicalcpu}, or 0 if unknown. Perf level 0 is always
     *                          the highest-performing level.
     * @return a map from core key to efficiency class, containing every key in {@code coreKeys}
     */
    static Map<Integer, Integer> deriveEfficiencyClasses(List<Integer> coreKeys,
            Map<Integer, Pair<@Nullable String, @Nullable String>> coreProperties, int topPerfLevelCores) {
        Map<Integer, Integer> efficiencyMap = new HashMap<>();
        // Strategy 1 and 2: cluster-type, directly and then propagated to cores sharing a codename
        Map<String, Integer> classByCodename = new HashMap<>();
        for (Integer key : coreKeys) {
            Integer efficiency = clusterTypeClass(coreProperties.get(key));
            if (efficiency != null) {
                efficiencyMap.put(key, efficiency);
                String codename = codename(coreProperties.get(key));
                if (codename != null) {
                    classByCodename.put(codename, efficiency);
                }
            }
        }
        if (!efficiencyMap.isEmpty() && efficiencyMap.size() < coreKeys.size()) {
            for (Integer key : coreKeys) {
                // Only the cores still unclassified: a core that reported its own cluster-type keeps that exact
                // reading rather than one inferred from another core sharing its codename.
                if (efficiencyMap.containsKey(key)) {
                    continue;
                }
                String codename = codename(coreProperties.get(key));
                Integer efficiency = codename == null ? null : classByCodename.get(codename);
                if (efficiency != null) {
                    efficiencyMap.put(key, efficiency);
                }
            }
        }
        if (efficiencyMap.size() == coreKeys.size()) {
            return efficiencyMap;
        }
        // Strategy 3: group by codename, ordered by lowest core number, cross-checked against the perf level count.
        // Only when cluster-type classified nothing at all: where it classified some cores, those readings are exact
        // and are kept, with strategy 4 below defaulting the remainder, rather than discarded in favor of an
        // inference over the whole set.
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        if (efficiencyMap.isEmpty()) {
            for (Integer key : coreKeys) {
                String codename = codename(coreProperties.get(key));
                if (codename == null) {
                    // A core with no codename cannot be grouped, so this strategy cannot classify every core
                    groups.clear();
                    break;
                }
                List<Integer> group = groups.get(codename);
                if (group == null) {
                    group = new ArrayList<>();
                    groups.put(codename, group);
                }
                group.add(key);
            }
        }
        List<List<Integer>> ordered = new ArrayList<>(groups.values());
        // Only decisive when the first group alone matches the highest perf level's core count; an even split such as
        // the base M1's 4+4 matches both groups and leaves the ordering to decide instead.
        if (ordered.size() == 2 && topPerfLevelCores > 0 && ordered.get(0).size() == topPerfLevelCores
                && ordered.get(1).size() != topPerfLevelCores) {
            Collections.reverse(ordered);
        }
        for (int i = 0; i < ordered.size(); i++) {
            for (Integer key : ordered.get(i)) {
                efficiencyMap.put(key, i);
            }
        }
        // Strategy 4: anything still unclassified is class 0
        for (Integer key : coreKeys) {
            if (!efficiencyMap.containsKey(key)) {
                efficiencyMap.put(key, 0);
            }
        }
        return efficiencyMap;
    }

    /**
     * Maps a core's {@code cluster-type} to an efficiency class.
     *
     * @param properties the core's properties, possibly null
     * @return 1 for a performance cluster, 0 for an efficiency cluster, or null if not recognized
     */
    private static @Nullable Integer clusterTypeClass(@Nullable Pair<@Nullable String, @Nullable String> properties) {
        String clusterType = properties == null ? null : properties.getB();
        if (clusterType == null || clusterType.isEmpty()) {
            return null;
        }
        char c = Character.toUpperCase(clusterType.charAt(0));
        if (c == 'P') {
            return 1;
        }
        return c == 'E' ? 0 : null;
    }

    /**
     * Extracts the Apple core codename from a core's {@code compatible} string.
     *
     * @param properties the core's properties, possibly null
     * @return the codename, e.g. {@code avalanche}, or null if absent
     */
    private static @Nullable String codename(@Nullable Pair<@Nullable String, @Nullable String> properties) {
        String compatible = properties == null ? null : properties.getA();
        if (compatible == null) {
            return null;
        }
        Matcher m = APPLE_CORE.matcher(compatible.toLowerCase(Locale.ROOT));
        return m.find() ? m.group(1) : null;
    }

    /**
     * Derives a microarchitecture description from the core codenames, highest-performing first, for example
     * {@code ARM64 SoC: Avalanche + Blizzard}. This lets a chip absent from the architecture table still be described,
     * rather than reported as unknown.
     *
     * @param physicalProcessors the physical processors, whose ID strings hold the IORegistry {@code compatible} value
     * @return the description, or null if no Apple core codename was found
     */
    static @Nullable String deriveMicroarchitecture(List<PhysicalProcessor> physicalProcessors) {
        // Sorting keys, ordering distinct codenames by descending efficiency class and then by core number
        Map<String, Pair<Integer, Integer>> codenames = new LinkedHashMap<>();
        for (PhysicalProcessor processor : physicalProcessors) {
            Matcher m = APPLE_CORE.matcher(processor.getIdString().toLowerCase(Locale.ROOT));
            if (m.find() && !codenames.containsKey(m.group(1))) {
                codenames.put(m.group(1),
                        new Pair<>(processor.getEfficiency(), processor.getPhysicalProcessorNumber()));
            }
        }
        if (codenames.isEmpty()) {
            return null;
        }
        List<Map.Entry<String, Pair<Integer, Integer>>> entries = new ArrayList<>(codenames.entrySet());
        Collections.sort(entries, (a, b) -> {
            int byEfficiency = b.getValue().getA().compareTo(a.getValue().getA());
            return byEfficiency == 0 ? a.getValue().getB().compareTo(b.getValue().getB()) : byEfficiency;
        });
        StringBuilder sb = new StringBuilder(MICROARCH_PREFIX);
        for (int i = 0; i < entries.size(); i++) {
            String codename = entries.get(i).getKey();
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(codename.substring(0, 1).toUpperCase(Locale.ROOT)).append(codename.substring(1));
        }
        return sb.toString();
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
        String microarchitecture = null;
        if (cpuName.startsWith("Apple")) {
            cpuVendor = vendor.get();
            cpuStepping = "0";
            cpuModel = "0";
            int type;
            int family;
            if (isArmCpu) {
                type = ARM_CPUTYPE;
                // No fallback when this is absent: guessing a family from the brand string reported an unrecognized
                // chip as an M1. A zero family simply misses the architecture table, and the derived
                // microarchitecture below covers that case instead.
                family = sysctlInt("hw.cpufamily", 0);
                microarchitecture = deriveMicroarchitecture(getPhysicalProcessors());
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
                cpuFreq, microarchitecture);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>, List<String>> initProcessorCounts() {
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
        Map<Integer, Pair<@Nullable String, @Nullable String>> coreProps = queryCoreProperties();
        int perflevels = sysctlIntNoWarn("hw.nperflevels", 1);
        int topPerfLevelCores = sysctlIntNoWarn("hw.perflevel0.physicalcpu", 0);
        List<Integer> coreKeys = pkgCoreKeys.stream().sorted().collect(Collectors.toList());
        Map<Integer, Integer> efficiencyMap = deriveEfficiencyClasses(coreKeys, coreProps, topPerfLevelCores);
        List<PhysicalProcessor> physProcs = new ArrayList<>(coreKeys.size());
        for (Integer k : coreKeys) {
            Pair<@Nullable String, @Nullable String> props = coreProps.get(k);
            String compat = props == null || props.getA() == null ? "" : props.getA().toLowerCase(Locale.ROOT);
            physProcs.add(new PhysicalProcessor(k >> 16, k & 0xffff, efficiencyMap.getOrDefault(k, 0), compat));
        }
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
                caches.add(new ProcessorCache(1, l1associativity, linesize, size, ProcessorCache.Type.INSTRUCTION));
            }
            size = sysctlIntNoWarn("hw.perflevel" + i + ".l1dcachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(1, l1associativity, linesize, size, ProcessorCache.Type.DATA));
            }
            size = sysctlIntNoWarn("hw.perflevel" + i + ".l2cachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(2, l2associativity, linesize, size, ProcessorCache.Type.UNIFIED));
            }
            size = sysctlIntNoWarn("hw.perflevel" + i + ".l3cachesize", 0);
            if (size > 0) {
                caches.add(new ProcessorCache(3, 0, linesize, size, ProcessorCache.Type.UNIFIED));
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
