/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OshiMetricsTest {

    private static MeterRegistry registry;
    private static HardwareAbstractionLayer hal;
    private static OperatingSystem os;

    @BeforeAll
    static void setUp() {
        registry = new SimpleMeterRegistry();
        SystemInfo si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();
        OshiMetrics.bindTo(registry, hal, os);
    }

    @Test
    void systemUptimeRegistered() {
        Gauge uptime = registry.find("system.uptime").gauge();
        assertNotNull(uptime, "system.uptime should be registered");
        assertTrue(uptime.value() > 0, "System uptime should be positive");
    }

    @Test
    void memoryUsageRegistered() {
        Gauge used = registry.find("system.memory.usage").tag("system.memory.state", "used").gauge();
        Gauge free = registry.find("system.memory.usage").tag("system.memory.state", "free").gauge();
        assertNotNull(used, "system.memory.usage{state=used} should be registered");
        assertNotNull(free, "system.memory.usage{state=free} should be registered");
        assertTrue(used.value() > 0, "Used memory should be positive");
        assertTrue(free.value() > 0, "Free memory should be positive");
    }

    @Test
    void memoryLimitRegistered() {
        Gauge limit = registry.find("system.memory.limit").gauge();
        assertNotNull(limit, "system.memory.limit should be registered");
        assertTrue(limit.value() > 0, "Memory limit should be positive");
    }

    @Test
    void memoryUtilizationRegistered() {
        Gauge used = registry.find("system.memory.utilization").tag("system.memory.state", "used").gauge();
        Gauge free = registry.find("system.memory.utilization").tag("system.memory.state", "free").gauge();
        assertNotNull(used, "system.memory.utilization{state=used} should be registered");
        assertNotNull(free, "system.memory.utilization{state=free} should be registered");
        double usedVal = used.value();
        double freeVal = free.value();
        assertTrue(usedVal >= 0 && usedVal <= 1, "Used utilization should be between 0 and 1");
        assertTrue(freeVal >= 0 && freeVal <= 1, "Free utilization should be between 0 and 1");
    }

    @Test
    void cpuTimeRegistered() {
        String[] modes = { "user", "nice", "system", "idle", "iowait", "interrupt", "softirq", "steal" };
        for (String mode : modes) {
            assertNotNull(registry.find("system.cpu.time").tag("cpu.mode", mode).functionCounter(),
                    "system.cpu.time{cpu.mode=" + mode + "} should be registered");
        }
        FunctionCounter idleCounter = registry.find("system.cpu.time").tag("cpu.mode", "idle").functionCounter();
        assertNotNull(idleCounter, "idle counter should exist");
        assertTrue(idleCounter.count() > 0, "Idle CPU time should be positive, got " + idleCounter.count());
    }

    @Test
    void cpuPhysicalCountRegistered() {
        Gauge count = registry.find("system.cpu.physical.count").gauge();
        assertNotNull(count, "system.cpu.physical.count should be registered");
        assertTrue(count.value() >= 1, "Physical CPU count should be at least 1");
    }

    @Test
    void cpuLogicalCountRegistered() {
        Gauge count = registry.find("system.cpu.logical.count").gauge();
        assertNotNull(count, "system.cpu.logical.count should be registered");
        assertTrue(count.value() >= 1, "Logical CPU count should be at least 1");
        // logical >= physical
        Gauge physical = registry.find("system.cpu.physical.count").gauge();
        assertTrue(count.value() >= physical.value(), "Logical count should be >= physical count");
    }

    @Test
    void cpuFrequencyRegistered() {
        // At least cpu.logical_number=0 should exist
        Gauge freq = registry.find("system.cpu.frequency").tag("cpu.logical_number", "0").gauge();
        assertNotNull(freq, "system.cpu.frequency{cpu.logical_number=0} should be registered");
        // Frequency may be 0 on some platforms (e.g., VMs), so just check non-negative
        assertTrue(freq.value() >= 0, "CPU frequency should be non-negative");
    }

    @Test
    void pagingUsageRegistered() {
        Gauge used = registry.find("system.paging.usage").tag("system.paging.state", "used").gauge();
        Gauge free = registry.find("system.paging.usage").tag("system.paging.state", "free").gauge();
        assertNotNull(used, "system.paging.usage{state=used} should be registered");
        assertNotNull(free, "system.paging.usage{state=free} should be registered");
        // Swap used may be 0, but free + used should equal total
        assertTrue(used.value() >= 0, "Paging used should be non-negative");
        assertTrue(free.value() >= 0, "Paging free should be non-negative");
    }

    @Test
    void pagingUtilizationRegistered() {
        Gauge used = registry.find("system.paging.utilization").tag("system.paging.state", "used").gauge();
        Gauge free = registry.find("system.paging.utilization").tag("system.paging.state", "free").gauge();
        assertNotNull(used, "system.paging.utilization{state=used} should be registered");
        assertNotNull(free, "system.paging.utilization{state=free} should be registered");
        assertTrue(used.value() >= 0 && used.value() <= 1, "Paging utilization used should be 0-1");
        assertTrue(free.value() >= 0 && free.value() <= 1, "Paging utilization free should be 0-1");
    }

    @Test
    void pagingOperationsRegistered() {
        FunctionCounter in = registry.find("system.paging.operations").tag("system.paging.direction", "in")
                .functionCounter();
        FunctionCounter out = registry.find("system.paging.operations").tag("system.paging.direction", "out")
                .functionCounter();
        assertNotNull(in, "system.paging.operations{direction=in} should be registered");
        assertNotNull(out, "system.paging.operations{direction=out} should be registered");
        assertTrue(in.count() >= 0, "Pages in should be non-negative");
        assertTrue(out.count() >= 0, "Pages out should be non-negative");
    }
}
