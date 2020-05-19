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
        SystemInfo si = new SystemInfo();

        for (HWDiskStore disk : si.getHardware().getDiskStores()) {
            assertEquals("Disk should be equal to itself.", disk, disk);
            assertNotEquals("Disk should not be null", disk, null);
            assertNotEquals(disk, "A String");
            List<HWPartition> parts = disk.getPartitions();
            List<HWPartition> partList = new ArrayList<>(parts.size());
            for (HWPartition part : parts) {
                partList.add(new HWPartition(part.getIdentification(), part.getName(), part.getType(), part.getUuid(),
                        part.getSize(), part.getMajor(), part.getMinor(), part.getMountPoint()));
            }

            assertNotNull("Disk name should not be null", disk.getName());
            assertNotNull("Disk model should not be null", disk.getModel());
            assertNotNull("Disk serial number should not be null", disk.getSerial());
            assertTrue("Disk size should be greater or equal 0", disk.getSize() >= 0);
            assertTrue("Number of read operations from the disk should be greater or equal to 0", disk.getReads() >= 0);
            assertTrue("Number of read bytes should be greater or equal to 0", disk.getReadBytes() >= 0);
            assertTrue("Number of write operations from the disk should be greater or equal to 0",
                    disk.getWrites() >= 0);
            assertTrue("Number of write bytes should be greater or equal to 0", disk.getWriteBytes() >= 0);
            assertTrue("Transfer times should be greater or equal 0", disk.getTransferTime() >= 0);
            assertTrue("Update time for statistics should be greater or equal 0", disk.getTimeStamp() >= 0);
            assertTrue("toString method should contain the disk name", disk.toString().contains(disk.getName()));

            long oldReads = disk.getReads();
            long oldReadBytes = disk.getReadBytes();
            assertTrue("Updating the disk statistics should work", disk.updateAttributes());
            assertTrue("Number of reads from the disk has not been updated", disk.getReads() >= oldReads);
            assertTrue("Number of read bytes from the disk has not been updated", disk.getReadBytes() >= oldReadBytes);

            for (HWPartition partition : disk.getPartitions()) {
                assertNotNull("Identification of partition is null", partition.getIdentification());
                assertNotNull("Name of partition is null", partition.getName());
                assertNotNull("Type of partition is null", partition.getType());
                assertNotNull("UUID of partition is null", partition.getUuid());
                assertNotNull("Mount point of partition is null", partition.getMountPoint());
                assertTrue("Partition size of partition " + partition.getName() + " is smaller 0",
                        partition.getSize() >= 0);
                assertTrue("Major device ID of partition " + partition.getName() + "is smaller 0",
                        partition.getMajor() >= 0);
                assertTrue("Minor device ID of partition " + partition.getName() + "is smaller 0",
                        partition.getMinor() >= 0);
                assertTrue("Partition's toString() method should contain the partitions identification.",
                        partition.toString().contains(partition.getIdentification()));
            }
        }
    }
}
