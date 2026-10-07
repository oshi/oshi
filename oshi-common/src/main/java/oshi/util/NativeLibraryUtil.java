/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Decides whether an optional native library may be loaded.
 * <p>
 * On Solaris and illumos, {@code dlerror()} state is shared by the whole process rather than kept per thread: the
 * runtime linker holds a single pointer into one static message buffer, which every failed {@code dlopen} or
 * {@code dlsym} on any thread rewrites. JNA reads that message with an unguarded {@code strlen} and {@code strcpy}, so
 * a failed load that overlaps another thread's failed lookup can crash the JVM or corrupt the native heap. The only
 * defense available from Java is not to attempt a load that will fail, so on those platforms this class checks that the
 * library file exists before the JNA or FFM backend loads it. Elsewhere {@code dlerror()} state is per thread, a failed
 * load is harmless, and loading is always allowed.
 */
@ThreadSafe
public final class NativeLibraryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NativeLibraryUtil.class);

    /**
     * The runtime linker's default directories for a 64-bit process on Solaris and illumos. Every JDK on these
     * platforms is 64-bit.
     */
    private static final List<String> SYSTEM_LIBRARY_DIRS = Arrays.asList("/lib/64", "/usr/lib/64");

    private static final boolean CHECK_BEFORE_LOADING = PlatformEnum.getCurrentPlatform() == PlatformEnum.SOLARIS;

    private NativeLibraryUtil() {
    }

    /**
     * Returns whether an optional native library may be loaded. On Solaris and illumos this is {@code true} only when a
     * file with exactly this name exists on the library search path, so that a load which would fail is never
     * attempted; on every other platform it is always {@code true}.
     * <p>
     * Pass the file name the loader will open: {@code System.mapLibraryName("cups")} for a JNA
     * {@code Native.load("cups", ...)} or an FFM lookup by simple name, or the literal name for an FFM lookup by file
     * name such as {@code "libkstat2.so.1"}.
     *
     * @param fileName the library file name, without a directory
     * @return {@code false} if loading the library on this platform should be skipped
     */
    public static boolean isSafeToLoad(String fileName) {
        if (!CHECK_BEFORE_LOADING) {
            return true;
        }
        boolean present = isPresent(fileName,
                librarySearchDirs(System.getenv("LD_LIBRARY_PATH_64"), System.getenv("LD_LIBRARY_PATH")));
        if (!present) {
            LOG.debug("{} not found on the library search path; not loading it.", fileName);
        }
        return present;
    }

    /**
     * Returns whether a file with this name exists in any of the directories. A dangling symbolic link does not count.
     *
     * @param fileName the library file name
     * @param dirs     the directories to search
     * @return whether the file exists in one of them
     */
    static boolean isPresent(String fileName, List<String> dirs) {
        for (String dir : dirs) {
            if (new File(dir, fileName).isFile()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the directories the runtime linker searches for a 64-bit process: the entries of
     * {@code LD_LIBRARY_PATH_64} if it is set, even to an empty value, since it then replaces {@code LD_LIBRARY_PATH};
     * otherwise the entries of {@code LD_LIBRARY_PATH}; followed by the default 64-bit library directories.
     *
     * @param ldLibraryPath64 the value of {@code LD_LIBRARY_PATH_64}, or null if unset
     * @param ldLibraryPath   the value of {@code LD_LIBRARY_PATH}, or null if unset
     * @return the directories, without empty entries
     */
    static List<String> librarySearchDirs(@Nullable String ldLibraryPath64, @Nullable String ldLibraryPath) {
        List<String> dirs = new ArrayList<>();
        @Nullable
        String path = ldLibraryPath64 != null ? ldLibraryPath64 : ldLibraryPath;
        if (path != null) {
            for (String dir : path.split(":", -1)) {
                if (!dir.isEmpty()) {
                    dirs.add(dir);
                }
            }
        }
        dirs.addAll(SYSTEM_LIBRARY_DIRS);
        return dirs;
    }
}
