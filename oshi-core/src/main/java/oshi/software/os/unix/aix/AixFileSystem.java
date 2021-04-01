/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.unix.aix;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.ParseUtil;

/**
 * The AIX File System contains {@link oshi.software.os.OSFileStore}s which are
 * a storage pool, device, partition, volume, concrete file system or other
 * implementation specific means of file storage.
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

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, localOnly);
    }

    // Called by AixOSFileStore
    static List<OSFileStore> getFileStoreMatching(String nameToMatch) {
        return getFileStoreMatching(nameToMatch, false);
    }

    private static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Get inode usage data
        Map<String, Long> inodeFreeMap = new HashMap<>();
        Map<String, Long> inodeTotalMap = new HashMap<>();
        String command = "df -i" + (localOnly ? " -l" : "");
        for (String line : ExecutingCommand.runNative(command)) {
            /*- Sample Output:
             $ df -i
            Filesystem            Inodes   IUsed   IFree IUse% Mounted on
            /dev/hd4               75081   16741   58340   23% /
            /dev/hd2              269640   43104  226536   16% /usr
            /dev/hd9var            43598    1370   42228    4% /var
            /dev/hd3               79936     386   79550    1% /tmp
            /dev/hd11admin         29138       7   29131    1% /admin
            /proc                      0       0       0    -  /proc
            /dev/hd10opt           47477    4232   43245    9% /opt
            /dev/livedump          58204       4   58200    1% /var/adm/ras/livedump
            /dev/fslv00          12419240  292668 12126572    3% /home
            */
            if (line.startsWith("/")) {
                String[] split = ParseUtil.whitespaces.split(line);
                if (split.length > 5) {
                    inodeTotalMap.put(split[0], ParseUtil.parseLongOrDefault(split[1], 0L));
                    inodeFreeMap.put(split[0], ParseUtil.parseLongOrDefault(split[3], 0L));
                }
            }
        }

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
                if ((localOnly && NETWORK_FS_TYPES.contains(type)) || !path.equals("/")
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
                    description = "Ram Disk";
                } else if (NETWORK_FS_TYPES.contains(type)) {
                    description = "Network Disk";
                } else {
                    description = "Mount Point";
                }

                fsList.add(new AixOSFileStore(name, volume, name, path, options, "", "", description, type, freeSpace,
                        usableSpace, totalSpace, inodeFreeMap.getOrDefault(volume, 0L),
                        inodeTotalMap.getOrDefault(volume, 0L)));
            }
        }
        return fsList;
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
}
