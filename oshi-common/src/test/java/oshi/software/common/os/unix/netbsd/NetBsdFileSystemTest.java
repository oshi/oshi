/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

class NetBsdFileSystemTest {

    @Test
    void testParseDfInodesTypical() {
        List<String> lines = Arrays.asList(
                "Filesystem  512-blocks      Used     Avail Capacity iused   ifree  %iused  Mounted on",
                "/dev/wd0a      2149212    908676   1133076    45%    8355  147163     5%   /",
                "/dev/wd0e      4050876        36   3848300     0%      10  285108     0%   /home",
                "/dev/wd0d      6082908   3343172   2435592    58%   27813  386905     7%   /usr");
        Pair<Map<String, Long>, Map<String, Long>> result = NetBsdFileSystem.parseDfInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> usedMap = result.getB();
        assertThat(freeMap, is(aMapWithSize(3)));
        assertThat(freeMap.get("/dev/wd0a"), is(147163L));
        assertThat(freeMap.get("/dev/wd0e"), is(285108L));
        assertThat(freeMap.get("/dev/wd0d"), is(386905L));
        assertThat(usedMap.get("/dev/wd0a"), is(8355L));
        assertThat(usedMap.get("/dev/wd0e"), is(10L));
        assertThat(usedMap.get("/dev/wd0d"), is(27813L));
    }

    @Test
    void testParseDfInodesEmpty() {
        Pair<Map<String, Long>, Map<String, Long>> result = NetBsdFileSystem.parseDfInodes(Collections.emptyList());
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }

    @Test
    void testParseDfInodesSkipsHeader() {
        List<String> lines = Arrays
                .asList("Filesystem  512-blocks      Used     Avail Capacity iused   ifree  %iused  Mounted on");
        Pair<Map<String, Long>, Map<String, Long>> result = NetBsdFileSystem.parseDfInodes(lines);
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }
}
