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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinuxCgroupInfoTest {

    private final LinuxCgroupInfo cgroup = new LinuxCgroupInfo();

    @Test
    void readCpuQuotaV2WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "200000 100000\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(200000L, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV2Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "max 100000\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuQuotaV2MissingFile(@TempDir Path tempDir) {
        assertEquals(UNLIMITED, cgroup.readCpuQuotaV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "200000 50000\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(50000L, cgroup.readCpuPeriodV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuPeriodV2Default(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.max"), "max\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(DEFAULT_CPU_PERIOD, cgroup.readCpuPeriodV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV2WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.max"), "1073741824\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1073741824L, cgroup.readMemoryLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryLimitV2Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.max"), "max\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(UNLIMITED_MEMORY, cgroup.readMemoryLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readMemoryUsageV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("memory.current"), "536870912\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(536870912L, cgroup.readMemoryUsageV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV2WithLimit(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "1024\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1024L, cgroup.readPidLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidLimitV2Unlimited(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.max"), "max\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(UNLIMITED, cgroup.readPidLimitV2(tempDir.toString() + "/"));
    }

    @Test
    void readPidCurrentV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pids.current"), "42\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(42L, cgroup.readPidCurrentV2(tempDir.toString() + "/"));
    }

    @Test
    void readCpuUsageV2(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("cpu.stat"),
                "usage_usec 5000000\nnr_periods 100\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // 5000000 usec * 1000 ns/usec = 5000000000 ns
        assertEquals(5_000_000_000L, cgroup.readCpuUsageV2(tempDir.toString() + "/"));
    }
}
