/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * A CPU.
 */
@ThreadSafe
public abstract class AbstractCentralProcessor implements CentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCentralProcessor.class);

    private final Supplier<ProcessorIdentifier> cpuid = memoize(this::queryProcessorId);
    private final Supplier<Long> maxFreq = memoize(this::queryMaxFreq, defaultExpiration());
    // Max often iterates current, intentionally making it shorter to re-memoize current
    private final Supplier<long[]> currentFreq = memoize(this::queryCurrentFreq, defaultExpiration() / 2L);
    private final Supplier<Long> contextSwitches = memoize(this::queryContextSwitches, defaultExpiration());
    private final Supplier<Long> interrupts = memoize(this::queryInterrupts, defaultExpiration());

    private final Supplier<long[]> systemCpuLoadTicks = memoize(this::querySystemCpuLoadTicks, defaultExpiration());
    private final Supplier<long[][]> processorCpuLoadTicks = memoize(this::queryProcessorCpuLoadTicks,
            defaultExpiration());

    // Logical and Physical Processor Counts
    private final int physicalPackageCount;
    private final int physicalProcessorCount;
    private final int logicalProcessorCount;

    // Processor info, initialized in constructor
    private final List<LogicalProcessor> logicalProcessors;
    private final List<PhysicalProcessor> physicalProcessors;
    private final List<ProcessorCache> processorCaches;
    private final List<String> featureFlags;

    /**
     * Create a Processor
     */
    protected AbstractCentralProcessor() {
        Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> processorLists = initProcessorCounts();
        // Populate logical processor lists.
        this.logicalProcessors = Collections.unmodifiableList(processorLists.getA());
        if (processorLists.getB() == null) {
            Set<Integer> pkgCoreKeys = this.logicalProcessors.stream()
                    .map(p -> (p.getPhysicalPackageNumber() << 16) + p.getPhysicalProcessorNumber())
                    .collect(Collectors.toSet());
            List<PhysicalProcessor> physProcs = pkgCoreKeys.stream().sorted()
                    .map(k -> new PhysicalProcessor(k >> 16, k & 0xffff)).collect(Collectors.toList());
            this.physicalProcessors = Collections.unmodifiableList(physProcs);
        } else {
            this.physicalProcessors = Collections.unmodifiableList(processorLists.getB());
        }
        this.processorCaches = processorLists.getC() == null ? Collections.emptyList()
                : Collections.unmodifiableList(processorLists.getC());
        // Init processor counts
        Set<Integer> physPkgs = new HashSet<>();
        for (LogicalProcessor logProc : this.logicalProcessors) {
            int pkg = logProc.getPhysicalPackageNumber();
            physPkgs.add(pkg);
        }
        this.logicalProcessorCount = this.logicalProcessors.size();
        this.physicalProcessorCount = this.physicalProcessors.size();
        this.physicalPackageCount = physPkgs.size();
        this.featureFlags = Collections.unmodifiableList(processorLists.getD());
    }

    /**
     * Initializes logical and physical processor lists and feature flags.
     *
     * @return Lists of initialized Logical Processors, Physical Processors, Processor Caches, and Feature Flags.
     */
    protected abstract Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts();

    /**
     * Updates logical and physical processor counts and arrays
     *
     * @return An array of initialized Logical Processors
     */
    protected abstract ProcessorIdentifier queryProcessorId();

    @Override
    public ProcessorIdentifier getProcessorIdentifier() {
        return cpuid.get();
    }

    @Override
    public long getMaxFreq() {
        return maxFreq.get();
    }

    /**
     * Get processor max frequency.
     *
     * @return The max frequency.
     */
    protected long queryMaxFreq() {
        return Arrays.stream(getCurrentFreq()).max().orElse(-1L);
    }

    @Override
    public long[] getCurrentFreq() {
        long[] freq = currentFreq.get();
        if (freq.length == getLogicalProcessorCount()) {
            return freq;
        }
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, freq[0]);
        return freqs;
    }

    /**
     * Get processor current frequency.
     *
     * @return The current frequency.
     */
    protected abstract long[] queryCurrentFreq();

    @Override
    public long getContextSwitches() {
        return contextSwitches.get();
    }

    /**
     * Get number of context switches
     *
     * @return The context switches
     */
    protected abstract long queryContextSwitches();

    @Override
    public long getInterrupts() {
        return interrupts.get();
    }

    /**
     * Get number of interrupts
     *
     * @return The interrupts
     */
    protected abstract long queryInterrupts();

    @Override
    public List<LogicalProcessor> getLogicalProcessors() {
        return this.logicalProcessors;
    }

    @Override
    public List<PhysicalProcessor> getPhysicalProcessors() {
        return this.physicalProcessors;
    }

    @Override
    public List<ProcessorCache> getProcessorCaches() {
        return this.processorCaches;
    }

    @Override
    public List<String> getFeatureFlags() {
        return this.featureFlags;
    }

    @Override
    public long[] getSystemCpuLoadTicks() {
        return systemCpuLoadTicks.get();
    }

    /**
     * Get the system CPU load ticks
     *
     * @return The system CPU load ticks
     */
    protected abstract long[] querySystemCpuLoadTicks();

    @Override
    public long[][] getProcessorCpuLoadTicks() {
        return processorCpuLoadTicks.get();
    }

    /**
     * Get the processor CPU load ticks
     *
     * @return The processor CPU load ticks
     */
    protected abstract long[][] queryProcessorCpuLoadTicks();

    @Override
    public double getSystemCpuLoadBetweenTicks(long[] oldTicks) {
        if (oldTicks.length != TickType.values().length) {
            throw new IllegalArgumentException("Provided tick array length " + oldTicks.length + " should have "
                    + TickType.values().length + " elements");
        }
        return getSystemCpuLoadBetweenTicks(oldTicks, getSystemCpuLoadTicks());
    }

    @Override
    public double getSystemCpuLoadBetweenTicks(long[] oldTicks, long[] ticks) {
        if (oldTicks.length != TickType.values().length || ticks.length != TickType.values().length) {
            throw new IllegalArgumentException("Tick arrays must both have " + TickType.values().length
                    + " elements, but were " + oldTicks.length + " and " + ticks.length + ".");
        }
        // Calculate total
        long total = 0;
        for (int i = 0; i < ticks.length; i++) {
            total += ticks[i] - oldTicks[i];
        }
        // Calculate idle from difference in idle and IOwait
        long idle = ticks[TickType.IDLE.getIndex()] + ticks[TickType.IOWAIT.getIndex()]
                - oldTicks[TickType.IDLE.getIndex()] - oldTicks[TickType.IOWAIT.getIndex()];
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

        return total > 0 ? (double) (total - idle) / total : 0d;
    }

    @Override
    public double[] getProcessorCpuLoadBetweenTicks(long[][] oldTicks) {
        return getProcessorCpuLoadBetweenTicks(oldTicks, getProcessorCpuLoadTicks());
    }

    @Override
    public double[] getProcessorCpuLoadBetweenTicks(long[][] oldTicks, long[][] ticks) {
        if (oldTicks.length != ticks.length || oldTicks[0].length != TickType.values().length) {
            throw new IllegalArgumentException("Provided tick array length " + oldTicks.length + " should be "
                    + ticks.length + ", each subarray having " + TickType.values().length + " elements");
        }
        double[] load = new double[ticks.length];
        for (int cpu = 0; cpu < ticks.length; cpu++) {
            long total = 0;
            for (int i = 0; i < ticks[cpu].length; i++) {
                total += ticks[cpu][i] - oldTicks[cpu][i];
            }
            // Calculate idle from difference in idle and IOwait
            long idle = ticks[cpu][TickType.IDLE.getIndex()] + ticks[cpu][TickType.IOWAIT.getIndex()]
                    - oldTicks[cpu][TickType.IDLE.getIndex()] - oldTicks[cpu][TickType.IOWAIT.getIndex()];
            LOG.trace("CPU: {}  Total ticks: {}  Idle ticks: {}", cpu, total, idle);
            // update
            load[cpu] = total > 0 && idle >= 0 ? (double) (total - idle) / total : 0d;
        }
        return load;
    }

    @Override
    public int getLogicalProcessorCount() {
        return this.logicalProcessorCount;
    }

    @Override
    public int getPhysicalProcessorCount() {
        return this.physicalProcessorCount;
    }

    @Override
    public int getPhysicalPackageCount() {
        return this.physicalPackageCount;
    }

    /**
     * The x86 CPUID feature flags carried in the EDX word of the processor ID, with the bit each one sets. Bits 42 and
     * 52 are reserved by the specification, which is why the numbering skips them. A constant with more than one name
     * is a feature that platforms report under different spellings, and those names share a bit.
     */
    private enum CpuidFeature {
        FPU(32, "fpu"), VME(33, "vme"), DE(34, "de"), PSE(35, "pse"), TSC(36, "tsc"), MSR(37, "msr"), PAE(38, "pae"),
        MCE(39, "mce"), CX8(40, "cx8"), APIC(41, "apic"), SEP(43, "sep"), MTRR(44, "mtrr"), PGE(45, "pge"),
        MCA(46, "mca"), CMOV(47, "cmov"), PAT(48, "pat"), PSE36(49, "pse-36", "pse36"), PSN(50, "psn"),
        CLFSH(51, "clfsh", "clflush"), DS(53, "ds"), ACPI(54, "acpi"), MMX(55, "mmx"), FXSR(56, "fxsr"), SSE(57, "sse"),
        SSE2(58, "sse2"), SS(59, "ss"), HTT(60, "htt", "ht"), TM(61, "tm"), IA64(62, "ia64"), PBE(63, "pbe");

        private final int bit;
        private final String[] names;

        CpuidFeature(int bit, String... names) {
            this.bit = bit;
            this.names = names;
        }
    }

    /**
     * Feature flag name to the bit it sets. Keyed by string because the names come from platform text such as
     * {@code /proc/cpuinfo}, so an {@code EnumMap} would be the wrong direction.
     */
    private static final Map<String, Integer> CPUID_FEATURE_BITS = mapCpuidFeatureBits();

    private static Map<String, Integer> mapCpuidFeatureBits() {
        Map<String, Integer> bits = new HashMap<>();
        for (CpuidFeature feature : CpuidFeature.values()) {
            for (String name : feature.names) {
                bits.put(name, feature.bit);
            }
        }
        return Collections.unmodifiableMap(bits);
    }

    /**
     * Creates a Processor ID by encoding the stepping, model, family, and feature flags.
     *
     * @param stepping The CPU stepping
     * @param model    The CPU model
     * @param family   The CPU family
     * @param flags    A space-delimited list of CPU feature flags
     * @return The Processor ID string
     */
    protected static String createProcessorID(String stepping, String model, String family, String[] flags) {
        return createProcessorID(stepping, model, family, flags, 0L);
    }

    /**
     * Creates a Processor ID by encoding the stepping, model, family, and feature flags.
     *
     * @param stepping The CPU stepping
     * @param model    The CPU model
     * @param family   The CPU family
     * @param flags    A space-delimited list of CPU feature flags
     * @param hwcap    Hardware capabilities from the auxiliary vector, or 0 if unavailable
     * @return The Processor ID string
     */
    protected static String createProcessorID(String stepping, String model, String family, String[] flags,
            long hwcap) {
        long processorIdBytes = 0L;
        long steppingL = ParseUtil.parseLongOrDefault(stepping, 0L);
        long modelL = ParseUtil.parseLongOrDefault(model, 0L);
        long familyL = ParseUtil.parseLongOrDefault(family, 0L);
        // 3:0 – Stepping
        processorIdBytes |= steppingL & 0xf;
        // 19:16,7:4 – Model
        processorIdBytes |= (modelL & 0xf) << 4;
        processorIdBytes |= (modelL & 0xf0) << 12; // shift high 4 bits
        // 27:20,11:8 – Family
        processorIdBytes |= (familyL & 0xf) << 8;
        processorIdBytes |= (familyL & 0xff0) << 16; // shift high 8 bits
        // 13:12 – Processor Type, assume 0
        if (hwcap != 0) {
            processorIdBytes |= hwcap << 32;
        } else {
            for (String flag : flags) {
                Integer bit = CPUID_FEATURE_BITS.get(flag);
                if (bit != null) {
                    processorIdBytes |= 1L << bit;
                }
            }
        }
        return String.format(Locale.ROOT, "%016X", processorIdBytes);
    }

    /**
     * Creates a list of physical processors from dmesg output.
     *
     * @param logProcs the list of logical processors
     * @param dmesg    a map of physical processor numbers to their dmesg identification strings
     * @return a list of physical processors
     */
    protected List<PhysicalProcessor> createProcListFromDmesg(List<LogicalProcessor> logProcs,
            Map<Integer, String> dmesg) {
        // Check if multiple CPU types
        boolean isHybrid = dmesg.values().stream().distinct().count() > 1;
        List<PhysicalProcessor> physProcs = new ArrayList<>();
        Set<Integer> pkgCoreKeys = new HashSet<>();
        for (LogicalProcessor logProc : logProcs) {
            int pkgId = logProc.getPhysicalPackageNumber();
            int coreId = logProc.getPhysicalProcessorNumber();
            int pkgCoreKey = (pkgId << 16) + coreId;
            if (!pkgCoreKeys.contains(pkgCoreKey)) {
                pkgCoreKeys.add(pkgCoreKey);
                String idStr = dmesg.getOrDefault(logProc.getProcessorNumber(), "");
                int efficiency = 0;
                // ARM v8 big.LITTLE chips just use the # for efficiency class
                // High-performance CPU (big): Cortex-A73, Cortex-A75, Cortex-A76
                // High-efficiency CPU (LITTLE): Cortex-A53, Cortex-A55
                if (isHybrid && ((idStr.startsWith("ARM Cortex") && ParseUtil.getFirstIntValue(idStr) >= 70)
                        || (idStr.startsWith("Apple")
                                && (idStr.contains("Firestorm") || (idStr.contains("Avalanche")))))) {
                    efficiency = 1;
                }
                physProcs.add(new PhysicalProcessor(pkgId, coreId, efficiency, idStr));
            }
        }
        physProcs.sort(Comparator.comparingInt(PhysicalProcessor::getPhysicalPackageNumber)
                .thenComparingInt(PhysicalProcessor::getPhysicalProcessorNumber));
        return physProcs;
    }

    /**
     * Filters a set of processor caches to an ordered list
     *
     * @param caches A set of unique caches.
     * @return A list sorted by level (desc), type, and size (desc)
     */
    // Type.ordinal() is intentional: declaration order (UNIFIED, INSTRUCTION, DATA) defines the sort order
    @SuppressWarnings("EnumOrdinal")
    public static List<ProcessorCache> orderedProcCaches(Set<ProcessorCache> caches) {
        return caches.stream().sorted(Comparator.comparing(
                c -> -1000 * c.getLevel() + 100 * c.getType().ordinal() - Integer.highestOneBit(c.getCacheSize())))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getProcessorIdentifier().getName());
        sb.append("\n ").append(getPhysicalPackageCount()).append(" physical CPU package(s)");
        sb.append("\n ").append(getPhysicalProcessorCount()).append(" physical CPU core(s)");
        Map<Integer, Integer> efficiencyCount = new HashMap<>();
        int maxEfficiency = 0;
        for (PhysicalProcessor cpu : getPhysicalProcessors()) {
            int eff = cpu.getEfficiency();
            efficiencyCount.merge(eff, 1, Integer::sum);
            if (eff > maxEfficiency) {
                maxEfficiency = eff;
            }
        }
        int pCores = efficiencyCount.getOrDefault(maxEfficiency, 0);
        int eCores = getPhysicalProcessorCount() - pCores;
        if (eCores > 0) {
            sb.append(" (").append(pCores).append(" performance + ").append(eCores).append(" efficiency)");
        }
        sb.append("\n ").append(getLogicalProcessorCount()).append(" logical CPU(s)");
        sb.append('\n').append("Identifier: ").append(getProcessorIdentifier().getIdentifier());
        sb.append('\n').append("ProcessorID: ").append(getProcessorIdentifier().getProcessorID());
        sb.append('\n').append("Microarchitecture: ").append(getProcessorIdentifier().getMicroarchitecture());
        return sb.toString();
    }
}
