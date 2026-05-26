/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.dragonflybsd.BsdSysctlUtil;

/**
 * The FreeBSD File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage.
 */
@ThreadSafe
public final class DragonFlyBsdFileSystem extends AbstractFileSystem {

    public static final String OSHI_DRAGONFLYBSD_FS_PATH_EXCLUDES = "oshi.os.dragonflybsd.filesystem.path.excludes";
    public static final String OSHI_DRAGONFLYBSD_FS_PATH_INCLUDES = "oshi.os.dragonflybsd.filesystem.path.includes";
    public static final String OSHI_DRAGONFLYBSD_FS_VOLUME_EXCLUDES = "oshi.os.dragonflybsd.filesystem.volume.excludes";
    public static final String OSHI_DRAGONFLYBSD_FS_VOLUME_INCLUDES = "oshi.os.dragonflybsd.filesystem.volume.includes";

    private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_DRAGONFLYBSD_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_DRAGONFLYBSD_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_DRAGONFLYBSD_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_DRAGONFLYBSD_FS_VOLUME_INCLUDES);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        // TODO map mount point to UUID?
        // is /etc/fstab useful for this?
        Map<String, String> uuidMap = new HashMap<>();
        // Now grab dmssg output
        String device = "";
        for (String line : ExecutingCommand.runNative("geom part list")) {
            if (line.contains("Name: ")) {
                device = line.substring(line.lastIndexOf(' ') + 1);
            }
            // If we aren't working with a current partition, continue
            if (device.isEmpty()) {
                continue;
            }
            line = line.trim();
            if (line.startsWith("rawuuid:")) {
                uuidMap.put(device, line.substring(line.lastIndexOf(' ') + 1));
                device = "";
            }
        }

        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeTotalMap = new HashMap<>();
        for (String line : ExecutingCommand.runNative("df -i")) {
            /*- Sample Output:
            Filesystem    1K-blocks   Used   Avail Capacity iused  ifree %iused  Mounted on
            /dev/twed0s1a   2026030 584112 1279836    31%    2751 279871    1%   /
            */
            if (line.startsWith("/")) {
                String[] split = ParseUtil.whitespaces.split(line);
                if (split.length > 8) {
                    long iused = ParseUtil.parseLongOrDefault(split[5], 0L);
                    long ifree = ParseUtil.parseLongOrDefault(split[6], 0L);
                    if (iused + ifree > 0) {
                        // Key by mount point (last column) to match later lookup
                        inodeFreeMap.put(split[8], ifree);
                        inodeTotalMap.put(split[8], iused + ifree);
                    }
                }
            }
        }

        // Get mount table
        for (String fs : ExecutingCommand.runNative("mount -p")) {
            String[] split = ParseUtil.whitespaces.split(fs);
            if (split.length < 5) {
                continue;
            }
            // 1st field is volume name
            // 2nd field is mount point
            // 3rd field is fs type
            // 4th field is options
            // other fields ignored
            String volume = split[0];
            String path = split[1];
            String type = split[2];
            String options = split[3];

            // Skip non-local drives if requested, and exclude pseudo file systems
            boolean isLocal = !NETWORK_FS_TYPES.contains(type);
            if ((localOnly && !isLocal)
                    || !path.equals("/") && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path,
                            volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES))) {
                continue;
            }

            String name = path.substring(path.lastIndexOf('/') + 1);
            // Special case for /, pull last element of volume instead
            if (name.isEmpty()) {
                name = volume.substring(volume.lastIndexOf('/') + 1);
            }
            File f = new File(path);
            long totalSpace = f.getTotalSpace();
            long usableSpace = f.getUsableSpace();
            long freeSpace = f.getFreeSpace();

            String description;
            if (volume.startsWith("/dev") || path.equals("/")) {
                description = "Local Disk";
            } else if (volume.equals("tmpfs")) {
                description = "Ram Disk";
            } else if (NETWORK_FS_TYPES.contains(type)) {
                description = "Network Disk";
            } else {
                description = "Mount Point";
            }
            // Match UUID
            String uuid = uuidMap.getOrDefault(name, "");

            fsList.add(new DragonFlyBsdOSFileStore(name, volume, name, path, options, uuid, isLocal, "", description,
                    type, freeSpace, usableSpace, totalSpace,
                    inodeFreeMap.containsKey(path) ? inodeFreeMap.get(path) : 0L,
                    inodeTotalMap.containsKey(path) ? inodeTotalMap.get(path) : 0L));
        }
        return fsList;
    }

    @Override
    public long getOpenFileDescriptors() {
        return BsdSysctlUtil.sysctl("kern.openfiles", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return BsdSysctlUtil.sysctl("kern.maxfiles", 0);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        // On DragonFlyBsd there is no process specific system-wide limit, so the general limit is returned
        return getMaxFileDescriptors();
    }
}
