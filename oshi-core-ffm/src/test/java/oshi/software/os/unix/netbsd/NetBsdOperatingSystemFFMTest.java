/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.ffm.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.platform.unix.netbsd.NetBsdCentralProcessorFFM;
import oshi.software.common.os.unix.netbsd.NetBsdOperatingSystem;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Exercises each native override against the command-line implementation it replaces.
 */
// JUnit's OS enum has no NetBSD constant
@EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
class NetBsdOperatingSystemFFMTest {

    private final SystemInfo si = new SystemInfo();
    private final OperatingSystem os = si.getOperatingSystem();

    @Test
    void testWiring() {
        assertThat(os, is(instanceOf(NetBsdOperatingSystemFFM.class)));
        assertThat(si.getHardware().getProcessor(), is(instanceOf(NetBsdCentralProcessorFFM.class)));
        assertThat(os.getCurrentProcess(), is(instanceOf(NetBsdOSProcessFFM.class)));
        assertThat(os.getNetworkParams(), is(instanceOf(NetBsdNetworkParamsFFM.class)));
    }

    @Test
    void testProcessAndThreadId() {
        assertThat(os.getProcessId(), is(new NetBsdOperatingSystem().getProcessId()));
        assertThat(os.getThreadId(), greaterThan(0));
    }

    @Test
    void testCpuTicks() {
        CentralProcessor cpu = si.getHardware().getProcessor();
        long[] ticks = cpu.getSystemCpuLoadTicks();
        assertThat(ticks[TickType.IDLE.getIndex()], greaterThan(0L));
        long[][] perCpu = cpu.getProcessorCpuLoadTicks();
        assertThat(perCpu, is(arrayWithSize(cpu.getLogicalProcessorCount())));
        for (long[] cpuTicks : perCpu) {
            assertThat(Arrays.stream(cpuTicks).sum(), greaterThan(0L));
        }
    }

    @Test
    void testLoadAverage() {
        double[] loadAvg = si.getHardware().getProcessor().getSystemLoadAverage(3);
        assertThat(Arrays.stream(loadAvg).boxed().toList(), everyItem(greaterThanOrEqualTo(0d)));
    }

    @Test
    void testOpenFileLimits() {
        OSProcess proc = os.getCurrentProcess();
        long soft = proc.getSoftOpenFileLimit();
        long hard = proc.getHardOpenFileLimit();
        assertThat(soft, greaterThan(0L));
        assertThat(soft, lessThanOrEqualTo(hard));
    }

    @Test
    void testRoutes() {
        // A VM always has at least a loopback route; the dump path must parse without falling back
        assertThat(os.getNetworkParams().getRoutes().size(), greaterThan(0));
    }

    @Test
    void testOtherProcessEnvironment() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("sleep", "30");
        pb.environment().put("OSHI_NETBSD_FFM", "present");
        Process child = pb.start();
        try {
            OSProcess proc = os.getProcess((int) child.pid());
            assertNotNull(proc);
            assertThat(proc.getEnvironmentVariables(), hasEntry("OSHI_NETBSD_FFM", "present"));
        } finally {
            child.destroy();
            child.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
