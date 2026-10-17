/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

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
import oshi.util.tuples.Pair;

/**
 * Abstract base for the FreeBSD FileSystem. Parses the mount table and builds the file stores; the JNA and FFM
 * subclasses supply the statfs reads.
 */
public abstract class FreeBsdFileSystem extends AbstractFileSystem {

    public static final String OSHI_FREEBSD_FS_PATH_EXCLUDES = "oshi.os.freebsd.filesystem.path.excludes";
    public static final String OSHI_FREEBSD_FS_PATH_INCLUDES = "oshi.os.freebsd.filesystem.path.includes";
    public static final String OSHI_FREEBSD_FS_VOLUME_EXCLUDES = "oshi.os.freebsd.filesystem.volume.excludes";
    public static final String OSHI_FREEBSD_FS_VOLUME_INCLUDES = "oshi.os.freebsd.filesystem.volume.includes";

    private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_FREEBSD_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_FREEBSD_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_FREEBSD_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_FREEBSD_FS_VOLUME_INCLUDES);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        Map<String, String> uuidMap = parseGeomPartList(ExecutingCommand.runNative("geom part list"));

        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Pair<Map<String, Long>, Map<String, Long>> inodes = parseDfInodes(ExecutingCommand.runNative("df -i"));
        Map<String, Long> inodeFreeMap = inodes.getA();
        Map<String, Long> inodeTotalMap = inodes.getB();

        // Get mount table
        for (String fs : ExecutingCommand.runNative("mount -p")) {
            String[] split = ParseUtil.whitespaces.split(fs, -1);
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
                    || (!path.equals("/") && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path,
                            volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES)))) {
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
            String uuid = uuidForVolume(volume, uuidMap);

            fsList.add(new FreeBsdOSFileStore(this, name, volume, name, path, options, uuid, isLocal, "", description,
                    type, freeSpace, usableSpace, totalSpace, inodeFreeMap.getOrDefault(path, 0L),
                    inodeTotalMap.getOrDefault(path, 0L)));
        }
        return fsList;
    }

    @Override
    public long getOpenFileDescriptors() {
        return querySysctl("kern.openfiles");
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
     * Looks up the partition UUID of a mounted volume by the device it names below {@code /dev}. Package-private for
     * testing.
     *
     * @param volume  the volume, as {@code mount -p} prints it
     * @param uuidMap the map from {@link #parseGeomPartList(List)}
     * @return the UUID, or an empty string for a volume that is not a partition device
     */
    static String uuidForVolume(String volume, Map<String, String> uuidMap) {
        return volume.startsWith("/dev/") ? uuidMap.getOrDefault(volume.substring(5), "") : "";
    }

    /**
     * Parses {@code geom part list} output into a map from each name a partition has below {@code /dev} to its rawuuid.
     * <p>
     * A partition can be mounted by its device ({@code da1p1}), by its GPT label ({@code gpt/rootfs}) or by its GPT ID
     * ({@code gptid/} followed by the rawuuid), so each is mapped. An empty label adds no entry.
     *
     * @param geomOutput the lines from {@code geom part list}
     * @return a map of device names, below {@code /dev}, to rawuuid
     */
    static Map<String, String> parseGeomPartList(List<String> geomOutput) {
        Map<String, String> uuidMap = new HashMap<>();
        String device = "";
        String uuid = "";
        for (String line : geomOutput) {
            line = line.trim();
            if (line.contains("Name: ")) {
                device = line.substring(line.lastIndexOf(' ') + 1);
                uuid = "";
            } else if (!device.isEmpty() && line.startsWith("rawuuid:")) {
                uuid = line.substring(line.lastIndexOf(' ') + 1);
                uuidMap.put(device, uuid);
                uuidMap.put("gptid/" + uuid, uuid);
            } else if (!uuid.isEmpty() && line.startsWith("label:")) {
                String label = line.substring("label:".length()).trim();
                if (!label.isEmpty() && !"(null)".equals(label)) {
                    uuidMap.put("gpt/" + label, uuid);
                }
            }
        }
        return uuidMap;
    }

    /**
     * Parses {@code df -i} output into inode free and total maps keyed by mount point.
     *
     * @param dfOutput the lines from {@code df -i}
     * @return a {@link Pair} of (inodeFreeMap, inodeTotalMap) both keyed by mount point
     */
    static Pair<Map<String, Long>, Map<String, Long>> parseDfInodes(List<String> dfOutput) {
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeTotalMap = new HashMap<>();
        for (String line : dfOutput) {
            /*- Sample Output:
            Filesystem    1K-blocks   Used   Avail Capacity iused  ifree %iused  Mounted on
            /dev/twed0s1a   2026030 584112 1279836    31%    2751 279871    1%   /
            */
            if (!line.startsWith("Filesystem")) {
                String[] split = ParseUtil.whitespaces.split(line, -1);
                if (split.length > 8) {
                    long ifree = ParseUtil.parseLongOrDefault(split[6], 0L);
                    long iused = ParseUtil.parseLongOrDefault(split[5], 0L);
                    // Key by mount point (last column) to match later lookup
                    inodeFreeMap.put(split[8], ifree);
                    inodeTotalMap.put(split[8], iused + ifree);
                }
            }
        }
        return new Pair<>(inodeFreeMap, inodeTotalMap);
    }

    /**
     * Reads a system-wide file-descriptor {@code sysctl} value via the subclass's native mechanism (JNA or FFM).
     *
     * @param name the sysctl name to query
     * @return the value, or 0 if unavailable
     */
    protected abstract long querySysctl(String name);
}
