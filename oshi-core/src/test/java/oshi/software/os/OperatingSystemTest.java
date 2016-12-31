/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;

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
}
