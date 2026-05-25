/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSProcess;
import oshi.software.os.windows.WindowsOperatingSystemFFM;

@EnabledForJreRange(min = JRE.JAVA_25)
@EnabledOnOs(OS.WINDOWS)
class WindowsOperatingSystemFFMTest {

    private final TestWindowsOperatingSystemFFM os = new TestWindowsOperatingSystemFFM();

    @Test
    void testQueryBitness() {
        assertThat("64-bit JVM bitness should be returned directly", os.queryBitnessForTest(64), is(64));
        assertThat("32-bit JVM bitness should be 32 or 64", os.queryBitnessForTest(32), is(oneOf(32, 64)));
    }

    @Test
    void testQueryChildProcesses() {
        int pid = os.getProcessId();
        assertThat("Current process id should be positive", pid, is(greaterThan(0)));
        List<OSProcess> childProcesses = os.queryChildProcessesForTest(pid);
        assertThat("Child process query should not be null", childProcesses, is(notNullValue()));
        assertThat("Child process query should include the queried process",
                childProcesses.stream().anyMatch(p -> p.getProcessID() == pid), is(true));
    }

    @Test
    void testQueryDescendantProcesses() {
        int pid = os.getProcessId();
        assertThat("Current process id should be positive", pid, is(greaterThan(0)));
        List<OSProcess> descendantProcesses = os.queryDescendantProcessesForTest(pid);
        assertThat("Descendant process query should not be null", descendantProcesses, is(notNullValue()));
        assertThat("Descendant process query should include the queried process",
                descendantProcesses.stream().anyMatch(p -> p.getProcessID() == pid), is(true));
    }

    private static final class TestWindowsOperatingSystemFFM extends WindowsOperatingSystemFFM {
        private int queryBitnessForTest(int jvmBitness) {
            return queryBitness(jvmBitness);
        }

        private List<OSProcess> queryChildProcessesForTest(int parentPid) {
            return queryChildProcesses(parentPid);
        }

        private List<OSProcess> queryDescendantProcessesForTest(int parentPid) {
            return queryDescendantProcesses(parentPid);
        }
    }
}
