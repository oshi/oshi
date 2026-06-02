/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FreeBsdVirtualMemory#parseSwapUsed(String)} and {@link FreeBsdVirtualMemory#sumSwapUsed(List)}.
 * Exercises the swapinfo -k parsing path without shelling out, so this runs on every PR CI rather than only the FreeBSD
 * VM.
 */
class FreeBsdVirtualMemoryTest {

    @Test
    void parsesUsedColumnAndConvertsKbToBytes() {
        // Sample row from `swapinfo -k`:
        // Device 1K-blocks Used Avail Capacity
        // /dev/da0p2.eli 524288 123456 400832 24%
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

    @Test
    void sumsAcrossMultipleSwapDevices() {
        List<String> swapinfo = Arrays.asList("Device          1K-blocks     Used    Avail Capacity",
                "/dev/da0p2.eli     524288   100000   424288    19%",
                "/dev/da1p2.eli     524288    50000   474288    10%");
        assertThat("Multi-device used should sum to 150000 KB", FreeBsdVirtualMemory.sumSwapUsed(swapinfo),
                is(150000L << 10));
    }

    @Test
    void totalSummaryRowOverridesPerDeviceSum() {
        // -hT output: per-device rows would sum to 150_000, but the Total row reports a different (canonical) value.
        List<String> swapinfo = Arrays.asList("Device          1K-blocks     Used    Avail Capacity",
                "/dev/da0p2.eli     524288   100000   424288    19%",
                "/dev/da1p2.eli     524288    50000   474288    10%",
                "Total             1048576   123456   925120    11%");
        assertThat("Total row should win when present", FreeBsdVirtualMemory.sumSwapUsed(swapinfo), is(123456L << 10));
    }

    @Test
    void sumSwapUsedHandlesSingleDevice() {
        List<String> swapinfo = Arrays.asList("Device          1K-blocks     Used    Avail Capacity",
                "/dev/da0p2.eli     524288   123456   400832    24%");
        assertThat("Single device matches the per-row parser", FreeBsdVirtualMemory.sumSwapUsed(swapinfo),
                is(123456L << 10));
    }

    @Test
    void sumSwapUsedReturnsZeroForHeaderOnly() {
        assertThat("Header row alone implies no swap is configured",
                FreeBsdVirtualMemory.sumSwapUsed(Arrays.asList("Device 1K-blocks Used Avail Capacity")), is(0L));
    }

    @Test
    void sumSwapUsedReturnsZeroForEmptyOutput() {
        assertThat("Empty command output returns 0", FreeBsdVirtualMemory.sumSwapUsed(emptyList()), is(0L));
    }
}
