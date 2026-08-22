/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.WINDOWS)
class LogicalProcessorInformationTest {
    @Test
    void testGetLogicalProcessorInformation() {
        Triplet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>> info = LogicalProcessorInformation
                .getLogicalProcessorInformation();
        assertThat("Logical Processor list must not be empty", info.getA(), is(not(empty())));
        assertThat("Physical Processor list is null", info.getB(), is(nullValue()));
        assertThat("Cache list is null", info.getC(), is(nullValue()));
    }

    @Test
    void testGetLogicalProcessorInformationEx() {
        Triplet<List<LogicalProcessor>, @Nullable List<PhysicalProcessor>, @Nullable List<ProcessorCache>> info = LogicalProcessorInformation
                .getLogicalProcessorInformationEx();
        List<PhysicalProcessor> physical = info.getB();
        List<ProcessorCache> caches = info.getC();
        // Unlike the non-Ex path above, the Ex path is expected to populate both
        assertNotNull(physical, "Physical Processor list should not be null");
        assertNotNull(caches, "Cache list should not be null");
        assertThat("Must be more Logical Processors than Physical Ones", info.getA().size(),
                greaterThanOrEqualTo(physical.size()));
        assertThat("Must be more Physical Processors than L3 Caches", physical.size(),
                greaterThanOrEqualTo((int) caches.stream().filter(c -> c.getLevel() == 3).count()));
        assertThat("Must be more Physical Processors than L2 Caches", physical.size(),
                greaterThanOrEqualTo((int) caches.stream().filter(c -> c.getLevel() == 2).count()));
    }
}
