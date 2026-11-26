/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * Tests that {@link WindowsFileSystem} formats volume flags as the same options string whichever backend read them.
 */
class WindowsFileSystemTest {

    @Test
    void testVolumeOptionsListFlagsInAFixedOrder() {
        // Flags giving the options NTFS reported on a windows-latest runner, listed in the same order by both backends
        assertThat(WindowsFileSystem.volumeOptions(0x03E706FF),
                is("rw,casepn,casess,fcomp,streams,acls,efs,oids,reparse,sparse,trans,journaled,unicode,quota"));
        // Read-only, with no other flags set
        assertThat(WindowsFileSystem.volumeOptions(0x00080000), is("ro"));
        assertThat(WindowsFileSystem.volumeOptions(0), is("rw"));
    }
}
