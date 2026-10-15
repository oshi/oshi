/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.GlobalConfig;

/**
 * Common methods for filesystem implementations
 */
@ThreadSafe
public abstract class AbstractFileSystem implements FileSystem {

    /**
     * Default constructor.
     */
    protected AbstractFileSystem() {
    }

    /**
     * FileSystem types which are network-based and should be excluded from local-only lists
     */
    protected static final List<String> NETWORK_FS_TYPES = Arrays
            .asList(GlobalConfig.get(GlobalConfig.OSHI_NETWORK_FILESYSTEM_TYPES, "").split(","));

    /**
     * FileSystem types which are pseudo-filesystems and should be excluded from local-only lists
     */
    protected static final List<String> PSEUDO_FS_TYPES = Arrays
            .asList(GlobalConfig.get(GlobalConfig.OSHI_PSEUDO_FILESYSTEM_TYPES, "").split(","));

    @Override
    public List<OSFileStore> getFileStores() {
        return getFileStores(false);
    }

    /**
     * Caps a per-process file descriptor limit at the system-wide limit, which the kernel enforces regardless of a
     * larger (or unlimited) resource limit.
     *
     * @param processLimit the per-process limit, or a non-positive value if unavailable
     * @param systemLimit  the system-wide limit, or a non-positive value if unavailable
     * @return the smaller of the two available limits, or 0 if neither is available
     */
    protected static long capAtSystemLimit(long processLimit, long systemLimit) {
        if (processLimit <= 0) {
            return Math.max(systemLimit, 0L);
        }
        return systemLimit > 0 ? Math.min(processLimit, systemLimit) : processLimit;
    }
}
