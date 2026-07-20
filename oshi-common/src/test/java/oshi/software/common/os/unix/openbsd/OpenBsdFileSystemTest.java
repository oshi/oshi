/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.openbsd;

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

class OpenBsdFileSystemTest {

    @Test
    void testParseDfInodesTypical() {
        List<String> lines = Arrays.asList(
                "Filesystem  512-blocks      Used     Avail Capacity iused   ifree  %iused  Mounted on",
                "/dev/sd0a      2149212    908676   1133076    45%    8355  147163     5%   /",
                "/dev/sd0e      4050876        36   3848300     0%      10  285108     0%   /home");
        Pair<Map<String, Long>, Map<String, Long>> result = OpenBsdFileSystem.parseDfInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> usedMap = result.getB();
        assertThat(freeMap, is(aMapWithSize(2)));
        assertThat(freeMap.get("/dev/sd0a"), is(147163L));
        assertThat(freeMap.get("/dev/sd0e"), is(285108L));
        assertThat(usedMap.get("/dev/sd0a"), is(8355L));
        assertThat(usedMap.get("/dev/sd0e"), is(10L));
    }

    @Test
    void testParseDfInodesEmpty() {
        Pair<Map<String, Long>, Map<String, Long>> result = OpenBsdFileSystem.parseDfInodes(Collections.emptyList());
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }

    @Test
    void testParseDfInodesSkipsNonDeviceLines() {
        List<String> lines = Arrays.asList(
                "Filesystem  512-blocks      Used     Avail Capacity iused   ifree  %iused  Mounted on",
                "mfs:12345      1048576    10240   1038336     1%       5   65531     0%   /tmp");
        Pair<Map<String, Long>, Map<String, Long>> result = OpenBsdFileSystem.parseDfInodes(lines);
        // Neither line starts with '/'
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }
}
