/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem.OSVersionInfo;

/**
 * Test OS
 */
public class OperatingSystemTest {

    /**
     * Test operating system
     */
    @Test
    public void testOperatingSystem() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os.getFamily());
        assertNotNull(os.getManufacturer());
        OSVersionInfo versionInfo = os.getVersionInfo();
        assertNotNull(versionInfo);

        assertTrue(os.getSystemUptime() > 0);
        assertTrue(os.getSystemBootTime() > 0);
        assertTrue(os.getSystemBootTime() < System.currentTimeMillis() / 1000L);

        assertTrue(os.getProcessCount() >= 1);
        assertTrue(os.getThreadCount() >= 1);
        assertTrue(os.getBitness() == 32 || os.getBitness() == 64);
        assertTrue(os.getProcessId() > 0);
        assertEquals(os.isElevated(), os.isElevated());

        assertTrue(os.getProcesses(0, null).length > 0);
        OSProcess proc = os.getProcess(os.getProcessId());
        assertTrue(proc.getName().length() > 0);
        assertTrue(proc.getPath().length() > 0);
        assertNotNull(proc.getCommandLine());
        assertNotNull(proc.getCurrentWorkingDirectory());
        assertNotNull(proc.getUser());
        assertNotNull(proc.getUserID());
        assertNotNull(proc.getGroup());
        assertNotNull(proc.getGroupID());
        assertNotNull(proc.getState());
        assertEquals(proc.getProcessID(), os.getProcessId());
        assertTrue(proc.getParentProcessID() >= 0);
        assertTrue(proc.getThreadCount() > 0);
        assertTrue(proc.getPriority() >= -20 && proc.getPriority() <= 128);
        assertTrue(proc.getVirtualSize() >= 0);
        assertTrue(proc.getResidentSetSize() >= 0);
        assertTrue(proc.getKernelTime() >= 0);
        assertTrue(proc.getUserTime() >= 0);
        assertTrue(proc.getUpTime() >= 0);
        assertTrue(proc.calculateCpuPercent() >= 0d);
        assertTrue(proc.getStartTime() >= 0);
        assertTrue(proc.getBytesRead() >= 0);
        assertTrue(proc.getBytesWritten() >= 0);
        assertTrue(proc.getBitness() >= 0);
        assertTrue(proc.getBitness() <= 64);
        assertTrue(proc.getOpenFiles() >= -1);
    }

    /**
     * Tests process query by pid list
     */
    @Test
    public void testProcessQueryByList() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os.getFamily());
        assertNotNull(os.getManufacturer());
        OSVersionInfo versionInfo = os.getVersionInfo();
        assertNotNull(versionInfo);

        assertTrue(os.getProcessCount() >= 1);
        assertTrue(os.getThreadCount() >= 1);
        assertTrue(os.getProcessId() > 0);

        OSProcess[] processes = os.getProcesses(5, null);
        assertNotNull(processes);
        // every OS should have at least one process running on it
        assertTrue(processes.length > 0);
        // the list of pids we want info on
        List<Integer> pids = new ArrayList<>();
        for (OSProcess p : processes) {
            pids.add(p.getProcessID());
        }
        // query for just those processes
        Collection<OSProcess> processes1 = os.getProcesses(pids);
        // there's a potential for a race condition here, if a process we queried
        // for initially wasn't running during the second query. In this case,
        // try again with the shorter list
        while (processes1.size() < pids.size()) {
            pids.clear();
            for (OSProcess p : processes1) {
                pids.add(p.getProcessID());
            }
            // query for just those processes
            processes1 = os.getProcesses(pids);
        }
        assertEquals(processes1.size(), pids.size());

    }

    /**
     * Tests child process getter
     */
    @Test
    public void testGetChildProcesses() {
        // Get list of PIDS
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        OSProcess[] processes = os.getProcesses(0, null);
        Map<Integer, Integer> childMap = new HashMap<>();
        // First iteration to set all 0's
        for (OSProcess p : processes) {
            childMap.put(p.getProcessID(), 0);
            childMap.put(p.getParentProcessID(), 0);
        }
        // Second iteration to count children
        for (OSProcess p : processes) {
            childMap.put(p.getParentProcessID(), childMap.get(p.getParentProcessID()) + 1);
        }
        // Find a PID with 0, 1, and N>1 children
        int zeroPid = -1;
        int onePid = -1;
        int nPid = -1;
        int nNum = 0;
        int mPid = -1;
        int mNum = 0;
        for (Integer i : childMap.keySet()) {
            if (zeroPid < 0 && childMap.get(i) == 0) {
                zeroPid = i;
            } else if (onePid < 0 && childMap.get(i) == 1) {
                onePid = i;
            } else if (nPid < 0 && childMap.get(i) > 1) {
                // nPid is probably PID=1 with all PIDs with no other parent
                nPid = i;
                nNum = childMap.get(i);
            } else if (mPid < 0 && childMap.get(i) > 1) {
                mPid = i;
                mNum = childMap.get(i);
            }
            if (zeroPid >= 0 && onePid >= 0 && nPid >= 0 && mPid >= 0) {
                break;
            }
        }
        if (zeroPid >= 0) {
            assertEquals(0, os.getChildProcesses(zeroPid, 0, null).length);
        }
        if (SystemInfo.getCurrentPlatformEnum() != PlatformEnum.SOLARIS) {
            // Due to race condition, a process may terminate before we count
            // its children. Play the odds.
            // At least one of these tests should work.
            if (onePid >= 0 && nPid >= 0 && mPid >= 0) {
                assertTrue(os.getChildProcesses(onePid, 0, null).length == 1
                        || os.getChildProcesses(nPid, 0, null).length == nNum
                        || os.getChildProcesses(mPid, 0, null).length == mNum);
            }
        }
    }

    /**
     * Test OSProcess setters and getters
     */
    @Test
    public void testOSProcessSetters() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        OSProcess oldProcess = os.getProcess(os.getProcessId());

        OSProcess newProcess = new OSProcess(os);
        newProcess.setName(oldProcess.getName());
        newProcess.setPath(oldProcess.getPath());
        newProcess.setCommandLine(oldProcess.getCommandLine());
        newProcess.setCurrentWorkingDirectory(oldProcess.getCurrentWorkingDirectory());
        newProcess.setUser(oldProcess.getUser());
        newProcess.setUserID(oldProcess.getUserID());
        newProcess.setGroup(oldProcess.getGroup());
        newProcess.setGroupID(oldProcess.getGroupID());
        newProcess.setState(oldProcess.getState());
        newProcess.setProcessID(oldProcess.getProcessID());
        newProcess.setParentProcessID(oldProcess.getParentProcessID());
        newProcess.setThreadCount(oldProcess.getThreadCount());
        newProcess.setPriority(oldProcess.getPriority());
        newProcess.setVirtualSize(oldProcess.getVirtualSize());
        newProcess.setResidentSetSize(oldProcess.getResidentSetSize());
        newProcess.setKernelTime(oldProcess.getKernelTime());
        newProcess.setUserTime(oldProcess.getUserTime());
        newProcess.setUpTime(oldProcess.getUpTime());
        newProcess.setStartTime(oldProcess.getStartTime());
        newProcess.setBytesRead(oldProcess.getBytesRead());
        newProcess.setBytesWritten(oldProcess.getBytesWritten());

        assertEquals(oldProcess.getName(), newProcess.getName());
        assertEquals(oldProcess.getPath(), newProcess.getPath());
        assertEquals(oldProcess.getCommandLine(), newProcess.getCommandLine());
        assertEquals(oldProcess.getCurrentWorkingDirectory(), newProcess.getCurrentWorkingDirectory());
        assertEquals(oldProcess.getUser(), newProcess.getUser());
        assertEquals(oldProcess.getUserID(), newProcess.getUserID());
        assertEquals(oldProcess.getGroup(), newProcess.getGroup());
        assertEquals(oldProcess.getGroupID(), newProcess.getGroupID());
        assertEquals(oldProcess.getState(), newProcess.getState());
        assertEquals(oldProcess.getProcessID(), newProcess.getProcessID());
        assertEquals(oldProcess.getParentProcessID(), newProcess.getParentProcessID());
        assertEquals(oldProcess.getThreadCount(), newProcess.getThreadCount());
        assertEquals(oldProcess.getPriority(), newProcess.getPriority());
        assertEquals(oldProcess.getVirtualSize(), newProcess.getVirtualSize());
        assertEquals(oldProcess.getResidentSetSize(), newProcess.getResidentSetSize());
        assertEquals(oldProcess.getKernelTime(), newProcess.getKernelTime());
        assertEquals(oldProcess.getUserTime(), newProcess.getUserTime());
        assertEquals(oldProcess.getUpTime(), newProcess.getUpTime());
        assertEquals(oldProcess.getStartTime(), newProcess.getStartTime());
        assertEquals(oldProcess.getBytesRead(), newProcess.getBytesRead());
        assertEquals(oldProcess.getBytesWritten(), newProcess.getBytesWritten());
    }

    @Test
    public void testGetCommandLine() {
        int processesWithNonEmptyCmdLine = 0;

        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        for (OSProcess process : os.getProcesses(0, null)) {
            if (!process.getCommandLine().trim().isEmpty()) {
                processesWithNonEmptyCmdLine++;
            }
        }

        assertTrue(processesWithNonEmptyCmdLine >= 1);
    }

    @Test
    public void testConstructProcessWithGivenPid() throws InstantiationException {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();

        // Test using current process ID, which we are sure will exist during
        // this test
        int givenPid = os.getProcessId();
        OSProcess oldProcess = os.getProcess(givenPid);
        OSProcess newProcess = new OSProcess(os, givenPid);

        assertEquals(oldProcess.getPath(), newProcess.getPath());
        assertEquals(oldProcess.getProcessID(), newProcess.getProcessID());
        assertTrue(newProcess.updateAttributes());

        // Change the pid to a nonexistent one
        oldProcess.setProcessID(-1);
        assertFalse(oldProcess.updateAttributes());

        // Try to instantiate with a nonexistent PID
        try {
            newProcess = new OSProcess(os, -1);
            fail("Expected an InstantiationException");
        } catch (InstantiationException expected) {
            assertEquals("A process with ID -1 does not exist.", expected.getMessage());
        }
    }
}
