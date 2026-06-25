/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.DevPath;
import oshi.util.linux.ProcPath;

/**
 * The Linux File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage. In Linux, these are found in the
 * /proc/mount filesystem, excluding temporary and kernel mounts.
 */
@ThreadSafe
public abstract class LinuxFileSystem extends AbstractFileSystem {

    /**
     * Default constructor.
     */
    protected LinuxFileSystem() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinuxFileSystem.class);

    /** Configuration key for path excludes. */
    public static final String OSHI_LINUX_FS_PATH_EXCLUDES = "oshi.os.linux.filesystem.path.excludes";
    /** Configuration key for path includes. */
    public static final String OSHI_LINUX_FS_PATH_INCLUDES = "oshi.os.linux.filesystem.path.includes";
    /** Configuration key for volume excludes. */
    public static final String OSHI_LINUX_FS_VOLUME_EXCLUDES = "oshi.os.linux.filesystem.volume.excludes";
    /** Configuration key for volume includes. */
    public static final String OSHI_LINUX_FS_VOLUME_INCLUDES = "oshi.os.linux.filesystem.volume.includes";

    private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_LINUX_FS_PATH_EXCLUDES);
    private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_LINUX_FS_PATH_INCLUDES);
    private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_LINUX_FS_VOLUME_EXCLUDES);
    private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_LINUX_FS_VOLUME_INCLUDES);

    private static final String UNICODE_SPACE = "\\040";

    private static final boolean CHECK_NFS = !"false"
            .equalsIgnoreCase(System.getProperty("oshi.os.linux.filesystem.checknfs"));

    // Matches addr= or mountaddr= in NFS mount options; captures up to the next comma
    // so IPv6 addresses (e.g. addr=2001:db8::1) are not truncated at the first colon.
    private static final Pattern NFS_ADDR_PATTERN = Pattern.compile("(?:^|,)(?:mount)?addr=([^,]+)");

    /**
     * Queries filesystem statistics for the given mount path.
     * <p>
     * Returns an array of [totalInodes, freeInodes, totalSpace, usableSpace, freeSpace], or {@code null} on failure
     * (the caller will fall back to {@link java.io.File} methods for space values).
     *
     * @param path the mount path to query
     * @return array of [totalInodes, freeInodes, totalSpace, usableSpace, freeSpace], or {@code null} on failure
     */
    protected abstract long[] queryStatvfs(String path);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, buildUuidMap(), localOnly);
    }

    /**
     * Builds a map of filesystem UUIDs to device paths.
     *
     * @return the UUID map
     */
    protected static Map<String, String> buildUuidMap() {
        // Map of volume with device path as key
        Map<String, String> volumeDeviceMap = new HashMap<>();
        File devMapper = new File(DevPath.MAPPER);
        File[] volumes = devMapper.listFiles();
        if (volumes != null) {
            for (File volume : volumes) {
                try {
                    volumeDeviceMap.put(volume.getCanonicalPath(), volume.getAbsolutePath());
                } catch (IOException e) {
                    LOG.debug("Couldn't get canonical path for {}. {}", volume.getName(), e.getMessage());
                }
            }
        }
        // Map uuids with device path as key
        Map<String, String> uuidMap = new HashMap<>();
        File uuidDir = new File(DevPath.DISK_BY_UUID);
        File[] uuids = uuidDir.listFiles();
        if (uuids != null) {
            for (File uuid : uuids) {
                try {
                    // Store UUID as value with path (e.g., /dev/sda1) and volumes as key
                    String canonicalPath = uuid.getCanonicalPath();
                    uuidMap.put(canonicalPath, uuid.getName().toLowerCase(Locale.ROOT));
                    if (volumeDeviceMap.containsKey(canonicalPath)) {
                        uuidMap.put(volumeDeviceMap.get(canonicalPath), uuid.getName().toLowerCase(Locale.ROOT));
                    }
                } catch (IOException e) {
                    LOG.debug("Couldn't get canonical path for {}. {}", uuid.getName(), e.getMessage());
                }
            }
        }
        return uuidMap;
    }

    List<OSFileStore> getFileStoreMatching(String nameToMatch, Map<String, String> uuidMap, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        Map<String, String> labelMap = queryLabelMap();

        // Parse /proc/mounts to get fs types
        List<String> mounts = FileUtil.readFile(ProcPath.MOUNTS);

        // Pre-probe all unique NFS hosts in parallel so a single 2-second timeout
        // covers all stale mounts regardless of how many there are.
        Map<String, Boolean> nfsHostReachable = CHECK_NFS ? probeNfsHosts(mounts) : Collections.emptyMap();

        for (String mount : mounts) {
            String[] split = mount.split(" ");
            // As reported in fstab(5) manpage, struct is:
            // 1st field is volume name
            // 2nd field is path with spaces escaped as \040
            // 3rd field is fs type
            // 4th field is mount options
            // 5th field is used by dump(8) (ignored)
            // 6th field is fsck order (ignored)
            if (split.length < 6) {
                continue;
            }

            // Exclude pseudo file systems
            String volume = split[0].replace(UNICODE_SPACE, " ");
            String name = volume;
            String path = split[1].replace(UNICODE_SPACE, " ");
            if (path.equals("/")) {
                name = "/";
            }
            String type = split[2];

            // Skip non-local drives if requested, and exclude pseudo file systems
            boolean isLocal = !NETWORK_FS_TYPES.contains(type);
            if ((localOnly && !isLocal)
                    || !path.equals("/") && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path,
                            volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES))) {
                continue;
            }

            String options = split[3];

            // If only updating for one name, skip others
            if (nameToMatch != null && !nameToMatch.equals(name)) {
                continue;
            }

            String uuid = uuidMap != null ? uuidMap.getOrDefault(split[0], "") : "";

            String description;
            if (volume.startsWith(DevPath.DEV)) {
                description = "Local Disk";
            } else if (volume.equals("tmpfs")) {
                description = "Ram Disk";
            } else if (NETWORK_FS_TYPES.contains(type)) {
                description = "Network Disk";
            } else {
                description = "Mount Point";
            }

            // Add in logical volume found at /dev/mapper, useful when linking
            // file system with drive.
            String logicalVolume = "";
            Path link = Paths.get(volume);
            if (link.toFile().exists() && Files.isSymbolicLink(link)) {
                try {
                    Path slink = Files.readSymbolicLink(link);
                    Path full = Paths.get(DevPath.MAPPER + slink.toString());
                    if (full.toFile().exists()) {
                        logicalVolume = full.normalize().toString();
                    }
                } catch (IOException e) {
                    LOG.debug("Couldn't access symbolic path  {}. {}", link, e.getMessage());
                }
            }

            long totalInodes = 0L;
            long freeInodes = 0L;
            long totalSpace = 0L;
            long usableSpace = 0L;
            long freeSpace = 0L;

            // For NFS mounts, skip statvfs if the server was found unreachable during
            // the parallel pre-probe above. Scoped to nfs/nfs4 only so non-NFS network
            // filesystems (cifs, fuse, ...) are not probed on port 2049.
            if (CHECK_NFS && isNfsType(type)) {
                String host = parseNfsAddr(options);
                if (host != null && Boolean.FALSE.equals(nfsHostReachable.get(host))) {
                    description = "Network Disk [unreachable]";
                    fsList.add(new LinuxOSFileStore(name, volume, labelMap.getOrDefault(path, name), path, options,
                            uuid, isLocal, logicalVolume, description, type, 0L, 0L, 0L, 0L, 0L, this, true));
                    continue;
                }
            }

            long[] vfs = queryStatvfs(path);
            if (vfs != null) {
                totalInodes = vfs[0];
                freeInodes = vfs[1];
                totalSpace = vfs[2];
                usableSpace = vfs[3];
                freeSpace = vfs[4];
            }
            // If native methods failed use JVM methods
            if (totalSpace == 0L) {
                File tmpFile = new File(path);
                totalSpace = tmpFile.getTotalSpace();
                usableSpace = tmpFile.getUsableSpace();
                freeSpace = tmpFile.getFreeSpace();
            }

            fsList.add(new LinuxOSFileStore(name, volume, labelMap.getOrDefault(path, name), path, options, uuid,
                    isLocal, logicalVolume, description, type, freeSpace, usableSpace, totalSpace, freeInodes,
                    totalInodes, this));
        }
        return fsList;
    }

    /** Returns true for mount types that use the NFS protocol and port 2049. */
    static boolean isNfsType(String type) {
        return "nfs".equals(type) || "nfs4".equals(type);
    }

    /**
     * Extracts the NFS server address from mount options. Returns {@code null} if no {@code addr=} or
     * {@code mountaddr=} field is present.
     */
    static String parseNfsAddr(String options) {
        Matcher m = NFS_ADDR_PATTERN.matcher(options);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Attempts a TCP connection to {@code host:port} within {@code timeoutMs}. Returns {@code true} on success or
     * refused connection (server is up); {@code false} on timeout or network error.
     */
    private static boolean tcpReachable(String host, int port, int timeoutMs) {
        // TCP connect to the standard NFS port (2049).
        // More reliable than InetAddress.isReachable() which uses ICMP (may need root)
        // or port 7 (echo, commonly firewalled).
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            LOG.debug("NFS host {} not reachable on port {}: {}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * Probes all unique NFS server hosts found in {@code mounts} in parallel, returning a map of host to reachability.
     * All probes share the same 2-second timeout window, so the total wait is at most 2 seconds regardless of the
     * number of NFS mounts.
     */
    private static Map<String, Boolean> probeNfsHosts(List<String> mounts) {
        Set<String> hosts = new HashSet<>();
        for (String mount : mounts) {
            String[] split = mount.split(" ");
            if (split.length >= 6 && isNfsType(split[2])) {
                String host = parseNfsAddr(split[3]);
                if (host != null) {
                    hosts.add(host);
                }
            }
        }
        Map<String, Boolean> reachable = new ConcurrentHashMap<>();
        if (hosts.isEmpty()) {
            return reachable;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(hosts.size(), 16));
        try {
            CompletableFuture<?>[] futures = hosts.stream()
                    .map(h -> CompletableFuture.runAsync(() -> reachable.put(h, tcpReachable(h, 2049, 2_000)), pool))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        } finally {
            pool.shutdownNow();
        }
        return reachable;
    }

    private static Map<String, String> queryLabelMap() {
        Map<String, String> labelMap = new HashMap<>();
        for (String line : ExecutingCommand.runNative("lsblk -o mountpoint,label")) {
            String[] split = ParseUtil.whitespaces.split(line, 2);
            if (split.length == 2) {
                labelMap.put(split[0], split[1]);
            }
        }
        return labelMap;
    }

    @Override
    public long getOpenFileDescriptors() {
        return getFileDescriptors(0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return getFileDescriptors(2);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return getFileDescriptorsPerProcess();
    }

    /**
     * Returns a value from the Linux system file /proc/sys/fs/file-nr.
     *
     * @param index The index of the value to retrieve. 0 returns the total allocated file descriptors. 1 returns the
     *              number of used file descriptors for kernel 2.4, or the number of unused file descriptors for kernel
     *              2.6. 2 returns the maximum number of file descriptors that can be allocated.
     * @return Corresponding file descriptor value from the Linux system file.
     */
    private static long getFileDescriptors(int index) {
        String filename = ProcPath.SYS_FS_FILE_NR;
        if (index < 0 || index > 2) {
            throw new IllegalArgumentException("Index must be between 0 and 2.");
        }
        List<String> osDescriptors = FileUtil.readFile(filename);
        if (!osDescriptors.isEmpty()) {
            String[] splittedLine = osDescriptors.get(0).split("\\D+");
            return ParseUtil.parseLongOrDefault(splittedLine[index], 0L);
        }
        return 0L;
    }

    private static long getFileDescriptorsPerProcess() {
        return FileUtil.getLongFromFile(ProcPath.SYS_FS_FILE_MAX);
    }
}
