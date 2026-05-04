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
    void memoryUsageRegistered() {
        Gauge used = registry.find("system.memory.usage").tag("system.memory.state", "used").gauge();
        Gauge free = registry.find("system.memory.usage").tag("system.memory.state", "free").gauge();
        assertNotNull(used, "system.memory.usage{state=used} should be registered");
        assertNotNull(free, "system.memory.usage{state=free} should be registered");
        assertTrue(used.value() > 0, "Used memory should be positive");
        assertTrue(free.value() > 0, "Free memory should be positive");
    }

    @Test
    void memoryUtilizationRegistered() {
        Gauge used = registry.find("system.memory.utilization").tag("system.memory.state", "used").gauge();
        Gauge free = registry.find("system.memory.utilization").tag("system.memory.state", "free").gauge();
        assertNotNull(used, "system.memory.utilization{state=used} should be registered");
        assertNotNull(free, "system.memory.utilization{state=free} should be registered");
        double usedVal = used.value();
        double freeVal = free.value();
        assertTrue(usedVal > 0 && usedVal < 1, "Used utilization should be between 0 and 1");
        assertTrue(freeVal > 0 && freeVal < 1, "Free utilization should be between 0 and 1");
    }

    @Test
    void cpuTimeRegistered() {
        // Should have one counter per cpu.mode
        String[] modes = { "user", "nice", "system", "idle", "iowait", "interrupt", "softirq", "steal" };
        for (String mode : modes) {
            assertNotNull(registry.find("system.cpu.time").tag("cpu.mode", mode).functionCounter(),
                    "system.cpu.time{cpu.mode=" + mode + "} should be registered");
        }
        // idle should be nonzero on any running system
        FunctionCounter idleCounter = registry.find("system.cpu.time").tag("cpu.mode", "idle").functionCounter();
        assertNotNull(idleCounter, "idle counter should exist");
        assertTrue(idleCounter.count() > 0, "Idle CPU time should be positive, got " + idleCounter.count());
    }
}
