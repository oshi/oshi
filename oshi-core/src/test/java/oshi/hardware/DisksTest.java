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
package oshi.hardware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
            List<HWPartition> parts = disk.getPartitions();
            List<HWPartition> partList = new ArrayList<>(parts.size());
            for (HWPartition part : parts) {
                partList.add(new HWPartition(part.getIdentification(), part.getName(), part.getType(), part.getUuid(),
                        part.getSize(), part.getMajor(), part.getMinor(), part.getMountPoint()));
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
            assertTrue(disk.toString().contains(disk.getName()));

            long oldReads = disk.getReads();
            long oldReadBytes = disk.getReadBytes();
            assertTrue(disk.updateAtrributes());
            assertTrue(disk.getReads() >= oldReads);
            assertTrue(disk.getReadBytes() >= oldReadBytes);

            lastDisk = new HWDiskStore();
            assertTrue(disk.compareTo(lastDisk) > 0);

            lastDisk.setModel(disk.getModel());
            lastDisk.setName(disk.getName());
            lastDisk.setSerial(disk.getSerial());
            lastDisk.setSize(disk.getSize());
            lastDisk.setPartitions(Collections.unmodifiableList(partList));

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
                assertTrue(partition.toString().contains(partition.getIdentification()));
            }
            HWPartition[] partitions = new HWPartition[2];
            partitions[0] = new HWPartition("id", "name", "type", "uuid", 123L, 345, 456, "mount");

            assertEquals(partitions[0], partitions[0]);
            assertNotEquals(partitions[0], null);
            assertNotEquals(partitions[0], "A string");

            partitions[1] = new HWPartition("id", "name", "type", "uuid", 123L, 345, 456, "mount");
            assertTrue(partitions[0].equals(partitions[1]));
            assertEquals(partitions[0].hashCode(), partitions[1].hashCode());

            partitions[1] = new HWPartition(null, "name", "type", "uuid", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1] = new HWPartition("", "name", "type", "uuid", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);
            assertTrue(partitions[0].compareTo(partitions[1]) > 0);

            partitions[1] = new HWPartition("id", "name", "type", "uuid", 123L, 0, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);
            partitions[1] = new HWPartition("id", "name", "type", "uuid", 123L, 345, 0, "mount");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1] = new HWPartition("id", "name", "type", "uuid", 123L, 345, 456, null);
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1] = new HWPartition("id", "name", "type", "uuid", 123L, 345, 456, "");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1] = new HWPartition("id", null, "type", "uuid", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1] = new HWPartition("id", "", "type", "uuid", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1] = new HWPartition("id", "name", "type", "uuid", 0L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1] = new HWPartition("id", "name", null, "uuid", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1] = new HWPartition("id", "name", "", "uuid", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);

            partitions[1] = new HWPartition("id", "name", "type", null, 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);
            assertNotEquals(partitions[1], partitions[0]);
            partitions[1] = new HWPartition("id", "name", "type", "", 123L, 345, 456, "mount");
            assertNotEquals(partitions[0], partitions[1]);

            disk.setPartitions(Collections.unmodifiableList(Arrays.asList(partitions)));
            List<HWPartition> diskPartitions = disk.getPartitions();
            assertEquals("id", diskPartitions.get(0).getIdentification());
            assertEquals("name", diskPartitions.get(0).getName());
            assertEquals("type", diskPartitions.get(0).getType());
            assertEquals("uuid", diskPartitions.get(0).getUuid());
            assertEquals("mount", diskPartitions.get(0).getMountPoint());
            assertEquals(123L, diskPartitions.get(0).getSize());
            assertEquals(345, diskPartitions.get(0).getMajor());
        }
    }
}
