/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.FileUtil;
import oshi.util.tuples.Pair;

class MacCentralProcessorTest {

    @Test
    void testParseX86FeatureFlagsWithFeatures() {
        Map<String, String> strings = new HashMap<>();
        strings.put("machdep.cpu.brand_string", "Test CPU");
        strings.put("machdep.cpu.features", "FPU VME SSE SSE2 SSE3");
        strings.put("machdep.cpu.extfeatures", "LAHF EM64T");
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(strings);

        List<String> flags = cpu.parseX86FeatureFlags();
        assertThat(flags, hasSize(2));
        assertThat(flags.get(0), is("machdep.cpu.features: FPU VME SSE SSE2 SSE3"));
        assertThat(flags.get(1), is("machdep.cpu.extfeatures: LAHF EM64T"));
    }

    @Test
    void testParseX86FeatureFlagsEmpty() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        List<String> flags = cpu.parseX86FeatureFlags();
        assertThat(flags, is(empty()));
    }

    @Test
    void testQueryProcessorIdX86() {
        // A non-Apple brand_string routes queryProcessorId into the x86 (Intel) identity branch, which reads
        // machdep.cpu.* sysctls directly. Representative Intel values (Core 2 Duo, Penryn family 6 model 23).
        Map<String, String> strings = new HashMap<>();
        strings.put("machdep.cpu.brand_string", "Intel(R) Core(TM)2 Duo CPU L9400 @ 1.86GHz");
        strings.put("machdep.cpu.vendor", "GenuineIntel");
        Map<String, Integer> ints = new HashMap<>();
        ints.put("machdep.cpu.family", 6);
        ints.put("machdep.cpu.model", 23);
        ints.put("machdep.cpu.stepping", 10);
        ints.put("machdep.cpu.signature", 0x000106A5);
        Map<String, Long> longs = new HashMap<>();
        longs.put("machdep.cpu.feature_bits", 0xBFEBFBFFL);
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(strings, ints, longs);

        CentralProcessor.ProcessorIdentifier id = cpu.queryProcessorId();
        assertThat(id.getVendor(), is("GenuineIntel"));
        assertThat(id.getName(), is("Intel(R) Core(TM)2 Duo CPU L9400 @ 1.86GHz"));
        assertThat(id.getFamily(), is("6"));
        assertThat(id.getModel(), is("23"));
        assertThat(id.getStepping(), is("10"));
        assertThat(id.isCpu64bit(), is(true));
        // processorID packs the signature in the low word and the feature bits in the high word
        assertThat(id.getProcessorID(), is(String.format(Locale.ROOT, "%016x", 0x000106A5L | (0xBFEBFBFFL << 32))));
    }

    @Test
    void testQuerySystemCpuLoadTicks() {
        // Mach cpu_ticks order: user, system, idle, nice
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[] { 10, 30, 50, 20 },
                new int[0], new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        assertThat(ticks[TickType.USER.getIndex()], is(10L));
        assertThat(ticks[TickType.NICE.getIndex()], is(20L));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(30L));
        assertThat(ticks[TickType.IDLE.getIndex()], is(50L));
    }

    @Test
    void testQuerySystemCpuLoadTicksShortArrayLeavesZeros() {
        // Fewer than CPU_STATE_MAX values: mapping is skipped, ticks stay zero
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[] { 10, 30, 50 }, new int[0],
                new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testQueryProcessorCpuLoadTicks() {
        // One processor (the stub reports a single logical processor): user, system, idle, nice
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0],
                new int[] { 10, 30, 50, 20 }, new double[0], 0);
        long[][] ticks = cpu.queryProcessorCpuLoadTicks();
        assertThat(ticks.length, is(1));
        assertThat(ticks[0][TickType.USER.getIndex()], is(10L));
        assertThat(ticks[0][TickType.SYSTEM.getIndex()], is(30L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(50L));
        assertThat(ticks[0][TickType.NICE.getIndex()], is(20L));
    }

    @Test
    void testQueryProcessorCpuLoadTicksCapsExtraProcessors() {
        // Native reports two processors but only one is expected: mapping caps to the expected count without error
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0],
                new int[] { 10, 30, 50, 20, 11, 31, 51, 21 }, new double[0], 0);
        long[][] ticks = cpu.queryProcessorCpuLoadTicks();
        assertThat(ticks.length, is(1));
        assertThat(ticks[0][TickType.USER.getIndex()], is(10L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(50L));
    }

    @Test
    void testLoadAverageRejectsBelowOne() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> cpu.getSystemLoadAverage(0));
    }

    @Test
    void testLoadAverageRejectsAboveThree() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> cpu.getSystemLoadAverage(4));
    }

    @Test
    void testLoadAveragePreservesFullResult() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0], new int[0],
                new double[] { 1.0, 5.0, 15.0 }, 3);
        double[] avg = cpu.getSystemLoadAverage(3);
        assertThat(avg[0], is(1.0));
        assertThat(avg[1], is(5.0));
        assertThat(avg[2], is(15.0));
    }

    @Test
    void testLoadAveragePartialResultFillsNegative() {
        // Native call returns fewer samples than requested: all-or-nothing, whole array becomes -1
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>(), new int[0], new int[0],
                new double[] { 1.0, 5.0, 15.0 }, 2);
        double[] avg = cpu.getSystemLoadAverage(3);
        assertThat(avg[0], is(-1.0));
        assertThat(avg[1], is(-1.0));
        assertThat(avg[2], is(-1.0));
    }

    // -- Apple Silicon cluster frequencies --

    @Test
    void testArmMaxFreqWithoutFirstQueryingTheProcessorIdentifier() {
        // The nominal frequencies were once read as a side effect of queryProcessorId(), so a caller asking for a
        // frequency before the identifier was answered with the DEFAULT_FREQUENCY placeholder instead.
        StubArmCentralProcessor cpu = new StubArmCentralProcessor();
        assertThat(cpu.queryMaxFreq(), is(StubArmCentralProcessor.PERF_FREQ));
    }

    @Test
    void testArmCurrentFreqIsPerCluster() {
        StubArmCentralProcessor cpu = new StubArmCentralProcessor();
        long[] freqs = cpu.queryCurrentFreq();
        assertThat(freqs.length, is(8));
        for (int i = 0; i < 4; i++) {
            assertThat("efficiency core " + i, freqs[i], is(StubArmCentralProcessor.EFF_FREQ));
        }
        for (int i = 4; i < 8; i++) {
            assertThat("performance core " + i, freqs[i], is(StubArmCentralProcessor.PERF_FREQ));
        }
    }

    @Test
    void testNominalFrequenciesAreQueriedOnceForAllThreeConsumers() {
        StubArmCentralProcessor cpu = new StubArmCentralProcessor();
        cpu.queryMaxFreq();
        cpu.queryCurrentFreq();
        assertThat("vendor frequency", cpu.queryProcessorId().getVendorFreq(), is(StubArmCentralProcessor.PERF_FREQ));
        assertThat("IORegistry walks", cpu.nominalFrequencyQueries(), is(1));
    }

    // -- efficiency class derivation --

    // Builds a core-properties map from parallel arrays, where a null element means the property is absent.
    private static Map<Integer, Pair<String, String>> coreProps(String[] codenames, String[] clusterTypes) {
        Map<Integer, Pair<String, String>> props = new HashMap<>();
        for (int i = 0; i < codenames.length; i++) {
            String compatible = codenames[i] == null ? null : "apple," + codenames[i] + " arm,v8";
            props.put(i, new Pair<>(compatible, clusterTypes[i]));
        }
        return props;
    }

    private static List<Integer> coreKeys(int count) {
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add(i);
        }
        return keys;
    }

    private static void assertClasses(String message, Map<Integer, Integer> actual, int... expected) {
        assertThat(message + " size", actual.size(), is(expected.length));
        for (int i = 0; i < expected.length; i++) {
            assertThat(message + " core " + i, actual.get(i), is(expected[i]));
        }
    }

    private static final String[] M2_MAX_CODENAMES = { "blizzard", "blizzard", "blizzard", "blizzard", "avalanche",
            "avalanche", "avalanche", "avalanche", "avalanche", "avalanche", "avalanche", "avalanche" };
    private static final String[] M2_MAX_CLUSTERS = { "E", "E", "E", "E", "P", "P", "P", "P", "P", "P", "P", "P" };
    private static final String[] M1_CODENAMES = { "icestorm", "icestorm", "icestorm", "icestorm", "firestorm",
            "firestorm", "firestorm", "firestorm" };
    private static final String[] M1_CLUSTERS = { "E", "E", "E", "E", "P", "P", "P", "P" };

    @Test
    void testClusterTypeClassifiesM2Max() {
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(12),
                coreProps(M2_MAX_CODENAMES, M2_MAX_CLUSTERS), 8);
        assertClasses("M2 Max", classes, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void testClusterTypeClassifiesM1EvenSplit() {
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, M1_CLUSTERS), 4);
        assertClasses("M1", classes, 0, 0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testCodenameFallbackWhenClusterTypeAbsent() {
        // Without cluster-type the codename groups decide, ordered by lowest core number. The M1's 4+4 split makes
        // the perf level count ambiguous, so ordering alone must still classify it correctly.
        Map<Integer, Integer> m1 = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, new String[8]), 4);
        assertClasses("M1 without cluster-type", m1, 0, 0, 0, 0, 1, 1, 1, 1);

        Map<Integer, Integer> m2 = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(12),
                coreProps(M2_MAX_CODENAMES, new String[12]), 8);
        assertClasses("M2 Max without cluster-type", m2, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void testPerfLevelCountOverridesCoreOrdering() {
        // A hypothetical chip whose performance cores occupy the low core numbers. The unambiguous
        // hw.perflevel0.physicalcpu count must win over the ordering assumption.
        String[] codenames = { "hydra", "hydra", "coyote", "coyote", "coyote", "coyote" };
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(6),
                coreProps(codenames, new String[6]), 2);
        assertClasses("perf level count wins", classes, 1, 1, 0, 0, 0, 0);
    }

    @Test
    void testPartialClusterTypeIsPropagatedByCodename() {
        // Only two cores report a cluster type; the rest are classified by sharing a codename with them.
        String[] clusters = new String[12];
        clusters[0] = "E";
        clusters[4] = "P";
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(12),
                coreProps(M2_MAX_CODENAMES, clusters), 0);
        assertClasses("propagated", classes, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void testPartialClusterTypeIsKeptWhenOtherCoresHaveNoProperties() {
        // Some cores report a cluster-type and a codename while others expose no readable properties at all, as
        // happens when an IORegistry node cannot be read. The exact readings must survive: discarding them here
        // classified every performance core as class 0, which is worse than the codename table this replaced.
        Map<Integer, Pair<String, String>> props = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            props.put(i, new Pair<>("apple,everest arm,v8", "P"));
        }
        for (int i = 4; i < 8; i++) {
            props.put(i, new Pair<>(null, null));
        }
        assertClasses("partial properties", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8), props, 0), 1, 1, 1,
                1, 0, 0, 0, 0);
    }

    @Test
    void testOwnClusterTypeIsNotOverwrittenByPropagation() {
        // A core that reported its own cluster-type must keep that exact reading. Two cores here share a codename but
        // report different cluster types, so propagating by codename must not overwrite either one's own value.
        Map<Integer, Pair<String, String>> props = new HashMap<>();
        props.put(0, new Pair<>("apple,everest arm,v8", "E"));
        props.put(1, new Pair<>("apple,everest arm,v8", "P"));
        props.put(2, new Pair<>("apple,everest arm,v8", null));
        props.put(3, new Pair<>("apple,everest arm,v8", null));
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4), props, 0);
        assertThat("core 0 keeps its own E reading", classes.get(0), is(0));
        assertThat("core 1 keeps its own P reading", classes.get(1), is(1));
    }

    @Test
    void testClusterTypeWithoutCodename() {
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(new String[8], M1_CLUSTERS), 0);
        assertClasses("cluster-type only", classes, 0, 0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testNoPropertiesYieldsAllZero() {
        assertClasses("no properties",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4), coreProps(new String[4], new String[4]), 0), 0,
                0, 0, 0);
        assertClasses("empty map", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4), new HashMap<>(), 0), 0, 0,
                0, 0);
        assertThat("no cores", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(0), new HashMap<>(), 0).size(),
                is(0));
    }

    @Test
    void testHomogeneousCoresAreAllClassZero() {
        String[] codenames = { "firestorm", "firestorm", "firestorm", "firestorm" };
        assertClasses("homogeneous",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4), coreProps(codenames, new String[4]), 4), 0, 0,
                0, 0);
    }

    @Test
    void testUnrecognizedClusterTypeFallsThroughToCodenames() {
        String[] clusters = { "X", "X", "X", "X", "X", "X", "X", "X" };
        assertClasses("unrecognized letter",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8), coreProps(M1_CODENAMES, clusters), 4), 0, 0, 0,
                0, 1, 1, 1, 1);
    }

    @Test
    void testFutureChipIsClassifiedWithoutATable() {
        // The point of the change: codenames absent from every hardcoded list still classify correctly.
        String[] codenames = { "coyote", "coyote", "coyote", "coyote", "hydra", "hydra", "hydra", "hydra" };
        String[] clusters = { "E", "E", "E", "E", "P", "P", "P", "P" };
        assertClasses("future chip",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8), coreProps(codenames, clusters), 4), 0, 0, 0, 0,
                1, 1, 1, 1);
    }

    // -- microarchitecture derivation --

    // Builds physical processors from parallel codename and efficiency arrays, as initProcessorCounts does.
    private static List<PhysicalProcessor> processors(String[] codenames, int[] efficiencies) {
        List<PhysicalProcessor> procs = new ArrayList<>();
        for (int i = 0; i < codenames.length; i++) {
            String idString = codenames[i] == null ? "" : "apple," + codenames[i] + " arm,v8";
            procs.add(new PhysicalProcessor(0, i, efficiencies[i], idString));
        }
        return procs;
    }

    @Test
    void testMicroarchitectureReproducesTheArchitectureTable() {
        // The strongest available check: the derived string must equal the entry already shipped for that chip, so
        // a chip absent from the table is described in exactly the same style as one present in it.
        Properties archProps = FileUtil.readPropertiesFromFilename("oshi.architecture.properties");
        // Without this the comparisons below would pass vacuously if the table could not be read, since a missing
        // property and a failed derivation are both null.
        String m1Expected = archProps.getProperty("apple.0x1b588bb3");
        String m2Expected = archProps.getProperty("apple.0xda33d83d");
        String m3Expected = archProps.getProperty("apple.0x8765edea");
        for (String expected : new String[] { m1Expected, m2Expected, m3Expected }) {
            assertThat("Architecture table entry must be present to compare against", expected, is(notNullValue()));
        }

        int[] m1Efficiency = { 0, 0, 0, 0, 1, 1, 1, 1 };
        assertThat("M1", MacCentralProcessor.deriveMicroarchitecture(processors(M1_CODENAMES, m1Efficiency)),
                is(m1Expected));
        int[] m2Efficiency = { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 };
        assertThat("M2", MacCentralProcessor.deriveMicroarchitecture(processors(M2_MAX_CODENAMES, m2Efficiency)),
                is(m2Expected));
        String[] m3Codenames = { "sawtooth", "sawtooth", "everest", "everest" };
        assertThat("M3", MacCentralProcessor.deriveMicroarchitecture(processors(m3Codenames, new int[] { 0, 0, 1, 1 })),
                is(m3Expected));
    }

    @Test
    void testMicroarchitectureOfAFutureChip() {
        String[] codenames = { "coyote", "coyote", "hydra", "hydra" };
        assertThat(MacCentralProcessor.deriveMicroarchitecture(processors(codenames, new int[] { 0, 0, 1, 1 })),
                is("ARM64 SoC: Hydra + Coyote"));
    }

    @Test
    void testMicroarchitectureOfHomogeneousAndMultiTierChips() {
        String[] single = { "firestorm", "firestorm" };
        assertThat("single codename joins nothing",
                MacCentralProcessor.deriveMicroarchitecture(processors(single, new int[] { 0, 0 })),
                is("ARM64 SoC: Firestorm"));
        String[] three = { "low", "mid", "high" };
        assertThat("ordered by descending efficiency class",
                MacCentralProcessor.deriveMicroarchitecture(processors(three, new int[] { 0, 1, 2 })),
                is("ARM64 SoC: High + Mid + Low"));
    }

    @Test
    void testMicroarchitectureIsNullWithoutAppleCodenames() {
        assertThat("no processors", MacCentralProcessor.deriveMicroarchitecture(new ArrayList<>()), is(nullValue()));
        assertThat("blank id strings",
                MacCentralProcessor.deriveMicroarchitecture(processors(new String[2], new int[] { 0, 0 })),
                is(nullValue()));
        List<PhysicalProcessor> intel = new ArrayList<>();
        intel.add(new PhysicalProcessor(0, 0, 0, "Intel Core i7"));
        assertThat("non-Apple id string", MacCentralProcessor.deriveMicroarchitecture(intel), is(nullValue()));
    }

    /**
     * Minimal stub providing sysctl values and raw Mach CPU-tick / load-average values without native calls. The
     * abstract superclass eagerly derives processor counts during construction, so the stub reports a single logical
     * processor; per-processor tests are written against that.
     */
    static class StubMacCentralProcessor extends MacCentralProcessor {

        private final Map<String, Integer> sysctlInts = new HashMap<>();
        private final Map<String, Long> sysctlLongs = new HashMap<>();
        private final Map<String, String> sysctlStrings;
        private final int[] hostCpuTicks;
        private final int[] processorCpuTicks;
        private final double[] loadavgValues;
        private final int loadavgRetval;

        StubMacCentralProcessor(Map<String, String> extraStrings) {
            this(extraStrings, new int[0], new int[0], new double[0], 0);
        }

        StubMacCentralProcessor(Map<String, String> extraStrings, Map<String, Integer> extraInts,
                Map<String, Long> extraLongs) {
            this(extraStrings, new int[0], new int[0], new double[0], 0);
            sysctlInts.putAll(extraInts);
            sysctlLongs.putAll(extraLongs);
        }

        StubMacCentralProcessor(Map<String, String> extraStrings, int[] hostCpuTicks, int[] processorCpuTicks,
                double[] loadavgValues, int loadavgRetval) {
            Map<String, String> defaults = new HashMap<>();
            defaults.put("machdep.cpu.brand_string", "Test CPU");
            defaults.putAll(extraStrings);
            this.sysctlStrings = defaults;
            sysctlInts.put("hw.logicalcpu", 1);
            sysctlInts.put("hw.physicalcpu", 1);
            sysctlInts.put("hw.packages", 1);
            sysctlInts.put("hw.cpu64bit_capable", 1);
            this.hostCpuTicks = hostCpuTicks;
            this.processorCpuTicks = processorCpuTicks;
            this.loadavgValues = loadavgValues;
            this.loadavgRetval = loadavgRetval;
        }

        @Override
        protected int sysctlInt(String name, int def) {
            return sysctlInts == null ? def : sysctlInts.getOrDefault(name, def);
        }

        @Override
        protected int sysctlIntNoWarn(String name, int def) {
            return sysctlInts == null ? def : sysctlInts.getOrDefault(name, def);
        }

        @Override
        protected long sysctlLong(String name, long def) {
            return sysctlLongs == null ? def : sysctlLongs.getOrDefault(name, def);
        }

        @Override
        protected String sysctlString(String name, String def) {
            return sysctlStrings == null ? def : sysctlStrings.getOrDefault(name, def);
        }

        @Override
        protected String sysctlStringNoWarn(String name, String def) {
            return sysctlStrings == null ? def : sysctlStrings.getOrDefault(name, def);
        }

        @Override
        protected SysctlProvider sysctlProvider() {
            return null; // Not used; sysctl methods are overridden directly
        }

        @Override
        protected IOKitProvider ioKitProvider() {
            return null; // Not used; platformExpert/queryCoreProperties/queryNominalFrequencies are overridden
        }

        @Override
        protected String platformExpert() {
            return "Apple";
        }

        @Override
        protected Map<Integer, Pair<String, String>> queryCoreProperties() {
            return new HashMap<>();
        }

        @Override
        protected Pair<Long, Long> queryNominalFrequencies() {
            return new Pair<>(DEFAULT_FREQUENCY, DEFAULT_FREQUENCY);
        }

        @Override
        protected int[] queryHostCpuLoadTicks() {
            return hostCpuTicks;
        }

        @Override
        protected int[] queryProcessorCpuTicks() {
            return processorCpuTicks;
        }

        @Override
        protected int getloadavgNative(double[] loadavg, int nelem) {
            for (int i = 0; i < nelem && i < loadavgValues.length; i++) {
                loadavg[i] = loadavgValues[i];
            }
            return loadavgRetval;
        }

        @Override
        public long queryContextSwitches() {
            return 0L;
        }

        @Override
        public long queryInterrupts() {
            return 0L;
        }
    }

    /**
     * An Apple Silicon variant of the stub: four efficiency plus four performance cores, with the two cluster
     * frequencies standing in for the {@code pmgr} voltage-state tables. The core count and cluster types must come
     * from constants rather than instance fields, because the superclass derives the processor topology during
     * construction, before any subclass field is assigned.
     */
    static class StubArmCentralProcessor extends StubMacCentralProcessor {

        static final long PERF_FREQ = 4_056_000_000L;
        static final long EFF_FREQ = 2_748_000_000L;
        private static final int CORE_COUNT = 8;

        private final AtomicInteger nominalFrequencyQueries = new AtomicInteger();

        StubArmCentralProcessor() {
            super(Collections.singletonMap("machdep.cpu.brand_string", "Apple M3 Pro"));
        }

        @Override
        protected int sysctlInt(String name, int def) {
            if ("hw.logicalcpu".equals(name) || "hw.physicalcpu".equals(name)) {
                return CORE_COUNT;
            }
            return super.sysctlInt(name, def);
        }

        @Override
        protected Map<Integer, Pair<String, String>> queryCoreProperties() {
            Map<Integer, Pair<String, String>> props = new HashMap<>();
            for (int i = 0; i < CORE_COUNT; i++) {
                boolean performance = i >= CORE_COUNT / 2;
                props.put(i, new Pair<>(performance ? "apple,everest arm,v8" : "apple,sawtooth arm,v8",
                        performance ? "P" : "E"));
            }
            return props;
        }

        @Override
        protected Pair<Long, Long> queryNominalFrequencies() {
            nominalFrequencyQueries.incrementAndGet();
            return new Pair<>(PERF_FREQ, EFF_FREQ);
        }

        int nominalFrequencyQueries() {
            return nominalFrequencyQueries.get();
        }
    }
}
