/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSProcess;

/**
 * Tests the JNA Windows operating system backend, mirroring {@code WindowsOperatingSystemFFMTest}.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsOperatingSystemJNATest {

    private final TestWindowsOperatingSystemJNA os = new TestWindowsOperatingSystemJNA();

    @Test
    void testQueryBitness() {
        assertThat("64-bit JVM bitness should be returned directly", os.queryBitnessForTest(64), is(64));
        assertThat("32-bit JVM bitness should be 32 or 64", os.queryBitnessForTest(32), is(oneOf(32, 64)));
    }

    @Test
    void testQueryChildProcesses() {
        int pid = os.getProcessId();
        assertThat("Current process id should be positive", pid, is(greaterThan(0)));
        List<OSProcess> childProcesses = os.queryChildProcesses(pid);
        assertThat("Child process query should not be null", childProcesses, is(notNullValue()));
    }

    @Test
    void testQueryDescendantProcesses() {
        int pid = os.getProcessId();
        assertThat("Current process id should be positive", pid, is(greaterThan(0)));
        List<OSProcess> descendantProcesses = os.queryDescendantProcesses(pid);
        assertThat("Descendant process query should not be null", descendantProcesses, is(notNullValue()));
    }

    @Test
    void testQueryMapsFromPerfCounters() {
        assertThat("Process map from performance counters should not be null",
                WindowsOperatingSystemJNA.queryProcessMapFromPerfCounters(), is(notNullValue()));
        assertThat("Thread map from performance counters should not be null",
                WindowsOperatingSystemJNA.queryThreadMapFromPerfCounters(), is(notNullValue()));
    }

    private static final class TestWindowsOperatingSystemJNA extends WindowsOperatingSystemJNA {
        private int queryBitnessForTest(int jvmBitness) {
            return queryBitness(jvmBitness);
        }
    }
}
