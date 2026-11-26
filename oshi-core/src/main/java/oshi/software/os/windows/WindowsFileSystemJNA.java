/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.perfmon.ProcessInformationJNA;
import oshi.driver.windows.wmi.Win32LogicalDiskJNA;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.software.common.os.windows.WindowsFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ParseUtil;

/**
 * The Windows File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage. In Windows, these are
 * represented by a drive letter, e.g., "A:\" and "C:\"
 */
@ThreadSafe
public class WindowsFileSystemJNA extends WindowsFileSystem {

    private static final int BUFSIZE = 255;

    private static final int SEM_FAILCRITICALERRORS = 0x0001;

    /**
     * Constructor for WindowsFileSystemJNA. Sets the Windows error mode via
     * {@code Kernel32.INSTANCE.SetErrorMode(SEM_FAILCRITICALERRORS)} to suppress system prompts for floppy/CD-ROM
     * drives that have no media.
     */
    public WindowsFileSystemJNA() {
        Kernel32.INSTANCE.SetErrorMode(SEM_FAILCRITICALERRORS);
    }

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        // Create list to hold results
        List<OSFileStore> result;

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
                result.add(new WindowsOSFileStoreJNA(wmiVolume.getName(), volume.getVolume(),
                        volume.getLabel().isEmpty() ? wmiVolume.getLabel() : volume.getLabel(), volume.getMount(),
                        volume.getOptions(), volume.getUUID(), volume.isLocal(), "", volume.getDescription(),
                        volume.getType(), volume.getFreeSpace(), volume.getUsableSpace(), volume.getTotalSpace(), 0,
                        0));
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
     * @param volumeToMatch an optional string to filter match, null otherwise
     * @return A list of {@link OSFileStore} objects representing all local mounted volumes
     */
    static List<OSFileStore> getLocalVolumes(@Nullable String volumeToMatch) {
        List<OSFileStore> fs;
        String volume;
        String strFsType;
        String strName;
        String strMount;
        WinNT.HANDLE hVol;
        WinNT.LARGE_INTEGER userFreeBytes;
        WinNT.LARGE_INTEGER totalBytes;
        WinNT.LARGE_INTEGER systemFreeBytes;
        char[] aVolume;
        char[] fstype;
        char[] name;
        char[] mount;

        fs = new ArrayList<>();
        aVolume = new char[BUFSIZE];

        hVol = Kernel32.INSTANCE.FindFirstVolume(aVolume, BUFSIZE);
        if (WinBase.INVALID_HANDLE_VALUE.equals(hVol)) {
            return fs;
        }
        try (CloseableIntByReference pFlags = new CloseableIntByReference();
                CloseableIntByReference pMountLen = new CloseableIntByReference()) {
            do {
                fstype = new char[16];
                name = new char[BUFSIZE];
                mount = new char[BUFSIZE];

                userFreeBytes = new WinNT.LARGE_INTEGER(0L);
                totalBytes = new WinNT.LARGE_INTEGER(0L);
                systemFreeBytes = new WinNT.LARGE_INTEGER(0L);

                volume = Native.toString(aVolume);
                // Skip the volume entirely if a native query fails, rather than emitting a phantom file store with a
                // blank name and zero size. Matches WindowsFileSystemFFM.
                if (!Kernel32.INSTANCE.GetVolumeInformation(volume, name, BUFSIZE, null, null, pFlags, fstype, 16)) {
                    continue;
                }
                final int flags = pFlags.getValue();
                // GetVolumePathNamesForVolumeName reports a too-small buffer with ERROR_MORE_DATA and writes the
                // required length; retry once with a right-sized buffer rather than dropping the volume.
                if (!Kernel32.INSTANCE.GetVolumePathNamesForVolumeName(volume, mount, mount.length, pMountLen)) {
                    if (Kernel32.INSTANCE.GetLastError() != WinError.ERROR_MORE_DATA) {
                        continue;
                    }
                    mount = new char[pMountLen.getValue()];
                    if (!Kernel32.INSTANCE.GetVolumePathNamesForVolumeName(volume, mount, mount.length, pMountLen)) {
                        continue;
                    }
                }

                strMount = Native.toString(mount);
                if (!strMount.isEmpty() && (volumeToMatch == null || volumeToMatch.equals(volume))) {
                    strName = Native.toString(name);
                    strFsType = Native.toString(fstype);

                    String options = volumeOptions(flags);
                    if (!Kernel32.INSTANCE.GetDiskFreeSpaceEx(volume, userFreeBytes, totalBytes, systemFreeBytes)) {
                        continue;
                    }
                    // Parse uuid from volume name
                    String uuid = ParseUtil.parseUuidOrDefault(volume, "");

                    fs.add(new WindowsOSFileStoreJNA(String.format(Locale.ROOT, "%s (%s)", strName, strMount), volume,
                            strName, strMount, options, uuid, true, "", getDriveType(strMount), strFsType,
                            systemFreeBytes.getValue(), userFreeBytes.getValue(), totalBytes.getValue(), 0, 0));
                }
            } while (Kernel32.INSTANCE.FindNextVolume(hVol, aVolume, BUFSIZE));
            return fs;
        } finally {
            Kernel32.INSTANCE.FindVolumeClose(hVol);
        }
    }

    /**
     * Package private method for getting logical drives listed in WMI.
     *
     * @param nameToMatch an optional string to filter match, null otherwise
     * @param localOnly   Whether to only search local drives
     * @return A list of {@link OSFileStore} objects representing all network mounted volumes
     */
    static List<OSFileStore> getWmiVolumes(@Nullable String nameToMatch, boolean localOnly) {
        long free;
        long total;
        List<OSFileStore> fs = new ArrayList<>();
        WmiResult<LogicalDiskProperty> drives = Win32LogicalDiskJNA.queryLogicalDisk(nameToMatch, localOnly);
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
                volume = Native.toString(chrVolume);
            } else {
                volume = WmiUtil.getString(drives, LogicalDiskProperty.PROVIDERNAME, i);
                // Last non-empty component, in case PROVIDERNAME ends with a trailing backslash
                String[] split = volume.split("\\\\", -1);
                for (int idx = split.length - 1; idx >= 0; idx--) {
                    if (!split[idx].isEmpty()) {
                        description = split[idx];
                        break;
                    }
                }
            }
            int driveType = Kernel32.INSTANCE.GetDriveType(name);
            boolean local = driveType == 2 || driveType == 3 || driveType == 6;
            fs.add(new WindowsOSFileStoreJNA(String.format(Locale.ROOT, "%s (%s)", description, name), volume, label,
                    name + "\\", options, "", local, "", getDriveType(name),
                    WmiUtil.getString(drives, LogicalDiskProperty.FILESYSTEM, i), free, free, total, 0, 0));
        }
        return fs;
    }

    /**
     * Private method for getting mounted drive type.
     *
     * @param drive Mounted drive
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
    protected Map<HandleCountProperty, Long> queryHandles() {
        return ProcessInformationJNA.queryHandles();
    }
}
