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
package oshi.hardware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test Disks
 */
public class DisksTest {

    /**
     * Test disks extraction.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testDisks() throws IOException {
        SystemInfo si = new SystemInfo();

        for (HWDiskStore disk : si.getHardware().getDiskStores()) {
            assertNotNull(disk.getName());
            assertNotNull(disk.getModel());
            assertNotNull(disk.getSerial());
            assertNotNull(disk.getSize());
            assertNotNull(disk.getReads());
            assertNotNull(disk.getWrites());

            disk.setName("name");
            disk.setModel("model");
            disk.setSerial("serial");
            disk.setSize(123L);
            disk.setReads(456L);
            disk.setWrites(789L);

            assertEquals("name", disk.getName());
            assertEquals("model", disk.getModel());
            assertEquals("serial", disk.getSerial());
            assertEquals(123L, disk.getSize());
            assertEquals(456L, disk.getReads());
            assertEquals(789L, disk.getWrites());
        }
    }
}
