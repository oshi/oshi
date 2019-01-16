/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.Kernel32; //NOSONAR
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.data.windows.PerfCounterQuery;
import oshi.data.windows.PerfCounterWildcardQuery;
import oshi.data.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * The Windows File System contains {@link OSFileStore}s which are a storage
 * pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Windows, these are represented by a drive
 * letter, e.g., "A:\" and "C:\"
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsFileSystem implements FileSystem {

    private static final long serialVersionUID = 1L;

    private static final int BUFSIZE = 255;

    private static final int SEM_FAILCRITICALERRORS = 0x0001;

    enum LogicalDiskProperty {
        DESCRIPTION, DRIVETYPE, FILESYSTEM, FREESPACE, NAME, PROVIDERNAME, SIZE;
    }

    private final transient WmiQuery<LogicalDiskProperty> logicalDiskQuery = new WmiQuery<>("Win32_LogicalDisk",
            LogicalDiskProperty.class);

    /*
     * For handle counts
     */
    enum HandleCountProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.TOTAL_INSTANCE),
        // Remaining elements define counters
        HANDLECOUNT("Handle Count");

        private final String counter;

        HandleCountProperty(String counter) {
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final transient PerfCounterWildcardQuery<HandleCountProperty> handlePerfCounters = new PerfCounterWildcardQuery<>(
            HandleCountProperty.class, "Process", "Win32_Process");

    private static final long MAX_WINDOWS_HANDLES;
    static {
        // Determine whether 32-bit or 64-bit handle limit, although both are
        // essentially infinite for practical purposes. See
        // https://blogs.technet.microsoft.com/markrussinovich/2009/09/29/pushing-the-limits-of-windows-handles/
        if (System.getenv("ProgramFiles(x86)") == null) {
            MAX_WINDOWS_HANDLES = 16_777_216L - 32_768L;
        } else {
            MAX_WINDOWS_HANDLES = 16_777_216L - 65_536L;
        }
    }

    public WindowsFileSystem() {
        // Set error mode to fail rather than prompt for FLoppy/CD-Rom
        Kernel32.INSTANCE.SetErrorMode(SEM_FAILCRITICALERRORS);
    }

    /**
     * Gets File System Information.
     *
     * @return An array of {@link OSFileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
    @Override
    public OSFileStore[] getFileStores() {
        // Create list to hold results
        ArrayList<OSFileStore> result;

        // Begin with all the local volumes
        result = getLocalVolumes();

        // Build a map of existing mount point to OSFileStore
        Map<String, OSFileStore> volumeMap = new HashMap<>();
        for (OSFileStore volume : result) {
            volumeMap.put(volume.getMount(), volume);
        }

        // Iterate through volumes in WMI and update description (if it exists)
        // or add new if it doesn't (expected for network drives)
        for (OSFileStore wmiVolume : getWmiVolumes()) {
            if (volumeMap.containsKey(wmiVolume.getMount())) {
                // If the volume is already in our list, update the name field
                // using WMI's more verbose name
                volumeMap.get(wmiVolume.getMount()).setName(wmiVolume.getName());
            } else {
                // Otherwise add the new volume in its entirety
                result.add(wmiVolume);
            }
        }
        return result.toArray(new OSFileStore[result.size()]);
    }

    /**
     * Private method for getting all mounted local drives.
     *
     * @return A list of {@link OSFileStore} objects representing all local
     *         mounted volumes
     */
    private ArrayList<OSFileStore> getLocalVolumes() {
        ArrayList<OSFileStore> fs;
        String volume;
        String strFsType;
        String strName;
        String strMount;
        WinNT.HANDLE hVol;
        WinNT.LARGE_INTEGER userFreeBytes;
        WinNT.LARGE_INTEGER totalBytes;
        WinNT.LARGE_INTEGER systemFreeBytes;
        boolean retVal;
        char[] aVolume;
        char[] fstype;
        char[] name;
        char[] mount;

        fs = new ArrayList<>();
        aVolume = new char[BUFSIZE];

        hVol = Kernel32.INSTANCE.FindFirstVolume(aVolume, BUFSIZE);
        if (hVol == WinNT.INVALID_HANDLE_VALUE) {
            return fs;
        }

        while (true) {
            fstype = new char[16];
            name = new char[BUFSIZE];
            mount = new char[BUFSIZE];

            userFreeBytes = new WinNT.LARGE_INTEGER(0L);
            totalBytes = new WinNT.LARGE_INTEGER(0L);
            systemFreeBytes = new WinNT.LARGE_INTEGER(0L);

            volume = new String(aVolume).trim();
            Kernel32.INSTANCE.GetVolumeInformation(volume, name, BUFSIZE, null, null, null, fstype, 16);
            Kernel32.INSTANCE.GetVolumePathNamesForVolumeName(volume, mount, BUFSIZE, null);
            Kernel32.INSTANCE.GetDiskFreeSpaceEx(volume, userFreeBytes, totalBytes, systemFreeBytes);

            strMount = new String(mount).trim();
            strName = new String(name).trim();
            strFsType = new String(fstype).trim();
            // Parse uuid from volume name
            String uuid = ParseUtil.parseUuidOrDefault(volume, "");

            if (!strMount.isEmpty()) {
                // Volume is mounted
                OSFileStore osStore = new OSFileStore();
                osStore.setName(String.format("%s (%s)", strName, strMount));
                osStore.setVolume(volume);
                osStore.setMount(strMount);
                osStore.setDescription(getDriveType(strMount));
                osStore.setType(strFsType);
                osStore.setUUID(uuid);
                osStore.setUsableSpace(systemFreeBytes.getValue());
                osStore.setTotalSpace(totalBytes.getValue());
                fs.add(osStore);
            }
            retVal = Kernel32.INSTANCE.FindNextVolume(hVol, aVolume, BUFSIZE);
            if (!retVal) {
                Kernel32.INSTANCE.FindVolumeClose(hVol);
                break;
            }
        }

        return fs;
    }

    /**
     * Private method for getting logical drives listed in WMI.
     *
     * @return A list of {@link OSFileStore} objects representing all network
     *         mounted volumes
     */
    private List<OSFileStore> getWmiVolumes() {
        long free;
        long total;
        List<OSFileStore> fs = new ArrayList<>();

        WmiResult<LogicalDiskProperty> drives = WmiQueryHandler.getInstance().queryWMI(this.logicalDiskQuery);

        for (int i = 0; i < drives.getResultCount(); i++) {
            free = WmiUtil.getUint64(drives, LogicalDiskProperty.FREESPACE, i);
            total = WmiUtil.getUint64(drives, LogicalDiskProperty.SIZE, i);
            String description = WmiUtil.getString(drives, LogicalDiskProperty.DESCRIPTION, i);
            String name = WmiUtil.getString(drives, LogicalDiskProperty.NAME, i);
            int type = WmiUtil.getUint32(drives, LogicalDiskProperty.DRIVETYPE, i);
            String volume;
            if (type != 4) {
                char[] chrVolume = new char[BUFSIZE];
                Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(name + "\\", chrVolume, BUFSIZE);
                volume = new String(chrVolume).trim();
            } else {
                volume = WmiUtil.getString(drives, LogicalDiskProperty.PROVIDERNAME, i);
                String[] split = volume.split("\\\\");
                if (split.length > 1 && split[split.length - 1].length() > 0) {
                    description = split[split.length - 1];
                }
            }

            OSFileStore osStore = new OSFileStore();
            osStore.setName(String.format("%s (%s)", description, name));
            osStore.setVolume(volume);
            osStore.setMount(name + "\\");
            osStore.setDescription(getDriveType(name));
            osStore.setType(WmiUtil.getString(drives, LogicalDiskProperty.FILESYSTEM, i));
            osStore.setUUID("");
            osStore.setUsableSpace(free);
            osStore.setTotalSpace(total);
            fs.add(osStore);
        }

        return fs;
    }

    /**
     * Private method for getting mounted drive type.
     *
     * @param drive
     *            Mounted drive
     * @return A drive type description
     */
    private String getDriveType(String drive) {
        switch (Kernel32.INSTANCE.GetDriveType(drive)) {
        case 2:
            return "Removable drive";
        case 3:
            return "Fixed drive";
        case 4:
            return "Network drive";
        case 5:
            return "CD-ROM";
        case 6:
            return "RAM drive";
        default:
            return "Unknown drive type";
        }
    }

    @Override
    public long getOpenFileDescriptors() {
        Map<HandleCountProperty, List<Long>> valueListMap = this.handlePerfCounters.queryValuesWildcard();
        List<Long> valueList = valueListMap.get(HandleCountProperty.HANDLECOUNT);
        long descriptors = 0L;
        for (int i = 0; i < valueList.size(); i++) {
            descriptors += valueList.get(i);
        }
        return descriptors;
    }

    @Override
    public long getMaxFileDescriptors() {
        return MAX_WINDOWS_HANDLES;
    }
}
