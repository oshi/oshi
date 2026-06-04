/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FstatUtil}. Pure parsing methods are covered with sample {@code fstat -sp} output.
 */
class FstatUtilTest {

    @Test
    void testParseOpenFilesCountsNonPipeAndNonUnixRows() {
        List<String> fstat = Arrays.asList("USER CMD PID FD MOUNT INUM MODE RW SZ FLAGS XS",
                "dan bash 123 0 /     12345 crw r  64 0 0", "dan bash 123 1 /dev  67890 crw rw 64 0 0",
                "dan bash 123 2 /dev  67890 crw rw 64 0 0", "dan bash 123 3 pipe  0     fff rw 0  0 0",
                "dan bash 123 4 unix  0     fff rw 0  0 0");
        assertThat(FstatUtil.parseOpenFiles(fstat), is(3L));
    }

    @Test
    void testParseOpenFilesHeaderOnly() {
        assertThat(FstatUtil.parseOpenFiles(Collections.singletonList("USER CMD PID FD MOUNT INUM MODE RW SZ FL XS")),
                is(0L));
    }

    @Test
    void testParseOpenFilesEmpty() {
        assertThat(FstatUtil.parseOpenFiles(Collections.<String>emptyList()), is(0L));
    }
}
