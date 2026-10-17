/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class NetBsdFileSystemTest {

    @Test
    void testParsePstatTotalFiles() {
        // Captured from pstat -T on NetBSD 10.1, run as an unprivileged user
        List<String> lines = Arrays.asList("1100/10000 files", " 747531 vnodes", "no swap devices configured");
        assertThat(NetBsdFileSystem.parsePstatTotalFiles(lines), is(1100L));
        // Each count is printed with %3d, so a small one is padded
        assertThat(NetBsdFileSystem.parsePstatTotalFiles(Arrays.asList(" 42/100 files")), is(42L));
        // The vnode line has no slash, and the open-files header from pstat -f is a different count
        assertThat(NetBsdFileSystem.parsePstatTotalFiles(Arrays.asList(" 747531 vnodes", "179/10000 open files")),
                is(0L));
        assertThat(NetBsdFileSystem.parsePstatTotalFiles(Collections.emptyList()), is(0L));
    }
}
