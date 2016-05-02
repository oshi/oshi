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
package oshi.software.os.mac;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.Statfs;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;

/**
 * The Mac File System contains {@link OSFileStore}s which are a storage pool,
 * device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Mac OS X, these are found in the /Volumes
 * directory.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacFileSystem extends AbstractFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacFileSystem.class);

    // Regexp matcher for /dev/disk1 etc.
    private static final Pattern localDisk = Pattern.compile("/dev/disk\\d");

    /**
     * Gets File System Information.
     * 
     * @return An array of {@link OSFileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
    public OSFileStore[] getFileStores() {
        // Use getfsstat to map filesystem paths to types
        Map<String, String> fstype = new HashMap<>();
        // Query with null to get total # required
        int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
        if (numfs > 0) {
            // Create array to hold results
            Statfs[] fs = new Statfs[numfs];
            // Fill array with results
            numfs = SystemB.INSTANCE.getfsstat64(fs, numfs * (new Statfs()).size(), SystemB.MNT_NOWAIT);
            for (int f = 0; f < numfs; f++) {
                // Mount to name will match canonical path.
                // Byte arrays are null-terminated strings
                fstype.put(new String(fs[f].f_mntonname).trim(), new String(fs[f].f_fstypename).trim());
            }
        }
        // Now list file systems
        List<OSFileStore> fsList = new ArrayList<>();
        FileSystemView fsv = FileSystemView.getFileSystemView();
        // Mac file systems are mounted in /Volumes
        File volumes = new File("/Volumes");
        if (volumes != null && volumes.listFiles() != null) {
            for (File f : volumes.listFiles()) {
                // Everyone hates DS Store
                if (f.getName().endsWith(".DS_Store")) {
                    continue;
                }
                String name = fsv.getSystemDisplayName(f);
                String description = "Volume";
                String type = "unknown";
                try {
                    String cp = f.getCanonicalPath();
                    if (cp.equals("/"))
                        name = name + " (/)";
                    FileStore fs = Files.getFileStore(f.toPath());
                    if (localDisk.matcher(fs.name()).matches()) {
                        description = "Local Disk";
                    }
                    if (fs.name().startsWith("localhost:") || fs.name().startsWith("//")) {
                        description = "Network Drive";
                    }
                    if (fstype.containsKey(cp)) {
                        type = fstype.get(cp);
                    }
                } catch (IOException e) {
                    LOG.trace("", e);
                    continue;
                }
                fsList.add(new OSFileStore(name, description, type, f.getUsableSpace(), f.getTotalSpace()));
            }
        }
        return fsList.toArray(new OSFileStore[fsList.size()]);
    }
}
