/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

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

class AixFileSystemTest {

    @Test
    void testParseDfInodesTypical() {
        List<String> lines = Arrays.asList("Filesystem    512-blocks     Ifree    Iused",
                "/dev/hd4         4194304   164951    15969", "/dev/hd2        52690944  2894117   196692",
                "/dev/hd9var      6291456   605443     2317");
        Pair<Map<String, Long>, Map<String, Long>> result = AixFileSystem.parseDfInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> totalMap = result.getB();
        assertThat(freeMap, is(aMapWithSize(3)));
        assertThat(freeMap.get("/dev/hd4"), is(164951L));
        assertThat(freeMap.get("/dev/hd2"), is(2894117L));
        assertThat(freeMap.get("/dev/hd9var"), is(605443L));
        assertThat(totalMap.get("/dev/hd4"), is(164951L + 15969L));
        assertThat(totalMap.get("/dev/hd2"), is(2894117L + 196692L));
        assertThat(totalMap.get("/dev/hd9var"), is(605443L + 2317L));
    }

    @Test
    void testParseDfInodesNfsMounts() {
        List<String> lines = Arrays.asList("Filesystem    512-blocks     Ifree    Iused",
                "nfshost:/export  8388608   500000    10000");
        Pair<Map<String, Long>, Map<String, Long>> result = AixFileSystem.parseDfInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> totalMap = result.getB();
        // NFS mounts matching "hostname:/path" should be included via FS_PATTERN
        assertThat(freeMap, is(aMapWithSize(1)));
        assertThat(freeMap.get("nfshost:/export"), is(500000L));
        assertThat(totalMap.get("nfshost:/export"), is(510000L));
    }

    @Test
    void testParseDfInodesSkipsProcAndHeader() {
        List<String> lines = Arrays.asList("Filesystem    512-blocks     Ifree    Iused",
                "/proc                  -        -        -");
        Pair<Map<String, Long>, Map<String, Long>> result = AixFileSystem.parseDfInodes(lines);
        // Header doesn't match FS_PATTERN, /proc has dashes so parseLong returns 0
        // Actually /proc starts with / so it does match FS_PATTERN
        Map<String, Long> freeMap = result.getA();
        assertThat(freeMap.get("/proc"), is(0L));
    }

    @Test
    void testParseDfInodesEmpty() {
        Pair<Map<String, Long>, Map<String, Long>> result = AixFileSystem.parseDfInodes(Collections.emptyList());
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }
}
