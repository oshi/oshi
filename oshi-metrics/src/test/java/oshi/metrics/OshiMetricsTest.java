/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void processCountRegistered() {
        Gauge count = registry.find("system.process.count").gauge();
        assertNotNull(count, "system.process.count should be registered");
        assertTrue(count.value() >= 1, "Process count should be at least 1");
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
        assertNotNull(physical, "system.cpu.physical.count should be registered");
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

    @Test
    void diskIoRegistered() {
        // At least one disk should exist with read direction
        FunctionCounter read = registry.find("system.disk.io").tag("disk.io.direction", "read").functionCounter();
        FunctionCounter write = registry.find("system.disk.io").tag("disk.io.direction", "write").functionCounter();
        assertNotNull(read, "system.disk.io{direction=read} should be registered");
        assertNotNull(write, "system.disk.io{direction=write} should be registered");
        assertTrue(read.count() >= 0, "Disk read bytes should be non-negative");
    }

    @Test
    void diskOperationsRegistered() {
        FunctionCounter read = registry.find("system.disk.operations").tag("disk.io.direction", "read")
                .functionCounter();
        assertNotNull(read, "system.disk.operations{direction=read} should be registered");
        assertTrue(read.count() >= 0, "Disk read operations should be non-negative");
    }

    @Test
    void diskIoTimeRegistered() {
        FunctionCounter ioTime = registry.find("system.disk.io_time").functionCounter();
        assertNotNull(ioTime, "system.disk.io_time should be registered");
        assertTrue(ioTime.count() >= 0, "Disk io_time should be non-negative");
    }

    @Test
    void diskLimitRegistered() {
        Gauge limit = registry.find("system.disk.limit").gauge();
        assertNotNull(limit, "system.disk.limit should be registered");
        // Some disks (loop devices) may have size 0, just verify registration
        assertTrue(limit.value() >= 0, "Disk limit should be non-negative");
    }

    @Test
    void filesystemUsageRegistered() {
        Gauge used = registry.find("system.filesystem.usage").tag("system.filesystem.state", "used").gauge();
        Gauge free = registry.find("system.filesystem.usage").tag("system.filesystem.state", "free").gauge();
        assertNotNull(used, "system.filesystem.usage{state=used} should be registered");
        assertNotNull(free, "system.filesystem.usage{state=free} should be registered");
        // Some filesystems (e.g., tmpfs) may report 0; just verify non-negative
        assertTrue(used.value() >= 0, "Filesystem used should be non-negative");
        assertTrue(free.value() >= 0, "Filesystem free should be non-negative");
    }

    @Test
    void filesystemLimitRegistered() {
        Gauge limit = registry.find("system.filesystem.limit").gauge();
        assertNotNull(limit, "system.filesystem.limit should be registered");
        assertTrue(limit.value() >= 0, "Filesystem limit should be non-negative");
    }

    @Test
    void filesystemUtilizationRegistered() {
        Gauge used = registry.find("system.filesystem.utilization").tag("system.filesystem.state", "used").gauge();
        Gauge free = registry.find("system.filesystem.utilization").tag("system.filesystem.state", "free").gauge();
        assertNotNull(used, "system.filesystem.utilization{state=used} should be registered");
        assertNotNull(free, "system.filesystem.utilization{state=free} should be registered");
        assertTrue(used.value() >= 0 && used.value() <= 1, "Filesystem utilization used should be in [0,1]");
        assertTrue(free.value() >= 0 && free.value() <= 1, "Filesystem utilization free should be in [0,1]");
    }

    @Test
    void networkIoRegistered() {
        FunctionCounter recv = registry.find("system.network.io").tag("network.io.direction", "receive")
                .functionCounter();
        FunctionCounter xmit = registry.find("system.network.io").tag("network.io.direction", "transmit")
                .functionCounter();
        assertNotNull(recv, "system.network.io{direction=receive} should be registered");
        assertNotNull(xmit, "system.network.io{direction=transmit} should be registered");
        assertTrue(recv.count() >= 0, "Network bytes received should be non-negative");
    }

    @Test
    void networkPacketCountRegistered() {
        FunctionCounter recv = registry.find("system.network.packet.count").tag("network.io.direction", "receive")
                .functionCounter();
        assertNotNull(recv, "system.network.packet.count{direction=receive} should be registered");
        assertTrue(recv.count() >= 0, "Network packets received should be non-negative");
    }

    @Test
    void networkErrorsRegistered() {
        FunctionCounter recv = registry.find("system.network.errors").tag("network.io.direction", "receive")
                .functionCounter();
        assertNotNull(recv, "system.network.errors{direction=receive} should be registered");
        assertTrue(recv.count() >= 0, "Network errors should be non-negative");
    }

    @Test
    void networkConnectionCountRegistered() {
        Gauge tcp = registry.find("system.network.connection.count").tag("network.transport", "tcp").gauge();
        Gauge udp = registry.find("system.network.connection.count").tag("network.transport", "udp").gauge();
        assertNotNull(tcp, "system.network.connection.count{transport=tcp} should be registered");
        assertNotNull(udp, "system.network.connection.count{transport=udp} should be registered");
        assertTrue(tcp.value() >= 0, "TCP connection count should be non-negative");
        assertTrue(udp.value() >= 0, "UDP connection count should be non-negative");
    }

    @Test
    void builderSelectiveRegistration() {
        MeterRegistry selective = new SimpleMeterRegistry();
        OshiMetrics.builder(hal, os).enableCpu(true).enableMemory(false).enablePaging(false).enableDisk(false)
                .enableFileSystem(false).enableNetwork(false).enableGeneral(false).enableProcess(false)
                .enableContainer(false).build().bindTo(selective);
        // CPU should be registered
        assertNotNull(selective.find("system.cpu.time").functionCounter(), "CPU metrics should be registered");
        // Memory should NOT be registered
        assertNull(selective.find("system.memory.usage").gauge(), "Memory metrics should not be registered");
        // Network should NOT be registered
        assertNull(selective.find("system.network.io").functionCounter(), "Network metrics should not be registered");
    }

    @Test
    void processCpuTimeRegistered() {
        FunctionCounter user = registry.find("process.cpu.time").tag("cpu.mode", "user").functionCounter();
        FunctionCounter system = registry.find("process.cpu.time").tag("cpu.mode", "system").functionCounter();
        assertNotNull(user, "process.cpu.time{cpu.mode=user} should be registered");
        assertNotNull(system, "process.cpu.time{cpu.mode=system} should be registered");
        assertTrue(user.count() >= 0, "Process user CPU time should be non-negative");
    }

    @Test
    void processMemoryRegistered() {
        Gauge rss = registry.find("process.memory.usage").gauge();
        Gauge virt = registry.find("process.memory.virtual").gauge();
        assertNotNull(rss, "process.memory.usage should be registered");
        assertNotNull(virt, "process.memory.virtual should be registered");
        assertTrue(rss.value() > 0, "Process RSS should be positive");
        assertTrue(virt.value() > 0, "Process virtual memory should be positive");
    }

    @Test
    void processThreadCountRegistered() {
        Gauge threads = registry.find("process.thread.count").gauge();
        assertNotNull(threads, "process.thread.count should be registered");
        assertTrue(threads.value() >= 1, "Process thread count should be at least 1");
    }

    @Test
    void processUptimeRegistered() {
        Gauge uptime = registry.find("process.uptime").gauge();
        assertNotNull(uptime, "process.uptime should be registered");
        assertTrue(uptime.value() > 0, "Process uptime should be positive");
    }
}
