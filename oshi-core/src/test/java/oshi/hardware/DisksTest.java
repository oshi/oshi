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
import static org.junit.Assert.assertTrue;

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
            assertTrue(disk.getSize() >= 0);
            assertTrue(disk.getReads() >= 0);
            assertTrue(disk.getReadBytes() >= 0);
            assertTrue(disk.getWrites() >= 0);
            assertTrue(disk.getWriteBytes() >= 0);
            assertTrue(disk.getTransferTime() >= 0);

            disk.setName("name");
            disk.setModel("model");
            disk.setSerial("serial");
            disk.setSize(123L);
            disk.setReads(456L);
            disk.setReadBytes(789L);
            disk.setWrites(101112L);
            disk.setWriteBytes(131415L);
            disk.setTransferTime(161718L);

            assertEquals("name", disk.getName());
            assertEquals("model", disk.getModel());
            assertEquals("serial", disk.getSerial());
            assertEquals(123L, disk.getSize());
            assertEquals(456L, disk.getReads());
            assertEquals(789L, disk.getReadBytes());
            assertEquals(101112L, disk.getWrites());
            assertEquals(131415L, disk.getWriteBytes());
            assertEquals(161718L, disk.getTransferTime());

            for (HWPartition partition : disk.getPartitions()) {
                assertNotNull(partition.getIdentification());
                assertNotNull(partition.getName());
                assertNotNull(partition.getType());
                assertNotNull(partition.getUuid());
                assertNotNull(partition.getMountPoint());
                assertTrue(partition.getSize() >= 0);
                assertTrue(partition.getMajor() >= 0);
                assertTrue(partition.getMinor() >= 0);
            }

            HWPartition[] partitions = new HWPartition[1];
            partitions[0] = new HWPartition();
            partitions[0].setIdentification("id");
            partitions[0].setName("name");
            partitions[0].setType("type");
            partitions[0].setUuid("uuid");
            partitions[0].setMountPoint("mount");
            partitions[0].setSize(123L);
            partitions[0].setMajor(345);
            partitions[0].setMinor(456);
            disk.setPartitions(partitions);

            partitions = disk.getPartitions();
            assertEquals("id", partitions[0].getIdentification());
            assertEquals("name", partitions[0].getName());
            assertEquals("type", partitions[0].getType());
            assertEquals("uuid", partitions[0].getUuid());
            assertEquals("mount", partitions[0].getMountPoint());
            assertEquals(123L, partitions[0].getSize());
            assertEquals(345, partitions[0].getMajor());
            assertEquals(456, partitions[0].getMinor());
        }
    }
}
