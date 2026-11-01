/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Exercises the native volume lookup that names the file store holding a virtual disk's backing file, which a runner
 * with no file-backed disk would otherwise never reach.
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsHWDiskStoreFFMTest {

    @Test
    void testVolumePathNameOfSystemRoot() {
        String systemRoot = System.getenv("SystemRoot");
        assertNotNull(systemRoot);
        String volume = WindowsHWDiskStoreFFM.queryVolumePathName(systemRoot);
        assertThat(volume, endsWith("\\"));
        assertThat(systemRoot + " should be on " + volume,
                systemRoot.toUpperCase(Locale.ROOT).startsWith(volume.toUpperCase(Locale.ROOT)), is(true));
    }
}
