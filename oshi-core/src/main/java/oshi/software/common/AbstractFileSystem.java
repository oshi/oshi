/*
 * Copyright 2016-2022 The OSHI Project Contributors
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
     * FileSystem types which are network-based and should be excluded from local-only lists
     */
    protected static final List<String> NETWORK_FS_TYPES = Arrays
            .asList(GlobalConfig.get(GlobalConfig.OSHI_NETWORK_FILESYSTEM_TYPES, "").split(","));

    protected static final List<String> PSEUDO_FS_TYPES = Arrays
            .asList(GlobalConfig.get(GlobalConfig.OSHI_PSEUDO_FILESYSTEM_TYPES, "").split(","));

    @Override
    public List<OSFileStore> getFileStores() {
        return getFileStores(false);
    }
}
