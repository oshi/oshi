/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
package oshi.hardware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
        long timeStamp = System.currentTimeMillis();
        SystemInfo si = new SystemInfo();

        HWDiskStore lastDisk = new HWDiskStore();
        for (HWDiskStore disk : si.getHardware().getDiskStores()) {
            assertEquals(disk, disk);
            assertNotEquals(disk, null);
            assertNotEquals(disk, "A String");
            assertNotEquals(disk, lastDisk);
            assertNotEquals(disk.hashCode(), lastDisk.hashCode());
            HWPartition[] parts = disk.getPartitions();
            HWPartition[] partArray = new HWPartition[parts.length];
            for (int i = 0; i < partArray.length; i++) {
                partArray[i] = new HWPartition();
                partArray[i].setIdentification(parts[i].getIdentification());
                partArray[i].setName(parts[i].getName());
                partArray[i].setType(parts[i].getType());
                partArray[i].setUuid(parts[i].getUuid());
                partArray[i].setMountPoint(parts[i].getMountPoint());
                partArray[i].setSize(parts[i].getSize());
                partArray[i].setMajor(parts[i].getMajor());
                partArray[i].setMinor(parts[i].getMinor());

            }

            assertNotNull(disk.getName());
            assertNotNull(disk.getModel());
            assertNotNull(disk.getSerial());
            assertTrue(disk.getSize() >= 0);
            assertTrue(disk.getReads() >= 0);
            assertTrue(disk.getReadBytes() >= 0);
            assertTrue(disk.getWrites() >= 0);
            assertTrue(disk.getWriteBytes() >= 0);
            assertTrue(disk.getTransferTime() >= 0);
            assertTrue(disk.getTimeStamp() >= 0);

            long oldReads = disk.getReads();
            long oldReadBytes = disk.getReadBytes();
            assertTrue(disk.updateDiskStats());
            assertTrue(disk.getReads() >= oldReads);
            assertTrue(disk.getReadBytes() >= oldReadBytes);

            lastDisk = new HWDiskStore();
            assertTrue(disk.compareTo(lastDisk) > 0);

            lastDisk.setModel(disk.getModel());
            lastDisk.setName(disk.getName());
            lastDisk.setSerial(disk.getSerial());
            lastDisk.setSize(disk.getSize());
            lastDisk.setPartitions(partArray);

            assertTrue(disk.equals(lastDisk));
            assertEquals(disk.hashCode(), lastDisk.hashCode());

            lastDisk.setModel("model");
            assertNotEquals(disk, lastDisk);
            assertNotEquals(lastDisk, disk);
            lastDisk.setModel(disk.getModel());
            assertEquals(disk, lastDisk);
            assertEquals(lastDisk, disk);

            lastDisk.setName("name");
            assertNotEquals(disk, lastDisk);
            assertNotEquals(lastDisk, disk);
            lastDisk.setName(disk.getName());
            assertEquals(disk, lastDisk);
            assertEquals(lastDisk, disk);

            lastDisk.setSerial("serial");
            assertNotEquals(lastDisk, disk);
            assertNotEquals(disk, lastDisk);
            lastDisk.setSerial(disk.getSerial());
            assertEquals(disk, lastDisk);
            assertEquals(lastDisk, disk);

            lastDisk.setSize(-1);
            assertNotEquals(lastDisk, disk);
            assertNotEquals(disk, lastDisk);
            lastDisk.setSize(disk.getSize());
            assertEquals(disk, lastDisk);
            assertEquals(lastDisk, disk);

            disk.setName("name");
            disk.setModel("model");
            disk.setSerial("serial");
            disk.setSize(123L);
            disk.setReads(456L);
            disk.setReadBytes(789L);
            disk.setWrites(101112L);
            disk.setWriteBytes(131415L);
            disk.setTransferTime(161718L);
            disk.setTimeStamp(timeStamp);

            assertEquals("name", disk.getName());
            assertEquals("model", disk.getModel());
            assertEquals("serial", disk.getSerial());
            assertEquals(123L, disk.getSize());
            assertEquals(456L, disk.getReads());
            assertEquals(789L, disk.getReadBytes());
            assertEquals(101112L, disk.getWrites());
            assertEquals(131415L, disk.getWriteBytes());
            assertEquals(161718L, disk.getTransferTime());
            assertEquals(timeStamp, disk.getTimeStamp());

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
            HWPartition[] partitions = new HWPartition[2];
            partitions[0] = new HWPartition();
            partitions[0].setIdentification("id");
            partitions[0].setName("name");
            partitions[0].setType("type");
            partitions[0].setUuid("uuid");
            partitions[0].setMountPoint("mount");
            partitions[0].setSize(123L);
            partitions[0].setMajor(345);
            partitions[0].setMinor(456);

            assertEquals(partitions[0], partitions[0]);
            assertNotEquals(partitions[0], null);
            assertNotEquals(partitions[0], "A string");

            partitions[1] = new HWPartition();
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1].setIdentification(null);
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1].setIdentification("");
            assertNotEquals(partitions[0], partitions[1]);
            assertTrue(partitions[0].compareTo(partitions[1]) > 0);
            partitions[1].setIdentification("id");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1].setMajor(345);
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1].setMinor(456);
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1].setMountPoint(null);
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1].setMountPoint("");
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1].setMountPoint("mount");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1].setName(null);
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1].setName("");
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1].setName("name");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1].setSize(123L);
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1].setType(null);
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1].setType("");
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1].setType("type");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1].setUuid(null);
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1].setUuid("");
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1].setUuid("uuid");
            assertTrue(partitions[0].equals(partitions[1]));
            assertEquals(partitions[0].hashCode(), partitions[1].hashCode());

            disk.setPartitions(partitions);
            partitions = disk.getPartitions();
            assertEquals("id", partitions[0].getIdentification());
            assertEquals("name", partitions[0].getName());
            assertEquals("type", partitions[0].getType());
            assertEquals("uuid", partitions[0].getUuid());
            assertEquals("mount", partitions[0].getMountPoint());
            assertEquals(123L, partitions[0].getSize());
            assertEquals(345, partitions[0].getMajor());
        }
    }
}
