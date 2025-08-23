/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfoFFM;
import oshi.software.os.OperatingSystem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.MAC)
public class MacOperatingSystemFFMTest {

    @Test
    void testGetMacOperatingSystem() {
        SystemInfoFFM si = new SystemInfoFFM();
        OperatingSystem os = si.getOperatingSystem();
        assertTrue(os instanceof MacOperatingSystemFFM);

        assertThat("OS should have 1 or more currently running processes", os.getProcessCount(), is(greaterThan(0)));
        assertThat("OS should have at least 1 currently running process", os.getProcesses(null, null, 0),
                is(not(empty())));
    }
}
