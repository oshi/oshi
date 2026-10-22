/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.util.ParseUtil;

/**
 * Tests for {@link FstatUtil}. The open file count is covered with sample {@code fstat} output; the integration test is
 * gated to NetBSD.
 */
class FstatUtilTest {

    @Test
    void testParseOpenFilesCountsDescriptorsOnly() {
        // fstat -p of a shell on NetBSD 10.1, with the text, root and trace rows fstat prints when they apply
        List<String> fstat = """
                USER     CMD          PID   FD  MOUNT         INUM MODE         SZ|DV R/W
                oshi     sh          4759 text  /         1059845 -r-xr-xr-x  171784 r
                oshi     sh          4759   wd  /home     16203776 drwxr-xr-x     512 r
                oshi     sh          4759 root  /               2 drwxr-xr-x     512 r
                oshi     sh          4759   tr  /home     16203790 -rw-r--r--    1024 w
                oshi     sh          4759    0* pipe 0xffff0004326db900 <- 0xffff00042c48eb00 r
                oshi     sh          4759    1* pipe 0xffff0000f9e16100 -> 0xffff0001e9d42400 w
                oshi     sh          4759   10  /home     16203791 -rw-r--r--      12 r
                """.lines().toList();
        assertThat(FstatUtil.parseOpenFiles(fstat, 4759), is(3L));
    }

    @Test
    void testParseOpenFilesCommandWithSpace() {
        // fstat prints the command name unescaped, so a space in it shifts the FD column one place to the right
        List<String> fstat = """
                USER     CMD          PID   FD  MOUNT         INUM MODE         SZ|DV R/W
                oshi     my tool     4760   wd  /home     16203776 drwxr-xr-x     512 r
                oshi     my tool     4760 text  /         1059845 -r-xr-xr-x  171784 r
                oshi     my tool     4760    0* pipe 0xffff0004326db900 <- 0xffff00042c48eb00 r
                oshi     my tool     4760    3  /home     16203791 -rw-r--r--      12 r
                """.lines().toList();
        assertThat(FstatUtil.parseOpenFiles(fstat, 4760), is(2L));
    }

    @Test
    void testParseOpenFilesHeaderOnlyOrEmpty() {
        assertThat(FstatUtil.parseOpenFiles(List.of("USER CMD PID FD ..."), 1), is(0L));
        assertThat(FstatUtil.parseOpenFiles(Collections.<String>emptyList(), 1), is(0L));
    }

    @Test
    @EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
    void testFstatLive() {
        // RuntimeMXBean#getName() returns "<pid>@<host>" — use it to avoid pulling oshi-core's SystemInfo into a
        // oshi-common test. Mirrors the pattern used in the FreeBSD ProcstatUtilTest.
        int pid = ParseUtil.parseIntOrDefault(ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0], -1);
        assertThat("Number of open files must be nonnegative", FstatUtil.getOpenFiles(pid),
                is(greaterThanOrEqualTo(0L)));
        // NetBSD builds the /proc/<pid>/cwd link through getcwd_common(), which resolves the vnode through the name
        // cache and fails when it holds no entry for the directory, so the link reads back empty. Tolerate that, but
        // when a path is returned it must be absolute.
        String cwd = FstatUtil.getCwd(pid);
        assertThat("getCwd must never return null", cwd, is(notNullValue()));
        if (!cwd.isEmpty()) {
            assertThat("A resolved cwd should be an absolute path", cwd, startsWith("/"));
        }
    }
}
