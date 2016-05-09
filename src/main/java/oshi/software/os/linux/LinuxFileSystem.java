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
 * enrico[dot]bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.linux;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FileUtil;

/**
 * The Mac File System contains {@link OSFileStore}s which are a storage pool,
 * device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Linux, these are found in the /proc/mount
 * filesystem, excluding temporary and kernel mounts.
 *
 * @author widdis[at]gmail[dot]com
 */
public class LinuxFileSystem extends AbstractFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxFileSystem.class);

    // Linux defines a set of virtual file systems
    private final List<String> pseudofs = Arrays.asList(new String[]{
        "sysfs", // SysFS file system
        "proc", // Proc file system
        "devtmpfs", // Dev temporary file system
        "devpts", // Dev pseudo terminal devices file system
        "securityfs", // Kernel security file system
        "tmpfs", // Temporary file system
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
        "fusectl", // FUSE control file system
        // NOTE: FUSE's fuseblk is not evalued because used as file system representation of a FUSE block storage
        //"fuseblk" // FUSE block file system
    });
    
    // System path mounted as tmpfs
    private final List<String> tmpfsPaths = Arrays.asList(new String[]{
        "/dev/shm",
        "/run",
        "/sys/fs/cgroup",
    });

    private Boolean listElementStarts(List<String> aList, String charSeq) {
        for (String match : aList) {
            if (match.startsWith(match)){
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
    
    /**
     * Gets File System Information.
     *
     * @return An array of {@link FileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
    @Override
    public OSFileStore[] getFileStores() {
        // Parse /proc/self/mounts to map filesystem paths to types
        Map<String, String> fstype = new HashMap<>();
        try {
            List<String> mounts = FileUtil.readFile("/proc/self/mounts");
            for (String mount : mounts) {
                String[] split = mount.split(" ");
                // 2nd field is path with spaces escaped as \040
                // 3rd field is fs type
                if (split.length < 6) {
                    continue;
                }
                fstype.put(split[1].replaceAll("\\\\040", " "), split[2]);
            }
        } catch (IOException e) {
            LOG.error("Error reading /proc/self/mounts. Can't detect filetypes.");
        }
        // Format
        // Now list file systems
        List<OSFileStore> fsList = new ArrayList<>();
        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            // FileStore toString starts with path, then a space, then name in
            // parentheses e.g., "/ (/dev/sda1)" and "/proc (proc)"
            String path = store.toString().replace(" (" + store.name() + ")", "");

            // Exclude pseudo file systems
            if (this.pseudofs.contains(store.name())) {
                if (store.name().equals("tmpfs")) {
                    // Exclude tmpfs system paths
                    if (listElementStarts(this.tmpfsPaths, path)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            String name = store.name();
            if (path.equals("/"))
                name = "/";
            String description = "Mount Point";
            if (store.name().startsWith("/dev"))
                description = "Local Disk";
            String type = "unknown";
            if (fstype.containsKey(path)) {
                type = fstype.get(path);
            }
            try {
                fsList.add(new OSFileStore(name, path, description, type, store.getUsableSpace(), store.getTotalSpace()));
            } catch (IOException e) {
                // get*Space() may fail for ejected CD-ROM, etc.
                LOG.trace("", e);
                continue;
            }
        }
        return fsList.toArray(new OSFileStore[fsList.size()]);
    }
}
