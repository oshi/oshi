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
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_DiskDrive}
 */
@ThreadSafe
public final class Win32DiskDrive {

    private static final String WIN32_DISK_DRIVE = "Win32_DiskDrive";

    /**
     * Disk drive properties
     */
    public enum DiskDriveProperty {
        INDEX, MANUFACTURER, MODEL, NAME, SERIALNUMBER, SIZE;
    }

    /**
     * DeviceID and serial properties
     */
    public enum DeviceIdProperty {
        PNPDEVICEID, SERIALNUMBER;
    }

    private Win32DiskDrive() {
    }

    /**
     * Queries the disk drive name info
     *
     * @return Information regarding each disk drive.
     */
    public static WmiResult<DiskDriveProperty> queryDiskDrive() {
        WmiQuery<DiskDriveProperty> diskDriveQuery = new WmiQuery<>(WIN32_DISK_DRIVE, DiskDriveProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(diskDriveQuery);
    }

    /**
     * Queries the disk drive id info
     *
     * @param whereClause
     *            WQL "WHERE" clause limiting the search
     * @return Information regarding each disk drive's device id and serial number
     */
    public static WmiResult<DeviceIdProperty> queryDiskDriveId(String whereClause) {
        WmiQuery<DeviceIdProperty> deviceIdQuery = new WmiQuery<>(WIN32_DISK_DRIVE + whereClause,
                DeviceIdProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(deviceIdQuery);
    }
}
