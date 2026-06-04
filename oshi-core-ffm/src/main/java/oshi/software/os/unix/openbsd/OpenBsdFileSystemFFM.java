/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.openbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.software.common.os.unix.openbsd.OpenBsdFileSystem;

/**
 * FFM-backed OpenBSD file system. All file-store enumeration (mount/df parsing) lives on {@link OpenBsdFileSystem};
 * only the file-descriptor sysctl reads are FFM-specific.
 */
@ThreadSafe
public final class OpenBsdFileSystemFFM extends OpenBsdFileSystem {

    @Override
    public long getOpenFileDescriptors() {
        return OpenBsdSysctlUtilFFM.sysctl("kern.nfiles", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return OpenBsdSysctlUtilFFM.sysctl("kern.maxfiles", 0);
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return OpenBsdSysctlUtilFFM.sysctl("kern.maxfilesperproc", 0);
    }
}
