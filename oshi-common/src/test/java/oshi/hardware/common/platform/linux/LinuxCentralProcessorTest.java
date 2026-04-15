/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static oshi.hardware.common.platform.linux.TestFileUtil.writeFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.util.tuples.Quartet;

class LinuxCentralProcessorTest {

    private static final double EPS = 1e-6;

    // -------------------------------------------------------------------------
    // createMIDR
    // -------------------------------------------------------------------------

    @Test
    void testCreateMIDRBasic() {
        // vendor=0x41 (ARM), stepping=r1p3, model=0xd07, family=8
        String midr = LinuxCentralProcessor.createMIDR("0x41", "r1p3", "0xd07", "8");
        // Implementer 0x41 << 24 = 0x41000000
        // Variant 1 << 20 = 0x00100000
        // Architecture 8 << 16 = 0x00080000
        // PartNum 0xd07 << 4 = 0x0000D070
        // Revision 3 = 0x00000003
        // Total = 0x41180073... let me compute:
        // 0x41 << 24 = 0x41000000
        // 1 << 20 = 0x00100000
        // 8 << 16 = 0x00080000
        // 0xd07 << 4 = 0x0000D070
        // 3 = 0x00000003
        // Sum = 0x4118D073
        assertThat(midr, is("4118D073"));
    }

    @Test
    void testCreateMIDRNoStepping() {
        // No rnpn stepping format
        String midr = LinuxCentralProcessor.createMIDR("0x41", "", "0xd03", "8");
        // 0x41 << 24 + 8 << 16 + 0xd03 << 4 = 0x4108D030
        assertThat(midr, is("4108D030"));
    }

    @Test
    void testCreateMIDRZeroValues() {
        String midr = LinuxCentralProcessor.createMIDR("0x0", "r0p0", "0x0", "0");
        assertThat(midr, is("00000000"));
    }

    // -------------------------------------------------------------------------
    // getSystemLoadAverage
    // -------------------------------------------------------------------------

    @Test
    void testGetSystemLoadAverageThreeElements() {
        double[] avg = LinuxCentralProcessor.getSystemLoadAverage(3, "1.25 0.75 0.50 1/234 5678");
        assertThat(avg.length, is(3));
        assertThat(avg[0], closeTo(1.25, EPS));
        assertThat(avg[1], closeTo(0.75, EPS));
        assertThat(avg[2], closeTo(0.50, EPS));
    }

    @Test
    void testGetSystemLoadAverageOneElement() {
        double[] avg = LinuxCentralProcessor.getSystemLoadAverage(1, "2.00 1.00 0.50 1/100 999");
        assertThat(avg.length, is(1));
        assertThat(avg[0], closeTo(2.0, EPS));
    }

    @Test
    void testGetSystemLoadAverageEmptyContent() {
        double[] avg = LinuxCentralProcessor.getSystemLoadAverage(3, "");
        assertThat(avg.length, is(3));
        assertThat(avg[0], closeTo(-1d, EPS));
        assertThat(avg[1], closeTo(-1d, EPS));
        assertThat(avg[2], closeTo(-1d, EPS));
    }

    @Test
    void testGetSystemLoadAverageInvalidNelem() {
        assertThrows(IllegalArgumentException.class, () -> LinuxCentralProcessor.getSystemLoadAverage(0, "1 2 3"));
        assertThrows(IllegalArgumentException.class, () -> LinuxCentralProcessor.getSystemLoadAverage(4, "1 2 3"));
    }

    // -------------------------------------------------------------------------
    // queryMaxFreqFromCpuFreqPath
    // -------------------------------------------------------------------------

    @Test
    void testQueryMaxFreqFromCpuFreqPathWithPolicies(@TempDir Path tempDir) throws IOException {
        Path cpuFreq = tempDir.resolve("cpufreq");
        Path policy0 = cpuFreq.resolve("policy0");
        Path policy1 = cpuFreq.resolve("policy1");
        Files.createDirectories(policy0);
        Files.createDirectories(policy1);
        writeFile(policy0.resolve("scaling_max_freq"), "3600000");
        writeFile(policy1.resolve("scaling_max_freq"), "4200000");

        long maxFreq = LinuxCentralProcessor.queryMaxFreqFromCpuFreqPath(cpuFreq.toString());
        // 4200000 kHz * 1000 = 4200000000 Hz
        assertThat(maxFreq, is(4200000000L));
    }

    @Test
    void testQueryMaxFreqFallbackToCpuinfoMaxFreq(@TempDir Path tempDir) throws IOException {
        Path cpuFreq = tempDir.resolve("cpufreq");
        Path policy0 = cpuFreq.resolve("policy0");
        Files.createDirectories(policy0);
        // scaling_max_freq is 0, fall back to cpuinfo_max_freq
        writeFile(policy0.resolve("scaling_max_freq"), "0");
        writeFile(policy0.resolve("cpuinfo_max_freq"), "2500000");

        long maxFreq = LinuxCentralProcessor.queryMaxFreqFromCpuFreqPath(cpuFreq.toString());
        assertThat(maxFreq, is(2500000000L));
    }

    @Test
    void testQueryMaxFreqNonexistentPath(@TempDir Path tempDir) {
        long maxFreq = LinuxCentralProcessor.queryMaxFreqFromCpuFreqPath(tempDir.resolve("missing").toString());
        assertThat(maxFreq, is(-1L));
    }

    @Test
    void testQueryMaxFreqNoPolicies(@TempDir Path tempDir) throws IOException {
        Path cpuFreq = tempDir.resolve("cpufreq");
        Files.createDirectories(cpuFreq);
        // Directory exists but no policy subdirectories

        long maxFreq = LinuxCentralProcessor.queryMaxFreqFromCpuFreqPath(cpuFreq.toString());
        assertThat(maxFreq, is(-1L));
    }

    // -------------------------------------------------------------------------
    // readTopologyFromSysfs
    // -------------------------------------------------------------------------

    @Test
    void testReadTopologyFromSysfsNonexistentPath(@TempDir Path tempDir) {
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromSysfs(tempDir.resolve("missing").toString());
        assertThat(result.getA(), is(empty()));
        assertThat(result.getB(), is(empty()));
        assertThat(result.getC().isEmpty(), is(true));
        assertThat(result.getD().isEmpty(), is(true));
    }

    @Test
    void testReadTopologyFromSysfsSingleCpu(@TempDir Path tempDir) throws IOException {
        Path cpu0 = tempDir.resolve("cpu0");
        Files.createDirectories(cpu0.resolve("topology"));
        writeFile(cpu0.resolve("topology/core_id"), "0");
        writeFile(cpu0.resolve("topology/physical_package_id"), "0");
        writeFile(cpu0.resolve("cpu_capacity"), "1024");

        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromSysfs(tempDir.toString());
        assertThat(result.getA(), hasSize(1));
        LogicalProcessor lp = result.getA().get(0);
        assertThat(lp.getPhysicalProcessorNumber(), is(0));
        assertThat(lp.getPhysicalPackageNumber(), is(0));
        // cpu_capacity value stored in coreEfficiencyMap keyed by (pkg<<16)+core = 0
        assertThat(result.getC().containsKey(0), is(true));
        assertThat(result.getC().get(0), is(1024));
    }

    @Test
    void testReadTopologyFromSysfsMultiCpu(@TempDir Path tempDir) throws IOException {
        for (int i = 0; i < 4; i++) {
            Path cpu = tempDir.resolve("cpu" + i);
            Files.createDirectories(cpu.resolve("topology"));
            writeFile(cpu.resolve("topology/core_id"), String.valueOf(i % 2));
            writeFile(cpu.resolve("topology/physical_package_id"), "0");
        }

        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromSysfs(tempDir.toString());
        assertThat(result.getA(), hasSize(4));
    }

    @Test
    void testReadTopologyFromSysfsWithCache(@TempDir Path tempDir) throws IOException {
        Path cpu0 = tempDir.resolve("cpu0");
        Files.createDirectories(cpu0.resolve("topology"));
        writeFile(cpu0.resolve("topology/core_id"), "0");
        writeFile(cpu0.resolve("topology/physical_package_id"), "0");

        Path index0 = cpu0.resolve("cache/index0");
        Files.createDirectories(index0);
        writeFile(index0.resolve("level"), "1");
        writeFile(index0.resolve("type"), "Data");
        writeFile(index0.resolve("ways_of_associativity"), "8");
        writeFile(index0.resolve("coherency_line_size"), "64");
        writeFile(index0.resolve("size"), "32K");

        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromSysfs(tempDir.toString());
        assertThat(result.getB(), hasSize(1));
        ProcessorCache cache = result.getB().get(0);
        assertThat(cache.getLevel(), is((byte) 1));
        assertThat(cache.getType(), is(ProcessorCache.Type.DATA));
        assertThat(cache.getAssociativity(), is((byte) 8));
        assertThat(cache.getLineSize(), is((short) 64));
        assertThat(cache.getCacheSize(), is(32768));
    }

    @Test
    void testReadTopologyFromSysfsWithModAlias(@TempDir Path tempDir) throws IOException {
        Path cpu0 = tempDir.resolve("cpu0");
        Files.createDirectories(cpu0.resolve("topology"));
        writeFile(cpu0.resolve("topology/core_id"), "0");
        writeFile(cpu0.resolve("topology/physical_package_id"), "0");
        writeFile(cpu0.resolve("uevent"), "MODALIAS=cpu:type:aarch64:feature:0001");

        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromSysfs(tempDir.toString());
        assertThat(result.getD().get(0), is("cpu:type:aarch64:feature:0001"));
    }

    // -------------------------------------------------------------------------
    // readTopologyFromCpuinfo
    // -------------------------------------------------------------------------

    @Test
    void testReadTopologyFromCpuinfoSingleProcessor() {
        List<String> lines = Arrays.asList("processor\t: 0", "core id\t\t: 0", "physical id\t: 0");
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromCpuinfo(lines);
        assertThat(result.getA(), hasSize(1));
        LogicalProcessor lp = result.getA().get(0);
        assertThat(lp.getProcessorNumber(), is(0));
        assertThat(lp.getPhysicalProcessorNumber(), is(0));
        assertThat(lp.getPhysicalPackageNumber(), is(0));
    }

    @Test
    void testReadTopologyFromCpuinfoMultiProcessor() {
        List<String> lines = Arrays.asList("processor\t: 0", "core id\t\t: 0", "physical id\t: 0", "", "processor\t: 1",
                "core id\t\t: 1", "physical id\t: 0", "", "processor\t: 2", "core id\t\t: 0", "physical id\t: 1");
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromCpuinfo(lines);
        assertThat(result.getA(), hasSize(3));
        // Verify second processor
        LogicalProcessor lp1 = result.getA().get(1);
        assertThat(lp1.getProcessorNumber(), is(1));
        assertThat(lp1.getPhysicalProcessorNumber(), is(1));
        // Verify third processor on different package
        LogicalProcessor lp2 = result.getA().get(2);
        assertThat(lp2.getPhysicalPackageNumber(), is(1));
    }

    @Test
    void testReadTopologyFromCpuinfoCpuNumber() {
        // Some architectures use "cpu number" instead of "core id"
        List<String> lines = Arrays.asList("processor\t: 0", "cpu number\t: 5", "physical id\t: 0");
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromCpuinfo(lines);
        assertThat(result.getA().get(0).getPhysicalProcessorNumber(), is(5));
    }

    @Test
    void testReadTopologyFromCpuinfoEmptyInput() {
        // Even with empty input, the method adds one default processor
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> result = LinuxCentralProcessor
                .readTopologyFromCpuinfo(Collections.emptyList());
        assertThat(result.getA(), hasSize(1));
        assertThat(result.getA().get(0).getProcessorNumber(), is(0));
    }
}
