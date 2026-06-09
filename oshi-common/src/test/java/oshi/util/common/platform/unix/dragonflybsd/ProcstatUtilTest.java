/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.dragonflybsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.util.ParseUtil;

/**
 * Tests for {@link ProcstatUtil}. Pure parsing methods are covered with sample {@code fstat} output; the integration
 * test is gated to DragonFly BSD.
 */
class ProcstatUtilTest {

    // DragonFly's fstat output positions the FD/type classifier ("wd", "root", "text", or a numeric fd) at the 5th
    // whitespace-split token (index 4). The parser excludes "wd"/"root"/"text" classifiers when counting open files
    // and uses the last token of the "wd" row as the working directory path. Sample rows here mimic that layout.

    @Test
    void testParseCwdFromFstatFindsWdRow() {
        List<String> fstat = Arrays.asList("USER     CMD       PID    NLWP   FD MOUNT INUM MODE NAME",
                "dan      bash      1234   1      wd /     45   drwx /home/dan",
                "dan      bash      1234   1      0  /dev  12   crw  /dev/tty");
        assertThat(ProcstatUtil.parseCwdFromFstat(fstat), is("/home/dan"));
    }

    @Test
    void testParseCwdFromFstatNoWdRowReturnsEmpty() {
        List<String> fstat = Arrays.asList("USER CMD PID NLWP FD MOUNT INUM MODE NAME",
                "dan  bash 1234 1 0  /dev 12 crw /dev/tty");
        assertThat(ProcstatUtil.parseCwdFromFstat(fstat), is(emptyString()));
    }

    @Test
    void testParseCwdFromFstatEmpty() {
        assertThat(ProcstatUtil.parseCwdFromFstat(Collections.<String>emptyList()), is(emptyString()));
    }

    @Test
    void testParseOpenFilesExcludesWdRootTextAndHeader() {
        // 5 rows total. The wd, root, and text rows are excluded; the header (token "FD" at index 4) is not excluded
        // by the filter but is removed via the -1 adjustment at the end. With 2 countable rows minus the header
        // adjustment, the result is 1.
        List<String> fstat = Arrays.asList("USER CMD PID NLWP FD MOUNT INUM MODE NAME",
                "dan  bash 1234 1 wd   /    45 drwx /home/dan", "dan  bash 1234 1 root /    1  drwx /",
                "dan  bash 1234 1 text /    99 -rxr /bin/bash", "dan  bash 1234 1 0    /dev 12 crw  /dev/tty",
                "dan  bash 1234 1 1    /dev 12 crw  /dev/tty");
        assertThat(ProcstatUtil.parseOpenFiles(fstat), is(2L));
    }

    @Test
    void testParseOpenFilesNoCountableRowsReturnsZero() {
        // Only header + excluded rows → no fd counted → returns 0 (per implementation: fd > 0 ? fd - 1 : 0)
        List<String> fstat = Arrays.asList("USER CMD PID NLWP FD MOUNT INUM MODE NAME",
                "dan  bash 1234 1 wd /home 45 drwx /home/dan");
        // Header row counts (split[4]="FD" not in {wd,root,text}), then the wd row is excluded.
        // Count=1 (header) → returns 1-1=0.
        assertThat(ProcstatUtil.parseOpenFiles(fstat), is(0L));
    }

    @Test
    void testParseOpenFilesEmpty() {
        assertThat(ProcstatUtil.parseOpenFiles(Collections.<String>emptyList()), is(0L));
    }

    @Test
    void testGetCwdMapNegativePidReturnsEmptyMap() {
        // No live command is run for a negative pid; the map is empty.
        assertThat(ProcstatUtil.getCwdMap(-1).size(), is(0));
    }

    @Test
    @EnabledIfSystemProperty(named = "os.name", matches = "(?i)dragonfly")
    void testProcstatLive() {
        // RuntimeMXBean#getName() returns "<pid>@<host>" — use it to avoid pulling oshi-core's SystemInfo into a
        // oshi-common test. Mirrors the pattern used in the FreeBSD ProcstatUtilTest and NetBSD FstatUtilTest.
        int pid = ParseUtil.parseIntOrDefault(ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0], -1);
        assertThat("Open files must be nonnegative", ProcstatUtil.getOpenFiles(pid), is(greaterThanOrEqualTo(0L)));
        assertThat("Cwd should be nonempty", ProcstatUtil.getCwd(pid), is(not(emptyString())));
    }
}
