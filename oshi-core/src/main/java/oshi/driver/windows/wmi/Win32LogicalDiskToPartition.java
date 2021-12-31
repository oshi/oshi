/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_LogicalDiskToPartition}
 */
@ThreadSafe
public final class Win32LogicalDiskToPartition {

    private static final String WIN32_LOGICAL_DISK_TO_PARTITION = "Win32_LogicalDiskToPartition";

    /**
     * Links disk drives to partitions
     */
    public enum DiskToPartitionProperty {
        ANTECEDENT, DEPENDENT, ENDINGADDRESS, STARTINGADDRESS;
    }

    private Win32LogicalDiskToPartition() {
    }

    /**
     * Queries the association between logical disk and partition.
     *
     * @param h
     *            An instantiated {@link WmiQueryHandler}. User should have already
     *            initialized COM.
     * @return Antecedent-dependent pairs of disk and partition.
     */
    public static WmiResult<DiskToPartitionProperty> queryDiskToPartition(WmiQueryHandler h) {
        WmiQuery<DiskToPartitionProperty> diskToPartitionQuery = new WmiQuery<>(WIN32_LOGICAL_DISK_TO_PARTITION,
                DiskToPartitionProperty.class);
        return h.queryWMI(diskToPartitionQuery, false);
    }
}
