/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Kernel32FFM;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ParseUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

@ThreadSafe
public class WindowsFileSystemFFM extends AbstractFileSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsFileSystemFFM.class);

    private static final int BUFSIZE = 255;

    private static final int SEM_FAILCRITICALERRORS = 0x0001;

    private static final int FILE_CASE_SENSITIVE_SEARCH = 0x00000001;
    private static final int FILE_CASE_PRESERVED_NAMES = 0x00000002;
    private static final int FILE_FILE_COMPRESSION = 0x00000010;
    private static final int FILE_DAX_VOLUME = 0x20000000;
    private static final int FILE_NAMED_STREAMS = 0x00040000;
    private static final int FILE_PERSISTENT_ACLS = 0x00000008;
    private static final int FILE_READ_ONLY_VOLUME = 0x00080000;
    private static final int FILE_SEQUENTIAL_WRITE_ONCE = 0x00100000;
    private static final int FILE_SUPPORTS_ENCRYPTION = 0x00020000;
    private static final int FILE_SUPPORTS_OBJECT_IDS = 0x00010000;
    private static final int FILE_SUPPORTS_REPARSE_POINTS = 0x00000080;
    private static final int FILE_SUPPORTS_SPARSE_FILES = 0x00000040;
    private static final int FILE_SUPPORTS_TRANSACTIONS = 0x00200000;
    private static final int FILE_SUPPORTS_USN_JOURNAL = 0x02000000;
    private static final int FILE_UNICODE_ON_DISK = 0x00000004;
    private static final int FILE_VOLUME_IS_COMPRESSED = 0x00008000;
    private static final int FILE_VOLUME_QUOTAS = 0x00000020;

    private static final Map<Integer, String> OPTIONS_MAP = new HashMap<>();
    static {
        OPTIONS_MAP.put(FILE_CASE_PRESERVED_NAMES, "casepn");
        OPTIONS_MAP.put(FILE_CASE_SENSITIVE_SEARCH, "casess");
        OPTIONS_MAP.put(FILE_FILE_COMPRESSION, "fcomp");
        OPTIONS_MAP.put(FILE_DAX_VOLUME, "dax");
        OPTIONS_MAP.put(FILE_NAMED_STREAMS, "streams");
        OPTIONS_MAP.put(FILE_PERSISTENT_ACLS, "acls");
        OPTIONS_MAP.put(FILE_SEQUENTIAL_WRITE_ONCE, "wronce");
        OPTIONS_MAP.put(FILE_SUPPORTS_ENCRYPTION, "efs");
        OPTIONS_MAP.put(FILE_SUPPORTS_OBJECT_IDS, "oids");
        OPTIONS_MAP.put(FILE_SUPPORTS_REPARSE_POINTS, "reparse");
        OPTIONS_MAP.put(FILE_SUPPORTS_SPARSE_FILES, "sparse");
        OPTIONS_MAP.put(FILE_SUPPORTS_TRANSACTIONS, "trans");
        OPTIONS_MAP.put(FILE_SUPPORTS_USN_JOURNAL, "journaled");
        OPTIONS_MAP.put(FILE_UNICODE_ON_DISK, "unicode");
        OPTIONS_MAP.put(FILE_VOLUME_IS_COMPRESSED, "vcomp");
        OPTIONS_MAP.put(FILE_VOLUME_QUOTAS, "quota");
    }

    static final long MAX_WINDOWS_HANDLES;
    static {
        MAX_WINDOWS_HANDLES = 16_777_216L - 65_536L;
    }

    public WindowsFileSystemFFM() {
        Kernel32FFM.SetErrorMode(SEM_FAILCRITICALERRORS);
    }

    @Override
    public List<OSFileStore> getFileStores(boolean localOnly) {
        // Create list to hold results
        ArrayList<OSFileStore> result;

        // Begin with all the local volumes
        result = getLocalVolumes(null);
        LOG.debug("FFM implementation for getFileStores currently consists of only local volumes and not WMI volumes");
        return result;
    }

    /**
     * method for getting all mounted local drives.
     *
     * @param volumeToMatch an optional string to filter match, null otherwise
     * @return A list of {@link OSFileStore} objects representing all local mounted volumes
     */
    static ArrayList<OSFileStore> getLocalVolumes(String volumeToMatch) {
        ArrayList<OSFileStore> fs = new ArrayList<>();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment volumeNameBuf = arena.allocate(BUFSIZE * JAVA_CHAR.byteSize());
            Optional<MemorySegment> hVolOpt = Kernel32FFM.FindFirstVolume(volumeNameBuf, BUFSIZE);
            if (hVolOpt.isEmpty()) {
                return fs;
            }
            MemorySegment hVol = hVolOpt.get();
            try {
                do {
                    MemorySegment fstypeBuf = arena.allocate(16 * JAVA_CHAR.byteSize());
                    MemorySegment nameBuf = arena.allocate(BUFSIZE * JAVA_CHAR.byteSize());
                    MemorySegment mountBuf = arena.allocate(BUFSIZE * JAVA_CHAR.byteSize());
                    MemorySegment flagsBuf = arena.allocate(JAVA_INT);
                    MemorySegment userFreeBytesBuf = arena.allocate(JAVA_LONG);
                    MemorySegment totalBytesBuf = arena.allocate(JAVA_LONG);
                    MemorySegment systemFreeBytesBuf = arena.allocate(JAVA_LONG);
                    String volume = readWideString(volumeNameBuf);

                    Kernel32FFM.GetVolumeInformation(toWideString(arena, volume), nameBuf, BUFSIZE, NULL, NULL,
                            flagsBuf, fstypeBuf, 16);
                    int flags = flagsBuf.get(JAVA_INT, 0);

                    Kernel32FFM.GetVolumePathNamesForVolumeName(toWideString(arena, volume), mountBuf, BUFSIZE, NULL);
                    String mount = readWideString(mountBuf);

                    if (!mount.isEmpty() && (volumeToMatch == null || mount.equals(volumeToMatch))) {
                        String name = readWideString(nameBuf);
                        String fsType = readWideString(fstypeBuf);
                        StringBuilder options = new StringBuilder((FILE_READ_ONLY_VOLUME & flags) == 0 ? "rw" : "ro");
                        String moreOptions = OPTIONS_MAP.entrySet().stream().filter(e -> (e.getKey() & flags) > 0)
                                .map(Map.Entry::getValue).collect(Collectors.joining(","));
                        if (!moreOptions.isEmpty()) {
                            options.append(',').append(moreOptions);
                        }

                        Kernel32FFM.GetDiskFreeSpaceEx(toWideString(arena, volume), userFreeBytesBuf, totalBytesBuf,
                                systemFreeBytesBuf);
                        long systemFreeBytes = systemFreeBytesBuf.get(JAVA_INT, 0);
                        long totalBytes = totalBytesBuf.get(JAVA_INT, 0);
                        long usedFreeBytes = userFreeBytesBuf.get(JAVA_INT, 0);

                        String uuid = ParseUtil.parseUuidOrDefault(volume, "");

                        fs.add(new WindowsOSFileStoreFFM(String.format(Locale.ROOT, "%s (%s)", name, mount), volume, name,
                                mount, options.toString(), uuid, "", getDriveType(mountBuf), fsType, systemFreeBytes,
                                usedFreeBytes, totalBytes, 0, 0));
                    }
                } while (Kernel32FFM.FindNextVolume(hVol, volumeNameBuf, BUFSIZE).orElse(0) != 0);
                return fs;
            } finally {
                Kernel32FFM.FindVolumeClose(hVol);
            }
        }
    }

    /**
     * Private method for getting mounted drive type.
     *
     * @param mountBuf memory segment containing the mounted drive path
     * @return A drive type description
     */
    private static String getDriveType(MemorySegment mountBuf) {
        int type = Kernel32FFM.GetDriveType(mountBuf).orElse(-1);

        return switch (type) {
            case 2 -> "Removable drive";
            case 3 -> "Fixed drive";
            case 4 -> "Network drive";
            case 5 -> "CD-ROM";
            case 6 -> "RAM drive";
            default -> "Unknown drive type";
        };
    }

    @Override
    public long getOpenFileDescriptors() {
        LOG.debug("FFM implementation for getOpenFileDescriptors is not yet available (WMI dependent)");
        return -1;
    }

    @Override
    public long getMaxFileDescriptors() {
        return MAX_WINDOWS_HANDLES;
    }

    @Override
    public long getMaxFileDescriptorsPerProcess() {
        return MAX_WINDOWS_HANDLES;
    }
}
