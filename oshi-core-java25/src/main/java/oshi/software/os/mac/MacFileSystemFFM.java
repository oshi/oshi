/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.CoreFoundationFunctions.CFAllocatorGetDefault;
import static oshi.ffm.mac.DiskArbitrationFunctions.DASessionCreate;
import static oshi.ffm.mac.MacSystem.F_FFREE;
import static oshi.ffm.mac.MacSystem.F_FILES;
import static oshi.ffm.mac.MacSystem.F_FLAGS;
import static oshi.ffm.mac.MacSystem.F_FSTYPENAME;
import static oshi.ffm.mac.MacSystem.F_MNTFROMNAME;
import static oshi.ffm.mac.MacSystem.F_MNTONNAME;
import static oshi.ffm.mac.MacSystem.MNT_NOWAIT;
import static oshi.ffm.mac.MacSystem.STATFS;
import static oshi.ffm.mac.MacSystemFunctions.getfsstat64;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.DiskArbitration.DADiskRef;
import oshi.ffm.mac.DiskArbitration.DASessionRef;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.software.os.OSFileStore;
import oshi.util.FileSystemUtil;
import oshi.util.platform.mac.CFUtilFFM;
import oshi.util.platform.mac.IOKitUtilFFM;
import oshi.util.platform.mac.SysctlUtilFFM;

/**
 * The Mac File System contains {@link oshi.software.os.OSFileStore}s which are a storage pool, device, partition,
 * volume, concrete file system or other implementation specific means of file storage. In macOS, these are found in the
 * /Volumes directory.
 */
@ThreadSafe
public class MacFileSystemFFM extends MacFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacFileSystemFFM.class);

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        // List of file systems
        return getFileStoreMatching(null, localOnly);
    }

    // Called by MacOSFileStore
    static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
        List<OSFileStore> fsList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            // Use getfsstat to find fileSystems
            // Query with null to get total # required
            int numfs = getfsstat64(MemorySegment.NULL, 0, 0);
            if (numfs <= 0) {
                return fsList;
            }
            // Create buffer to hold results
            long statfsSize = STATFS.byteSize(); // Size of one structure
            MemorySegment statfsBuffer = arena.allocate(statfsSize * numfs);
            numfs = getfsstat64(statfsBuffer, (int) statfsBuffer.byteSize(), MNT_NOWAIT);
            if (numfs <= 0) {
                return fsList;
            }

            // Open a DiskArbitration session to get VolumeName of file systems
            // with bsd names
            CFAllocatorRef allocator = new CFAllocatorRef(CFAllocatorGetDefault());
            DASessionRef session = new DASessionRef(DASessionCreate(allocator.segment()));
            if (session.segment() == null) {
                LOG.error("Unable to open session to DiskArbitration framework.");
                return fsList;
            }

            try {
                CFStringRef daVolumeNameKey = CFStringRef.createCFString("DAVolumeName");
                try {
                    for (int f = 0; f < numfs; f++) {
                        MemorySegment statfs = statfsBuffer.asSlice(f * statfsSize, statfsSize);
                        // Mount on name will match mounted path, e.g. /Volumes/foo
                        // Mount to name will match canonical path., e.g., /dev/disk0s2
                        // Byte arrays are null-terminated strings
                        String volume = statfs.getString(STATFS.byteOffset(F_MNTFROMNAME));
                        String path = statfs.getString(STATFS.byteOffset(F_MNTONNAME));
                        String type = statfs.getString(STATFS.byteOffset(F_FSTYPENAME));
                        int flags = statfs.get(JAVA_INT, STATFS.byteOffset(F_FLAGS));
                        long ffree = statfs.get(JAVA_LONG, STATFS.byteOffset(F_FFREE));
                        long files = statfs.get(JAVA_LONG, STATFS.byteOffset(F_FILES));

                        // Skip non-local drives if requested, and exclude pseudo file systems
                        boolean nonLocal = (flags & MNT_LOCAL) == 0;
                        if ((localOnly && nonLocal) || !path.equals("/")
                                && (PSEUDO_FS_TYPES.contains(type) || FileSystemUtil.isFileStoreExcluded(path, volume,
                                        FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES))) {
                            continue;
                        }

                        String description = "Volume";
                        if (LOCAL_DISK.matcher(volume).matches()) {
                            description = "Local Disk";
                        } else if (volume.startsWith("localhost:") || volume.startsWith("//")
                                || volume.startsWith("smb://") || NETWORK_FS_TYPES.contains(type)) {
                            description = "Network Drive";
                        }
                        File file = new File(path);
                        String name = file.getName();
                        // getName() for / is still blank, so:
                        if (name.isEmpty()) {
                            name = file.getPath();
                        }
                        if (nameToMatch != null && !nameToMatch.equals(name)) {
                            continue;
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
                            // Get the DiskArbitration dictionary for this disk
                            DADiskRef disk = null;
                            CFDictionaryRef diskInfo = null;
                            try {
                                disk = DADiskRef.createFromBSDName(allocator, session, volume);
                                if (!disk.isNull()) {
                                    diskInfo = disk.copyDescription();
                                    if (!diskInfo.isNull()) {
                                        // Get volume name
                                        MemorySegment result = diskInfo.getValue(daVolumeNameKey);
                                        if (!result.equals(MemorySegment.NULL)) {
                                            name = CFUtilFFM.cfPointerToString(result);
                                        }
                                    }
                                }
                            } finally {
                                if (diskInfo != null) {
                                    diskInfo.release();
                                }
                                if (disk != null) {
                                    disk.release();
                                }
                            }
                            // Search for bsd name in IOKit registry for UUID
                            MemorySegment matchingDict = IOKitUtilFFM.getBSDNameMatchingDict(bsdName);
                            if (matchingDict != null) {
                                // search for all IOservices that match the bsd name
                                IOIterator fsIter = IOKitUtilFFM.getMatchingServices(matchingDict);
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

                        fsList.add(new MacOSFileStore(name, volume, name, path, options.toString(),
                                uuid == null ? "" : uuid, !nonLocal, "", description, type, file.getFreeSpace(),
                                file.getUsableSpace(), file.getTotalSpace(), ffree, files));
                    }
                } finally {
                    daVolumeNameKey.release();
                }
            } finally {
                session.release();
            }
        } catch (Throwable e) {
            LOG.warn("Failed to query file systems: {}", e.getMessage(), e);
            return fsList;
        }
        return fsList;
    }

    @Override
    public long getOpenFileDescriptors() {
        return SysctlUtilFFM.sysctl("kern.num_files", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return SysctlUtilFFM.sysctl("kern.maxfiles", 0);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return SysctlUtilFFM.sysctl("kern.maxfilesperproc", 0);
    }
}
