/**
 * Oshi (https://github.com/dblock/oshi)
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os;

import org.junit.Test;
import oshi.SystemInfo;
import oshi.software.os.windows.WindowsProcess;

import static org.junit.Assert.*;

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
    public void testOSProcessSetters(){
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        OSProcess oldProcess = os.getProcess(os.getProcessId());

        WindowsProcess newProcess = new WindowsProcess(oldProcess.getName(),oldProcess.getPath(),
                7,oldProcess.getProcessID(),oldProcess.getParentProcessID(),oldProcess.getThreadCount(),
                oldProcess.getPriority(),oldProcess.getVirtualSize(),oldProcess.getResidentSetSize(),
                oldProcess.getKernelTime(),oldProcess.getUserTime(),oldProcess.getStartTime(),
                oldProcess.getBytesRead(),oldProcess.getBytesWritten(),oldProcess.getUpTime());

        newProcess.setState(oldProcess.getState());

        assertEquals(oldProcess.getBytesRead(),newProcess.getBytesRead());
        assertEquals(oldProcess.getBytesWritten(),newProcess.getBytesWritten());
        assertEquals(oldProcess.getKernelTime(),newProcess.getKernelTime());
        assertEquals(oldProcess.getName(),newProcess.getName());
        assertEquals(oldProcess.getParentProcessID(),newProcess.getParentProcessID());
        assertEquals(oldProcess.getPath(),newProcess.getPath());
        assertEquals(oldProcess.getPriority(),newProcess.getPriority());
        assertEquals(oldProcess.getProcessID(),newProcess.getProcessID());
        assertEquals(oldProcess.getResidentSetSize(),newProcess.getResidentSetSize());
        assertEquals(oldProcess.getBytesWritten(),newProcess.getBytesWritten());
        assertEquals(oldProcess.getKernelTime(),newProcess.getKernelTime());
        assertEquals(oldProcess.getStartTime(),newProcess.getStartTime());
        assertEquals(oldProcess.getVirtualSize(),newProcess.getVirtualSize());
        assertEquals(oldProcess.getState(),newProcess.getState());
        assertEquals(oldProcess.getUserTime(),newProcess.getUserTime());
    }
}
