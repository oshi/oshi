/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Decides whether an optional native library may be loaded.
 * <p>
 * On Solaris and illumos, AIX, NetBSD, DragonFly BSD and OpenBSD, {@code dlerror()} state is shared by the whole
 * process rather than kept per thread. Another thread's failed {@code dlopen} or {@code dlsym} can rewrite the message
 * a thread is about to read, and another thread's {@code dlerror()} call clears it. JNA reads that message with an
 * unguarded {@code strlen} and {@code strcpy}, so a failed load that overlaps another thread's failed lookup can crash
 * the JVM on a null message or, where the message lives in a reused buffer, corrupt the native heap. The only defense
 * available from Java is not to attempt a load that will fail, so on those platforms this class checks that the library
 * file exists before the JNA or FFM backend loads it. On Linux, macOS, FreeBSD and Windows the state is per thread, a
 * failed load is harmless, and loading is always allowed. NetBSD shares the state too, but OSHI loads no optional
 * library there, so it is not checked.
 * <p>
 * The check covers the runtime linker's default directories and its library path environment variables, not directories
 * an administrator has added to the linker configuration. A library found only there is reported absent, and the caller
 * uses its fallback.
 */
@ThreadSafe
public final class NativeLibraryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NativeLibraryUtil.class);

    private static final PlatformEnum PLATFORM = PlatformEnum.getCurrentPlatform();

    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)?");

    private NativeLibraryUtil() {
    }

    /**
     * Returns whether an optional native library may be loaded. On a platform whose {@code dlerror()} state is shared
     * by the process, this is {@code true} only when the library file exists on the runtime linker's search path, so
     * that a load which would fail is never attempted; on every other platform it is always {@code true}.
     * <p>
     * Pass the file name the loader will open: {@code System.mapLibraryName("cups")} for a JNA
     * {@code Native.load("cups", ...)} or an FFM lookup by simple name, or the literal name for an FFM lookup by file
     * name such as {@code "libkstat2.so.1"}. On AIX, JNA opens the {@code .a} archive in place of a {@code .so} name,
     * so a JNA caller passes that. On OpenBSD, whose runtime linker resolves an unversioned name to a versioned file, a
     * versioned file also counts. On AIX, an archive name such as {@code "libcups.a"} never counts, because an archive
     * can be loaded only by naming a member inside it.
     *
     * @param fileName the library file name, without a directory
     * @return {@code false} if loading the library on this platform should be skipped
     */
    public static boolean isSafeToLoad(String fileName) {
        return isSafeToLoad(fileName, null);
    }

    /**
     * Returns whether an optional native library may be loaded, as {@link #isSafeToLoad(String)} does, also searching
     * directories the loader adds ahead of the runtime linker's own, such as JNA's {@code jna.library.path}. The loader
     * opens a file there by its full path, so only the exact name counts in those directories.
     *
     * @param fileName  the library file name, without a directory
     * @param extraPath further directories to search, separated by {@code :}, or null
     * @return {@code false} if loading the library on this platform should be skipped
     */
    public static boolean isSafeToLoad(String fileName, @Nullable String extraPath) {
        List<String> dirs = searchDirs(PLATFORM, System::getenv);
        if (dirs.isEmpty()) {
            return true;
        }
        List<String> extraDirs = new ArrayList<>();
        addEntries(extraDirs, extraPath);
        boolean present = isPresent(PLATFORM, fileName, dirs, extraDirs);
        if (!present) {
            LOG.debug("{} not found on the library search path; not loading it.", fileName);
        }
        return present;
    }

    /**
     * Returns whether the runtime linker on this platform would find a library by this name in any of the directories.
     *
     * @param platform the platform whose naming rules apply
     * @param fileName the library file name
     * @param dirs     the directories to search
     * @return whether a matching file exists in one of them
     */
    static boolean isPresent(PlatformEnum platform, String fileName, List<String> dirs) {
        if (isArchive(platform, fileName)) {
            return false;
        }
        return isPresent(fileName, dirs) || (platform == PlatformEnum.OPENBSD && isVersionPresent(fileName, dirs));
    }

    /**
     * Returns whether a library by this name would be found in the runtime linker's directories or, by its exact name,
     * in directories the loader searches itself. The loader opens a file there by its full path, and only under the
     * name it was asked for, so a versioned file in those directories is never reached.
     *
     * @param platform   the platform whose naming rules apply
     * @param fileName   the library file name
     * @param linkerDirs the directories the runtime linker searches
     * @param loaderDirs the directories the loader searches before handing the name to the runtime linker
     * @return whether a matching file exists in one of them
     */
    static boolean isPresent(PlatformEnum platform, String fileName, List<String> linkerDirs, List<String> loaderDirs) {
        return isPresent(platform, fileName, linkerDirs)
                || (!isArchive(platform, fileName) && isPresent(fileName, loaderDirs));
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
     * Returns whether a versioned file for this name, such as {@code libcups.so.2} or {@code libcups.so.7.3} for
     * {@code libcups.so}, exists in any of the directories. The suffix must be a major number with an optional minor
     * number, the form OpenBSD's runtime linker accepts, so a different library sharing the prefix, such as
     * {@code libcupsfilters.so}, does not match.
     *
     * @param fileName the unversioned library file name
     * @param dirs     the directories to search
     * @return whether a versioned file exists in one of them
     */
    static boolean isVersionPresent(String fileName, List<String> dirs) {
        for (String dir : dirs) {
            String[] names = new File(dir).list();
            if (names == null) {
                continue;
            }
            for (String name : names) {
                if (isVersionOf(fileName, name) && new File(dir, name).isFile()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether a file name is a versioned name for a library: the library name, a dot, and a major number with
     * an optional minor number.
     *
     * @param fileName the unversioned library file name
     * @param name     the file name to test
     * @return whether {@code name} is a versioned name for {@code fileName}
     */
    static boolean isVersionOf(String fileName, String name) {
        String prefix = fileName + ".";
        return name.startsWith(prefix) && VERSION.matcher(name.substring(prefix.length())).matches();
    }

    /**
     * Builds the directories the runtime linker searches by default on a platform whose {@code dlerror()} state is
     * shared by the process: the entries of its library path environment variables, followed by its default
     * directories.
     * <p>
     * On Solaris and illumos, every JDK is 64-bit, so {@code LD_LIBRARY_PATH_64} replaces {@code LD_LIBRARY_PATH} when
     * it is set, even to an empty value. On AIX, {@code LIBPATH} likewise replaces {@code LD_LIBRARY_PATH}.
     *
     * @param platform the platform
     * @param getenv   reads an environment variable, returning null if it is unset
     * @return the directories, without empty entries; empty if the platform's {@code dlerror()} state is per thread
     */
    static List<String> searchDirs(PlatformEnum platform, Function<String, @Nullable String> getenv) {
        List<String> dirs = new ArrayList<>();
        switch (platform) {
            case SOLARIS:
                @Nullable
                String path64 = getenv.apply("LD_LIBRARY_PATH_64");
                addEntries(dirs, path64 != null ? path64 : getenv.apply("LD_LIBRARY_PATH"));
                dirs.addAll(Arrays.asList("/lib/64", "/usr/lib/64"));
                break;
            case AIX:
                @Nullable
                String libpath = getenv.apply("LIBPATH");
                addEntries(dirs, libpath != null ? libpath : getenv.apply("LD_LIBRARY_PATH"));
                dirs.addAll(Arrays.asList("/usr/lib", "/lib"));
                break;
            case OPENBSD:
                addEntries(dirs, getenv.apply("LD_LIBRARY_PATH"));
                dirs.addAll(Arrays.asList("/usr/lib", "/usr/X11R6/lib", "/usr/local/lib"));
                break;
            case DRAGONFLYBSD:
                addEntries(dirs, getenv.apply("LD_LIBRARY_PATH"));
                dirs.addAll(Arrays.asList("/lib", "/usr/lib", "/usr/local/lib", "/usr/local/lib/compat/pkg"));
                break;
            default:
                break;
        }
        return dirs;
    }

    // An AIX archive loads only by member name, as libfoo.a(member), which no caller supplies
    private static boolean isArchive(PlatformEnum platform, String fileName) {
        return platform == PlatformEnum.AIX && fileName.endsWith(".a");
    }

    private static void addEntries(List<String> dirs, @Nullable String path) {
        if (path != null) {
            for (String dir : path.split(":", -1)) {
                if (!dir.isEmpty()) {
                    dirs.add(dir);
                }
            }
        }
    }
}
