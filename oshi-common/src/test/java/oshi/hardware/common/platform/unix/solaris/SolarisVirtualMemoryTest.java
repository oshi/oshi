/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

class SolarisVirtualMemoryTest {

    @Test
    void testSumKstatLong() {
        List<String> kstat = Arrays.asList(//
                "cpu_stat:0:cpu_stat0:pgswapin\t42", //
                "cpu_stat:1:cpu_stat1:pgswapin\t18");
        assertThat(SolarisVirtualMemory.sumKstatLong(kstat), is(60L));
    }

    @Test
    void testSumKstatLongEmpty() {
        assertThat(SolarisVirtualMemory.sumKstatLong(Collections.emptyList()), is(0L));
    }

    @Test
    void testParseSwapInfo() {
        // swap -lk output: device totalK freeK
        String swapLine = "/dev/zvol/dsk/rpool/swap  1048576K  532480K";
        Pair<Long, Long> result = SolarisVirtualMemory.parseSwapInfo(swapLine);
        // total = 1048576 * 1024
        assertThat(result.getB(), is(1048576L * 1024L));
        // used = (1048576 - 532480) * 1024
        assertThat(result.getA(), is((1048576L - 532480L) * 1024L));
    }

    @Test
    void testParseSwapInfoEmpty() {
        Pair<Long, Long> result = SolarisVirtualMemory.parseSwapInfo("");
        assertThat(result.getA(), is(0L));
        assertThat(result.getB(), is(0L));
    }
}
