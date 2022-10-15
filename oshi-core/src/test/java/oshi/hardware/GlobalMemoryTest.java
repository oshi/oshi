/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import oshi.SystemInfo;

/**
 * Test GlobalMemory
 */
@TestInstance(Lifecycle.PER_CLASS)
class GlobalMemoryTest {
    private GlobalMemory memory = null;

    @BeforeAll
    void setUp() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        this.memory = hal.getMemory();
    }

    @Test
    void testGlobalMemory() {
        assertThat("Memory shouldn't be null", memory, is(notNullValue()));
        assertThat("Total memory should be greater than zero", memory.getTotal(), is(greaterThan(0L)));
        assertThat("Available memory should be between 0 and total memory", memory.getAvailable(),
                is(both(greaterThanOrEqualTo(0L)).and(lessThanOrEqualTo(memory.getTotal()))));
        assertThat("Memory page size should be greater than zero", memory.getPageSize(), is(greaterThan(0L)));
        assertThat("Memory toString should contain the substring \"Available\"", memory.toString(),
                containsString("Available"));
    }

    @Test
    void testPhysicalMemory() {
        List<PhysicalMemory> pm = memory.getPhysicalMemory();
        for (PhysicalMemory m : pm) {
            assertThat("Bank label shouldn't be null", m.getBankLabel(), is(notNullValue()));
            assertThat("Capacity should be nonnegative", m.getCapacity(), is(greaterThanOrEqualTo(0L)));
            assertThat("Speed should be nonnegative or -1", m.getClockSpeed(), is(greaterThanOrEqualTo(-1L)));
            assertThat("Manufacturer shouldn't be null", m.getManufacturer(), is(notNullValue()));
            assertThat("Memory type shouldn't be null", m.getMemoryType(), is(notNullValue()));
        }
    }
}
