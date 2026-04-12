/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.util.FileSystemUtil;

/**
 * Common constants and configuration for macOS file system implementations.
 */
@ThreadSafe
public abstract class MacFileSystem extends AbstractFileSystem {

    public static final String OSHI_MAC_FS_PATH_EXCLUDES = "oshi.os.mac.filesystem.path.excludes";
    public static final String OSHI_MAC_FS_PATH_INCLUDES = "oshi.os.mac.filesystem.path.includes";
    public static final String OSHI_MAC_FS_VOLUME_EXCLUDES = "oshi.os.mac.filesystem.volume.excludes";
    public static final String OSHI_MAC_FS_VOLUME_INCLUDES = "oshi.os.mac.filesystem.volume.includes";

    protected static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_MAC_FS_PATH_EXCLUDES);
    protected static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_MAC_FS_PATH_INCLUDES);
    protected static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_MAC_FS_VOLUME_EXCLUDES);
    protected static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil
            .loadAndParseFileSystemConfig(OSHI_MAC_FS_VOLUME_INCLUDES);

    // Regexp matcher for /dev/disk0s2 etc.
    protected static final Pattern LOCAL_DISK = Pattern.compile("/dev/disk\\d+(s\\d+)?");

    // User specifiable flags.
    protected static final int MNT_RDONLY = 0x00000001;
    protected static final int MNT_SYNCHRONOUS = 0x00000002;
    protected static final int MNT_NOEXEC = 0x00000004;
    protected static final int MNT_NOSUID = 0x00000008;
    protected static final int MNT_NODEV = 0x00000010;
    protected static final int MNT_UNION = 0x00000020;
    protected static final int MNT_ASYNC = 0x00000040;
    protected static final int MNT_CPROTECT = 0x00000080;
    protected static final int MNT_EXPORTED = 0x00000100;
    protected static final int MNT_QUARANTINE = 0x00000400;
    protected static final int MNT_LOCAL = 0x00001000;
    protected static final int MNT_QUOTA = 0x00002000;
    protected static final int MNT_ROOTFS = 0x00004000;
    protected static final int MNT_DOVOLFS = 0x00008000;
    protected static final int MNT_DONTBROWSE = 0x00100000;
    protected static final int MNT_IGNORE_OWNERSHIP = 0x00200000;
    protected static final int MNT_AUTOMOUNTED = 0x00400000;
    protected static final int MNT_JOURNALED = 0x00800000;
    protected static final int MNT_NOUSERXATTR = 0x01000000;
    protected static final int MNT_DEFWRITE = 0x02000000;
    protected static final int MNT_MULTILABEL = 0x04000000;
    protected static final int MNT_NOATIME = 0x10000000;

    protected static final Map<Integer, String> OPTIONS_MAP = new HashMap<>();
    static {
        OPTIONS_MAP.put(MNT_SYNCHRONOUS, "synchronous");
        OPTIONS_MAP.put(MNT_NOEXEC, "noexec");
        OPTIONS_MAP.put(MNT_NOSUID, "nosuid");
        OPTIONS_MAP.put(MNT_NODEV, "nodev");
        OPTIONS_MAP.put(MNT_UNION, "union");
        OPTIONS_MAP.put(MNT_ASYNC, "asynchronous");
        OPTIONS_MAP.put(MNT_CPROTECT, "content-protection");
        OPTIONS_MAP.put(MNT_EXPORTED, "exported");
        OPTIONS_MAP.put(MNT_QUARANTINE, "quarantined");
        OPTIONS_MAP.put(MNT_LOCAL, "local");
        OPTIONS_MAP.put(MNT_QUOTA, "quotas");
        OPTIONS_MAP.put(MNT_ROOTFS, "rootfs");
        OPTIONS_MAP.put(MNT_DOVOLFS, "volfs");
        OPTIONS_MAP.put(MNT_DONTBROWSE, "nobrowse");
        OPTIONS_MAP.put(MNT_IGNORE_OWNERSHIP, "noowners");
        OPTIONS_MAP.put(MNT_AUTOMOUNTED, "automounted");
        OPTIONS_MAP.put(MNT_JOURNALED, "journaled");
        OPTIONS_MAP.put(MNT_NOUSERXATTR, "nouserxattr");
        OPTIONS_MAP.put(MNT_DEFWRITE, "defwrite");
        OPTIONS_MAP.put(MNT_MULTILABEL, "multilabel");
        OPTIONS_MAP.put(MNT_NOATIME, "noatime");
    }
}
