/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * The AIX File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage.
 */
@ThreadSafe
public class AixFileSystem extends AbstractFileSystem {

    public static final String OSHI_AIX_FS_PATH_EXCLUDES = "oshi.os.aix.filesystem.path.excludes";
    public static final String OSHI_AIX_FS_PATH_INCLUDES = "oshi.os.aix.filesystem.path.includes";
    public static final String OSHI_AIX_FS_VOLUME_EXCLUDES = "oshi.os.aix.filesystem.volume.excludes";
    public static final String OSHI_AIX_FS_VOLUME_INCLUDES = "oshi.os.aix.filesystem.volume.includes";

    private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_AIX_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_AIX_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_AIX_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_AIX_FS_VOLUME_INCLUDES);

    private static final Pattern FS_PATTERN = Pattern.compile("^(?:[\\w.]+:)?/");

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, localOnly);
    }

    // Called by AixOSFileStore
    static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        // AIX 7.3+ supports POSIX df -i, but AIX 7.1/7.2 only have the native format-specifier
        // syntax. df -F %n %l (%n = Ifree, %l = Iused) works across all three versions.
        String command = "df -F %n %l" + (localOnly ? " -T local" : "");
        Pair<Map<String, Long>, Map<String, Long>> inodes = parseDfInodes(ExecutingCommand.runNative(command));
        Map<String, Long> inodeFreeMap = inodes.getA();
        Map<String, Long> inodeTotalMap = inodes.getB();

        // Get mount table
        for (String fs : ExecutingCommand.runNative("mount")) { // NOSONAR squid:S135
            /*- Sample Output:
             *   node       mounted        mounted over    vfs       date        options
            * -------- ---------------  ---------------  ------ ------------ ---------------
            *          /dev/hd4         /                jfs2   Jun 16 09:12 rw,log=/dev/hd8
            *          /dev/hd2         /usr             jfs2   Jun 16 09:12 rw,log=/dev/hd8
            *          /dev/hd9var      /var             jfs2   Jun 16 09:12 rw,log=/dev/hd8
            *          /dev/hd3         /tmp             jfs2   Jun 16 09:12 rw,log=/dev/hd8
            *          /dev/hd11admin   /admin           jfs2   Jun 16 09:13 rw,log=/dev/hd8
            *          /proc            /proc            procfs Jun 16 09:13 rw
            *          /dev/hd10opt     /opt             jfs2   Jun 16 09:13 rw,log=/dev/hd8
            *          /dev/livedump    /var/adm/ras/livedump jfs2   Jun 16 09:13 rw,log=/dev/hd8
            * foo      /dev/fslv00      /home            jfs2   Jun 16 09:13 rw,log=/dev/loglv00
             */
            // Lines begin with optional node, which we don't use. To force sensible split
            // behavior, append any character at the beginning of the string
            String[] split = ParseUtil.whitespaces.split("x" + fs);
            if (split.length > 7) {
                // 1st field is volume name [0-index]
                // 2nd field is mount point
                // 3rd field is fs type
                // 4th-6th fields are date, ignored
                // 7th field is options
                String volume = split[1];
                String path = split[2];
                String type = split[3];
                String options = split[4];

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
                if (!f.exists() || f.getTotalSpace() < 0) {
                    continue;
                }
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

                fsList.add(new AixOSFileStore(name, volume, name, path, options, "", isLocal, "", description, type,
                        freeSpace, usableSpace, totalSpace, inodeFreeMap.getOrDefault(volume, 0L),
                        inodeTotalMap.getOrDefault(volume, 0L)));
            }
        }
        return fsList;
    }

    /**
     * Parses {@code df -F %n %l} output into inode free and total maps keyed by filesystem.
     *
     * @param dfOutput the lines from {@code df -F %n %l}
     * @return a {@link Pair} of (inodeFreeMap, inodeTotalMap) both keyed by filesystem
     */
    static Pair<Map<String, Long>, Map<String, Long>> parseDfInodes(List<String> dfOutput) {
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeTotalMap = new HashMap<>();
        for (String line : dfOutput) {
            /*- Sample Output:
             * $ df -F %n %l
             * Filesystem    512-blocks     Ifree    Iused
             * /dev/hd4         4194304   164951    15969
             * /dev/hd2        52690944  2894117   196692
             * /dev/hd9var      6291456   605443     2317
             * /dev/hd3         6291456   450968     1349
             * /dev/hd1         6291456   550843      110
             * /dev/hd11admin    2097152   233048        5
             * /proc                  -        -        -
             * /dev/hd10opt    16777216   600058    17220
             * /dev/livedump     524288    58200        4
             * NFS mounts appear as hostname:/path or ip:/path and are matched by FS_PATTERN
             */
            if (FS_PATTERN.matcher(line).find()) {
                String[] split = ParseUtil.whitespaces.split(line);
                // Columns: Filesystem, 512-blocks, Ifree (%n), Iused (%l)
                if (split.length >= 4) {
                    long free = ParseUtil.parseLongOrDefault(split[split.length - 2], 0L);
                    long used = ParseUtil.parseLongOrDefault(split[split.length - 1], 0L);
                    inodeTotalMap.put(split[0], free + used);
                    inodeFreeMap.put(split[0], free);
                }
            }
        }
        return new Pair<>(inodeFreeMap, inodeTotalMap);
    }

    @Override
    public long getOpenFileDescriptors() {
        boolean header = false;
        long openfiles = 0L;
        for (String f : ExecutingCommand.runNative("lsof -nl")) {
            if (!header) {
                header = f.startsWith("COMMAND");
            } else {
                openfiles++;
            }
        }
        return openfiles;
    }

    @Override
    public long getMaxFileDescriptors() {
        return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("ulimit -n"), 0L);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        final List<String> lines = FileUtil.readFile("/etc/security/limits");
        for (final String line : lines) {
            if (line.trim().startsWith("nofiles")) {
                return ParseUtil.parseLastLong(line, Long.MAX_VALUE);
            }
        }
        return Long.MAX_VALUE;
    }
}
