/*
 * MIT License
 *
 * Copyright (c) 2016-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test Disks
 */
class DisksTest {

    /**
     * Test disks extraction.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    void testDisks() throws IOException {
        SystemInfo si = new SystemInfo();

        for (HWDiskStore disk : si.getHardware().getDiskStores()) {
            assertThat(disk, is(notNullValue()));
            List<HWPartition> parts = disk.getPartitions();
            List<HWPartition> partList = new ArrayList<>(parts.size());
            for (HWPartition part : parts) {
                partList.add(new HWPartition(part.getIdentification(), part.getName(), part.getType(), part.getUuid(),
                        part.getSize(), part.getMajor(), part.getMinor(), part.getMountPoint()));
            }

            assertThat("Disk name should not be null", disk.getName(), is(notNullValue()));
            assertThat("Disk model should not be null", disk.getModel(), is(notNullValue()));
            assertThat("Disk serial number should not be null", disk.getSerial(), is(notNullValue()));
            assertThat("Disk size should be greater or equal 0", disk.getSize(), is(greaterThanOrEqualTo(0L)));
            assertThat("Number of read operations from the disk should be greater or equal to 0", disk.getReads(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("Number of read bytes should be greater or equal to 0", disk.getReadBytes(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("Number of write operations from the disk should be greater or equal to 0", disk.getWrites(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("Number of write bytes should be greater or equal to 0", disk.getWriteBytes(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("Transfer times should be greater or equal 0", disk.getTransferTime(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("Update time for statistics should be greater or equal 0", disk.getTimeStamp(),
                    is(greaterThanOrEqualTo(0L)));
            assertThat("toString method should contain the disk name", disk.toString(), containsString(disk.getName()));

            long oldReads = disk.getReads();
            long oldReadBytes = disk.getReadBytes();
            if (oldReads > 0) {
                assertThat("Updating the disk statistics should work for " + disk.getName(), disk.updateAttributes(),
                        is(true));
                assertThat("Number of reads from the disk has not been updated", disk.getReads(),
                        is(greaterThanOrEqualTo(oldReads)));
                assertThat("Number of read bytes from the disk has not been updated", disk.getReadBytes(),
                        is(greaterThanOrEqualTo(oldReadBytes)));
            }
            for (HWPartition partition : disk.getPartitions()) {
                assertThat("Identification of partition is null", partition.getIdentification(), is(notNullValue()));
                assertThat("Name of partition is null", partition.getName(), is(notNullValue()));
                assertThat("Type of partition is null", partition.getType(), is(notNullValue()));
                assertThat("UUID of partition is null", partition.getUuid(), is(notNullValue()));
                assertThat("Mount point of partition is null", partition.getMountPoint(), is(notNullValue()));
                assertThat("Partition size of partition " + partition.getName() + " is < 0", partition.getSize(),
                        is(greaterThanOrEqualTo(0L)));
                assertThat("Major device ID of partition " + partition.getName() + "is < 0", partition.getMajor(),
                        is(greaterThanOrEqualTo(0)));
                assertThat("Minor device ID of partition " + partition.getName() + "is < 0", partition.getMinor(),
                        is(greaterThanOrEqualTo(0)));
                assertThat("Partition's toString() method should contain the partitions identification.",
                        partition.toString(), containsString(partition.getIdentification()));
            }
        }
    }
}
