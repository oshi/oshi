/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR
import com.sun.jna.platform.linux.LibC;

import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * The Linux File System contains {@link OSFileStore}s which are a storage pool,
 * device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Linux, these are found in the /proc/mount
 * filesystem, excluding temporary and kernel mounts.
 *
 * @author widdis[at]gmail[dot]com
 */
public class LinuxFileSystem implements FileSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxFileSystem.class);

    // Linux defines a set of virtual file systems
    private final List<String> pseudofs = Arrays.asList(new String[] { //
            "rootfs", // Minimal fs to support kernel boot
            "sysfs", // SysFS file system
            "proc", // Proc file system
            "devtmpfs", // Dev temporary file system
            "devpts", // Dev pseudo terminal devices file system
            "securityfs", // Kernel security file system
            "cgroup", // Cgroup file system
            "pstore", // Pstore file system
            "hugetlbfs", // Huge pages support file system
            "configfs", // Config file system
            "selinuxfs", // SELinux file system
            "systemd-1", // Systemd file system
            "binfmt_misc", // Binary format support file system
            "mqueue", // Message queue file system
            "debugfs", // Debug file system
            "nfsd", // NFS file system
            "sunrpc", // Sun RPC file system
            "rpc_pipefs", // Sun RPC file system
            "fusectl", // FUSE control file system
            // NOTE: FUSE's fuseblk is not evalued because used as file system
            // representation of a FUSE block storage
            // "fuseblk" // FUSE block file system
            // "tmpfs", // Temporary file system
            // NOTE: tmpfs is evaluated apart, because Linux uses it for
            // RAMdisks
    });

    // System path mounted as tmpfs
    private final List<String> tmpfsPaths = Arrays.asList(new String[] { "/dev/shm", "/run", "/sys", "/proc" });

    /**
     * Checks if file path equals or starts with an element in the given list
     *
     * @param aList
     *            A list of path prefixes
     * @param charSeq
     *            a path to check
     * @return true if the charSeq exactly equals, or starts with the directory
     *         in aList
     */
    private boolean listElementStartsWith(List<String> aList, String charSeq) {
        for (String match : aList) {
            if (charSeq.equals(match) || charSeq.startsWith(match + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets File System Information.
     *
     * @return An array of {@link OSFileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
    @Override
    public OSFileStore[] getFileStores() {
        // Map uuids with device path as key
        Map<String, String> uuidMap = new HashMap<>();
        File uuidDir = new File("/dev/disk/by-uuid");
        if (uuidDir.listFiles() != null) {
            for (File uuid : uuidDir.listFiles()) {
                try {
                    // Store UUID as value with path (e.g., /dev/sda1) as key
                    uuidMap.put(uuid.getCanonicalPath(), uuid.getName().toLowerCase());
                } catch (IOException e) {
                    LOG.error("Couldn't get canonical path for {}. {}", uuid.getName(), e);
                }
            }
        }

        // List file systems
        List<OSFileStore> fsList = new ArrayList<>();

        // Parse /proc/self/mounts to get fs types
        List<String> mounts = FileUtil.readFile("/proc/self/mounts");
        for (String mount : mounts) {
            String[] split = mount.split(" ");
            // As reported in fstab(5) manpage, struct is:
            // 1st field is volume name
            // 2nd field is path with spaces escaped as \040
            // 3rd field is fs type
            // 4th field is mount options (ignored)
            // 5th field is used by dump(8) (ignored)
            // 6th field is fsck order (ignored)
            if (split.length < 6) {
                continue;
            }

            // Exclude pseudo file systems
            String path = split[1].replaceAll("\\\\040", " ");
            String type = split[2];
            if (this.pseudofs.contains(type) || path.equals("/dev") || listElementStartsWith(this.tmpfsPaths, path)) {
                continue;
            }

            String name = split[0].replaceAll("\\\\040", " ");
            if (path.equals("/")) {
                name = "/";
            }
            String volume = split[0].replaceAll("\\\\040", " ");
            String uuid = uuidMap.getOrDefault(split[0], "");

            String description;
            if (volume.startsWith("/dev")) {
                description = "Local Disk";
            } else if (volume.equals("tmpfs")) {
                description = "Ram Disk";
            } else if (type.startsWith("nfs") || type.equals("cifs")) {
                description = "Network Disk";
            } else {
                description = "Mount Point";
            }

            // Add in logical volume found at /dev/mapper, useful when linking
            // file system with drive.
            String logicalVolume = "";
            String volumeMapperDirectory = "/dev/mapper/";
            Path link = Paths.get(volume);
            if (Files.exists(link) && Files.isSymbolicLink(link)) {
                try {
                    Path slink = Files.readSymbolicLink(link);
                    Path full = Paths.get(volumeMapperDirectory + slink.toString());
                    if (Files.exists(full)) {
                        logicalVolume = full.normalize().toString();
                    }
                } catch (IOException e) {
                    LOG.warn("Couldn't access symbolic path  {}. {}", link, e);
                }
            }

            long totalInodes = 0L;
            long freeInodes = 0L;
            long totalSpace = 0L;
            long usableSpace = 0L;

            try {
                LibC.Statvfs vfsStat = new LibC.Statvfs();
                if (0 == LibC.INSTANCE.statvfs(path, vfsStat)) {
                    totalInodes = vfsStat.f_files.longValue();
                    freeInodes = vfsStat.f_ffree.longValue();
                    totalSpace = vfsStat.f_blocks.longValue() * vfsStat.f_bsize.longValue();
                    usableSpace = vfsStat.f_bfree.longValue() * vfsStat.f_bsize.longValue();
                } else {
                    File tmpFile = new File(path);
                    totalSpace = tmpFile.getTotalSpace();
                    usableSpace = tmpFile.getUsableSpace();
                    LOG.error("Failed to get statvfs. Error code: {}", Native.getLastError());
                }
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                LOG.error("Failed to get file counts from statvfs. {}", e);
            }

            OSFileStore osStore = new OSFileStore();
            osStore.setName(name);
            osStore.setVolume(volume);
            osStore.setMount(path);
            osStore.setDescription(description);
            osStore.setType(type);
            osStore.setUUID(uuid);
            osStore.setUsableSpace(usableSpace);
            osStore.setTotalSpace(totalSpace);
            osStore.setFreeInodes(freeInodes);
            osStore.setTotalInodes(totalInodes);
            osStore.setLogicalVolume(logicalVolume);

            fsList.add(osStore);
        }

        return fsList.toArray(new OSFileStore[0]);
    }

    @Override
    public long getOpenFileDescriptors() {
        return getFileDescriptors(0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return getFileDescriptors(2);
    }

    /**
     * Returns a value from the Linux system file /proc/sys/fs/file-nr.
     *
     * @param index
     *            The index of the value to retrieve. 0 returns the total
     *            allocated file descriptors. 1 returns the number of used file
     *            descriptors for kernel 2.4, or the number of unused file
     *            descriptors for kernel 2.6. 2 returns the maximum number of
     *            file descriptors that can be allocated.
     * @return Corresponding file descriptor value from the Linux system file.
     */
    private long getFileDescriptors(int index) {
        String filename = "/proc/sys/fs/file-nr";
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
}
