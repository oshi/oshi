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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.software.os.CgroupInfo;

class LinuxCgroupInfoTest {

    private final LinuxCgroupInfo cgroup = new LinuxCgroupInfo();

    @Test
    void readCpuQuotaV2WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "200000 100000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(200000L, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV2Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "max 100000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV2MissingFile(@TempDir Path tempDir) {
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "200000 50000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(50000L, cgroup.readCpuPeriodV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV2Default(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "max\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(DEFAULT_CPU_PERIOD, cgroup.readCpuPeriodV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV2WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.max"), "1073741824\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(1073741824L, cgroup.readMemoryLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV2Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.max"), "max\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED_MEMORY, cgroup.readMemoryLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryUsageV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.current"), "536870912\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(536870912L, cgroup.readMemoryUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV2WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "1024\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(1024L, cgroup.readPidLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV2Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "max\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readPidLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidCurrentV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.current"), "42\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(42L, cgroup.readPidCurrentV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.stat"),
                "usage_usec 5000000\nnr_periods 100\n".getBytes(StandardCharsets.UTF_8));
        // 5000000 usec * 1000 ns/usec = 5000000000 ns
        assertEquals(5_000_000_000L, cgroup.readCpuUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV2MissingFile(@TempDir Path tempDir) {
        assertEquals(DEFAULT_CPU_PERIOD, cgroup.readCpuPeriodV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV2MissingFile(@TempDir Path tempDir) {
        assertEquals(UNLIMITED_MEMORY, cgroup.readMemoryLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryUsageV2MissingFile(@TempDir Path tempDir) {
        assertEquals(0L, cgroup.readMemoryUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV2MissingFile(@TempDir Path tempDir) {
        assertEquals(UNLIMITED, cgroup.readPidLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidCurrentV2MissingFile(@TempDir Path tempDir) {
        assertEquals(0L, cgroup.readPidCurrentV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV2MissingFile(@TempDir Path tempDir) {
        assertEquals(0L, cgroup.readCpuUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV2LineOrder(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.stat"),
                "nr_periods 100\nusage_usec 3000\nnr_throttled 5\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(3_000_000L, cgroup.readCpuUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void effectiveCpusWithQuota() {
        assertEquals(2.0d, new CgroupInfo() {
            @Override
            public long getCpuQuota() {
                return 200000L;
            }

            @Override
            public long getCpuPeriod() {
                return 100000L;
            }
        }.getEffectiveCpus());
    }

    @Test
    void effectiveCpusUnlimited() {
        assertEquals(CgroupInfo.UNLIMITED_CPUS, new CgroupInfo() {
        }.getEffectiveCpus());
    }

    // --- V1 tests ---

    @Test
    void readCpuQuotaV1WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.cfs_quota_us"), "50000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(50000L, cgroup.readCpuQuotaV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV1Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.cfs_quota_us"), "-1\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(-1L, cgroup.readCpuQuotaV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV1MissingFile(@TempDir Path tempDir) {
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV1(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.cfs_period_us"), "50000\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(50000L, cgroup.readCpuPeriodV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV1Default(@TempDir Path tempDir) {
        assertEquals(DEFAULT_CPU_PERIOD, cgroup.readCpuPeriodV1(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV1(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpuacct.usage"), "123456789\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(123456789L, cgroup.readCpuUsageV1(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV1WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.limit_in_bytes"), "2147483648\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(2147483648L, cgroup.readMemoryLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV1Unlimited(@TempDir Path tempDir) throws IOException {
        // Kernel reports near-max value for unlimited
        String nearMax = String.valueOf(Long.MAX_VALUE - 4095);
        Files.write(tempDir.resolve("memory.limit_in_bytes"), (nearMax + "\n").getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED_MEMORY, cgroup.readMemoryLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryUsageV1(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.usage_in_bytes"), "1048576\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(1048576L, cgroup.readMemoryUsageV1(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV1WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "512\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(512L, cgroup.readPidLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV1Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "max\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readPidLimitV1(tempDir.toString() + "/"));
    }

    @Test
    void readPidCurrentV1(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.current"), "17\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(17L, cgroup.readPidCurrentV1(tempDir.toString() + "/"));
    }
}
