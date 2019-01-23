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
package oshi.software.os.unix.solaris;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * The Solaris File System contains {@link OSFileStore}s which are a storage
 * pool, device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Solaris, these are found in the
 * /proc/mount filesystem, excluding temporary and kernel mounts.
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisFileSystem implements FileSystem {

    private static final long serialVersionUID = 1L;

    // Solaris defines a set of virtual file systems
    private final List<String> pseudofs = Arrays.asList(new String[] { //
            "proc", // Proc file system
            "devfs", // Dev temporary file system
            "ctfs", // Contract file system
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
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeTotalMap = new HashMap<>();
        String key = null;
        String total = null;
        String free = null;
        for (String line : ExecutingCommand.runNative("df -g")) {
            /*- Sample Output:
            /                  (/dev/md/dsk/d0    ):         8192 block size          1024 frag size
            41310292 total blocks   18193814 free blocks 17780712 available        2486848 total files
             2293351 free files     22282240 filesys id
                 ufs fstype       0x00000004 flag             255 filename length
            */
            if (line.startsWith("/")) {
                key = ParseUtil.whitespaces.split(line)[0];
                total = null;
            } else if (line.contains("available") && line.contains("total files")) {
                total = ParseUtil.getTextBetweenStrings(line, "available", "total files").trim();
            } else if (line.contains("free files")) {
                free = ParseUtil.getTextBetweenStrings(line, "", "free files").trim();
                if (key != null && total != null) {
                    inodeFreeMap.put(key, ParseUtil.parseLongOrDefault(free, 0L));
                    inodeTotalMap.put(key, ParseUtil.parseLongOrDefault(total, 0L));
                    key = null;
                }
            }
        }

        // Get mount table
        for (String fs : ExecutingCommand.runNative("cat /etc/mnttab")) {
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

            // Add to the list
            OSFileStore osStore = new OSFileStore();
            osStore.setName(name);
            osStore.setVolume(volume);
            osStore.setMount(path);
            osStore.setDescription(description);
            osStore.setType(type);
            osStore.setUUID(""); // No UUID info on Solaris
            osStore.setUsableSpace(usableSpace);
            osStore.setTotalSpace(totalSpace);
            osStore.setFreeInodes(inodeFreeMap.containsKey(path) ? inodeFreeMap.get(path) : 0L);
            osStore.setTotalInodes(inodeTotalMap.containsKey(path) ? inodeTotalMap.get(path) : 0L);
            fsList.add(osStore);
        }
        return fsList.toArray(new OSFileStore[0]);
    }

    @Override
    public long getOpenFileDescriptors() {
        Kstat ksp = KstatUtil.kstatLookup(null, -1, "file_cache");
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            return KstatUtil.kstatDataLookupLong(ksp, "buf_inuse");
        }
        return 0L;
    }

    @Override
    public long getMaxFileDescriptors() {
        Kstat ksp = KstatUtil.kstatLookup(null, -1, "file_cache");
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            return KstatUtil.kstatDataLookupLong(ksp, "buf_max");
        }
        return 0L;
    }
}
