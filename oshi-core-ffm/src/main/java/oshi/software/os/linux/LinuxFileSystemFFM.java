/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static org.slf4j.event.Level.DEBUG;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.LinuxLibcFunctions;
import oshi.software.common.os.linux.LinuxFileSystem;

/**
 * FFM-based Linux file system implementation. Implements {@code statvfs} via FFM.
 */
@ThreadSafe
final class LinuxFileSystemFFM extends LinuxFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxFileSystemFFM.class);

    @Override
    protected long[] queryStatvfs(String path) {
        return callInArenaOrDefault(arena -> {
            MemorySegment pathSeg = arena.allocateFrom(path);
            MemorySegment buf = arena.allocate(LinuxLibcFunctions.STATVFS_LAYOUT);
            if (0 == LinuxLibcFunctions.statvfs(pathSeg, buf)) {
                long frsize = LinuxLibcFunctions.statvfsFrsize(buf);
                return new long[] { LinuxLibcFunctions.statvfsFiles(buf), LinuxLibcFunctions.statvfsFfree(buf),
                        LinuxLibcFunctions.statvfsBlocks(buf) * frsize, LinuxLibcFunctions.statvfsBavail(buf) * frsize,
                        LinuxLibcFunctions.statvfsBfree(buf) * frsize };
            }
            LOG.debug("statvfs failed for path: {}", path);
            return null;
        }, LOG, DEBUG, "FFM statvfs error for path " + path, null);
    }
}
