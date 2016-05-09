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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
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

    /**
     * Gets File System Information.
     * 
     * @return An array of {@link FileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
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
            // Exclude special directories
            if (path.startsWith("/proc") || path.startsWith("/sys") || path.startsWith("/run") || path.equals("/dev")
                    || path.equals("/dev/pts"))
                continue;
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
                fsList.add(new OSFileStore(name, description, type, store.getUsableSpace(), store.getTotalSpace()));
            } catch (IOException e) {
                // get*Space() may fail for ejected CD-ROM, etc.
                LOG.trace("", e);
                continue;
            }
        }
        return fsList.toArray(new OSFileStore[fsList.size()]);
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
     *            allocated file descriptors. 1 returns the number of used
     *            file descriptors for kernel 2.4, or the number of unused
     *            file descriptors for kernel 2.6. 2 returns the maximum
     *            number of file descriptors that can be allocated.
     * @return Corresponding file descriptor value from the Linux system file.
     */
    private long getFileDescriptors(int index) {
        if ( index < 0 || index > 2 ) {
            throw new IllegalArgumentException("Index must be between 0 and 2.");
        } else if (new File("/proc/sys/fs/file-nr").exists()) {
            try {
                List<String> osDescriptors = FileUtil.readFile("/proc/sys/fs/file-nr");
                for (String line : osDescriptors) {
                    String [] splittedLine = line.split("\\D+");
                    return Long.parseLong(splittedLine[index]);
                }
            } catch (Exception e) {
                LOG.trace("", e);
            }
        }
        return 0L;
    }
}
