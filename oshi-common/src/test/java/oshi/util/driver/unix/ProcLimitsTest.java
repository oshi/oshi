/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProcLimits}, driven by real {@code /proc/<pid>/limits} content.
 */
class ProcLimitsTest {

    private static final int SOFT = 1;
    private static final int HARD = 2;

    private static final String HEADER = "Limit                     Soft Limit           Hard Limit           Units";

    private static List<String> limits(String maxOpenFilesRow) {
        return List.of(HEADER, "Max cpu time              unlimited            unlimited            seconds",
                maxOpenFilesRow, "Max processes             31573                31573                processes");
    }

    @Test
    void testBothLimitsNumeric() {
        List<String> lines = limits(
                "Max open files            1024                 4096                 files            ");
        assertThat("soft limit", ProcLimits.parseOpenFileLimit(lines, SOFT), is(1024L));
        assertThat("hard limit", ProcLimits.parseOpenFileLimit(lines, HARD), is(4096L));
    }

    @Test
    void testHardUnlimitedKeepsSoftLimit() {
        // A root process after `ulimit -Hn unlimited` still has a real soft limit. Reading the row as a whole and
        // bailing out on the word "unlimited" anywhere in it discarded that soft limit and returned -1.
        List<String> lines = limits(
                "Max open files            1024                 unlimited            files            ");
        assertThat("soft limit survives an unlimited hard limit", ProcLimits.parseOpenFileLimit(lines, SOFT),
                is(1024L));
        assertThat("unlimited hard limit is unknown", ProcLimits.parseOpenFileLimit(lines, HARD), is(-1L));
    }

    @Test
    void testBothUnlimited() {
        List<String> lines = limits(
                "Max open files            unlimited            unlimited            files            ");
        assertThat("soft limit", ProcLimits.parseOpenFileLimit(lines, SOFT), is(-1L));
        assertThat("hard limit", ProcLimits.parseOpenFileLimit(lines, HARD), is(-1L));
    }

    @Test
    void testRowAbsentOrEmpty() {
        List<String> noRow = List.of(HEADER,
                "Max processes             31573                31573                processes");
        assertThat("no Max open files row", ProcLimits.parseOpenFileLimit(noRow, SOFT), is(-1L));
        assertThat("empty input", ProcLimits.parseOpenFileLimit(Collections.emptyList(), SOFT), is(-1L));
    }

    @Test
    void testIndexBeyondTheRow() {
        // Only one numeric field present, so the hard limit index is past the end of the split
        List<String> lines = limits("Max open files            1024                 unlimited            files");
        assertThat(ProcLimits.parseOpenFileLimit(lines, 3), is(-1L));
    }

    @Test
    void testIndexBelowTheFirstField() {
        // Element 0 is the empty string to the left of the label, and anything lower would index out of bounds
        List<String> lines = limits(
                "Max open files            1024                 4096                 files            ");
        assertThat("index 0 is the label remnant", ProcLimits.parseOpenFileLimit(lines, 0), is(-1L));
        assertThat("negative index", ProcLimits.parseOpenFileLimit(lines, -1), is(-1L));
    }
}
