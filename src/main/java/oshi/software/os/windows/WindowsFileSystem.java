/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.WinNT;

import oshi.jna.platform.windows.Kernel32;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.platform.windows.WmiUtil;

/**
 * The Windows File System contains {@link OSFileStore}s which are a storage
 * pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Windows, these are represented by a drive
 * letter, e.g., "A:\" and "C:\"
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsFileSystem extends AbstractFileSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsFileSystem.class);

    private static final Pattern UUID_PATTERN = Pattern
            .compile(".+([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).+");

    private final int BUFSIZE = 255;

    public WindowsFileSystem() {
        // Set error mode to fail rather than prompt for FLoppy/CD-Rom
        Kernel32.INSTANCE.SetErrorMode(Kernel32.SEM_FAILCRITICALERRORS);
    }

    /**
     * Gets File System Information.
     *
     * @return An array of {@link OSFileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
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
        String volume, strFsType, strName, strMount;
        WinNT.HANDLE hVol;
        WinNT.LARGE_INTEGER userFreeBytes, totalBytes, systemFreeBytes;
        boolean retVal;
        char[] aVolume, fstype, name, mount;

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
            String uuid = "";
            Matcher m = UUID_PATTERN.matcher(volume.toLowerCase());
            if (m.matches()) {
                uuid = m.group(1);
            }

            if (!strMount.isEmpty()) {
                // Volume is mounted
                fs.add(new OSFileStore(String.format("%s (%s)", strName, strMount), volume, strMount,
                        getDriveType(strMount), strFsType, uuid, systemFreeBytes.getValue(), totalBytes.getValue()));
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
        Map<String, List<String>> drives;
        List<OSFileStore> fs;
        String volume, s;
        long free, total;

        fs = new ArrayList<>();

        drives = WmiUtil.selectStringsFrom(null, "Win32_LogicalDisk",
                "Name,Description,ProviderName,FileSystem,Freespace,Size", null);

        for (int i = 0; i < drives.get("Name").size(); i++) {
            free = 0L;
            total = 0L;
            try {
                s = drives.get("Freespace").get(i);
                free = s.equals("unknown") ? 0L : Long.parseLong(s);
                s = drives.get("Size").get(i);
                total = s.equals("unknown") ? 0L : Long.parseLong(s);
            } catch (NumberFormatException e) {
                LOG.error("Failed to parse drive space.");
                // leave as zero
            }
            String description = drives.get("Description").get(i);

            long type = WmiUtil.selectUint32From(null, "Win32_LogicalDisk", "DriveType",
                    "WHERE Name = '" + drives.get("Name").get(i) + "'");
            if (type != 4) {
                char[] chrVolume = new char[BUFSIZE];
                Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(drives.get("Name").get(i) + "\\", chrVolume,
                        BUFSIZE);
                volume = new String(chrVolume).trim();
            } else {
                volume = drives.get("ProviderName").get(i);
                String[] split = volume.split("\\\\");
                if (split.length > 1 && split[split.length - 1].length() > 0) {
                    description = split[split.length - 1];
                }
            }

            fs.add(new OSFileStore(String.format("%s (%s)", description, drives.get("Name").get(i)), volume,
                    drives.get("Name").get(i) + "\\", getDriveType(drives.get("Name").get(i)),
                    drives.get("FileSystem").get(i), "", free, total));
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
        return 0L;
    }

    @Override
    public long getMaxFileDescriptors() {
        return 0L;
    }
}
