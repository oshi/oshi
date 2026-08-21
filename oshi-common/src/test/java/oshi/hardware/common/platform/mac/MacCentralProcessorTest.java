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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
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

    // -- voltage state table units --

    // Builds a pmgr voltage state table: 8-byte entries of little-endian (frequency, millivolts), ascending.
    private static byte[] voltageStates(long... frequencies) {
        byte[] data = new byte[frequencies.length * 8];
        for (int i = 0; i < frequencies.length; i++) {
            for (int b = 0; b < 4; b++) {
                data[i * 8 + b] = (byte) (frequencies[i] >> (8 * b));
                data[i * 8 + 4 + b] = (byte) (1000 >> (8 * b)); // millivolts, never read
            }
        }
        return data;
    }

    // Builds a pmgr acc-clusters property from alternating voltage state table number and cluster tier.
    private static byte[] accClusters(int... tableAndTier) {
        byte[] data = new byte[tableAndTier.length / 2 * 8];
        for (int i = 0; i < tableAndTier.length; i += 2) {
            data[i / 2 * 8] = (byte) tableAndTier[i];
            data[i / 2 * 8 + 1] = (byte) tableAndTier[i + 1];
        }
        return data;
    }

    // The last entry of a real pmgr voltage state table on each of these chips. The M3 Pro, M3 Ultra and A18 Pro tables
    // are in Hz; the M4 and M5 Max tables report the same kind of value in kHz.
    private static final long M3_PRO_PERFORMANCE_HZ = 4_056_000_000L;
    private static final long M3_ULTRA_EFFICIENCY_HZ = 2_568_000_000L;
    private static final long A18_PRO_PERFORMANCE_HZ = 4_044_000_000L;
    private static final long M4_PERFORMANCE_KHZ = 4_464_000L;
    private static final long M4_EFFICIENCY_KHZ = 2_892_000L;
    private static final long M5_MAX_SUPER_KHZ = 4_608_000L;
    private static final long M5_MAX_PERFORMANCE_KHZ = 4_380_000L;

    // The complete voltage state tables of a real M3 Pro (Mac15,7), read from pmgr's voltage-states1-sram and
    // voltage-states5-sram. Their lengths are what the IOReport performance state counts have to match.
    static final long[] M3_PRO_EFFICIENCY_TABLE = { 744_000_000L, 1_044_000_000L, 1_476_000_000L, 2_004_000_000L,
            2_268_000_000L, 2_448_000_000L, 2_640_000_000L, 2_748_000_000L };
    static final long[] M3_PRO_PERFORMANCE_TABLE = { 696_000_000L, 1_092_000_000L, 1_356_000_000L, 1_596_000_000L,
            1_884_000_000L, 2_172_000_000L, 2_424_000_000L, 2_616_000_000L, 2_808_000_000L, 2_988_000_000L,
            3_144_000_000L, 3_288_000_000L, 3_420_000_000L, 3_576_000_000L, 3_624_000_000L, 3_708_000_000L,
            3_780_000_000L, 3_864_000_000L, 3_960_000_000L, 4_056_000_000L };

    @Test
    void testHertzTablesAreNotScaled() {
        assertThat("M3 Pro", MacCentralProcessor.toHz(M3_PRO_PERFORMANCE_HZ), is(4_056_000_000L));
        assertThat("M3 Ultra", MacCentralProcessor.toHz(M3_ULTRA_EFFICIENCY_HZ), is(2_568_000_000L));
        assertThat("A18 Pro", MacCentralProcessor.toHz(A18_PRO_PERFORMANCE_HZ), is(4_044_000_000L));
        // Both the M4 and the M5 Max publish their GPU table in Hz while their CPU tables are in kHz, so the unit
        // cannot be decided per chip
        assertThat("M4 GPU", MacCentralProcessor.toHz(1_578_000_000L), is(1_578_000_000L));
        assertThat("M5 Max GPU", MacCentralProcessor.toHz(1_620_000_000L), is(1_620_000_000L));
        assertThat("lowest maximum ever observed", MacCentralProcessor.toHz(338_000_000L), is(338_000_000L));
    }

    @Test
    void testKilohertzTablesAreScaled() {
        assertThat("M4 performance", MacCentralProcessor.toHz(M4_PERFORMANCE_KHZ), is(4_464_000_000L));
        assertThat("M4 efficiency", MacCentralProcessor.toHz(M4_EFFICIENCY_KHZ), is(2_892_000_000L));
        assertThat("M5 Max super", MacCentralProcessor.toHz(M5_MAX_SUPER_KHZ), is(4_608_000_000L));
    }

    @Test
    void testNonPositiveFrequenciesAreNotScaled() {
        assertThat("unreadable table", MacCentralProcessor.toHz(0L), is(0L));
        assertThat("negative", MacCentralProcessor.toHz(-1L), is(-1L));
    }

    @Test
    void testMaxFrequencyComesFromTheLastTableEntry() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        assertThat("last entry, scaled",
                cpu.getMaxFreqFromByteArray(voltageStates(1_308_000L, 2_400_000L, M5_MAX_SUPER_KHZ)),
                is(4_608_000_000L));
        assertThat("single entry", cpu.getMaxFreqFromByteArray(voltageStates(M3_PRO_PERFORMANCE_HZ)),
                is(M3_PRO_PERFORMANCE_HZ));
        assertThat("absent property", cpu.getMaxFreqFromByteArray(new byte[0]),
                is(MacCentralProcessor.DEFAULT_FREQUENCY));
        assertThat("truncated entry", cpu.getMaxFreqFromByteArray(new byte[7]),
                is(MacCentralProcessor.DEFAULT_FREQUENCY));
    }

    // -- voltage state table discovery --

    @Test
    void testAccClustersNamesTheVoltageStateTables() {
        // The M5 Max property: table 22 at tier 0, table 23 at tier 1, table 5 at tier 2
        assertArrayEquals(new int[] { 22, 23, 5 },
                MacCentralProcessor.parseClusterTables(accClusters(22, 0, 23, 1, 5, 2)), "M5 Max");
    }

    @Test
    void testAccClustersAreOrderedByTier() {
        assertArrayEquals(new int[] { 22, 23, 5 },
                MacCentralProcessor.parseClusterTables(accClusters(5, 2, 22, 0, 23, 1)), "listed out of tier order");
        // Two clusters of the same type share a tier, and must keep the order they were listed in
        assertArrayEquals(new int[] { 22, 23, 5 },
                MacCentralProcessor.parseClusterTables(accClusters(22, 0, 23, 0, 5, 1)), "tier shared by two clusters");
    }

    @Test
    void testAccClustersAbsentOrUnreadable() {
        assertArrayEquals(new int[0], MacCentralProcessor.parseClusterTables(null), "absent before the M5");
        assertArrayEquals(new int[0], MacCentralProcessor.parseClusterTables(new byte[7]), "shorter than one entry");
    }

    // The pmgr properties of a real M5 Max (Mac17,6, macOS 26.4, pmgr1,t6050), verbatim from the ioreg dump in
    // https://github.com/vladkens/macmon/issues/47. The builders above encode the layout this project assumes; these
    // decode the layout Apple actually publishes.
    private static final String M5_MAX_ACC_CLUSTERS = "160000000000000017010000000000000502000000000000";
    private static final String M5_MAX_TABLE_22 = "00821400fd020000e01519001b03000040651e003e030000002823005c030000"
            + "607728007a030000203a2d009d03000040703100b103000080773500ca030000"
            + "80653800e3030000a0243b00f7030000e0b43d000604000080b83f0015040000"
            + "605e41002404000020bc41002404000060d5420024040000";
    private static final String M5_MAX_TABLE_5 = "60f51300f802000020b818001103000060361e003403000020f9220057030000"
            + "205e27007503000000f22b0098030000e0853000b6030000405e3400d4030000"
            + "00aa3700f203000020693a00fc03000080ca3c0015040000409f3e0029040000"
            + "60e73f0033040000e0a2400033040000605e41003304000020bc410033040000"
            + "e019420033040000e09043003304000000d94400600400000050460060040000";
    private static final String M5_MAX_TABLE_9_GPU = "000000007d000000807825145302000080c5f71c760200000097e8259e020000"
            + "00ff712fdf02000000ceed34e902000000afe33a0203000000879c4025030000"
            + "80e373462f03000080bb2c4c5c0300008093e55184030000806b9e57b1030000" + "805e0e5efc030000003d8f601f040000";

    @Test
    void testRealAccClustersPropertyNamesTablesTwentyTwoTwentyThreeAndFive() {
        assertArrayEquals(new int[] { 22, 23, 5 },
                MacCentralProcessor.parseClusterTables(ParseUtil.hexStringToByteArray(M5_MAX_ACC_CLUSTERS)),
                "M5 Max acc-clusters as dumped");
    }

    @Test
    void testRealVoltageStateTablesDecodeToThePublishedMaxima() {
        StubMacCentralProcessor cpu = new StubMacCentralProcessor(new HashMap<>());
        // 15 entries ending 60d5420024040000: 0x0042d560 kHz at 1060 mV
        assertThat("M5 Max performance", cpu.getMaxFreqFromByteArray(ParseUtil.hexStringToByteArray(M5_MAX_TABLE_22)),
                is(4_380_000_000L));
        // 20 entries ending 0050460060040000: 0x00465000 kHz at 1120 mV
        assertThat("M5 Max super", cpu.getMaxFreqFromByteArray(ParseUtil.hexStringToByteArray(M5_MAX_TABLE_5)),
                is(4_608_000_000L));
        // And the GPU table on the same chip ends 003d8f601f040000: 0x608f3d00, already in Hz
        assertThat("M5 Max GPU", cpu.getMaxFreqFromByteArray(ParseUtil.hexStringToByteArray(M5_MAX_TABLE_9_GPU)),
                is(1_620_000_000L));
    }

    @Test
    void testRealM5MaxPropertiesYieldItsTwoCoreFrequencies() {
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("acc-clusters", ParseUtil.hexStringToByteArray(M5_MAX_ACC_CLUSTERS));
        pmgr.put("voltage-states22-sram", ParseUtil.hexStringToByteArray(M5_MAX_TABLE_22));
        // Tier 1 is the second performance cluster, whose table differs from tier 0's only in voltage
        pmgr.put("voltage-states23-sram", ParseUtil.hexStringToByteArray(M5_MAX_TABLE_22));
        pmgr.put("voltage-states5-sram", ParseUtil.hexStringToByteArray(M5_MAX_TABLE_5));
        pmgr.put("voltage-states9", ParseUtil.hexStringToByteArray(M5_MAX_TABLE_9_GPU));
        assertArrayEquals(new long[] { 4_380_000_000L, 4_608_000_000L },
                new PmgrCentralProcessor(pmgr).queryClusterFrequencies(),
                "three clusters and a GPU table yield two core frequencies");
    }

    @Test
    void testZeroFilledTableIsIgnoredRatherThanReportedAsZeroHertz() {
        // A table long enough to parse but holding no frequency must not become a cluster running at 0 Hz, and must
        // not count as a successful discovery either, or it would suppress the legacy tables below it.
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("acc-clusters", accClusters(22, 0, 23, 1));
        pmgr.put("voltage-states22-sram", new byte[24]);
        pmgr.put("voltage-states23-sram", voltageStates(M5_MAX_PERFORMANCE_KHZ));
        assertArrayEquals(new long[] { 4_380_000_000L }, new PmgrCentralProcessor(pmgr).queryClusterFrequencies(),
                "zero-filled table dropped");

        pmgr.put("voltage-states23-sram", new byte[8]);
        pmgr.put("voltage-states1-sram", voltageStates(StubArmCentralProcessor.EFF_FREQ));
        pmgr.put("voltage-states5-sram", voltageStates(M3_PRO_PERFORMANCE_HZ));
        assertArrayEquals(new long[] { StubArmCentralProcessor.EFF_FREQ, M3_PRO_PERFORMANCE_HZ },
                new PmgrCentralProcessor(pmgr).queryClusterFrequencies(), "every discovered table zero-filled");
    }

    @Test
    void testM4ClusterFrequenciesComeFromTheLegacyTables() {
        // The M4 publishes no acc-clusters, so the fixed table numbers still apply; only the unit changed
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("voltage-states1-sram", voltageStates(1_020_000L, 1_800_000L, M4_EFFICIENCY_KHZ));
        pmgr.put("voltage-states5-sram", voltageStates(1_260_000L, 3_336_000L, M4_PERFORMANCE_KHZ));
        assertArrayEquals(new long[] { 2_892_000_000L, 4_464_000_000L },
                new PmgrCentralProcessor(pmgr).queryClusterFrequencies(), "M4");
    }

    @Test
    void testM3UltraIgnoresItsSecondDiesDuplicateTable() {
        // Two fused dies give the M3 Ultra a voltage-states13-sram identical to its voltage-states5-sram. It publishes
        // no acc-clusters, so nothing names table 13 and the legacy pair alone is read.
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("voltage-states1-sram", voltageStates(1_020_000_000L, M3_ULTRA_EFFICIENCY_HZ));
        pmgr.put("voltage-states5-sram", voltageStates(1_260_000_000L, M3_PRO_PERFORMANCE_HZ));
        pmgr.put("voltage-states13-sram", voltageStates(1_260_000_000L, M3_PRO_PERFORMANCE_HZ));
        assertArrayEquals(new long[] { M3_ULTRA_EFFICIENCY_HZ, M3_PRO_PERFORMANCE_HZ },
                new PmgrCentralProcessor(pmgr).queryClusterFrequencies(), "M3 Ultra");
    }

    @Test
    void testM5MaxClusterFrequenciesComeFromAccClusters() {
        // voltage-states1-sram is gone, and acc-clusters names tables 22, 23 and 5. Its two performance clusters
        // publish identical tables, so three clusters yield the two frequencies its two core types run at.
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("acc-clusters", accClusters(22, 0, 23, 1, 5, 2));
        pmgr.put("voltage-states22-sram", voltageStates(1_344_000L, M5_MAX_PERFORMANCE_KHZ));
        pmgr.put("voltage-states23-sram", voltageStates(1_344_000L, M5_MAX_PERFORMANCE_KHZ));
        pmgr.put("voltage-states5-sram", voltageStates(1_308_000L, M5_MAX_SUPER_KHZ));
        assertArrayEquals(new long[] { 4_380_000_000L, 4_608_000_000L },
                new PmgrCentralProcessor(pmgr).queryClusterFrequencies(), "M5 Max");
    }

    @Test
    void testLegacyTablesAreReadWhenAccClustersNamesNothingReadable() {
        // A chip that publishes acc-clusters naming tables that cannot be read must still fall back rather than
        // report no frequency at all.
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("acc-clusters", accClusters(30, 0, 31, 1));
        pmgr.put("voltage-states1-sram", voltageStates(StubArmCentralProcessor.EFF_FREQ));
        pmgr.put("voltage-states5-sram", voltageStates(M3_PRO_PERFORMANCE_HZ));
        assertArrayEquals(new long[] { StubArmCentralProcessor.EFF_FREQ, M3_PRO_PERFORMANCE_HZ },
                new PmgrCentralProcessor(pmgr).queryClusterFrequencies(), "fallback");
    }

    @Test
    void testLegacyTablesAreNotMixedIntoAPartialDiscovery() {
        // Only one of the tables acc-clusters names can be read. The legacy numbers belong to an earlier generation's
        // naming, so adding them here could pair one chip's efficiency frequency with another's performance frequency.
        // Reporting the one frequency actually discovered is better: every class then reports it.
        Map<String, byte[]> pmgr = new HashMap<>();
        pmgr.put("acc-clusters", accClusters(22, 0, 31, 1));
        pmgr.put("voltage-states22-sram", voltageStates(M5_MAX_PERFORMANCE_KHZ));
        pmgr.put("voltage-states1-sram", voltageStates(StubArmCentralProcessor.EFF_FREQ));
        assertArrayEquals(new long[] { 4_380_000_000L }, new PmgrCentralProcessor(pmgr).queryClusterFrequencies(),
                "partial discovery");
    }

    @Test
    void testNoVoltageStateTablesYieldsNoClusterFrequencies() {
        assertArrayEquals(new long[0], new PmgrCentralProcessor(new HashMap<>()).queryClusterFrequencies(),
                "no readable tables");
    }

    // -- mapping cluster frequencies onto efficiency classes --

    @Test
    void testOneClusterFrequencyPerEfficiencyClass() {
        long[] m5Max = { 4_380_000_000L, 4_608_000_000L };
        assertArrayEquals(m5Max, MacCentralProcessor.mapClusterFrequencies(m5Max, 2), "two of each");
    }

    @Test
    void testFewerClusterFrequenciesThanEfficiencyClasses() {
        // Only one table could be read, so every class reports it rather than one class reporting the placeholder
        assertArrayEquals(new long[] { M3_PRO_PERFORMANCE_HZ, M3_PRO_PERFORMANCE_HZ },
                MacCentralProcessor.mapClusterFrequencies(new long[] { M3_PRO_PERFORMANCE_HZ }, 2), "one frequency");
    }

    @Test
    void testMoreClusterFrequenciesThanEfficiencyClasses() {
        // Aligned at the top, so the highest class always reports the true maximum
        assertArrayEquals(new long[] { 4_608_000_000L },
                MacCentralProcessor.mapClusterFrequencies(new long[] { 4_380_000_000L, 4_608_000_000L }, 1),
                "one class");
        assertArrayEquals(new long[] { 3_000_000_000L, 4_608_000_000L }, MacCentralProcessor.mapClusterFrequencies(
                new long[] { 2_000_000_000L, 3_000_000_000L, 4_608_000_000L }, 2), "two classes");
    }

    @Test
    void testNoClusterFrequenciesFallsBackToThePlaceholder() {
        assertArrayEquals(new long[] { MacCentralProcessor.DEFAULT_FREQUENCY, MacCentralProcessor.DEFAULT_FREQUENCY },
                MacCentralProcessor.mapClusterFrequencies(new long[0], 2), "nothing read");
        assertArrayEquals(new long[] { MacCentralProcessor.DEFAULT_FREQUENCY },
                MacCentralProcessor.mapClusterFrequencies(new long[0], 0), "no classes either");
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

    // A single perf level count, which cannot partition the cores by itself. The strategies below the perf level counts
    // decide, with this value available to strategy 4 as a tie-break.
    private static int[] topPerfLevel(int cores) {
        return new int[] { cores };
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
    void testPerfLevelCountsClassifyM4() {
        // hw.nperflevels 2, hw.perflevel0.physicalcpu 4 (performance), hw.perflevel1.physicalcpu 6 (efficiency).
        // Level 0 is always the highest-performing level, and macOS numbers the lowest-performing cores first.
        assertClasses("M4",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(10), new HashMap<>(), new int[] { 4, 6 }), 0, 0, 0,
                0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testPerfLevelCountsClassifyAChipWithNoEfficiencyCores() {
        // An M5 Max reports 6 Super cores and 12 Performance cores, and no efficiency cores at all. The classes must
        // still start at 0: the efficiency class is relative within a machine, as it is on Windows.
        assertClasses("M5 Max",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(18), new HashMap<>(), new int[] { 6, 12 }), 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void testPerfLevelCountsClassifyThreeTiers() {
        assertClasses("three perf levels",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(10), new HashMap<>(), new int[] { 2, 4, 4 }), 0, 0,
                0, 0, 1, 1, 1, 1, 2, 2);
    }

    @Test
    void testPerfLevelCountsThatDoNotPartitionTheCoresAreIgnored() {
        // Counts that do not add up to the core count cannot be trusted to partition it, so cluster-type decides
        assertClasses("counts do not sum", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, M1_CLUSTERS), new int[] { 4, 6 }), 0, 0, 0, 0, 1, 1, 1, 1);
        // As does a level whose count could not be read at all
        assertClasses("count missing", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, M1_CLUSTERS), new int[] { 4, 0 }), 0, 0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testSuperClusterOutranksPerformance() {
        String[] clusters = { "E", "E", "P", "P", "S", "S" };
        assertClasses("E, P and S", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(6),
                coreProps(new String[6], clusters), topPerfLevel(0)), 0, 0, 1, 1, 2, 2);
    }

    @Test
    void testClusterTypeClassesAreDenseWhenATypeIsAbsent() {
        // Performance and Super clusters but no efficiency cluster: ranking only the types present keeps the classes
        // 0 and 1, rather than reporting a 1 and 2 that no caller could interpret.
        String[] clusters = { "P", "P", "P", "P", "S", "S" };
        assertClasses("P and S only", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(6),
                coreProps(new String[6], clusters), topPerfLevel(0)), 0, 0, 0, 0, 1, 1);
    }

    @Test
    void testClusterTypeClassifiesM2Max() {
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(12),
                coreProps(M2_MAX_CODENAMES, M2_MAX_CLUSTERS), topPerfLevel(8));
        assertClasses("M2 Max", classes, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void testClusterTypeClassifiesM1EvenSplit() {
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, M1_CLUSTERS), topPerfLevel(4));
        assertClasses("M1", classes, 0, 0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testCodenameFallbackWhenClusterTypeAbsent() {
        // Without cluster-type the codename groups decide, ordered by lowest core number. The M1's 4+4 split makes
        // the perf level count ambiguous, so ordering alone must still classify it correctly.
        Map<Integer, Integer> m1 = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, new String[8]), topPerfLevel(4));
        assertClasses("M1 without cluster-type", m1, 0, 0, 0, 0, 1, 1, 1, 1);

        Map<Integer, Integer> m2 = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(12),
                coreProps(M2_MAX_CODENAMES, new String[12]), topPerfLevel(8));
        assertClasses("M2 Max without cluster-type", m2, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void testPerfLevelCountOverridesCoreOrdering() {
        // A hypothetical chip whose performance cores occupy the low core numbers. The unambiguous
        // hw.perflevel0.physicalcpu count must win over the ordering assumption.
        String[] codenames = { "hydra", "hydra", "coyote", "coyote", "coyote", "coyote" };
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(6),
                coreProps(codenames, new String[6]), topPerfLevel(2));
        assertClasses("perf level count wins", classes, 1, 1, 0, 0, 0, 0);
    }

    @Test
    void testPartialClusterTypeIsPropagatedByCodename() {
        // Only two cores report a cluster type; the rest are classified by sharing a codename with them.
        String[] clusters = new String[12];
        clusters[0] = "E";
        clusters[4] = "P";
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(12),
                coreProps(M2_MAX_CODENAMES, clusters), topPerfLevel(0));
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
        assertClasses("partial properties",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8), props, topPerfLevel(0)), 1, 1, 1, 1, 0, 0, 0,
                0);
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
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4), props,
                topPerfLevel(0));
        assertThat("core 0 keeps its own E reading", classes.get(0), is(0));
        assertThat("core 1 keeps its own P reading", classes.get(1), is(1));
    }

    @Test
    void testClusterTypeWithoutCodename() {
        Map<Integer, Integer> classes = MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(new String[8], M1_CLUSTERS), topPerfLevel(0));
        assertClasses("cluster-type only", classes, 0, 0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testNoPropertiesYieldsAllZero() {
        assertClasses("no properties", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4),
                coreProps(new String[4], new String[4]), topPerfLevel(0)), 0, 0, 0, 0);
        assertClasses("empty map",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4), new HashMap<>(), topPerfLevel(0)), 0, 0, 0, 0);
        assertThat("no cores",
                MacCentralProcessor.deriveEfficiencyClasses(coreKeys(0), new HashMap<>(), topPerfLevel(0)).size(),
                is(0));
    }

    @Test
    void testHomogeneousCoresAreAllClassZero() {
        String[] codenames = { "firestorm", "firestorm", "firestorm", "firestorm" };
        assertClasses("homogeneous", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4),
                coreProps(codenames, new String[4]), topPerfLevel(4)), 0, 0, 0, 0);
    }

    @Test
    void testUnrecognizedClusterTypeFallsThroughToCodenames() {
        String[] clusters = { "X", "X", "X", "X", "X", "X", "X", "X" };
        assertClasses("unrecognized letter", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(M1_CODENAMES, clusters), topPerfLevel(4)), 0, 0, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testUnrecognizedTopClusterTypeFallsThroughForEveryCore() {
        // No dump of an M5 cluster-type value exists, so ranking its top tier as "S" is an informed guess. A chip
        // whose top tier is spelled some other way must not classify that tier as class 0 alongside the efficiency
        // cores, which is what ranking only the letters it recognized would do, so one unrankable letter drops the
        // whole strategy in favor of the codename groups.
        String[] codenames = { "icestorm", "icestorm", "firestorm", "firestorm", "hydra", "hydra" };
        String[] clusters = { "E", "E", "P", "P", "Z", "Z" };
        assertClasses("unrecognized top letter", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(6),
                coreProps(codenames, clusters), topPerfLevel(2)), 0, 0, 1, 1, 2, 2);
    }

    @Test
    void testUnrecognizedClusterTypeWithNothingToGroupByIsAllClassZero() {
        // With the letters discredited and no codename to group by, every core is class 0. Reporting a hybrid split
        // from a vocabulary that has already proven wrong would be a guess; reporting none is the documented default.
        String[] clusters = { "E", "E", "Z", "Z" };
        assertClasses("no codenames either", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(4),
                coreProps(new String[4], clusters), topPerfLevel(2)), 0, 0, 0, 0);
    }

    @Test
    void testFutureChipIsClassifiedWithoutATable() {
        // The point of the change: codenames absent from every hardcoded list still classify correctly.
        String[] codenames = { "coyote", "coyote", "coyote", "coyote", "hydra", "hydra", "hydra", "hydra" };
        String[] clusters = { "E", "E", "E", "E", "P", "P", "P", "P" };
        assertClasses("future chip", MacCentralProcessor.deriveEfficiencyClasses(coreKeys(8),
                coreProps(codenames, clusters), topPerfLevel(4)), 0, 0, 0, 0, 1, 1, 1, 1);
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
            return null; // Not used; platformExpert/queryCoreProperties/queryClusterFrequencies are overridden
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
        protected long[][] queryClusterFrequencyTables() {
            // With no IORegistry to walk there are no tables to read; PmgrCentralProcessor supplies one
            return ioKitProvider() == null ? new long[0][] : super.queryClusterFrequencyTables();
        }

        @Override
        protected @Nullable IOReportCpuSampler createCpuFrequencySampler() {
            return null; // Overridden where a live frequency is under test
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
        private final AtomicInteger samplerRequests = new AtomicInteger();

        /** Set before the first frequency query to exercise the live path; null leaves the nominal path in place. */
        private @Nullable IOReportCpuSampler sampler;

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
        protected long[][] queryClusterFrequencyTables() {
            nominalFrequencyQueries.incrementAndGet();
            return new long[][] { M3_PRO_EFFICIENCY_TABLE, M3_PRO_PERFORMANCE_TABLE };
        }

        @Override
        protected @Nullable IOReportCpuSampler createCpuFrequencySampler() {
            samplerRequests.incrementAndGet();
            return sampler;
        }

        void setSampler(@Nullable IOReportCpuSampler sampler) {
            this.sampler = sampler;
        }

        int nominalFrequencyQueries() {
            return nominalFrequencyQueries.get();
        }

        int samplerRequests() {
            return samplerRequests.get();
        }
    }

    /**
     * A stub that reads its cluster frequencies through the real IORegistry walk, with the properties of the
     * {@code pmgr} node supplied as a map. This is what covers table discovery, the legacy fallback and the unit
     * conversion together, the way a real chip exercises them.
     */
    static class PmgrCentralProcessor extends StubMacCentralProcessor {

        private final IOKitProvider provider;

        PmgrCentralProcessor(Map<String, byte[]> pmgrProperties) {
            super(Collections.singletonMap("machdep.cpu.brand_string", "Apple M0"));
            this.provider = new PmgrProvider(pmgrProperties);
        }

        @Override
        protected IOKitProvider ioKitProvider() {
            return provider;
        }
    }

    /**
     * An {@link IOKitProvider} exposing exactly one {@code pmgr} entry, whose byte array properties come from a map.
     * Absent keys read as an empty array rather than null, matching what the real providers return for a property the
     * node does not publish.
     */
    static class PmgrProvider implements IOKitProvider, IOKitProvider.RegistryEntry {

        private final Map<String, byte[]> properties;

        PmgrProvider(Map<String, byte[]> properties) {
            this.properties = properties;
        }

        @Override
        public <T> @Nullable T withMatchingService(String serviceName, Function<RegistryEntry, @Nullable T> extractor) {
            return extractor.apply(this);
        }

        @Override
        public void forEachMatchingService(String serviceName, Consumer<RegistryEntry> consumer) {
            consumer.accept(this);
        }

        @Override
        public void forEachMatchingServiceUntil(String serviceName, Predicate<RegistryEntry> visitor) {
            // Honoring the early exit even over one entry, so a visitor that stops on pmgr behaves as it does natively
            for (RegistryEntry entry : Collections.<RegistryEntry>singletonList(this)) {
                if (visitor.test(entry)) {
                    break;
                }
            }
        }

        @Override
        public String getName() {
            return "pmgr";
        }

        @Override
        public byte[] getByteArrayProperty(String key) {
            byte[] value = properties.get(key);
            return value == null ? new byte[0] : value;
        }

        @Override
        public String getStringProperty(String key) {
            return "";
        }

        @Override
        public Integer getIntegerProperty(String key) {
            return 0;
        }

        @Override
        public Boolean getBooleanProperty(String key) {
            return Boolean.FALSE;
        }
    }
}
