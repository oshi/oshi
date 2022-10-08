/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.tuples.Pair;

@EnabledOnOs(OS.AIX)
class LssradTest {
    @Test
    void testQueryLssrad() {
        Map<Integer, Pair<Integer, Integer>> nodeMap = Lssrad.queryNodesPackages();
        assertThat("Node Map shouldn't be empty", nodeMap, not(anEmptyMap()));
    }
}
