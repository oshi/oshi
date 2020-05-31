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
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.jna.platform.win32.Kernel32; //NOSONAR
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.ProcessInformation;
import oshi.driver.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.windows.wmi.Win32LogicalDisk;
import oshi.driver.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

/**
 * The Windows File System contains {@link oshi.software.os.OSFileStore}s which
 * are a storage pool, device, partition, volume, concrete file system or other
 * implementation specific means of file storage. In Windows, these are
 * represented by a drive letter, e.g., "A:\" and "C:\"
 */
@ThreadSafe
public class WindowsFileSystem extends AbstractFileSystem {

    private static final int BUFSIZE = 255;

    private static final int SEM_FAILCRITICALERRORS = 0x0001;

    private static final int FILE_CASE_SENSITIVE_SEARCH = 0x00000001;
    private static final int FILE_CASE_PRESERVED_NAMES = 0x00000002;
    private static final int FILE_FILE_COMPRESSION = 0x00000010;
    private static final int FILE_DAX_VOLUME = 0x20000000;
    private static final int FILE_NAMED_STREAMS = 0x00040000;
    private static final int FILE_PERSISTENT_ACLS = 0x00000008;
    private static final int FILE_READ_ONLY_VOLUME = 0x00080000;
    private static final int FILE_SEQUENTIAL_WRITE_ONCE = 0x00100000;
    private static final int FILE_SUPPORTS_ENCRYPTION = 0x00020000;
    private static final int FILE_SUPPORTS_OBJECT_IDS = 0x00010000;
    private static final int FILE_SUPPORTS_REPARSE_POINTS = 0x00000080;
    private static final int FILE_SUPPORTS_SPARSE_FILES = 0x00000040;
    private static final int FILE_SUPPORTS_TRANSACTIONS = 0x00200000;
    private static final int FILE_SUPPORTS_USN_JOURNAL = 0x02000000;
    private static final int FILE_UNICODE_ON_DISK = 0x00000004;
    private static final int FILE_VOLUME_IS_COMPRESSED = 0x00008000;
    private static final int FILE_VOLUME_QUOTAS = 0x00000020;

    private static final Map<Integer, String> OPTIONS_MAP = new HashMap<>();
    static {
        OPTIONS_MAP.put(FILE_CASE_PRESERVED_NAMES, "casepn");
        OPTIONS_MAP.put(FILE_CASE_SENSITIVE_SEARCH, "casess");
        OPTIONS_MAP.put(FILE_FILE_COMPRESSION, "fcomp");
        OPTIONS_MAP.put(FILE_DAX_VOLUME, "dax");
        OPTIONS_MAP.put(FILE_NAMED_STREAMS, "streams");
        OPTIONS_MAP.put(FILE_PERSISTENT_ACLS, "acls");
        OPTIONS_MAP.put(FILE_SEQUENTIAL_WRITE_ONCE, "wronce");
        OPTIONS_MAP.put(FILE_SUPPORTS_ENCRYPTION, "efs");
        OPTIONS_MAP.put(FILE_SUPPORTS_OBJECT_IDS, "oids");
        OPTIONS_MAP.put(FILE_SUPPORTS_REPARSE_POINTS, "reparse");
        OPTIONS_MAP.put(FILE_SUPPORTS_SPARSE_FILES, "sparse");
        OPTIONS_MAP.put(FILE_SUPPORTS_TRANSACTIONS, "trans");
        OPTIONS_MAP.put(FILE_SUPPORTS_USN_JOURNAL, "journaled");
        OPTIONS_MAP.put(FILE_UNICODE_ON_DISK, "unicode");
        OPTIONS_MAP.put(FILE_VOLUME_IS_COMPRESSED, "vcomp");
        OPTIONS_MAP.put(FILE_VOLUME_QUOTAS, "quota");
    }

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

    /**
     * <p>
     * Constructor for WindowsFileSystem.
     * </p>
     */
    public WindowsFileSystem() {
        // Set error mode to fail rather than prompt for FLoppy/CD-Rom
        Kernel32.INSTANCE.SetErrorMode(SEM_FAILCRITICALERRORS);
    }

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        // Create list to hold results
        ArrayList<OSFileStore> result;

        // Begin with all the local volumes
        result = getLocalVolumes(null);

        // Build a map of existing mount point to OSFileStore
        Map<String, OSFileStore> volumeMap = new HashMap<>();
        for (OSFileStore volume : result) {
            volumeMap.put(volume.getMount(), volume);
        }

        // Iterate through volumes in WMI and update description (if it exists)
        // or add new if it doesn't (expected for network drives)
        for (OSFileStore wmiVolume : getWmiVolumes(null, localOnly)) {
            if (volumeMap.containsKey(wmiVolume.getMount())) {
                // If the volume is already in our list, update the name field
                // using WMI's more verbose name and update label if needed
                OSFileStore volume = volumeMap.get(wmiVolume.getMount());
                result.remove(volume);
                result.add(new WindowsOSFileStore(wmiVolume.getName(), volume.getVolume(),
                        volume.getLabel().isEmpty() ? wmiVolume.getLabel() : volume.getLabel(), volume.getMount(),
                        volume.getOptions(), volume.getUUID(), "", volume.getDescription(), volume.getType(),
                        volume.getFreeSpace(), volume.getUsableSpace(), volume.getTotalSpace(), 0, 0));
            } else if (!localOnly) {
                // Otherwise add the new volume in its entirety
                result.add(wmiVolume);
            }
        }
        return result;
    }

    /**
     * Package private method for getting all mounted local drives.
     *
     * @param volumeToMatch
     *            an optional string to filter match, null otherwise
     * @return A list of {@link OSFileStore} objects representing all local mounted
     *         volumes
     */
    static ArrayList<OSFileStore> getLocalVolumes(String volumeToMatch) {
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
        IntByReference pFlags;

        fs = new ArrayList<>();
        aVolume = new char[BUFSIZE];

        hVol = Kernel32.INSTANCE.FindFirstVolume(aVolume, BUFSIZE);
        if (hVol == WinBase.INVALID_HANDLE_VALUE) {
            return fs;
        }

        while (true) {
            fstype = new char[16];
            name = new char[BUFSIZE];
            mount = new char[BUFSIZE];
            pFlags = new IntByReference();

            userFreeBytes = new WinNT.LARGE_INTEGER(0L);
            totalBytes = new WinNT.LARGE_INTEGER(0L);
            systemFreeBytes = new WinNT.LARGE_INTEGER(0L);

            volume = new String(aVolume).trim();
            Kernel32.INSTANCE.GetVolumeInformation(volume, name, BUFSIZE, null, null, pFlags, fstype, 16);
            final int flags = pFlags.getValue();
            Kernel32.INSTANCE.GetVolumePathNamesForVolumeName(volume, mount, BUFSIZE, null);

            strMount = new String(mount).trim();
            if (!strMount.isEmpty() && (volumeToMatch == null || volumeToMatch.equals(volume))) {
                strName = new String(name).trim();
                strFsType = new String(fstype).trim();

                StringBuilder options = new StringBuilder((FILE_READ_ONLY_VOLUME & flags) == 0 ? "rw" : "ro");
                String moreOptions = OPTIONS_MAP.entrySet().stream().filter(e -> (e.getKey() & flags) > 0)
                        .map(Map.Entry::getValue).collect(Collectors.joining(","));
                if (!moreOptions.isEmpty()) {
                    options.append(',').append(moreOptions);
                }
                Kernel32.INSTANCE.GetDiskFreeSpaceEx(volume, userFreeBytes, totalBytes, systemFreeBytes);
                // Parse uuid from volume name
                String uuid = ParseUtil.parseUuidOrDefault(volume, "");

                fs.add(new WindowsOSFileStore(String.format("%s (%s)", strName, strMount), volume, strName, strMount,
                        options.toString(), uuid, "", getDriveType(strMount), strFsType, systemFreeBytes.getValue(),
                        userFreeBytes.getValue(), totalBytes.getValue(), 0, 0));
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
     * Package private method for getting logical drives listed in WMI.
     *
     * @param nameToMatch
     *            an optional string to filter match, null otherwise
     * @param localOnly
     *            Whether to only search local drives
     * @return A list of {@link OSFileStore} objects representing all network
     *         mounted volumes
     */
    static List<OSFileStore> getWmiVolumes(String nameToMatch, boolean localOnly) {
        long free;
        long total;
        List<OSFileStore> fs = new ArrayList<>();
        WmiResult<LogicalDiskProperty> drives = Win32LogicalDisk.queryLogicalDisk(nameToMatch, localOnly);
        for (int i = 0; i < drives.getResultCount(); i++) {
            free = WmiUtil.getUint64(drives, LogicalDiskProperty.FREESPACE, i);
            total = WmiUtil.getUint64(drives, LogicalDiskProperty.SIZE, i);
            String description = WmiUtil.getString(drives, LogicalDiskProperty.DESCRIPTION, i);
            String name = WmiUtil.getString(drives, LogicalDiskProperty.NAME, i);
            String label = WmiUtil.getString(drives, LogicalDiskProperty.VOLUMENAME, i);
            String options = WmiUtil.getUint16(drives, LogicalDiskProperty.ACCESS, i) == 1 ? "ro" : "rw";
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
            fs.add(new WindowsOSFileStore(String.format("%s (%s)", description, name), volume, label, name + "\\",
                    options, "", "", getDriveType(name), WmiUtil.getString(drives, LogicalDiskProperty.FILESYSTEM, i),
                    free, free, total, 0, 0));
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
    private static String getDriveType(String drive) {
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
        Map<HandleCountProperty, List<Long>> valueListMap = ProcessInformation.queryHandles().getB();
        List<Long> valueList = valueListMap.get(HandleCountProperty.HANDLECOUNT);
        long descriptors = 0L;
        if (valueList != null) {
            for (int i = 0; i < valueList.size(); i++) {
                descriptors += valueList.get(i);
            }
        }
        return descriptors;
    }

    @Override
    public long getMaxFileDescriptors() {
        return MAX_WINDOWS_HANDLES;
    }
}
