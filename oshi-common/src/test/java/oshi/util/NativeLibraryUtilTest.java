/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeLibraryUtilTest {

    @TempDir
    private Path tempDir;

    @Test
    void testIsPresentFindsFileInAnyDirectory() throws IOException {
        Path empty = Files.createDirectory(tempDir.resolve("empty"));
        Path lib = Files.createDirectory(tempDir.resolve("lib"));
        Files.createFile(lib.resolve("libcups.so"));
        List<String> dirs = List.of(empty.toString(), lib.toString());

        assertThat(NativeLibraryUtil.isPresent("libcups.so", dirs), is(true));
        assertThat(NativeLibraryUtil.isPresent("libkstat2.so", dirs), is(false));
    }

    @Test
    void testIsPresentRequiresExactName() throws IOException {
        Files.createFile(tempDir.resolve("libcupsfilters.so"));
        Files.createFile(tempDir.resolve("libcups.so.2"));

        assertThat(NativeLibraryUtil.isPresent("libcups.so", List.of(tempDir.toString())), is(false));
    }

    @Test
    void testIsPresentIgnoresDirectoriesAndMissingDirs() throws IOException {
        Files.createDirectory(tempDir.resolve("libcups.so"));

        assertThat(NativeLibraryUtil.isPresent("libcups.so",
                List.of(tempDir.toString(), tempDir.resolve("missing").toString())), is(false));
    }

    @Test
    void testIsPresentIgnoresDanglingLink() throws IOException {
        Path link = tempDir.resolve("libcups.so");
        try {
            Files.createSymbolicLink(link, tempDir.resolve("libcups.so.2"));
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Symbolic links are not supported here");
        }

        assertThat(NativeLibraryUtil.isPresent("libcups.so", List.of(tempDir.toString())), is(false));
    }

    @Test
    void testVersionedNameCountsOnlyWhereTheLinkerResolvesIt() throws IOException {
        // OpenBSD packages ship only a versioned file, which dlopen("libcups.so") still finds. JNA on DragonFly BSD
        // reaches a versioned file only after a failed load of the unversioned name.
        Files.createFile(tempDir.resolve("libcups.so.7.3"));
        List<String> dirs = List.of(tempDir.toString());

        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.OPENBSD, "libcups.so", dirs), is(true));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.DRAGONFLYBSD, "libcups.so", dirs), is(false));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.SOLARIS, "libcups.so", dirs), is(false));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.AIX, "libcups.so", dirs), is(false));
    }

    @Test
    void testVersionedNameRequiresNumericSuffix() throws IOException {
        // Names with a trailing dot cannot be created as files on Windows, so the rule is tested on the names
        for (String name : List.of("libcups.so.2", "libcups.so.7.3")) {
            assertThat(name, NativeLibraryUtil.isVersionOf("libcups.so", name), is(true));
        }
        for (String name : List.of("libcups.so", "libcupsfilters.so.1", "libcups.so.debug", "libcups.so.",
                "libcups.so.7.", "libcups.so.7.3.1", "libcups.so..7", "libcups.so.7a")) {
            assertThat(name, NativeLibraryUtil.isVersionOf("libcups.so", name), is(false));
        }

        Files.createFile(tempDir.resolve("libcupsfilters.so.1"));
        Files.createFile(tempDir.resolve("libcups.so.debug"));
        Files.createDirectory(tempDir.resolve("libcups.so.2"));

        assertThat(NativeLibraryUtil.isVersionPresent("libcups.so", List.of(tempDir.toString())), is(false));
        assertThat(NativeLibraryUtil.isVersionPresent("libcups.so", List.of(tempDir.resolve("missing").toString())),
                is(false));
    }

    @Test
    void testAixRejectsArchives() throws IOException {
        Files.createFile(tempDir.resolve("libcups.a"));
        Files.createFile(tempDir.resolve("libfoo.so"));
        List<String> dirs = List.of(tempDir.toString());

        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.AIX, "libcups.a", dirs), is(false));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.AIX, "libcups.so", dirs), is(false));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.AIX, "libfoo.so", dirs), is(true));
    }

    @Test
    void testSolarisSearchDirs() {
        // LD_LIBRARY_PATH_64 replaces LD_LIBRARY_PATH for a 64-bit process
        assertThat(
                searchDirs(PlatformEnum.SOLARIS,
                        Map.of("LD_LIBRARY_PATH_64", "/opt/a64::/opt/b64", "LD_LIBRARY_PATH", "/opt/c")),
                contains("/opt/a64", "/opt/b64", "/lib/64", "/usr/lib/64"));
        assertThat(searchDirs(PlatformEnum.SOLARIS, Map.of("LD_LIBRARY_PATH", "/opt/c:")),
                contains("/opt/c", "/lib/64", "/usr/lib/64"));
        // An empty LD_LIBRARY_PATH_64 still replaces LD_LIBRARY_PATH
        assertThat(searchDirs(PlatformEnum.SOLARIS, Map.of("LD_LIBRARY_PATH_64", "", "LD_LIBRARY_PATH", "/opt/c")),
                contains("/lib/64", "/usr/lib/64"));
        assertThat(searchDirs(PlatformEnum.SOLARIS, Map.of()), contains("/lib/64", "/usr/lib/64"));
    }

    @Test
    void testOtherSearchDirs() {
        // LIBPATH replaces LD_LIBRARY_PATH
        assertThat(searchDirs(PlatformEnum.AIX, Map.of("LIBPATH", "/opt/a", "LD_LIBRARY_PATH", "/opt/b")),
                contains("/opt/a", "/usr/lib", "/lib"));
        assertThat(searchDirs(PlatformEnum.AIX, Map.of("LD_LIBRARY_PATH", "/opt/b")),
                contains("/opt/b", "/usr/lib", "/lib"));
        assertThat(searchDirs(PlatformEnum.OPENBSD, Map.of("LD_LIBRARY_PATH", "/opt/a")),
                contains("/opt/a", "/usr/lib", "/usr/X11R6/lib", "/usr/local/lib"));
        assertThat(searchDirs(PlatformEnum.DRAGONFLYBSD, Map.of()),
                contains("/lib", "/usr/lib", "/usr/local/lib", "/usr/local/lib/compat/pkg"));
        // Per-thread dlerror state: nothing to check
        for (PlatformEnum p : List.of(PlatformEnum.LINUX, PlatformEnum.MACOS, PlatformEnum.FREEBSD, PlatformEnum.NETBSD,
                PlatformEnum.WINDOWS)) {
            assertThat(p.name(), searchDirs(p, Map.of("LD_LIBRARY_PATH", "/opt/a")), is(empty()));
        }
    }

    @Test
    void testAlwaysSafeWhereDlerrorIsPerThread() {
        assumeTrue(searchDirs(PlatformEnum.getCurrentPlatform(), Map.of()).isEmpty());
        assertThat(NativeLibraryUtil.isSafeToLoad("libdoesnotexist.so"), is(true));
    }

    @Test
    void testLoaderDirsCountOnlyExactNames() throws IOException {
        Path linker = Files.createDirectory(tempDir.resolve("linker"));
        Path loader = Files.createDirectory(tempDir.resolve("loader"));
        Files.createFile(loader.resolve("libcups.so.7.3"));
        Files.createFile(loader.resolve("libcups.a"));
        Files.createFile(loader.resolve("libfoo.so"));
        List<String> linkerDirs = List.of(linker.toString());
        List<String> loaderDirs = List.of(loader.toString());

        assertThat(NativeLibraryUtil.isVersionPresent("libcups.so", List.of(loader.toString())), is(true));
        // The OpenBSD linker would resolve the versioned file, but it never searches the loader's directories
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.OPENBSD, "libcups.so", linkerDirs, loaderDirs), is(false));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.OPENBSD, "libcups.so", loaderDirs, linkerDirs), is(true));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.AIX, "libcups.a", linkerDirs, loaderDirs), is(false));
        assertThat(NativeLibraryUtil.isPresent(PlatformEnum.OPENBSD, "libfoo.so", linkerDirs, loaderDirs), is(true));
    }

    @Test
    void testExtraPathIsSearched() throws IOException {
        assumeTrue(!searchDirs(PlatformEnum.getCurrentPlatform(), Map.of()).isEmpty());
        Path lib = Files.createDirectory(tempDir.resolve("lib"));
        Files.createFile(lib.resolve("liboshitest.so"));

        assertThat(NativeLibraryUtil.isSafeToLoad("liboshitest.so"), is(false));
        assertThat(NativeLibraryUtil.isSafeToLoad("liboshitest.so", "/nonexistent:" + lib), is(true));
    }

    private static List<String> searchDirs(PlatformEnum platform, Map<String, String> env) {
        return NativeLibraryUtil.searchDirs(platform, env::get);
    }
}
