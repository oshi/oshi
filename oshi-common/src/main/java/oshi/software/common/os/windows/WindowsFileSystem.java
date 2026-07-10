/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.software.common.AbstractFileSystem;

/**
 * Common file-descriptor logic for the Windows file system implementations. The volume enumeration
 * ({@code getFileStores}) is backend-specific (Kernel32/WMI via JNA vs FFM) and remains in the subclasses; only the
 * handle-count query differs by a single native call, abstracted here via {@link #queryHandles()}.
 */
@ThreadSafe
public abstract class WindowsFileSystem extends AbstractFileSystem {

    /**
     * The maximum number of Windows handles. Both the 32-bit and 64-bit limits are essentially infinite for practical
     * purposes. See
     * <a href="https://blogs.technet.microsoft.com/markrussinovich/2009/09/29/pushing-the-limits-of-windows-handles/">
     * Pushing the Limits of Windows: Handles</a>.
     */
    protected static final long MAX_WINDOWS_HANDLES;
    static {
        if (System.getenv("ProgramFiles(x86)") == null) {
            MAX_WINDOWS_HANDLES = 16_777_216L - 32_768L;
        } else {
            MAX_WINDOWS_HANDLES = 16_777_216L - 65_536L;
        }
    }

    @Override
    public long getOpenFileDescriptors() {
        return queryHandles().getOrDefault(HandleCountProperty.HANDLECOUNT, 0L);
    }

    @Override
    public long getMaxFileDescriptors() {
        return MAX_WINDOWS_HANDLES;
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        // Windows has no separate per-process handle limit; return the system-wide maximum.
        return MAX_WINDOWS_HANDLES;
    }

    /**
     * Queries the current system-wide open handle count via the subclass's native mechanism (JNA or FFM).
     *
     * @return a map including the {@link HandleCountProperty#HANDLECOUNT} total
     */
    protected abstract Map<HandleCountProperty, Long> queryHandles();
}
