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
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.software.os.OSProcess;

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

    @Test
    void testBuildMapsFromPerfCounters() {
        assertThat("Process map from performance counters should not be null",
                os.buildProcessMapFromPerfCountersForTest(), is(notNullValue()));
        assertThat("Thread map from performance counters should not be null",
                os.buildThreadMapFromPerfCountersForTest(), is(notNullValue()));
    }

    @Test
    void testQueryParentPidMap() {
        Map<Integer, Integer> parentPidMap = os.queryParentPidMapForTest();
        assertThat("Parent pid map should not be null", parentPidMap, is(notNullValue()));
        assertThat("Parent pid map should include the current process", parentPidMap.containsKey(os.getProcessId()),
                is(true));
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

        private @Nullable Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCountersForTest() {
            return buildProcessMapFromPerfCounters(null);
        }

        private @Nullable Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCountersForTest() {
            return buildThreadMapFromPerfCounters(null);
        }

        private Map<Integer, Integer> queryParentPidMapForTest() {
            return queryParentPidMap();
        }
    }
}
