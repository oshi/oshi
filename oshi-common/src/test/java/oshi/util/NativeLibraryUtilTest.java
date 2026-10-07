/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void testLibrarySearchDirs() {
        // LD_LIBRARY_PATH_64 replaces LD_LIBRARY_PATH for a 64-bit process
        assertThat(NativeLibraryUtil.librarySearchDirs("/opt/a64::/opt/b64", "/opt/c"),
                contains("/opt/a64", "/opt/b64", "/lib/64", "/usr/lib/64"));
        assertThat(NativeLibraryUtil.librarySearchDirs(null, "/opt/c:"), contains("/opt/c", "/lib/64", "/usr/lib/64"));
        // An empty LD_LIBRARY_PATH_64 still replaces LD_LIBRARY_PATH
        assertThat(NativeLibraryUtil.librarySearchDirs("", "/opt/c"), contains("/lib/64", "/usr/lib/64"));
        assertThat(NativeLibraryUtil.librarySearchDirs(null, null), contains("/lib/64", "/usr/lib/64"));
    }

    @Test
    void testAlwaysSafeOffSolaris() {
        assumeTrue(PlatformEnum.getCurrentPlatform() != PlatformEnum.SOLARIS);
        assertThat(NativeLibraryUtil.isSafeToLoad("libdoesnotexist.so"), is(true));
    }
}
