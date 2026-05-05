/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import oshi.software.os.CgroupInfo;

/**
 * Integration tests that run inside a Docker container with known resource limits. Enabled only when the
 * {@code OSHI_CGROUP_TEST} environment variable is set.
 *
 * <p>
 * Expected container configuration:
 * <ul>
 * <li>{@code --cpus=1.5} (quota=150000, period=100000)</li>
 * <li>{@code --memory=512m}</li>
 * <li>{@code --pids-limit=256}</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "OSHI_CGROUP_TEST", matches = "true")
class LinuxCgroupInfoContainerTest {

    private final LinuxCgroupInfo cgroup = new LinuxCgroupInfo();

    @Test
    void isContainerized() {
        assertTrue(cgroup.isContainerized(), "Should detect containerized environment");
    }

    @Test
    void versionIsV2OrV1() {
        int version = cgroup.getVersion();
        assertTrue(version == 1 || version == 2, "Version should be 1 or 2 in container, got " + version);
    }

    @Test
    void cpuQuotaMatchesLimit() {
        // --cpus=1.5 sets quota=150000 period=100000
        assertEquals(150000L, cgroup.getCpuQuota(), "CPU quota should be 150000 us");
    }

    @Test
    void cpuPeriodIsDefault() {
        assertEquals(CgroupInfo.DEFAULT_CPU_PERIOD, cgroup.getCpuPeriod(), "CPU period should be 100000 us");
    }

    @Test
    void effectiveCpus() {
        assertEquals(1.5d, cgroup.getEffectiveCpus(), "Effective CPUs should be 1.5");
    }

    @Test
    void cpuUsagePositive() {
        assertTrue(cgroup.getCpuUsage() > 0, "CPU usage should be positive in a running container");
    }

    @Test
    void memoryLimitMatches512MB() {
        // --memory=512m = 536870912 bytes
        assertEquals(536870912L, cgroup.getMemoryLimit(), "Memory limit should be 512 MiB");
    }

    @Test
    void memoryUsagePositive() {
        long usage = cgroup.getMemoryUsage();
        assertTrue(usage > 0, "Memory usage should be positive");
        assertTrue(usage <= 536870912L, "Memory usage should not exceed limit");
    }

    @Test
    void pidLimitMatches() {
        assertEquals(256L, cgroup.getPidLimit(), "PID limit should be 256");
    }

    @Test
    void pidCurrentPositive() {
        long current = cgroup.getPidCurrent();
        assertTrue(current > 0, "PID current should be positive");
        assertTrue(current <= 256, "PID current should not exceed limit");
    }
}
