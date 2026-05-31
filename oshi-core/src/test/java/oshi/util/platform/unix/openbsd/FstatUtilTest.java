/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static oshi.util.PlatformEnum.OPENBSD;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.util.PlatformEnum;

/**
 * Tests for {@link FstatUtil}. Pure parsing methods are covered with sample {@code fstat -sp} output; the integration
 * test is gated to OpenBSD.
 */
class FstatUtilTest {

    // OpenBSD `fstat -sp <pid>` produces ~11 whitespace-separated columns. The parser excludes rows whose 5th column
    // (index 4) is a substring of "pipe" or "unix" — in practice the MOUNT column of a pipe row is the literal "pipe"
    // and "unix" for unix sockets. Sample rows are constructed so each split with limit 11 has exactly 11 fields and
    // the MOUNT column at index 4 takes the discriminating value.

    @Test
    void testParseOpenFilesCountsNonPipeAndNonUnixRows() {
        List<String> fstat = Arrays.asList("USER CMD PID FD MOUNT INUM MODE RW SZ FLAGS XS",
                "dan bash 123 0 /     12345 crw r  64 0 0", "dan bash 123 1 /dev  67890 crw rw 64 0 0",
                "dan bash 123 2 /dev  67890 crw rw 64 0 0", "dan bash 123 3 pipe  0     fff rw 0  0 0",
                "dan bash 123 4 unix  0     fff rw 0  0 0");
        // Header row's split[4] = "MOUNT" — neither "pipe" nor "unix" contains "MOUNT", so the header is counted in
        // the loop and then removed by the trailing -1. The two normal rows count; pipe and unix rows are excluded.
        // Counted = 1 header + 3 normal = 4; result = 4 - 1 = 3.
        assertThat(FstatUtil.parseOpenFiles(fstat), is(3L));
    }

    @Test
    void testParseOpenFilesHeaderOnly() {
        // single header row → count is 1, minus header = 0
        assertThat(FstatUtil.parseOpenFiles(Collections.singletonList("USER CMD PID FD MOUNT INUM MODE RW SZ FL XS")),
                is(0L));
    }

    @Test
    void testParseOpenFilesEmpty() {
        // Empty input: 0 counted - 1 for header subtraction = -1. This documents the existing behaviour: callers must
        // either always pass real `fstat` output that includes a header, or treat negative results as zero.
        assertThat(FstatUtil.parseOpenFiles(Collections.<String>emptyList()), is(-1L));
    }

    @Test
    void testFstatLive() {
        if (PlatformEnum.getCurrentPlatform().equals(OPENBSD)) {
            int pid = new SystemInfo().getOperatingSystem().getProcessId();

            assertThat("Number of open files must be nonnegative", FstatUtil.getOpenFiles(pid),
                    is(greaterThanOrEqualTo(0L)));

            assertThat("Cwd should not be empty", FstatUtil.getCwd(pid), is(not(emptyString())));
        }
    }
}
