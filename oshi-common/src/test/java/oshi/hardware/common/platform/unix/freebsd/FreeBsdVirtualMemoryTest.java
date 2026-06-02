/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FreeBsdVirtualMemory#parseSwapUsed(String)}. Exercises the swapinfo -k parsing path without
 * shelling out, so this runs on every PR CI rather than only the FreeBSD VM.
 */
class FreeBsdVirtualMemoryTest {

    @Test
    void parsesUsedColumnAndConvertsKbToBytes() {
        // Sample row from `swapinfo -k`:
        //   Device          1K-blocks     Used    Avail Capacity
        //   /dev/da0p2.eli     524288   123456   400832    24%
        long bytes = FreeBsdVirtualMemory.parseSwapUsed("/dev/da0p2.eli 524288 123456 400832 24%");
        assertThat("123456 KB should convert to 126418944 bytes", bytes, is(123456L << 10));
    }

    @Test
    void returnsZeroWhenUsedColumnIsZero() {
        long bytes = FreeBsdVirtualMemory.parseSwapUsed("/dev/da0p2.eli 524288 0 524288 0%");
        assertThat("Zero used should return 0 bytes", bytes, is(0L));
    }

    @Test
    void returnsZeroForShortRow() {
        assertThat("Too few columns is treated as no data", FreeBsdVirtualMemory.parseSwapUsed("foo bar baz"), is(0L));
    }

    @Test
    void returnsZeroForEmptyRow() {
        assertThat("Empty input returns 0", FreeBsdVirtualMemory.parseSwapUsed(""), is(0L));
    }

    @Test
    void returnsZeroForUnparseableUsedColumn() {
        assertThat("Non-numeric Used column is treated as 0",
                FreeBsdVirtualMemory.parseSwapUsed("/dev/da0p2.eli 524288 abc 400832 24%"), is(0L));
    }
}
