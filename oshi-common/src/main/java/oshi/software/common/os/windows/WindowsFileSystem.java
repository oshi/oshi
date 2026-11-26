/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import java.util.Collections;
import java.util.LinkedHashMap;
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

    private static final int FILE_READ_ONLY_VOLUME = 0x00080000;

    /**
     * The {@code GetVolumeInformation} file system flags reported as mount options, in the order they are listed. The
     * names follow the {@code winnt.h} constants: {@code FILE_CASE_PRESERVED_NAMES} is {@code casepn}, and so on.
     */
    private static final Map<Integer, String> OPTIONS_MAP;

    static {
        Map<Integer, String> options = new LinkedHashMap<>();
        options.put(0x00000002, "casepn"); // FILE_CASE_PRESERVED_NAMES
        options.put(0x00000001, "casess"); // FILE_CASE_SENSITIVE_SEARCH
        options.put(0x00000010, "fcomp"); // FILE_FILE_COMPRESSION
        options.put(0x20000000, "dax"); // FILE_DAX_VOLUME
        options.put(0x00040000, "streams"); // FILE_NAMED_STREAMS
        options.put(0x00000008, "acls"); // FILE_PERSISTENT_ACLS
        options.put(0x00100000, "wronce"); // FILE_SEQUENTIAL_WRITE_ONCE
        options.put(0x00020000, "efs"); // FILE_SUPPORTS_ENCRYPTION
        options.put(0x00010000, "oids"); // FILE_SUPPORTS_OBJECT_IDS
        options.put(0x00000080, "reparse"); // FILE_SUPPORTS_REPARSE_POINTS
        options.put(0x00000040, "sparse"); // FILE_SUPPORTS_SPARSE_FILES
        options.put(0x00200000, "trans"); // FILE_SUPPORTS_TRANSACTIONS
        options.put(0x02000000, "journaled"); // FILE_SUPPORTS_USN_JOURNAL
        options.put(0x00000004, "unicode"); // FILE_UNICODE_ON_DISK
        options.put(0x00008000, "vcomp"); // FILE_VOLUME_IS_COMPRESSED
        options.put(0x00000020, "quota"); // FILE_VOLUME_QUOTAS
        OPTIONS_MAP = Collections.unmodifiableMap(options);
    }

    /**
     * Formats the flags {@code GetVolumeInformation} returns as a mount options string: {@code ro} or {@code rw},
     * followed by each set flag's name in a fixed order.
     *
     * @param flags the file system flags
     * @return the options, such as {@code rw,casepn,casess,unicode}
     */
    protected static String volumeOptions(int flags) {
        StringBuilder options = new StringBuilder((FILE_READ_ONLY_VOLUME & flags) == 0 ? "rw" : "ro");
        OPTIONS_MAP.forEach((flag, name) -> {
            if ((flag & flags) != 0) {
                options.append(',').append(name);
            }
        });
        return options.toString();
    }

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
