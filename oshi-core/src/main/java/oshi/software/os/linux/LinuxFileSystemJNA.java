/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.linux.LinuxFileSystem;

/**
 * JNA-based Linux file system implementation. Implements {@code statvfs} via JNA.
 */
@ThreadSafe
public class LinuxFileSystemJNA extends LinuxFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxFileSystemJNA.class);

    @Override
    protected long[] queryStatvfs(String path) {
        try {
            LibC.Statvfs vfsStat = new LibC.Statvfs();
            if (0 == LibC.INSTANCE.statvfs(path, vfsStat)) {
                long frsize = vfsStat.f_frsize.longValue();
                return new long[] { vfsStat.f_files.longValue(), vfsStat.f_ffree.longValue(),
                        vfsStat.f_blocks.longValue() * frsize, vfsStat.f_bavail.longValue() * frsize,
                        vfsStat.f_bfree.longValue() * frsize };
            }
            LOG.debug("Failed to get information to use statvfs. path: {}, Error code: {}", path,
                    Native.getLastError());
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get file counts from statvfs. {}", e.getMessage());
        }
        return null;
    }
}
