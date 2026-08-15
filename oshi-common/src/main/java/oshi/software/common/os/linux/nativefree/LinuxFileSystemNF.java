/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux.nativefree;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.linux.LinuxFileSystem;

/**
 * Native-free Linux file system implementation. Extends {@link LinuxFileSystem}, returning {@code null} from
 * {@link #queryStatvfs(String)} to trigger the Java {@link java.io.File} fallback for space/inode values.
 */
@ThreadSafe
public class LinuxFileSystemNF extends LinuxFileSystem {

    @Override
    protected long @Nullable [] queryStatvfs(String path) {
        return null;
    }
}
