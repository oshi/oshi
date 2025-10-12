/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import com.sun.jna.Platform;

import oshi.SystemInfo;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.software.os.OperatingSystem.ProcessFiltering;
import oshi.software.os.OperatingSystem.ProcessSorting;

/**
 * Test OS
 */
@EnabledForJreRange(max = JRE.JAVA_25)
@TestInstance(Lifecycle.PER_CLASS)
class OperatingSystemTest {

    protected OperatingSystem createOperatingSystem() {
        return new SystemInfo().getOperatingSystem();
    }

    protected OperatingSystem os = createOperatingSystem();
    protected OSProcess proc = os.getProcess(os.getProcessId());

    @BeforeAll
    void setUp() {
        // In rare cases on procfs based systems the proc call may result in null, so
        // we'll try a second time
        if (this.proc == null) {
            this.proc = os.getProcess(os.getProcessId());
        }
        // Fail here rather than more confusing NPEs later
        assertThat("Current process PID returned null", proc, is(notNullValue()));
    }

    @Test
    void testOperatingSystem() {
        assertThat("OS family shouldn't be null", os.getFamily(), is(notNullValue()));
        assertThat("OS manufacturer shouldn't be null", os.getManufacturer(), is(notNullValue()));
        OSVersionInfo versionInfo = os.getVersionInfo();
        assertThat("OS version info shouldn't be null", versionInfo, is(notNullValue()));

        assertThat("OS uptime in seconds should be greater than 0", os.getSystemUptime(), is(greaterThan(0L)));
        assertThat("OS boot time in seconds since Unix epoch should be greater than 0", os.getSystemBootTime(),
                is(greaterThan(0L)));
        assertThat("OS boot time in seconds since Unix epoch should be before the current time", os.getSystemBootTime(),
                is(lessThan(System.currentTimeMillis() / 1000L)));

        assertThat("OS should have 1 or more currently running processes", os.getProcessCount(), is(greaterThan(0)));
        assertThat("OS should have 1 or more currently running threads", os.getThreadCount(), is(greaterThan(0)));
        assertThat("OS bitness should either be 32 or 64 ", os.getBitness(), is(oneOf(32, 64)));
        assertThat("The current process id should be greater than 0", os.getProcessId(), is(greaterThan(0)));
        // Just exercise this code without error
        assertThat(
                "The current process' permissions (if has sudo or Administrator privileges) should be determined correctly",
                os.isElevated(), is(anything()));

        assertThat("OS should have at least 1 currently running process", os.getProcesses(null, null, 0),
                is(not(empty())));
    }

    @Test
    void testProcessStrings() {
        assertThat("Current running process name shouldn't be empty", proc.getName(), is(not(emptyString())));
        assertThat("Current running process path name shouldn't be empty", proc.getPath(), is(not(emptyString())));
        assertThat("Current running process command line shouldn't be null", proc.getCommandLine(), is(notNullValue()));
        assertThat("Current running process working directory shouldn't be null", proc.getCurrentWorkingDirectory(),
                is(notNullValue()));
        assertThat("Current running process user name shouldn't be null", proc.getUser(), is(notNullValue()));
        assertThat("Current running process user id shouldn't be null ", proc.getUserID(), is(notNullValue()));
        assertThat("Current running process group shouldn't be null", proc.getGroup(), is(notNullValue()));
        assertThat("Current running process group id shouldn't be null", proc.getGroupID(), is(notNullValue()));
    }

    @Test
    void testProcessStats() {
        assertThat("Current running process state shouldn't be INVALID", proc.getState(), is(not(State.INVALID)));
        assertThat("Current running process id should be equal to the OS current running process id", os.getProcessId(),
                is(proc.getProcessID()));
        assertThat("Current running process parent process id should be 0 or higher", proc.getParentProcessID(),
                is(greaterThanOrEqualTo(0)));
        assertThat("Current running process thread count should be greater than 0", proc.getThreadCount(),
                is(greaterThan(0)));
        if (Platform.isAIX()) {
            assertThat("Current running process priority should be between -20 and 255", proc.getPriority(),
                    is(both(greaterThanOrEqualTo(1)).and(lessThanOrEqualTo(255))));
        } else {
            assertThat("Current running process priority should be between -20 and 128", proc.getPriority(),
                    is(both(greaterThanOrEqualTo(-20)).and(lessThanOrEqualTo(128))));
        }
        assertThat("Current running process virtual memory size should be 0 or higher", proc.getVirtualSize(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current running process resident set size should be 0 or higher", proc.getResidentSetSize(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current running process time elapsed in system/kernel should be 0 or higher", proc.getKernelTime(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current running process time elapsed in user mode should be 0 or higher", proc.getUserTime(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current running process uptime should be 0 or higher", proc.getUpTime(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current process minor faults should be 0 or higher", proc.getMinorFaults(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current process major faults should be 0 or higher", proc.getMajorFaults(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current process context switches should be 0 or higher", proc.getContextSwitches(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current process cumulative cpu usage should be 0.0 or higher", proc.getProcessCpuLoadCumulative(),
                is(greaterThanOrEqualTo(0d)));
        assertThat("Current process cumulative cpu usage should be the same as the current process",
                proc.getProcessCpuLoadBetweenTicks(null),
                is(closeTo(proc.getProcessCpuLoadCumulative(), Double.MIN_VALUE)));
        assertThat(
                "Current process cumulative cpu usage should be the same for a previous snapshot of the same process",
                proc.getProcessCpuLoadBetweenTicks(proc),
                is(closeTo(proc.getProcessCpuLoadCumulative(), Double.MIN_VALUE)));
        assertThat("Current process start time should be 0 or higher", proc.getStartTime(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current process bytes read from disk should be 0 or higher", proc.getBytesRead(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Current process bytes written to disk should be 0 or higher", proc.getBytesWritten(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Process bitness can't exceed OS bitness", proc.getBitness(),
                is(lessThanOrEqualTo(os.getBitness())));
        assertThat("Bitness must be 0, 32 or 64", proc.getBitness(), is(oneOf(0, 32, 64)));
        assertThat("Current process open file handles should be -1 or higher", proc.getOpenFiles(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Soft open file limit for process should be -1 or higher", proc.getSoftOpenFileLimit(),
                is(greaterThanOrEqualTo(-1L)));
        assertThat("Hard open file limit for process should be -1 or higher", proc.getHardOpenFileLimit(),
                is(greaterThanOrEqualTo(-1L)));
    }

    @Test
    void testThreads() {
        List<OSThread> threads = proc.getThreadDetails();
        for (OSThread thread : threads) {
            assertThat("OS thread shouldn't be null", thread, is(notNullValue()));
        }
    }

    /**
     * Tests process query by pid list
     */
    @Test
    void testProcessQueryByList() {
        OperatingSystem os = createOperatingSystem();
        assertThat("OS family shouldn't be null", os.getFamily(), is(notNullValue()));
        assertThat("OS manufacturer shouldn't be null", os.getManufacturer(), is(notNullValue()));
        OSVersionInfo versionInfo = os.getVersionInfo();
        assertThat("OS version info shouldn't be null", versionInfo, is(notNullValue()));

        assertThat("OS currently running processes should be 1 or higher", os.getProcessCount(), is(greaterThan(0)));
        assertThat("OS thread count should be 1 or higher", os.getThreadCount(), is(greaterThan(0)));
        assertThat("OS current running process id should be 0 or higher", os.getProcessId(),
                is(greaterThanOrEqualTo(0)));

        List<OSProcess> processes = os.getProcesses(null, null, 0);
        assertThat("Currently running processes shouldn't be null", processes, is(notNullValue()));
        assertThat("every OS should have at least one process running on it", processes, is(not(empty())));
        // the list of pids we want info on
        List<Integer> pids = new ArrayList<>();
        for (OSProcess p : processes) {
            pids.add(p.getProcessID());
        }
        // query for just those processes
        Collection<OSProcess> processes1 = os.getProcesses(pids);
        // there's a potential for a race condition here, if a process we
        // queried for initially wasn't running during the second query. In this case,
        // try again with the shorter list
        while (processes1.size() < pids.size()) {
            pids.clear();
            for (OSProcess p : processes1) {
                pids.add(p.getProcessID());
            }
            // query for just those processes
            processes1 = os.getProcesses(pids);
        }
        assertThat("OS processes should match processes with pids we want info on", pids, hasSize(processes1.size()));

    }

    /**
     * Tests child and dependent process getter
     */
    @Test
    void testGetChildAndDependentProcesses() {
        // Testing child processes is tricky because we don't really know a priori what
        // processes might have children, and if we do test the full list vs. individual
        // processes, we run into a race condition where child processes can start or
        // stop before we measure a second time. So we can't really test for one-to-one
        // correspondence of child process lists.
        //
        // We can expect code logic failures to occur all/most of the time for
        // categories of processes, however, and allow occasional differences due to
        // race conditions. So we will test three categories of processes: Those with 0
        // children, those with exactly 1 child process, and those with multiple child
        // processes. On the second poll, we expect at least half of processes in those
        // categories to still be in the same category.
        //
        OperatingSystem os = createOperatingSystem();
        List<OSProcess> processes = os.getProcesses(null, null, 0);
        Map<Integer, Long> zeroChildMap = new HashMap<>();
        Map<Integer, Long> oneChildMap = new HashMap<>();
        Map<Integer, Long> manyChildMap = new HashMap<>();
        // Initialize all processes with no children
        for (OSProcess p : processes) {
            zeroChildMap.put(p.getProcessID(), p.getStartTime());
        }
        // Move parents with 1 or more children to other set
        for (OSProcess p : processes) {
            int ppid = p.getParentProcessID();
            long startTime = p.getStartTime();
            if (zeroChildMap.containsKey(ppid) && zeroChildMap.get(ppid) >= startTime) {
                // Zero to One
                oneChildMap.put(ppid, zeroChildMap.get(ppid));
                zeroChildMap.remove(ppid);
            } else if (oneChildMap.containsKey(ppid) && oneChildMap.get(ppid) >= startTime) {
                // One to many
                manyChildMap.put(ppid, oneChildMap.get(ppid));
                oneChildMap.remove(ppid);
            }
        }
        // Now test that majority of each set is in same category
        // Zero
        int matchedChild = 0;
        int matchedDescendant = 0;
        int descendantNotLessThanChild = 0;
        if (zeroChildMap.size() > 9) {
            int total = 0;
            for (Integer i : zeroChildMap.keySet()) {
                List<OSProcess> children = os.getChildProcesses(i, null, null, 0);
                List<OSProcess> descendants = os.getDescendantProcesses(i, null, null, 0);
                if (children.size() == 0) {
                    matchedChild++;
                }
                if (descendants.size() == 0) {
                    matchedDescendant++;
                }
                // This is more than enough to test
                if (++total > 9) {
                    break;
                }
            }
            assertThat("Most processes with no children should not suddenly have them.", matchedChild,
                    is(greaterThan(total / 3)));
            assertThat("Most processes with no children should not suddenly have descendants.", matchedDescendant,
                    is(greaterThan(total / 3)));
        }
        // One child
        matchedChild = 0;
        matchedDescendant = 0;
        descendantNotLessThanChild = 0;
        if (oneChildMap.size() > 9) {
            int total = 0;
            for (Integer i : oneChildMap.keySet()) {
                List<OSProcess> children = os.getChildProcesses(i, null, null, 0);
                List<OSProcess> descendants = os.getDescendantProcesses(i, null, null, 0);
                if (children.size() == 1) {
                    matchedChild++;
                }
                if (descendants.size() >= 1) {
                    matchedDescendant++;
                }
                if (descendants.size() >= children.size()) {
                    descendantNotLessThanChild++;
                }
                // This is more than enough to test
                if (++total > 9) {
                    break;
                }
            }
            assertThat("Most processes with one child should not suddenly have zero or more than one.", matchedChild,
                    is(greaterThan(total / 3)));
            assertThat("Most processes with one child should not suddenly have zero descendants.", matchedDescendant,
                    is(greaterThan(total / 3)));
            assertThat("Most processes with one child should have no more children than descendants",
                    descendantNotLessThanChild, is(greaterThan(total / 3)));
        }
        // Many children
        matchedChild = 0;
        matchedDescendant = 0;
        descendantNotLessThanChild = 0;
        if (manyChildMap.size() > 9) {
            int total = 0;
            for (Integer i : manyChildMap.keySet()) {
                // Use a non-null sorting for test purposes
                List<OSProcess> children = os.getChildProcesses(i, ProcessFiltering.VALID_PROCESS,
                        ProcessSorting.CPU_DESC, Integer.MAX_VALUE);
                List<OSProcess> descendants = os.getDescendantProcesses(i, ProcessFiltering.VALID_PROCESS,
                        ProcessSorting.CPU_DESC, Integer.MAX_VALUE);
                if (children.size() > 0) {
                    matchedChild++;
                }
                if (descendants.size() > 0) {
                    matchedDescendant++;
                }
                if (descendants.size() >= children.size()) {
                    descendantNotLessThanChild++;
                }
                // This is more than enough to test
                if (++total > 9) {
                    break;
                }
            }
            assertThat("Most processes with more than one child should not suddenly have none.", matchedChild,
                    is(greaterThan(total / 3)));
            assertThat("Most processes with more than one child should not suddenly have no descendants.",
                    matchedDescendant, is(greaterThan(total / 3)));
            assertThat("Most processes with more than one child should have no more children than descendants",
                    descendantNotLessThanChild, is(greaterThan(total / 3)));
        }
    }

    @Test
    void testGetCommandLine() {
        int processesWithNonEmptyCmdLine = 0;

        OperatingSystem os = createOperatingSystem();
        for (OSProcess process : os.getProcesses(null, null, 0)) {
            if (!process.getCommandLine().trim().isEmpty()) {
                processesWithNonEmptyCmdLine++;
            }
        }

        assertThat("Processes with non-empty command line should be 1 or higher", processesWithNonEmptyCmdLine,
                is(greaterThan(0)));
    }

    @Test
    void testGetArguments() {
        OperatingSystem os = createOperatingSystem();
        List<OSProcess> processesWithNonEmptyArguments = os.getProcesses(p -> !p.getArguments().isEmpty(), null, 0);

        assertThat("Processes with non-empty arguments should be non-empty", processesWithNonEmptyArguments,
                not(empty()));
    }

    @Test
    void testGetEnvironment() {
        int processesWithNonEmptyEnvironment = 0;

        OperatingSystem os = createOperatingSystem();
        for (OSProcess process : os.getProcesses(null, null, 0)) {
            if (!process.getEnvironmentVariables().isEmpty()) {
                processesWithNonEmptyEnvironment++;
            }
        }

        assertThat("Processes with non-empty environment should be 1 or higher", processesWithNonEmptyEnvironment,
                is(greaterThan(0)));
    }

    /**
     * Tests services getter
     */
    @Test
    void testGetServices() {
        OperatingSystem os = createOperatingSystem();
        List<OSService> services = os.getServices();
        // macOS CI typically has none, though, and some linux distros aren't covered
        if (!services.isEmpty()) {
            int stopped = 0;
            int running = 0;
            for (OSService svc : services) {
                assertThat(svc.getName(), is(not(emptyString())));
                switch (svc.getState()) {
                    case STOPPED:
                        stopped++;
                        break;
                    case RUNNING:
                        running++;
                        break;
                    default:
                        break;
                }
            }
            // Should be at least one of each
            assertThat("There should be at least 1 stopped service", stopped, is(greaterThan(0)));
            assertThat("There should be at least 1 running service", running, is(greaterThan(0)));
        }
    }

    /**
     * Tests sessions getter
     */
    @Test
    void testGetSessions() {
        OperatingSystem os = createOperatingSystem();
        for (OSSession sess : os.getSessions()) {
            assertThat("Logged in user's name for the session shouldn't be empty", sess.getUserName(),
                    is(not(emptyString())));
            assertThat("Sessions' terminal device name shouldn't be empty", sess.getTerminalDevice(),
                    is(not(emptyString())));
            // Login time
            assertThat(
                    String.format(Locale.ROOT, "Logon time should be before now: %d < %d%n%s", sess.getLoginTime(),
                            System.currentTimeMillis(), sess),
                    sess.getLoginTime(), is(lessThanOrEqualTo(System.currentTimeMillis())));
            assertThat("Session host shouldn't be null", sess.getHost(), is(notNullValue()));
        }
    }

    /**
     * Test get desktop windows
     */
    @Test
    void testGetDesktopWindows() {
        OperatingSystem os = createOperatingSystem();
        List<OSDesktopWindow> allWindows = os.getDesktopWindows(false);
        List<OSDesktopWindow> visibleWindows = os.getDesktopWindows(true);
        assertThat("Visible should be a subset of all windows", visibleWindows.size(),
                is(lessThanOrEqualTo(allWindows.size())));
        Set<Long> windowIds = new HashSet<>();
        for (OSDesktopWindow dw : visibleWindows) {
            assertThat("Visible window should be visible", dw.isVisible(), is(true));
            assertThat("Height should be nononegative", dw.getLocAndSize().height, is(greaterThanOrEqualTo(0)));
            assertThat("Width should be nononegative", dw.getLocAndSize().width, is(greaterThanOrEqualTo(0)));
            windowIds.add(dw.getWindowId());
        }
        assertThat("Window IDs should be unique", windowIds.size(), is(visibleWindows.size()));
    }
}
