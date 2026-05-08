/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.spi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.nativefree.SystemInfo;
import oshi.software.os.OperatingSystem;

@EnabledOnOs(OS.LINUX)
class LinuxNativeFreeProviderTest {

    private static SystemInfo provider;

    @BeforeAll
    static void setUp() {
        provider = new SystemInfo();
    }

    @Test
    void testIsAvailable() {
        assertThat(provider.isAvailable(), is(true));
    }

    @Test
    void testPriority() {
        assertThat(provider.getPriority(), is(0));
    }

    @Test
    void testOperatingSystem() {
        OperatingSystem os = provider.getOperatingSystem();
        assertThat(os, is(notNullValue()));
        assertThat(os.getProcessId(), greaterThan(0));
        assertThat(os.getThreadCount(), greaterThan(0));
        assertThat(os.getFamily(), is(notNullValue()));
        assertThat(os.getFamily(), is(not("")));
    }

    @Test
    void testProcessor() {
        HardwareAbstractionLayer hal = provider.getHardware();
        CentralProcessor cpu = hal.getProcessor();
        assertThat(cpu, is(notNullValue()));
        assertThat(cpu.getLogicalProcessorCount(), greaterThan(0));
        assertThat(cpu.getPhysicalProcessorCount(), greaterThan(0));
        assertThat(cpu.getProcessorIdentifier().getName(), is(not("")));
    }

    @Test
    void testMemory() {
        HardwareAbstractionLayer hal = provider.getHardware();
        GlobalMemory mem = hal.getMemory();
        assertThat(mem, is(notNullValue()));
        assertThat(mem.getTotal(), greaterThan(0L));
        assertThat(mem.getAvailable(), greaterThanOrEqualTo(0L));
    }

    @Test
    void testFileSystem() {
        OperatingSystem os = provider.getOperatingSystem();
        assertThat(os.getFileSystem(), is(notNullValue()));
        assertThat(os.getFileSystem().getFileStores().size(), greaterThan(0));
    }
}
