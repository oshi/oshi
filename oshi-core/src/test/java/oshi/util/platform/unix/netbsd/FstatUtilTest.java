/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.SystemInfo;

/**
 * Tests for {@link FstatUtil}. Pure parsing methods are covered with sample {@code fstat} output; the integration test
 * is gated to NetBSD.
 */
class FstatUtilTest {

    @Test
    void testParseCwdFromFstatFindsWdRow() {
        // Sample NetBSD `fstat -p <pid>` output. The wd row's 5th column (index 4) is the working directory path.
        List<String> fstat = Arrays.asList("USER     CMD          PID   FD MOUNT      INUM MODE         SZ|DV R/W",
                "root     bash        1234   wd /home/dan      12 drwxr-xr-x   512 r",
                "root     bash        1234    0 /dev          678 crw-rw-rw-  /dev/tty r",
                "root     bash        1234    1 /dev          678 crw-rw-rw-  /dev/tty w");

        assertThat(FstatUtil.parseCwdFromFstat(fstat), is("/home/dan"));
    }

    @Test
    void testParseCwdFromFstatNoWdRow() {
        List<String> fstat = Arrays.asList("USER  CMD  PID  FD  MOUNT  INUM  MODE  SZ|DV  R/W",
                "root  bash 1234  0  /dev   678   crw   tty    r");
        assertThat(FstatUtil.parseCwdFromFstat(fstat), is(emptyString()));
    }

    @Test
    void testParseCwdFromFstatEmptyInput() {
        assertThat(FstatUtil.parseCwdFromFstat(Collections.<String>emptyList()), is(emptyString()));
    }

    @Test
    void testParseOpenFilesSubtractsHeader() {
        List<String> fstat = Arrays.asList("USER  CMD  PID  FD  MOUNT  INUM  MODE  SZ|DV  R/W",
                "root  bash 1234  0  /dev   678   crw   tty    r", "root  bash 1234  1  /dev   678   crw   tty    w",
                "root  bash 1234  2  /dev   678   crw   tty    w");
        // 4 lines - 1 header = 3 open files
        assertThat(FstatUtil.parseOpenFiles(fstat), is(3L));
    }

    @Test
    void testParseOpenFilesHeaderOnlyOrEmpty() {
        assertThat(FstatUtil.parseOpenFiles(Collections.singletonList("USER CMD PID FD ...")), is(0L));
        assertThat(FstatUtil.parseOpenFiles(Collections.<String>emptyList()), is(0L));
    }

    @Test
    @EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
    void testFstatLive() {
        int pid = new SystemInfo().getOperatingSystem().getProcessId();
        assertThat("Number of open files must be nonnegative", FstatUtil.getOpenFiles(pid),
                is(greaterThanOrEqualTo(0L)));
        assertThat("Cwd should not be empty", FstatUtil.getCwd(pid), is(not(emptyString())));
    }
}
