/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OperatingSystem;
import oshi.software.os.mac.MacOperatingSystemFFM;

@EnabledForJreRange(min = JRE.JAVA_25)
@TestInstance(Lifecycle.PER_CLASS)
public class OperatingSystemFFMTest {

    @Test
    void testOperatingSystem() {
        OperatingSystem os = new SystemInfo().getOperatingSystem();
        assertThat("OS shouldn't be null", os, is(notNullValue()));
        assertThat("OS family shouldn't be null", os.getFamily(), is(notNullValue()));
        assertThat("OS should have 1 or more currently running processes", os.getProcessCount(), is(greaterThan(0)));
        assertThat("OS should have at least 1 currently running process", os.getProcesses(null, null, 0),
                is(not(empty())));
    }

    @EnabledOnOs(OS.MAC)
    @Test
    void testGetMacOperatingSystem() {
        assertTrue(new SystemInfo().getOperatingSystem() instanceof MacOperatingSystemFFM);
    }
}
