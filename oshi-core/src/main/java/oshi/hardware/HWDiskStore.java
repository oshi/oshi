/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A storage mechanism where data are recorded by various electronic, magnetic,
 * optical, or mechanical changes to a surface layer of one or more rotating
 * disks or or flash storage such as a removable or solid state drive. In
 * constrast to a File System, defining the way an Operating system uses the
 * storage, the Disk Store represents the hardware which a FileSystem uses for
 * its File Stores.
 * <p>
 * Thread safe for the designed use of retrieving the most recent data. Users
 * should be aware that the {@link #updateAttributes()} method may update
 * attributes, including the time stamp, and should externally synchronize such
 * usage to ensure consistent calculations.
 */
@ThreadSafe
public interface HWDiskStore {

    /**
     * The disk name
     *
     * @return the name
     */
    String getName();

    /**
     * The disk model
     *
     * @return the model
     */
    String getModel();

    /**
     * The disk serial number, if available.
     *
     * @return the serial number
     */
    String getSerial();

    /**
     * The size of the disk
     *
     * @return the disk size, in bytes
     */
    long getSize();

    /**
     * The number of reads from the disk
     *
     * @return the reads
     */
    long getReads();

    /**
     * The number of bytes read from the disk
     *
     * @return the bytes read
     */
    long getReadBytes();

    /**
     * The number of writes to the disk
     *
     * @return the writes
     */
    long getWrites();

    /**
     * The number of bytes written to the disk
     *
     * @return the bytes written
     */
    long getWriteBytes();

    /**
     * The length of the disk queue (#I/O's in progress). Includes I/O requests that
     * have been issued to the device driver but have not yet completed. Not
     * supported on macOS.
     *
     * @return the current disk queue length
     */
    long getCurrentQueueLength();

    /**
     * The time spent reading or writing, in milliseconds.
     *
     * @return the transfer time
     */
    long getTransferTime();

    /**
     * The partitions on this disk.
     *
     * @return an {@code UnmodifiableList} of the partitions on this drive.
     */
    List<HWPartition> getPartitions();

    /**
     * The time this disk's statistics were updated.
     *
     * @return the timeStamp, in milliseconds since the epoch.
     */
    long getTimeStamp();

    /**
     * Make a best effort to update all the statistics about the drive without
     * needing to recreate the drive list. This method provides for more frequent
     * periodic updates of individual drive statistics but may be less efficient to
     * use if updating all drives. It will not detect if a removable drive has been
     * removed and replaced by a different drive in between method calls.
     *
     * @return True if the update was (probably) successful, false if the disk was
     *         not found
     */
    boolean updateAttributes();
}
