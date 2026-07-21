/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.tuples.Pair;

class LssradTest {

    @Test
    void testParseNodesPackages() {
        // lssrad -av: leading-digit lines set the REF (node); indented lines carry SRAD (slot) + CPU ranges.
        // A range line without a decimal MEM column is a continuation that keeps the previous slot.
        Map<Integer, Pair<Integer, Integer>> map = Lssrad.parseNodesPackages(Arrays.asList(//
                "REF1        SRAD        MEM        CPU", //
                "0", //
                "               0       32749.12    0-63", //
                "               1        9462.00    64-67 72-75", //
                "                                   80-83 88-91", //
                "1", //
                "               2        2471.19    92-95"));
        // node 0, slot 0
        assertThat(map.get(0).getA(), is(0));
        assertThat(map.get(0).getB(), is(0));
        assertThat(map.get(63).getA(), is(0));
        // node 0, slot 1
        assertThat(map.get(64).getA(), is(0));
        assertThat(map.get(64).getB(), is(1));
        // continuation line keeps node 0, slot 1
        assertThat(map.get(80).getA(), is(0));
        assertThat(map.get(80).getB(), is(1));
        // node 1, slot 2
        assertThat(map.get(92).getA(), is(1));
        assertThat(map.get(92).getB(), is(2));
    }

    @Test
    void testParseNodesPackagesEmpty() {
        assertThat(Lssrad.parseNodesPackages(Collections.emptyList()), is(anEmptyMap()));
    }

    @Test
    @EnabledOnOs(OS.AIX)
    void testQueryLssrad() {
        Map<Integer, Pair<Integer, Integer>> nodeMap = Lssrad.queryNodesPackages();
        assertThat("Node Map shouldn't be empty", nodeMap, not(anEmptyMap()));
    }
}
