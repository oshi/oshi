/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static oshi.software.os.CgroupInfo.DEFAULT_CPU_PERIOD;
import static oshi.software.os.CgroupInfo.UNLIMITED;
import static oshi.software.os.CgroupInfo.UNLIMITED_MEMORY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import oshi.util.linux.SysPath;

/**
 * Tests the v1 dispatch paths and limit resolution in LinuxCgroupInfo that are not covered by the main test.
 */
class LinuxCgroupInfoV1DispatchTest {

    private final LinuxCgroupInfo cgroup = new LinuxCgroupInfo();

    // --- V1 limit/quota tests via package-private methods ---

    @Test
    void readCpuQuotaV1WithPositiveValue(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.cfs_quota_us"), "250000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(250000L, cgroup.readCpuQuotaV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV1ZeroMeansUnlimited(@TempDir Path tempDir) throws IOException {
        // A zero value from getLongFromFile (file empty or parse failure) should map to UNLIMITED
        Files.write(tempDir.resolve("cpu.cfs_quota_us"), "0\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV1WithValue(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.cfs_period_us"), "100000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(100000L, cgroup.readCpuPeriodV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV1ZeroMeansDefault(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.cfs_period_us"), "0\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(DEFAULT_CPU_PERIOD, cgroup.readCpuPeriodV1(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV1ZeroMeansUnlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.limit_in_bytes"), "0\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED_MEMORY, cgroup.readMemoryLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV1ZeroMeansUnlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "0\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readPidLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV1WithNumericValue(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "2048\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(2048L, cgroup.readPidLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readPidCurrentV1WithValue(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.current"), "99\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(99L, cgroup.readPidCurrentV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV1WithValue(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpuacct.usage"), "987654321\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(987654321L, cgroup.readCpuUsageV1(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryUsageV1WithValue(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.usage_in_bytes"), "67108864\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(67108864L, cgroup.readMemoryUsageV1(tempDir.toString() + "/"));
    }

    // --- V2 additional edge cases ---

    @Test
    void readCpuQuotaV2SingleTokenMax(@TempDir Path tempDir) throws IOException {
        // cpu.max with just "max" (no period)
        Files.write(tempDir.resolve("cpu.max"), "max\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV2SingleToken(@TempDir Path tempDir) throws IOException {
        // cpu.max with just one token (no period specified)
        Files.write(tempDir.resolve("cpu.max"), "200000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(DEFAULT_CPU_PERIOD, cgroup.readCpuPeriodV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV2EmptyFile(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.max"), "".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED_MEMORY, cgroup.readMemoryLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV2EmptyFile(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readPidLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV2NoUsageLine(@TempDir Path tempDir) throws IOException {
        // cpu.stat without usage_usec line
        Files.write(tempDir.resolve("cpu.stat"), "nr_periods 100\nnr_throttled 5\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(0L, cgroup.readCpuUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryUsageV2EmptyFile(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.current"), "".getBytes(StandardCharsets.UTF_8));
        assertEquals(0L, cgroup.readMemoryUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidCurrentV2EmptyFile(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.current"), "".getBytes(StandardCharsets.UTF_8));
        assertEquals(0L, cgroup.readPidCurrentV2(tempDir.toString() + "/"));
    }

    // --- V1 controller path resolution from /proc/self/cgroup ---

    // Real hybrid (v1 + unified) /proc/self/cgroup: numbered v1 hierarchies with comma-separated controllers, plus
    // a trailing "0::" unified (v2) line whose controllers field is empty.
    private static final List<String> SELF_CGROUP = Arrays.asList("12:hugetlb:/",
            "11:cpu,cpuacct:/user.slice/user-1000.slice", "10:memory:/user.slice/user-1000.slice/session-3.scope",
            "3:pids:/user.slice/user-1000.slice", "1:name=systemd:/user.slice/user-1000.slice/session-3.scope",
            "0::/user.slice/user-1000.slice/session-3.scope");

    // SysPath's static initializer resolves the live sysfs mount, so these Linux-only paths are asserted on Linux CI
    // (which is also where coverage is collected).

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveV1ControllerPathExactMatchInCommaList() {
        // "cpu" is one of the comma-separated controllers; the full "cpu,cpuacct" mount name is used in the path
        assertEquals(SysPath.CGROUP + "cpu,cpuacct/user.slice/user-1000.slice/",
                LinuxCgroupInfo.resolveV1ControllerPath(SELF_CGROUP, "cpu"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveV1ControllerPathSingleController() {
        assertEquals(SysPath.CGROUP + "memory/user.slice/user-1000.slice/session-3.scope/",
                LinuxCgroupInfo.resolveV1ControllerPath(SELF_CGROUP, "memory"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveV1ControllerPathNotFoundUsesDefault() {
        // No hierarchy lists "blkio"; fall back to the bare controller mount
        assertEquals(SysPath.CGROUP + "blkio/", LinuxCgroupInfo.resolveV1ControllerPath(SELF_CGROUP, "blkio"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveV1ControllerPathSkipsUnifiedLine() {
        // A pure-v2 cgroup (only the "0::" unified line, empty controllers) never matches -> default path
        List<String> unifiedOnly = Collections.singletonList("0::/user.slice/user-1000.slice/session-3.scope");
        assertEquals(SysPath.CGROUP + "cpu/", LinuxCgroupInfo.resolveV1ControllerPath(unifiedOnly, "cpu"));
    }
}
