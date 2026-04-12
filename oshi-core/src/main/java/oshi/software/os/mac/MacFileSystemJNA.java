/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.DiskArbitration;
import com.sun.jna.platform.mac.DiskArbitration.DADiskRef;
import com.sun.jna.platform.mac.DiskArbitration.DASessionRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Statfs;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.mac.MacFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FileSystemUtil;
import oshi.util.platform.mac.CFUtil;
import oshi.util.platform.mac.SysctlUtil;

/**
 * The Mac File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage. In macOS, these are found in the
 * /Volumes directory.
 */
@ThreadSafe
public class MacFileSystemJNA extends MacFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacFileSystemJNA.class);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        return getFileStoreMatching(null, localOnly);
    }

    // Called by MacOSFileStoreJNA
    static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();

        // Use getfsstat to find fileSystems
        // Query with null to get total # required
        int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
        if (numfs > 0) {
            // Open a DiskArbitration session to get VolumeName of file systems
            // with bsd names
            DASessionRef session = DiskArbitration.INSTANCE
                    .DASessionCreate(CoreFoundation.INSTANCE.CFAllocatorGetDefault());
            if (session == null) {
                LOG.error("Unable to open session to DiskArbitration framework.");
            } else {
                CFStringRef daVolumeNameKey = CFStringRef.createCFString("DAVolumeName");

                // Create array to hold results
                Statfs s = new Statfs();
                Statfs[] fs = (Statfs[]) s.toArray(numfs);
                // Fill array with results
                numfs = SystemB.INSTANCE.getfsstat64(fs, fs[0].size() * fs.length, SystemB.MNT_NOWAIT);
                for (int f = 0; f < numfs; f++) {
                    // Mount on name will match mounted path, e.g. /Volumes/foo
                    // Mount to name will match canonical path., e.g., /dev/disk0s2
                    // Byte arrays are null-terminated strings

                    // Get volume and path name, and type
                    String volume = Native.toString(fs[f].f_mntfromname, StandardCharsets.UTF_8);
                    String path = Native.toString(fs[f].f_mntonname, StandardCharsets.UTF_8);
                    String type = Native.toString(fs[f].f_fstypename, StandardCharsets.UTF_8);
                    // Skip non-local drives if requested, skip system types
                    final int flags = fs[f].f_flags;
                    boolean isLocal = (flags & MNT_LOCAL) != 0;
                    // Skip non-local drives if requested, and exclude pseudo file systems
                    if ((localOnly && !isLocal) || !path.equals("/")
                            && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path, volume,
                                    FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES))) {
                        continue;
                    }

                    String description = "Volume";
                    if (LOCAL_DISK.matcher(volume).matches()) {
                        description = "Local Disk";
                    } else if (volume.startsWith("localhost:") || volume.startsWith("//") || volume.startsWith("smb://")
                            || NETWORK_FS_TYPES.contains(type)) {
                        description = "Network Drive";
                    }
                    File file = new File(path);
                    String name = file.getName();
                    // getName() for / is still blank, so:
                    if (name.isEmpty()) {
                        name = file.getPath();
                    }

                    StringBuilder options = new StringBuilder((MNT_RDONLY & flags) == 0 ? "rw" : "ro");
                    String moreOptions = OPTIONS_MAP.entrySet().stream().filter(e -> (e.getKey() & flags) > 0)
                            .map(Map.Entry::getValue).collect(Collectors.joining(","));
                    if (!moreOptions.isEmpty()) {
                        options.append(',').append(moreOptions);
                    }

                    String uuid = "";
                    // Use volume to find DiskArbitration volume name and search for
                    // the registry entry for UUID
                    String bsdName = volume.replace("/dev/disk", "disk");
                    if (bsdName.startsWith("disk")) {
                        // Get the DiskArbitration dictionary for this disk,
                        // which has volumename
                        DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromBSDName(
                                CoreFoundation.INSTANCE.CFAllocatorGetDefault(), session, volume);
                        if (disk != null) {
                            CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                            if (diskInfo != null) {
                                // get volume name from its key
                                Pointer result = diskInfo.getValue(daVolumeNameKey);
                                name = CFUtil.cfPointerToString(result);
                                diskInfo.release();
                            }
                            disk.release();
                        }
                        // Search for bsd name in IOKit registry for UUID
                        CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
                        if (matchingDict != null) {
                            // search for all IOservices that match the bsd name
                            IOIterator fsIter = IOKitUtil.getMatchingServices(matchingDict);
                            if (fsIter != null) {
                                // getMatchingServices releases matchingDict
                                // Should only match one logical drive
                                IORegistryEntry fsEntry = fsIter.next();
                                if (fsEntry != null && fsEntry.conformsTo("IOMedia")) {
                                    // Now get the UUID
                                    uuid = fsEntry.getStringProperty("UUID");
                                    if (uuid != null) {
                                        uuid = uuid.toLowerCase(Locale.ROOT);
                                    }
                                    fsEntry.release();
                                }
                                fsIter.release();
                            }
                        }
                    }

                    if (nameToMatch != null && !nameToMatch.equals(name)) {
                        continue;
                    }

                    fsList.add(new MacOSFileStoreJNA(name, volume, name, path, options.toString(),
                            uuid == null ? "" : uuid, isLocal, "", description, type, file.getFreeSpace(),
                            file.getUsableSpace(), file.getTotalSpace(), fs[f].f_ffree, fs[f].f_files));
                }
                daVolumeNameKey.release();
                // Close DA session
                session.release();
            }
        }
        return fsList;
    }

    @Override
    public long getOpenFileDescriptors() {
        return SysctlUtil.sysctl("kern.num_files", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return SysctlUtil.sysctl("kern.maxfiles", 0);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return SysctlUtil.sysctl("kern.maxfilesperproc", 0);
    }
}
