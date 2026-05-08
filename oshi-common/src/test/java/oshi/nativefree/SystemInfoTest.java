/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.nativefree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Exercises the native-free Linux provider and logs human-readable output for validation.
 */
@EnabledOnOs(OS.LINUX)
class SystemInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoTest.class);

    private static oshi.nativefree.SystemInfo si;
    private static OperatingSystem os;
    private static HardwareAbstractionLayer hal;

    @BeforeAll
    static void setUp() {
        si = new oshi.nativefree.SystemInfo();
        os = si.getOperatingSystem();
        hal = si.getHardware();
    }

    @Test
    void testOperatingSystem() {
        assertThat(os, is(notNullValue()));
        logger.info("OS: {} {} {}", os.getFamily(), os.getManufacturer(), os.getVersionInfo());
        logger.info("Bitness: {}", os.getBitness());
        logger.info("Boot time: {}", os.getSystemBootTime());
        logger.info("Uptime: {} sec", os.getSystemUptime());
        logger.info("Process ID: {}", os.getProcessId());
        logger.info("Thread ID: {}", os.getThreadId());
        logger.info("Thread count: {}", os.getThreadCount());
        logger.info("Process count: {}", os.getProcessCount());

        assertThat(os.getFamily(), is(not("")));
        assertThat(os.getProcessId(), greaterThan(0));
        assertThat(os.getThreadCount(), greaterThan(0));
        assertThat(os.getSystemBootTime(), greaterThan(0L));
        assertThat(os.getSystemUptime(), greaterThan(0L));
    }

    @Test
    void testProcessor() {
        CentralProcessor cpu = hal.getProcessor();
        assertThat(cpu, is(notNullValue()));

        logger.info("Processor: {}", cpu.getProcessorIdentifier().getName());
        logger.info("  Vendor: {}", cpu.getProcessorIdentifier().getVendor());
        logger.info("  Logical CPUs: {}", cpu.getLogicalProcessorCount());
        logger.info("  Physical CPUs: {}", cpu.getPhysicalProcessorCount());
        logger.info("  Packages: {}", cpu.getPhysicalPackageCount());
        logger.info("  Max freq: {} Hz", cpu.getMaxFreq());

        assertThat(cpu.getLogicalProcessorCount(), greaterThan(0));
        assertThat(cpu.getPhysicalProcessorCount(), greaterThan(0));
        assertThat(cpu.getProcessorIdentifier().getName(), is(not("")));

        List<LogicalProcessor> logProcs = cpu.getLogicalProcessors();
        assertThat(logProcs.size(), is(cpu.getLogicalProcessorCount()));

        List<PhysicalProcessor> physProcs = cpu.getPhysicalProcessors();
        assertThat(physProcs.size(), is(cpu.getPhysicalProcessorCount()));

        List<ProcessorCache> caches = cpu.getProcessorCaches();
        for (ProcessorCache cache : caches) {
            logger.info("  Cache: L{} {} {} KB", cache.getLevel(), cache.getType(), cache.getCacheSize() / 1024);
        }

        long[] ticks = cpu.getSystemCpuLoadTicks();
        assertThat(ticks.length, is(TickType.values().length));
        logger.info("  Ticks: {}", Arrays.toString(ticks));

        long[] freqs = cpu.getCurrentFreq();
        assertThat(freqs.length, is(cpu.getLogicalProcessorCount()));
    }

    @Test
    void testMemory() {
        GlobalMemory mem = hal.getMemory();
        assertThat(mem, is(notNullValue()));

        logger.info("Memory: {} total, {} available", mem.getTotal(), mem.getAvailable());
        logger.info("  Page size: {}", mem.getPageSize());

        assertThat(mem.getTotal(), greaterThan(0L));
        assertThat(mem.getAvailable(), greaterThanOrEqualTo(0L));
        assertThat(mem.getPageSize(), greaterThan(0L));

        VirtualMemory vm = mem.getVirtualMemory();
        logger.info("  Swap: {} total, {} used", vm.getSwapTotal(), vm.getSwapUsed());
        assertThat(vm.getSwapTotal(), greaterThanOrEqualTo(0L));
    }

    @Test
    void testFileSystem() {
        FileSystem fs = os.getFileSystem();
        assertThat(fs, is(notNullValue()));

        List<OSFileStore> stores = fs.getFileStores();
        assertThat(stores.size(), greaterThan(0));

        for (OSFileStore store : stores) {
            logger.info("  {} ({}) at {} - {} total, {} usable", store.getName(), store.getType(), store.getMount(),
                    store.getTotalSpace(), store.getUsableSpace());
        }
    }

    @Test
    void testNetworkParams() {
        NetworkParams net = os.getNetworkParams();
        assertThat(net, is(notNullValue()));

        logger.info("Network: host={}, domain={}", net.getHostName(), net.getDomainName());
        logger.info("  DNS: {}", Arrays.toString(net.getDnsServers()));
        logger.info("  IPv4 gateway: {}", net.getIpv4DefaultGateway());
        logger.info("  IPv6 gateway: {}", net.getIpv6DefaultGateway());

        assertThat(net.getHostName(), is(not("")));
    }

    @Test
    void testCurrentProcess() {
        int pid = os.getProcessId();
        OSProcess proc = os.getProcess(pid);
        assertThat(proc, is(notNullValue()));

        logger.info("Current process [{}]: {}", pid, proc.getName());
        logger.info("  Path: {}", proc.getPath());
        logger.info("  User: {} ({})", proc.getUser(), proc.getUserID());
        logger.info("  Group: {} ({})", proc.getGroup(), proc.getGroupID());
        logger.info("  Threads: {}", proc.getThreadCount());
        logger.info("  RSS: {}", proc.getResidentMemory());
        logger.info("  Open files: {}", proc.getOpenFiles());

        assertThat(proc.getName(), is(not("")));
        assertThat(proc.getProcessID(), is(pid));
    }
}
