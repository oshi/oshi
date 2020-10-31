/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import oshi.SystemInfo;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.software.os.OperatingSystem.ProcessSort;

/**
 * Test OS
 */
@TestInstance(Lifecycle.PER_CLASS)
class OperatingSystemTest {

    private OperatingSystem os = null;
    private OSProcess proc = null;

    @BeforeAll
    void setUp() {
        SystemInfo si = new SystemInfo();
        this.os = si.getOperatingSystem();
        this.proc = os.getProcess(os.getProcessId());
    }

    @Test
    void testOperatingSystem() {
        assertThat("OS family shouldn't be null", os.getFamily(), is(not(nullValue())));
        assertThat("OS manufacturer shouldn't be null", os.getManufacturer(), is(not(nullValue())));
        OSVersionInfo versionInfo = os.getVersionInfo();
        assertThat("OS version info shouldn't be null", versionInfo, is(not(nullValue())));

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
                os.isElevated(), is(either(equalTo(true)).or(equalTo(false))));

        assertThat("OS should have at least 1 currently running process", os.getProcesses(0, null), is(not(empty())));
    }

    @Test
    void testProcessStrings() {
        assertThat("Current running process name shouldn't be empty", proc.getName(), is(not(emptyString())));
        assertThat("Current running process path name shouldn't be empty", proc.getPath(), is(not(emptyString())));
        assertThat("Current running process command line shouldn't be null", proc.getCommandLine(),
                is(not(nullValue())));
        assertThat("Current running process working directory shouldn't be null", proc.getCurrentWorkingDirectory(),
                is(not(nullValue())));
        assertThat("Current running process user name shouldn't be null", proc.getUser(), is(not(nullValue())));
        assertThat("Current running process user id shouldn't be null ", proc.getUserID(), is(not(nullValue())));
        assertThat("Current running process group shouldn't be null", proc.getGroup(), is(not(nullValue())));
        assertThat("Current running process group id shouldn't be null", proc.getGroupID(), is(not(nullValue())));
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
        assertThat("Current running process priority should be between -20 and 128", proc.getPriority(),
                is(both(greaterThanOrEqualTo(-20)).and(lessThanOrEqualTo(128))));
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
        assertThat("Current process open file handles should be -1 or higher", proc.getOpenFiles() >= -1, is(true));
    }

    @Test
    void testThreads() {
        List<OSThread> threads = proc.getThreadDetails();
        for (OSThread thread : threads) {
            assertThat("OS thread shouldn't be null", thread, is(not(nullValue())));
        }
    }

    /**
     * Tests process query by pid list
     */
    @Test
    void testProcessQueryByList() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        assertThat("OS family shouldn't be null", os.getFamily(), is(not(nullValue())));
        assertThat("OS manufacturer shouldn't be null", os.getManufacturer(), is(not(nullValue())));
        OSVersionInfo versionInfo = os.getVersionInfo();
        assertThat("OS version info shouldn't be null", versionInfo, is(not(nullValue())));

        assertThat("OS currently running processes should be 1 or higher", os.getProcessCount(), is(greaterThan(0)));
        assertThat("OS thread count should be 1 or higher", os.getThreadCount(), is(greaterThan(0)));
        assertThat("OS current running process id should be 0 or higher", os.getProcessId(),
                is(greaterThanOrEqualTo(0)));

        List<OSProcess> processes = os.getProcesses(5, null);
        assertThat("Currently running processes shouldn't be null", processes, is(not(nullValue())));
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
     * Tests child process getter
     */
    @Test
    void testGetChildProcesses() {
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
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        List<OSProcess> processes = os.getProcesses(0, null);
        Set<Integer> zeroChildSet = new HashSet<>();
        Set<Integer> oneChildSet = new HashSet<>();
        Set<Integer> manyChildSet = new HashSet<>();
        // Initialize all processes with no children
        for (OSProcess p : processes) {
            zeroChildSet.add(p.getProcessID());
        }
        // Move parents with 1 or more children to other set
        for (OSProcess p : processes) {
            if (zeroChildSet.contains(p.getParentProcessID())) {
                // Zero to One
                zeroChildSet.remove(p.getParentProcessID());
                oneChildSet.add(p.getParentProcessID());
            } else if (oneChildSet.contains(p.getParentProcessID())) {
                // One to many
                oneChildSet.remove(p.getParentProcessID());
                manyChildSet.add(p.getParentProcessID());
            }
        }
        // Now test that majority of each set is in same category
        int matched = 0;
        int total = 0;
        for (Integer i : zeroChildSet) {
            if (os.getChildProcesses(i, 0, null).isEmpty()) {
                matched++;
            }
            // Quit if enough to test
            if (++total > 9) {
                break;
            }
        }
        if (total > 4) {
            assertThat("Most processes with no children should not suddenly have them.", matched,
                    is(greaterThan(total / 2)));
        }
        matched = 0;
        total = 0;
        for (Integer i : oneChildSet) {
            if (os.getChildProcesses(i, 0, null).size() == 1) {
                matched++;
            }
            // Quit if enough to test
            if (++total > 9) {
                break;
            }
        }
        if (total > 4) {
            assertThat("Most processes with one child should not suddenly have zero or more than one.", matched,
                    is(greaterThan(total / 2)));
        }
        matched = 0;
        total = 0;
        for (Integer i : manyChildSet) {
            // Use a non-null sorting for test purposes
            if (os.getChildProcesses(i, Integer.MAX_VALUE, ProcessSort.PID).size() > 1) {
                matched++;
            }
            // Quit if enough to test
            if (++total > 9) {
                break;
            }
        }
        if (total > 4) {
            assertThat("Most processes with more than one child should not suddenly have one or less.", matched,
                    is(greaterThan(total / 2)));
        }
    }

    @Test
    void testGetCommandLine() {
        int processesWithNonEmptyCmdLine = 0;

        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        for (OSProcess process : os.getProcesses(0, null)) {
            if (!process.getCommandLine().trim().isEmpty()) {
                processesWithNonEmptyCmdLine++;
            }
        }

        assertThat("Processes with non-empty command link should be 1 or higher", processesWithNonEmptyCmdLine,
                is(greaterThan(0)));
    }

    /**
     * Tests services getter
     */
    @Test
    void testGetServices() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        int stopped = 0;
        int running = 0;
        for (OSService svc : os.getServices()) {
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

    /**
     * Tests sessions getter
     */
    @Test
    void testGetSessions() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        for (OSSession sess : os.getSessions()) {
            assertThat("Logged in user's name for the session shouldn't be empty", sess.getUserName(),
                    is(not(emptyString())));
            assertThat("Sessions' terminal device name shouldn't be empty", sess.getTerminalDevice(),
                    is(not(emptyString())));
            // Login time
            assertThat(
                    String.format("Logon time should be before now: %d < %d%n%s", sess.getLoginTime(),
                            System.currentTimeMillis(), sess),
                    sess.getLoginTime(), is(lessThanOrEqualTo(System.currentTimeMillis())));
            assertThat("Session host shouldn't be null", sess.getHost(), is(not(nullValue())));
        }
    }
}
