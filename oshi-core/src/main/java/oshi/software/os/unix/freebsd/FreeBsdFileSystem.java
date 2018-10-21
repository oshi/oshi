/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.software.os.unix.freebsd;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * The Solaris File System contains {@link OSFileStore}s which are a storage
 * pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Linux, these are found in the /proc/mount
 * filesystem, excluding temporary and kernel mounts.
 *
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdFileSystem implements FileSystem {

    private static final long serialVersionUID = 1L;

    // Linux defines a set of virtual file systems
    private final List<String> pseudofs = Arrays.asList(new String[] { //
            "procfs", // Proc file system
            "devfs", // Dev temporary file system
            "ctfs", // Contract file system
            "fdescfs", // fd
            "objfs", // Object file system
            "mntfs", // Mount file system
            "sharefs", // Share file system
            "lofs", // Library file system
            "autofs" // Auto mounting fs
            // "tmpfs", // Temporary file system
            // NOTE: tmpfs is evaluated apart, because Solaris uses it for
            // RAMdisks
    });

    // System path mounted as tmpfs
    private final List<String> tmpfsPaths = Arrays.asList(new String[] { "/system", "/tmp", "/dev/fd" });

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
        // Find any partition UUIDs and map them
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

        // Get mount table
        for (String fs : ExecutingCommand.runNative("mount -p")) {
            String[] split = ParseUtil.whitespaces.split(fs);
            if (split.length < 5) {
                continue;
            }
            // 1st field is volume name
            // 2nd field is mount point
            // 3rd field is fs type
            // other fields ignored
            String volume = split[0];
            String path = split[1];
            String type = split[2];

            // Exclude pseudo file systems
            if (this.pseudofs.contains(type) || path.equals("/dev") || listElementStartsWith(this.tmpfsPaths, path)
                    || volume.startsWith("rpool") && !path.equals("/")) {
                continue;
            }

            String name = path.substring(path.lastIndexOf('/') + 1);
            // Special case for /, pull last element of volume instead
            if (name.isEmpty()) {
                name = volume.substring(volume.lastIndexOf('/') + 1);
            }
            long totalSpace = new File(path).getTotalSpace();
            long usableSpace = new File(path).getUsableSpace();

            String description;
            if (volume.startsWith("/dev") || path.equals("/")) {
                description = "Local Disk";
            } else if (volume.equals("tmpfs")) {
                description = "Ram Disk";
            } else if (type.startsWith("nfs") || type.equals("cifs")) {
                description = "Network Disk";
            } else {
                description = "Mount Point";
            }
            // Match UUID
            String uuid = MapUtil.getOrDefault(uuidMap, name, "");
            OSFileStore osStore = new OSFileStore(name, volume, path, description, type, uuid, usableSpace, totalSpace);
            fsList.add(osStore);
        }
        return fsList.toArray(new OSFileStore[fsList.size()]);
    }

    @Override
    public long getOpenFileDescriptors() {
        return BsdSysctlUtil.sysctl("kern.openfiles", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return BsdSysctlUtil.sysctl("kern.maxfiles", 0);
    }
}
