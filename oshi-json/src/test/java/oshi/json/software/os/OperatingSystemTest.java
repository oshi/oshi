/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.software.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.json.SystemInfo;

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
        OperatingSystemVersion version = os.getVersion();
        assertNotNull(version);
        assertNotNull(version.getVersion());
        assertNotNull(version.getCodeName());
        assertNotNull(version.getBuildNumber());

        assertTrue(os.getProcessCount() >= 1);
        assertTrue(os.getThreadCount() >= 1);
        assertTrue(os.getProcessId() > 0);

        assertTrue(os.getProcesses(0, null).length > 0);
        OSProcess proc = os.getProcess(os.getProcessId());
        assertTrue(proc.getName().length() > 0);
        assertTrue(proc.getPath().length() > 0);
        assertTrue(proc.getCommandLine().length() > 0);
        assertNotNull(proc.getCurrentWorkingDirectory());
        assertNotNull(proc.getUser());
        assertNotNull(proc.getUserID());
        assertNotNull(proc.getGroup());
        assertNotNull(proc.getGroupID());
        assertNotNull(proc.getState());
        assertEquals(proc.getProcessID(), os.getProcessId());
        assertTrue(proc.getParentProcessID() > 0);
        assertTrue(proc.getThreadCount() > 0);
        assertTrue(proc.getPriority() >= -20 && proc.getPriority() <= 128);
        assertTrue(proc.getVirtualSize() >= proc.getResidentSetSize());
        assertTrue(proc.getResidentSetSize() >= 0);
        assertTrue(proc.getKernelTime() >= 0);
        assertTrue(proc.getUserTime() >= 0);
        assertTrue(proc.getUpTime() >= 0);
        assertTrue(proc.getStartTime() >= 0);
        assertTrue(proc.getBytesRead() >= 0);
        assertTrue(proc.getBytesWritten() >= 0);
    }

    /**
     * Test OSProcess setters and getters
     */
    @Test
    public void testOSProcessSetters() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        OSProcess oldProcess = os.getProcess(os.getProcessId());

        OSProcess newProcess = new OSProcess();
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
}
