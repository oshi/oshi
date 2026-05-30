/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.netbsd;

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
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * The NetBSD File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage.
 */
@ThreadSafe
public class NetBsdFileSystem extends AbstractFileSystem {

    public static final String OSHI_NETBSD_FS_PATH_EXCLUDES = "oshi.os.netbsd.filesystem.path.excludes";
    public static final String OSHI_NETBSD_FS_PATH_INCLUDES = "oshi.os.netbsd.filesystem.path.includes";
    public static final String OSHI_NETBSD_FS_VOLUME_EXCLUDES = "oshi.os.netbsd.filesystem.volume.excludes";
    public static final String OSHI_NETBSD_FS_VOLUME_INCLUDES = "oshi.os.netbsd.filesystem.volume.includes";

    private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_NETBSD_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_NETBSD_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_NETBSD_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_NETBSD_FS_VOLUME_INCLUDES);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, localOnly);
    }

    // Called by NetBsdOSFileStore
    static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeUsedlMap = new HashMap<>();
        String command = "df -i" + (localOnly ? " -l" : "");
        for (String line : ExecutingCommand.runNative(command)) {
            /*- Sample Output:
             $ df -i
            Filesystem  512-blocks      Used     Avail Capacity iused   ifree  %iused  Mounted on
            /dev/wd0a      2149212    908676   1133076    45%    8355  147163     5%   /
            /dev/wd0e      4050876        36   3848300     0%      10  285108     0%   /home
            /dev/wd0d      6082908   3343172   2435592    58%   27813  386905     7%   /usr
            */
            if (line.startsWith("/")) {
                String[] split = ParseUtil.whitespaces.split(line);
                if (split.length > 6) {
                    inodeUsedlMap.put(split[0], ParseUtil.parseLongOrDefault(split[5], 0L));
                    inodeFreeMap.put(split[0], ParseUtil.parseLongOrDefault(split[6], 0L));
                }
            }
        }

        // Get mount table
        for (String fs : ExecutingCommand.runNative("mount")) { // NOSONAR squid:S135
            /*-
             Sample Output:
             /dev/dk0 on / type ffs (local)
             tmpfs on /tmp type tmpfs (local)
             kernfs on /kern type kernfs (local)
             */
            String[] split = ParseUtil.whitespaces.split(fs, 6);
            if (split.length >= 6) {
                // 1st field is volume name
                // 3rd field is mount point
                // 5th field is fs type
                // 6th field is options
                String volume = split[0];
                String path = split[2];
                String type = split[4];
                String options = split[5];
                String uuid = "";

                // Skip non-local drives if requested, and exclude pseudo file systems
                boolean isLocal = !NETWORK_FS_TYPES.contains(type);
                if ((localOnly && !isLocal) || !path.equals("/")
                        && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path, volume,
                                FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES))) {
                    continue;
                }

                String name = path.substring(path.lastIndexOf('/') + 1);
                // Special case for /, pull last element of volume instead
                if (name.isEmpty()) {
                    name = volume.substring(volume.lastIndexOf('/') + 1);
                }

                if (nameToMatch != null && !nameToMatch.equals(name)) {
                    continue;
                }
                File f = new File(path);
                long totalSpace = f.getTotalSpace();
                long usableSpace = f.getUsableSpace();
                long freeSpace = f.getFreeSpace();

                String description;
                if (volume.startsWith("/dev") || path.equals("/")) {
                    description = "Local Disk";
                } else if (volume.equals("tmpfs")) {
                    // dynamic size in memory FS
                    description = "Ram Disk (dynamic)";
                } else if (volume.equals("mfs")) {
                    // fixed size in memory FS
                    description = "Ram Disk (fixed)";
                } else if (NETWORK_FS_TYPES.contains(type)) {
                    description = "Network Disk";
                } else {
                    description = "Mount Point";
                }

                fsList.add(new NetBsdOSFileStore(name, volume, name, path, options, uuid, isLocal, "", description,
                        type, freeSpace, usableSpace, totalSpace, inodeFreeMap.getOrDefault(volume, 0L),
                        inodeUsedlMap.getOrDefault(volume, 0L) + inodeFreeMap.getOrDefault(volume, 0L)));
            }
        }
        return fsList;
    }

    @Override
    public long getOpenFileDescriptors() {
        return NetBsdSysctlUtil.sysctl("kern.nfiles", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return NetBsdSysctlUtil.sysctl("kern.maxfiles", 0);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return NetBsdSysctlUtil.sysctl("kern.maxfilesperproc", 0);
    }
}
