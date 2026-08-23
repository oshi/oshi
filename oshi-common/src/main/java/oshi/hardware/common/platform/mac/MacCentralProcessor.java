/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.mac.CpuFrequencyResidency;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.hardware.common.platform.mac.IOKitProvider.RegistryEntry;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.GlobalConfig;
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

    /** Power manager property listing each CPU cluster's voltage state table. Absent before the M5. */
    private static final String ACC_CLUSTERS = "acc-clusters";
    /** Voltage state table of the efficiency cluster on chips that do not publish {@link #ACC_CLUSTERS}. */
    private static final String LEGACY_EFFICIENCY_TABLE = "voltage-states1-sram";
    /** Voltage state table of the performance cluster on chips that do not publish {@link #ACC_CLUSTERS}. */
    private static final String LEGACY_PERFORMANCE_TABLE = "voltage-states5-sram";
    /** Size in bytes of one {@link #ACC_CLUSTERS} entry, and of one voltage state table entry. */
    private static final int ENTRY_SIZE = 8;
    /**
     * Lower bound of a plausible cluster frequency expressed in Hz. Voltage state tables switched from Hz to kHz with
     * the M4, and the two representations are nearly two orders of magnitude apart, so the unit can be inferred from
     * the value.
     */
    private static final long MIN_PLAUSIBLE_HZ = 100_000_000L;

    private final Supplier<String> vendor = memoize(this::platformExpert);
    private final boolean isArmCpu = isArmCpu();

    // Nominal cluster frequencies are fixed hardware properties, so these are memoized indefinitely. They are queried
    // lazily rather than populated as a side effect of queryProcessorId(): a caller asking for a frequency first
    // would otherwise be answered with the DEFAULT_FREQUENCY placeholder. The maxima are derived from the tables
    // rather than read separately, so both consumers share the one IORegistry walk.
    private final Supplier<long[][]> nominalFrequencyTables = memoize(this::queryNominalFrequencyTables);
    private final Supplier<long[]> nominalFrequencies = memoize(this::queryNominalFrequencies);

    // Null whenever a live frequency is unavailable: on an Intel Mac, when the configuration property is unset, or
    // when the subscription failed. Memoized indefinitely, which caches the null too, so a failed subscription is
    // attempted once rather than on every poll.
    private final Supplier<@Nullable IOReportCpuSampler> cpuSampler = memoize(this::cpuFrequencySampler);

    /**
     * Returns the sysctl provider for this implementation.
     *
     * @return the sysctl provider
     */
    protected abstract SysctlProvider sysctlProvider();

    /**
     * Subscribes to the IOReport CPU performance state channels, which report how long each core spent at each of its
     * cluster's frequencies. Called at most once, and only on Apple Silicon.
     *
     * @return the sampler, or null if the subscription could not be created
     */
    protected abstract @Nullable IOReportCpuSampler createCpuFrequencySampler();

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
        return manufacturer == null || manufacturer.isEmpty() ? "Apple Inc." : manufacturer;
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
     * <li>The number of cores at each performance level, from {@code hw.nperflevels} and
     * {@code hw.perflevel<n>.physicalcpu}. Level 0 is always the highest-performing level and macOS numbers the
     * lowest-performing cores first, so the counts partition the cores in descending class order from the end. This is
     * a documented Apple API, it produces dense classes for any number of levels, and it is the only source that can
     * separate two levels whose clusters report the same {@code cluster-type}.</li>
     * <li>The {@code cluster-type} property read from each core: {@code E} is an efficiency cluster, {@code P} a
     * performance cluster, and {@code S} a higher-performing still cluster. Chip-independent and exact, and the only
     * source available before macOS 12 introduced {@code hw.nperflevels}. A value outside that ranking abandons this
     * strategy for the whole machine rather than only for the core reporting it, since it shows the letters here are
     * not the letters being ranked.</li>
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
     * Note that {@code hw.nperflevels} gates only the first strategy, and does not gate the rest: it does not exist
     * before macOS 12, so requiring it would misclassify an M1 running macOS 11 as non-hybrid.
     *
     * @param coreKeys       the physical core keys, which sort in core number order
     * @param coreProperties per-core ({@code compatible}, {@code cluster-type}) values from
     *                       {@link #queryCoreProperties()}; may be empty, and either element of any pair may be null
     * @param perfLevelCores the value of {@code hw.perflevel<n>.physicalcpu} for each level, highest-performing level
     *                       first. May be empty, or hold zeros, if the sysctls are unavailable.
     * @return a map from core key to efficiency class, containing every key in {@code coreKeys}
     */
    static Map<Integer, Integer> deriveEfficiencyClasses(List<Integer> coreKeys,
            Map<Integer, Pair<@Nullable String, @Nullable String>> coreProperties, int[] perfLevelCores) {
        // Strategy 1: the per-performance-level core counts
        Map<Integer, Integer> efficiencyMap = perfLevelClasses(coreKeys, perfLevelCores);
        if (!efficiencyMap.isEmpty()) {
            return efficiencyMap;
        }
        // Strategy 2: the cluster-type each core reports, ranked
        Map<Integer, Integer> rankByKey = new HashMap<>();
        Map<String, Integer> rankByCodename = new HashMap<>();
        boolean unrankedClusterType = false;
        for (Integer key : coreKeys) {
            String clusterType = clusterType(coreProperties.get(key));
            Integer rank = clusterType == null ? null : clusterTypeRank(clusterType);
            if (rank != null) {
                rankByKey.put(key, rank);
                String codename = codename(coreProperties.get(key));
                if (codename != null) {
                    rankByCodename.put(codename, rank);
                }
            } else if (clusterType != null) {
                unrankedClusterType = true;
            }
        }
        if (unrankedClusterType) {
            // A cluster type present but outside the known ranking means this machine's letters are not the ones being
            // ranked, which discredits the letters it did report rather than only the ones it did not. Ranking the
            // rest would place an unknown top tier at the same class as the efficiency cores, so the codename groups
            // below, which need no vocabulary, decide instead.
            rankByKey.clear();
            rankByCodename.clear();
        }
        // Strategy 3: propagate those readings to cores sharing a codename with a core that reported one
        if (!rankByKey.isEmpty() && rankByKey.size() < coreKeys.size()) {
            for (Integer key : coreKeys) {
                // Only the cores still unclassified: a core that reported its own cluster-type keeps that exact
                // reading rather than one inferred from another core sharing its codename.
                if (rankByKey.containsKey(key)) {
                    continue;
                }
                String codename = codename(coreProperties.get(key));
                Integer rank = codename == null ? null : rankByCodename.get(codename);
                if (rank != null) {
                    rankByKey.put(key, rank);
                }
            }
        }
        // Only the cluster types actually present are ranked, so that a chip with performance and higher-performing
        // clusters but no efficiency cluster reports classes 0 and 1 rather than 1 and 2. The efficiency class is
        // relative within a machine, as it is on Windows.
        Set<Integer> ranks = new TreeSet<>(rankByKey.values());
        if (rankByKey.size() < coreKeys.size()) {
            // Cores this strategy could not classify default to class 0 below, so that class is occupied whether or not
            // any core reported an efficiency cluster type. Compressing without it would collapse a partial reading of
            // one cluster type onto the very default it has to remain distinguishable from.
            ranks.add(0);
        }
        List<Integer> presentRanks = new ArrayList<>(ranks);
        for (Map.Entry<Integer, Integer> rank : rankByKey.entrySet()) {
            efficiencyMap.put(rank.getKey(), presentRanks.indexOf(rank.getValue()));
        }
        if (efficiencyMap.size() == coreKeys.size()) {
            return efficiencyMap;
        }
        int topPerfLevelCores = perfLevelCores.length > 0 ? perfLevelCores[0] : 0;
        // Strategy 4: group by codename, ordered by lowest core number, cross-checked against the perf level count.
        // Only when cluster-type classified nothing at all: where it classified some cores, those readings are exact
        // and are kept, with strategy 5 below defaulting the remainder, rather than discarded in favor of an
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
        // Strategy 5: anything still unclassified is class 0
        for (Integer key : coreKeys) {
            if (!efficiencyMap.containsKey(key)) {
                efficiencyMap.put(key, 0);
            }
        }
        return efficiencyMap;
    }

    /**
     * Assigns an efficiency class to every core from the number of cores at each performance level.
     * <p>
     * Requires at least two levels, a positive count for each, and a total matching the number of cores. Anything else
     * means the sysctls are absent, this is not a hybrid chip, or the values disagree with the topology, in each of
     * which cases another strategy should decide instead of this one guessing.
     *
     * @param coreKeys       the physical core keys, in core number order
     * @param perfLevelCores the core count of each performance level, highest-performing level first
     * @return a map from core key to efficiency class, or an empty map if the counts cannot be trusted
     */
    private static Map<Integer, Integer> perfLevelClasses(List<Integer> coreKeys, int[] perfLevelCores) {
        Map<Integer, Integer> efficiencyMap = new HashMap<>();
        if (perfLevelCores.length < 2) {
            return efficiencyMap;
        }
        int total = 0;
        for (int levelCores : perfLevelCores) {
            if (levelCores < 1) {
                return efficiencyMap;
            }
            total += levelCores;
        }
        if (total != coreKeys.size()) {
            return efficiencyMap;
        }
        // Level 0 is the highest-performing level and holds the highest-numbered cores, so fill from the end
        int index = coreKeys.size();
        for (int level = 0; level < perfLevelCores.length; level++) {
            int efficiency = perfLevelCores.length - 1 - level;
            for (int i = 0; i < perfLevelCores[level]; i++) {
                efficiencyMap.put(coreKeys.get(--index), efficiency);
            }
        }
        return efficiencyMap;
    }

    /**
     * Extracts a core's {@code cluster-type} value, distinguishing a property that was not read from one that was.
     *
     * @param properties the core's properties, possibly null
     * @return the value, or null if the property is absent or empty
     */
    private static @Nullable String clusterType(@Nullable Pair<@Nullable String, @Nullable String> properties) {
        String clusterType = properties == null ? null : properties.getB();
        return clusterType == null || clusterType.isEmpty() ? null : clusterType;
    }

    /**
     * Ranks a core's {@code cluster-type} against the other cluster types. The value is an absolute ranking rather than
     * an efficiency class, because a class must be dense across the types a given chip actually has.
     *
     * @param clusterType the core's non-empty {@code cluster-type} value
     * @return 0 for an efficiency cluster, 1 for a performance cluster, 2 for a higher-performing still cluster, or
     *         null if not recognized
     */
    private static @Nullable Integer clusterTypeRank(String clusterType) {
        switch (Character.toUpperCase(clusterType.charAt(0))) {
            case 'E':
                return 0;
            case 'P':
                return 1;
            case 'S':
                return 2;
            default:
                return null;
        }
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
     * Queries the nominal maximum frequency of each efficiency class from the power manager's voltage state tables.
     *
     * @return the frequency in Hz of each efficiency class, indexed by class so that the last element is the frequency
     *         of the highest-performing cores. Never empty; every element is {@link #DEFAULT_FREQUENCY} if no table
     *         could be read.
     */
    protected long[] queryNominalFrequencies() {
        long[][] tables = nominalFrequencyTables.get();
        long[] byClass = new long[tables.length];
        for (int i = 0; i < tables.length; i++) {
            byClass[i] = tables[i].length == 0 ? DEFAULT_FREQUENCY : tables[i][tables[i].length - 1];
        }
        return byClass;
    }

    /**
     * Queries the frequency table of each efficiency class from the power manager's voltage state tables.
     *
     * @return the frequencies in Hz each efficiency class can run at, indexed by class so that the last element belongs
     *         to the highest-performing cores, and each in ascending order. Never empty; every element is empty if no
     *         table could be read.
     */
    protected long[][] queryNominalFrequencyTables() {
        return mapClusterTables(queryClusterFrequencyTables(), efficiencyClassCount());
    }

    /**
     * Reads the maximum frequency of every CPU cluster from the power manager's voltage state tables.
     *
     * @return the distinct cluster frequencies in Hz, in ascending order, or an empty array if none could be read
     */
    protected long[] queryClusterFrequencies() {
        long[][] tables = queryClusterFrequencyTables();
        long[] maxima = new long[tables.length];
        for (int i = 0; i < tables.length; i++) {
            maxima[i] = tables[i][tables[i].length - 1];
        }
        return maxima;
    }

    /**
     * Reads the complete voltage state table of every CPU cluster from the power manager.
     * <p>
     * Which tables hold the CPU clusters is chip-dependent. Since the M5 the power manager lists them in its
     * {@code acc-clusters} property; before that the numbering was fixed, so those two tables are the fallback.
     * <p>
     * Clusters of the same core type publish identical tables, so there are usually fewer tables returned than the chip
     * has clusters.
     *
     * @return the distinct cluster frequency tables in Hz, each in ascending order and ordered by their maxima, or an
     *         empty array if none could be read. No returned table is empty.
     */
    protected long[][] queryClusterFrequencyTables() {
        List<long[]> tables = new ArrayList<>();
        ioKitProvider().forEachMatchingServiceUntil("AppleARMIODevice", entry -> {
            if ("pmgr".equalsIgnoreCase(entry.getName())) {
                for (int table : parseClusterTables(entry.getByteArrayProperty(ACC_CLUSTERS))) {
                    addTable(tables, entry, "voltage-states" + table + "-sram");
                }
                if (tables.isEmpty()) {
                    addTable(tables, entry, LEGACY_EFFICIENCY_TABLE);
                    addTable(tables, entry, LEGACY_PERFORMANCE_TABLE);
                }
                return true;
            }
            return false;
        });
        Collections.sort(tables, (a, b) -> Long.compare(a[a.length - 1], b[b.length - 1]));
        List<long[]> distinct = new ArrayList<>(tables.size());
        for (long[] table : tables) {
            long[] previous = distinct.isEmpty() ? null : distinct.get(distinct.size() - 1);
            if (previous == null || previous[previous.length - 1] != table[table.length - 1]) {
                distinct.add(table);
            }
        }
        return distinct.toArray(new long[0][]);
    }

    /**
     * Reads one voltage state table and adds it to a list, ignoring a table that is absent, too short to hold an entry,
     * or whose maximum is not a positive frequency.
     *
     * @param tables the list to add to
     * @param entry  the power manager registry entry
     * @param key    the voltage state table property name
     */
    private void addTable(List<long[]> tables, RegistryEntry entry, String key) {
        long[] table = parseFrequencyTable(entry.getByteArrayProperty(key));
        if (table.length > 0 && table[table.length - 1] > 0) {
            // A zero-filled table is long enough to parse but says nothing, so leave it out rather than report a
            // cluster running at 0 Hz. Dropping it also keeps the list empty, so the legacy tables are still read.
            tables.add(table);
        }
    }

    /**
     * Parses a voltage state table property. Each entry pairs a frequency with the voltage needed to reach it, in
     * ascending order.
     *
     * @param data the raw property value, or null if the property is absent
     * @return every frequency in the table in Hz, in ascending order, or an empty array if the property is absent or
     *         too short to hold an entry
     */
    static long[] parseFrequencyTable(byte @Nullable [] data) {
        if (data == null || data.length < ENTRY_SIZE) {
            return new long[0];
        }
        long[] frequencies = new long[data.length / ENTRY_SIZE];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = toHz(
                    ParseUtil.byteArrayToLong(Arrays.copyOfRange(data, i * ENTRY_SIZE, i * ENTRY_SIZE + 4), 4, false));
        }
        return frequencies;
    }

    /**
     * Parses the power manager's {@code acc-clusters} property, which lists the CPU clusters. Each entry holds the
     * number of the cluster's {@code voltage-states} table in its first byte and the cluster's tier in its second, so
     * an M5 Max reporting {@code 16 00 …, 17 01 …, 05 02 …} has three clusters described by
     * {@code voltage-states22-sram}, {@code voltage-states23-sram} and {@code voltage-states5-sram}.
     *
     * @param data the raw property value, or null on a chip that does not publish it
     * @return the voltage state table numbers, in ascending tier order, or an empty array if the property is absent or
     *         too short to hold an entry
     */
    static int[] parseClusterTables(byte @Nullable [] data) {
        if (data == null || data.length < ENTRY_SIZE) {
            return new int[0];
        }
        int clusters = data.length / ENTRY_SIZE;
        int[] tables = new int[clusters];
        Integer[] order = new Integer[clusters];
        for (int i = 0; i < clusters; i++) {
            order[i] = i;
        }
        // Sorted by tier rather than assuming the entries are already in tier order, and stably, so that two clusters
        // reporting the same tier keep their listed order
        Arrays.sort(order, (a, b) -> Integer.compare(data[a * ENTRY_SIZE + 1] & 0xff, data[b * ENTRY_SIZE + 1] & 0xff));
        for (int i = 0; i < clusters; i++) {
            tables[i] = data[order[i] * ENTRY_SIZE] & 0xff;
        }
        return tables;
    }

    /**
     * Distributes the cluster frequencies over the efficiency classes. The two lists are aligned at the top, so the
     * highest class always reports the highest frequency the chip publishes, and a class with no frequency of its own
     * borrows the nearest one rather than reporting a placeholder.
     *
     * @param clusterFrequencies the distinct cluster frequencies in Hz, in ascending order
     * @param classCount         the number of efficiency classes, at least 1
     * @return the frequency in Hz of each efficiency class, indexed by class
     */
    static long[] mapClusterFrequencies(long[] clusterFrequencies, int classCount) {
        int[] indices = CpuFrequencyResidency.alignAtTop(clusterFrequencies.length, classCount);
        if (indices.length == 0) {
            long[] byClass = new long[Math.max(classCount, 1)];
            Arrays.fill(byClass, DEFAULT_FREQUENCY);
            return byClass;
        }
        long[] byClass = new long[indices.length];
        for (int i = 0; i < indices.length; i++) {
            byClass[i] = clusterFrequencies[indices[i]];
        }
        return byClass;
    }

    /**
     * Distributes the cluster frequency tables over the efficiency classes, aligned at the top exactly as
     * {@link #mapClusterFrequencies} aligns their maxima, so that a class reports the table whose maximum it reports as
     * its nominal frequency.
     *
     * @param clusterTables the distinct cluster frequency tables, ordered by their maxima
     * @param classCount    the number of efficiency classes, at least 1
     * @return the frequency table of each efficiency class, indexed by class. Every element is empty if no table could
     *         be read.
     */
    static long[][] mapClusterTables(long[][] clusterTables, int classCount) {
        int[] indices = CpuFrequencyResidency.alignAtTop(clusterTables.length, classCount);
        if (indices.length == 0) {
            long[][] byClass = new long[Math.max(classCount, 1)][];
            Arrays.fill(byClass, new long[0]);
            return byClass;
        }
        long[][] byClass = new long[indices.length][];
        for (int i = 0; i < indices.length; i++) {
            byClass[i] = clusterTables[indices[i]];
        }
        return byClass;
    }

    /**
     * Counts the efficiency classes this processor reports, which is one more than the highest class of any core.
     *
     * @return the number of efficiency classes, at least 1
     */
    private int efficiencyClassCount() {
        int highest = 0;
        for (PhysicalProcessor processor : getPhysicalProcessors()) {
            highest = Math.max(highest, processor.getEfficiency());
        }
        return highest + 1;
    }

    /**
     * Gets the nominal frequency of the highest-performing cores, which is the maximum frequency of the processor.
     *
     * @return the frequency in Hz
     */
    protected long getPerformanceCoreFrequency() {
        long[] byClass = nominalFrequencies.get();
        return byClass[byClass.length - 1];
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
        long cpuFreq = isArmCpu ? getPerformanceCoreFrequency() : sysctlLong("hw.cpufrequency", 0L);
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
        int perflevels = Math.max(1, sysctlIntNoWarn("hw.nperflevels", 1));
        int[] perfLevelCores = new int[perflevels];
        for (int i = 0; i < perflevels; i++) {
            perfLevelCores[i] = sysctlIntNoWarn("hw.perflevel" + i + ".physicalcpu", 0);
        }
        List<Integer> coreKeys = pkgCoreKeys.stream().sorted().collect(Collectors.toList());
        Map<Integer, Integer> efficiencyMap = deriveEfficiencyClasses(coreKeys, coreProps, perfLevelCores);
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
            long[] byClass = nominalFrequencies.get();
            long topFreq = byClass[byClass.length - 1];
            Map<Integer, Long> physFreqMap = new HashMap<>();
            // A class outside the array cannot happen, since the array was sized from the classes the cores report,
            // but clamping keeps a future mismatch from becoming an exception on a path callers poll
            getPhysicalProcessors().forEach(p -> physFreqMap.put(p.getPhysicalProcessorNumber(),
                    byClass[Math.min(Math.max(p.getEfficiency(), 0), byClass.length - 1)]));
            applyLiveFrequencies(physFreqMap);
            return getLogicalProcessors().stream().map(LogicalProcessor::getPhysicalProcessorNumber)
                    .map(p -> physFreqMap.getOrDefault(p, topFreq)).mapToLong(f -> f).toArray();
        }
        return new long[] { getProcessorIdentifier().getVendorFreq() };
    }

    /**
     * Replaces nominal core frequencies with the frequency each core actually ran at since the previous call, where
     * IOReport can supply it. A core whose residency cannot be paired with a frequency table keeps its nominal
     * frequency, as do all of them if no sample is available or if the channels cannot be matched to the cores.
     *
     * @param physFreqMap the frequency in Hz of each physical processor, updated in place
     */
    private void applyLiveFrequencies(Map<Integer, Long> physFreqMap) {
        IOReportCpuSampler sampler = cpuSampler.get();
        if (sampler == null) {
            return;
        }
        CpuResidencySample sample = sampler.sampleResidencyDelta();
        if (sample == null) {
            // No previous sample to subtract from, as on the first call
            return;
        }
        Map<String, Map<String, Long>> residency = sample.getCoreStates();
        if (residency.isEmpty()) {
            return;
        }
        // Both lists run in ascending performance order, so they line up only if they describe the same chip. Any
        // disagreement leaves the nominal frequencies in place rather than risk charging a core with another cluster's
        // frequency table, which would report a plausible but wrong number.
        Map<Integer, List<String>> channelGroups = groupChannelsByCoreType(residency.keySet());
        Map<Integer, List<PhysicalProcessor>> coreGroups = groupCoresByEfficiencyClass();
        if (channelGroups.containsKey(CpuFrequencyResidency.UNKNOWN_RANK)) {
            // A core type this release does not know. Where it belongs among the ones it does know is a guess, and
            // guessing wrong charges a core with another cluster's frequency table.
            LOG.debug("IOReport reports a CPU core type this release does not recognize: {}",
                    channelGroups.get(CpuFrequencyResidency.UNKNOWN_RANK));
            return;
        }
        if (channelGroups.size() != coreGroups.size()) {
            LOG.debug("IOReport reports {} core types but this processor has {} efficiency classes.",
                    channelGroups.size(), coreGroups.size());
            return;
        }
        Iterator<List<String>> channelIterator = channelGroups.values().iterator();
        for (Map.Entry<Integer, List<PhysicalProcessor>> cores : coreGroups.entrySet()) {
            List<String> channels = channelIterator.next();
            if (channels.size() != cores.getValue().size()) {
                LOG.debug("IOReport reports {} cores of one type but efficiency class {} has {}.", channels.size(),
                        cores.getKey(), cores.getValue().size());
                return;
            }
        }
        // The state each cluster's hardware actually ran at, which is what any core in it ran at while it was running.
        // Only usable if the clusters reported are the core types reported, since otherwise which table weights which
        // cluster is a guess.
        Map<Integer, Map<String, Long>> complexByRank = CpuFrequencyResidency
                .realizedComplexStates(sample.getComplexStates());
        boolean complexMatchesCores = complexByRank.keySet().equals(channelGroups.keySet());
        long[][] tables = nominalFrequencyTables.get();
        Iterator<Map.Entry<Integer, List<String>>> channelEntries = channelGroups.entrySet().iterator();
        for (Map.Entry<Integer, List<PhysicalProcessor>> cores : coreGroups.entrySet()) {
            Map.Entry<Integer, List<String>> channelEntry = channelEntries.next();
            List<String> channels = channelEntry.getValue();
            long[] table = tables[Math.min(Math.max(cores.getKey(), 0), tables.length - 1)];
            Map<String, Long> cluster = complexMatchesCores ? complexByRank.get(channelEntry.getKey()) : null;
            long realized = cluster == null ? 0L : CpuFrequencyResidency.activeWeightedFrequency(cluster, table);
            for (int i = 0; i < channels.size(); i++) {
                Map<String, Long> states = residency.get(channels.get(i));
                long frequency = states == null ? 0L : CpuFrequencyResidency.activeWeightedFrequency(states, table);
                if (frequency == 0L) {
                    // Nothing was observed, or the core's states are not a list this table can be paired with
                    continue;
                }
                if (realized > 0 && frequency != table[0]) {
                    // A core's own residency names the state it asked for, which is the fastest one whenever it has
                    // work, so under a power or thermal cap it reads high by as much as the cap. Where the cluster can
                    // be read it says what the core got instead, and only a core that did not run at all keeps the
                    // lowest frequency its cluster can run at rather than being credited with its siblings' work.
                    frequency = realized;
                }
                physFreqMap.put(cores.getValue().get(i).getPhysicalProcessorNumber(), frequency);
            }
        }
    }

    /**
     * Groups the IOReport channel names by the core type they report, in ascending performance order.
     *
     * @param channelNames the channel names sampled
     * @return the channel names of each core type, keyed by that type's rank, with the names of one type in core order
     */
    private static Map<Integer, List<String>> groupChannelsByCoreType(Collection<String> channelNames) {
        Map<Integer, List<String>> groups = new TreeMap<>();
        for (String channel : CpuFrequencyResidency.orderChannels(channelNames)) {
            int rank = CpuFrequencyResidency.prefixRank(channel);
            groups.computeIfAbsent(rank, k -> new ArrayList<>()).add(channel);
        }
        return groups;
    }

    /**
     * Groups this processor's physical cores by efficiency class, in ascending performance order.
     *
     * @return the cores of each class, keyed by class, with the cores of one class in processor number order. A class
     *         with no cores is absent.
     */
    private Map<Integer, List<PhysicalProcessor>> groupCoresByEfficiencyClass() {
        Map<Integer, List<PhysicalProcessor>> groups = new TreeMap<>();
        for (PhysicalProcessor processor : getPhysicalProcessors()) {
            int efficiency = Math.max(processor.getEfficiency(), 0);
            groups.computeIfAbsent(efficiency, k -> new ArrayList<>()).add(processor);
        }
        for (List<PhysicalProcessor> group : groups.values()) {
            Collections.sort(group,
                    (a, b) -> Integer.compare(a.getPhysicalProcessorNumber(), b.getPhysicalProcessorNumber()));
        }
        return groups;
    }

    /**
     * Subscribes to the IOReport CPU performance state channels, if a live frequency was asked for and this chip
     * publishes them.
     *
     * @return the sampler, or null if a live frequency is unavailable
     */
    private @Nullable IOReportCpuSampler cpuFrequencySampler() {
        if (!isArmCpu || !GlobalConfig.get(GlobalConfig.OSHI_OS_MAC_CPU_FREQUENCY_IOREPORT, false)) {
            return null;
        }
        IOReportCpuSampler sampler = createCpuFrequencySampler();
        if (sampler == null) {
            LOG.warn("Unable to subscribe to the IOReport CPU performance states."
                    + " Reporting nominal cluster frequencies instead.");
        }
        return sampler;
    }

    @Override
    public long queryMaxFreq() {
        if (isArmCpu) {
            return getPerformanceCoreFrequency();
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
     * Extracts the maximum frequency from a voltage state table property. Each entry pairs a frequency with the voltage
     * needed to reach it, in ascending order, so the last entry holds the maximum.
     *
     * @param data the byte array from IOKit
     * @return the frequency in Hz, or DEFAULT_FREQUENCY if unavailable
     */
    protected long getMaxFreqFromByteArray(byte[] data) {
        long[] table = parseFrequencyTable(data);
        return table.length == 0 ? DEFAULT_FREQUENCY : table[table.length - 1];
    }

    /**
     * Converts a voltage state table frequency to Hz.
     * <p>
     * The M4 changed the unit of the CPU tables from Hz to kHz, which is why an unconverted value reads as a few
     * megahertz. The unit is inferred from the magnitude rather than from the chip, because the two units are nearly
     * two orders of magnitude apart and because they are not consistent across the tables of one chip: on the M4 the
     * CPU tables are in kHz while the GPU table is still in Hz.
     *
     * @param frequency the raw table value
     * @return the frequency in Hz
     */
    static long toHz(long frequency) {
        return frequency > 0 && frequency < MIN_PLAUSIBLE_HZ ? frequency * 1000L : frequency;
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
