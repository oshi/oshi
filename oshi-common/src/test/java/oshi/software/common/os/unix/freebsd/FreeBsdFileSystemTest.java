/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

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

class FreeBsdFileSystemTest {

    @Test
    void testParseGeomPartListTypical() {
        List<String> lines = Arrays.asList("Geom name: ada0", "Providers:", "1. Name: ada0p1",
                "   rawuuid: 3e1cbe42-e2b9-11e6-85c6-0025909460ac", "2. Name: ada0p2",
                "   rawuuid: 3e2e5580-e2b9-11e6-85c6-0025909460ac", "3. Name: ada0p3",
                "   rawuuid: 3e3ed3be-e2b9-11e6-85c6-0025909460ac");
        Map<String, String> result = FreeBsdFileSystem.parseGeomPartList(lines);
        assertThat(result, is(aMapWithSize(3)));
        assertThat(result.get("ada0p1"), is("3e1cbe42-e2b9-11e6-85c6-0025909460ac"));
        assertThat(result.get("ada0p2"), is("3e2e5580-e2b9-11e6-85c6-0025909460ac"));
        assertThat(result.get("ada0p3"), is("3e3ed3be-e2b9-11e6-85c6-0025909460ac"));
    }

    @Test
    void testParseGeomPartListEmpty() {
        Map<String, String> result = FreeBsdFileSystem.parseGeomPartList(Collections.emptyList());
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    void testParseDfInodesTypical() {
        List<String> lines = Arrays.asList(
                "Filesystem    1K-blocks   Used   Avail Capacity iused  ifree %iused  Mounted on",
                "/dev/twed0s1a   2026030 584112 1279836    31%    2751 279871    1%   /",
                "/dev/twed0s1e  10240000 2048000 8192000    20%    5000 100000    5%   /usr");
        Pair<Map<String, Long>, Map<String, Long>> result = FreeBsdFileSystem.parseDfInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> totalMap = result.getB();
        assertThat(freeMap, is(aMapWithSize(2)));
        assertThat(freeMap.get("/"), is(279871L));
        assertThat(freeMap.get("/usr"), is(100000L));
        assertThat(totalMap.get("/"), is(2751L + 279871L));
        assertThat(totalMap.get("/usr"), is(5000L + 100000L));
    }

    @Test
    void testParseDfInodesEmpty() {
        Pair<Map<String, Long>, Map<String, Long>> result = FreeBsdFileSystem.parseDfInodes(Collections.emptyList());
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }

    @Test
    void testParseDfInodesSkipsHeaderAndNonSlash() {
        List<String> lines = Arrays.asList(
                "Filesystem    1K-blocks   Used   Avail Capacity iused  ifree %iused  Mounted on",
                "devfs               1      1       0   100%       0      0  100%   /dev");
        Pair<Map<String, Long>, Map<String, Long>> result = FreeBsdFileSystem.parseDfInodes(lines);
        // Header doesn't start with '/', devfs doesn't start with '/' either
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }
}
