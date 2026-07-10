/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.ParseUtil;

public abstract class OpenBsdFileSystem extends AbstractFileSystem {

    public static final String OSHI_OPENBSD_FS_PATH_EXCLUDES = "oshi.os.openbsd.filesystem.path.excludes";
    public static final String OSHI_OPENBSD_FS_PATH_INCLUDES = "oshi.os.openbsd.filesystem.path.includes";
    public static final String OSHI_OPENBSD_FS_VOLUME_EXCLUDES = "oshi.os.openbsd.filesystem.volume.excludes";
    public static final String OSHI_OPENBSD_FS_VOLUME_INCLUDES = "oshi.os.openbsd.filesystem.volume.includes";

    private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_OPENBSD_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_OPENBSD_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_OPENBSD_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_OPENBSD_FS_VOLUME_INCLUDES);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, localOnly);
    }

    @Override
    public long getOpenFileDescriptors() {
        return querySysctl("kern.nfiles");
    }

    @Override
    public long getMaxFileDescriptors() {
        return querySysctl("kern.maxfiles");
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return querySysctl("kern.maxfilesperproc");
    }

    /**
     * Reads a system-wide file-descriptor {@code sysctl} value via the subclass's native mechanism (JNA or FFM).
     *
     * @param name the sysctl name to query
     * @return the value, or 0 if unavailable
     */
    protected abstract long querySysctl(String name);

    /**
     * Gets file stores matching the given name (or all if null).
     *
     * @param nameToMatch name to filter on, or null for all
     * @param localOnly   if true, only return local file systems
     * @return list of matching file stores
     */
    public List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeUsedlMap = new HashMap<>();
        String command = "df -i" + (localOnly ? " -l" : "");
        for (String line : ExecutingCommand.runNative(command)) {
            if (line.startsWith("/")) {
                String[] split = ParseUtil.whitespaces.split(line);
                if (split.length > 6) {
                    inodeUsedlMap.put(split[0], ParseUtil.parseLongOrDefault(split[5], 0L));
                    inodeFreeMap.put(split[0], ParseUtil.parseLongOrDefault(split[6], 0L));
                }
            }
        }

        // Get mount table
        for (String fs : ExecutingCommand.runNative("mount -v")) { // NOSONAR squid:S135
            String[] split = ParseUtil.whitespaces.split(fs, 7);
            if (split.length == 7) {
                String volume = split[0];
                String uuid = split[1];
                String path = split[3];
                String type = split[5];
                String options = split[6];

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
                    description = "Ram Disk (dynamic)";
                } else if (volume.equals("mfs")) {
                    description = "Ram Disk (fixed)";
                } else if (NETWORK_FS_TYPES.contains(type)) {
                    description = "Network Disk";
                } else {
                    description = "Mount Point";
                }

                fsList.add(new OpenBsdOSFileStore(this, name, volume, name, path, options, uuid, isLocal, "",
                        description, type, freeSpace, usableSpace, totalSpace, inodeFreeMap.getOrDefault(volume, 0L),
                        inodeUsedlMap.getOrDefault(volume, 0L) + inodeFreeMap.getOrDefault(volume, 0L)));
            }
        }
        return fsList;
    }
}
