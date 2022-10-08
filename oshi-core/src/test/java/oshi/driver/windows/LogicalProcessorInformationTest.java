/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

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
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> info = LogicalProcessorInformation
                .getLogicalProcessorInformation();
        assertThat("Logical Processor list must not be empty", info.getA(), is(not(empty())));
        assertThat("Physical Processor list is null", info.getB(), is(nullValue()));
        assertThat("Cache list is null", info.getC(), is(nullValue()));
    }

    @Test
    void testGetLogicalProcessorInformationEx() {
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> info = LogicalProcessorInformation
                .getLogicalProcessorInformationEx();
        assertThat("Must be more Logical Processors than Physical Ones", info.getA().size(),
                greaterThanOrEqualTo(info.getB().size()));
        assertThat("Must be more Physical Processors than L3 Caches", info.getB().size(),
                greaterThanOrEqualTo((int) info.getC().stream().filter(c -> c.getLevel() == 3).count()));
        assertThat("Must be more Physical Processors than L2 Caches", info.getB().size(),
                greaterThanOrEqualTo((int) info.getC().stream().filter(c -> c.getLevel() == 2).count()));
    }
}
