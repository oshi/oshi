/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.mac.SystemB.Statfs;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.mac.SystemB;
import oshi.software.common.os.mac.MacOSFileStore;
import oshi.software.os.OSFileStore;

/**
 * OSFileStore implementation
 */
@ThreadSafe
public class MacOSFileStoreJNA extends MacOSFileStore {

    private static final Logger LOG = LoggerFactory.getLogger(MacOSFileStoreJNA.class);

    public MacOSFileStoreJNA(String name, String volume, String label, String mount, String options, String uuid,
            boolean local, String logicalVolume, String description, String fsType, long freeSpace, long usableSpace,
            long totalSpace, long freeInodes, long totalInodes) {
        super(name, volume, label, mount, options, uuid, local, logicalVolume, description, fsType, freeSpace,
                usableSpace, totalSpace, freeInodes, totalInodes);
    }

    @Override
    public boolean updateAttributes() {
        // Fast path: call statfs64 directly on the known mount point
        try {
            Statfs stat = new Statfs();
            if (SystemB.INSTANCE.statfs64(getMount(), stat) == 0) {
                File f = new File(getMount());
                updateSpaceAndInodes(f.getFreeSpace(), f.getUsableSpace(), f.getTotalSpace(), stat.f_ffree,
                        stat.f_files);
                return true;
            }
        } catch (Throwable e) {
            LOG.debug("statfs64 fast path failed for {}: {}", getMount(), e.getMessage());
        }
        // Fall back to full enumeration
        for (OSFileStore fileStore : MacFileSystemJNA.getFileStoreMatching(getName(), isLocal())) {
            if (getVolume().equals(fileStore.getVolume()) && getMount().equals(fileStore.getMount())) {
                updateFrom(fileStore);
                return true;
            }
        }
        return false;
    }
}
