/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.driver.common.unix.bsd.disk.MountTable;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.tuples.Pair;

/**
 * Abstract base for the OpenBSD FileSystem. Parses the mount table and builds the file stores; the JNA and FFM
 * subclasses supply the statfs reads.
 */
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
        // OpenBSD has no per-process sysctl; the hard RLIMIT_NOFILE is the ceiling a process may raise its limit to
        return capAtSystemLimit(queryHardOpenFileLimit(), getMaxFileDescriptors());
    }

    /**
     * Reads a system-wide file-descriptor {@code sysctl} value via the subclass's native mechanism (JNA or FFM).
     *
     * @param name the sysctl name to query
     * @return the value, or 0 if unavailable
     */
    protected abstract long querySysctl(String name);

    /**
     * Reads this process's hard {@code RLIMIT_NOFILE} via the subclass's native mechanism (JNA or FFM).
     *
     * @return the hard limit, or a negative value if unavailable
     */
    protected abstract long queryHardOpenFileLimit();

    /**
     * Gets file stores matching the given name (or all if null).
     *
     * @param nameToMatch name to filter on, or null for all
     * @param localOnly   if true, only return local file systems
     * @return list of matching file stores
     */
    public List<OSFileStore> getFileStoreMatching(@Nullable String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        String command = "df -i" + (localOnly ? " -l" : "");
        Pair<Map<String, Long>, Map<String, Long>> inodes = MountTable
                .parseDfInodes(ExecutingCommand.runNative(command));
        Map<String, Long> inodeFreeMap = inodes.getA();
        Map<String, Long> inodeTotalMap = inodes.getB();

        // Get mount table
        List<MountTable.Entry> mounts = MountTable.parseOpenBsdVerbose(ExecutingCommand.runNative("mount -v"));
        for (MountTable.Entry mount : mounts) { // NOSONAR java:S135
            String volume = mount.getVolume();
            String uuid = mount.getUuid();
            String path = mount.getPath();
            String type = mount.getType();
            String options = mount.getOptions();

            // Skip non-local drives if requested, and exclude pseudo file systems
            boolean isLocal = !NETWORK_FS_TYPES.contains(type);
            if ((localOnly && !isLocal)
                    || (!path.equals("/") && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path,
                            volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES)))) {
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

            fsList.add(new OpenBsdOSFileStore(this, name, volume, name, path, options, uuid, isLocal, "", description,
                    type, freeSpace, usableSpace, totalSpace, inodeFreeMap.getOrDefault(path, 0L),
                    inodeTotalMap.getOrDefault(path, 0L)));
        }
        return fsList;
    }
}
