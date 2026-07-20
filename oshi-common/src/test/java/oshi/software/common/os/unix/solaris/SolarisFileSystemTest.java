/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

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

class SolarisFileSystemTest {

    @Test
    void testParseDfgInodesTypical() {
        List<String> lines = Arrays.asList(
                "/                  (/dev/md/dsk/d0    ):         8192 block size          1024 frag size",
                "41310292 total blocks   18193814 free blocks 17780712 available        2486848 total files",
                " 2293351 free files     22282240 filesys id",
                "     ufs fstype       0x00000004 flag             255 filename length",
                "/usr               (/dev/md/dsk/d1    ):         8192 block size          1024 frag size",
                "20000000 total blocks    5000000 free blocks  4800000 available        1000000 total files",
                "  800000 free files     33333333 filesys id",
                "     ufs fstype       0x00000004 flag             255 filename length");
        Pair<Map<String, Long>, Map<String, Long>> result = SolarisFileSystem.parseDfgInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> totalMap = result.getB();
        assertThat(freeMap, is(aMapWithSize(2)));
        assertThat(freeMap.get("/"), is(2293351L));
        assertThat(freeMap.get("/usr"), is(800000L));
        assertThat(totalMap.get("/"), is(2486848L));
        assertThat(totalMap.get("/usr"), is(1000000L));
    }

    @Test
    void testParseDfgInodesEmpty() {
        Pair<Map<String, Long>, Map<String, Long>> result = SolarisFileSystem.parseDfgInodes(Collections.emptyList());
        assertThat(result.getA(), is(anEmptyMap()));
        assertThat(result.getB(), is(anEmptyMap()));
    }

    @Test
    void testParseDfgInodesSingleMount() {
        List<String> lines = Arrays.asList(
                "/export/home       (/dev/dsk/c0t0d0s7):         8192 block size          1024 frag size",
                "10000000 total blocks    3000000 free blocks  2900000 available         500000 total files",
                "  450000 free files     44444444 filesys id",
                "     ufs fstype       0x00000004 flag             255 filename length");
        Pair<Map<String, Long>, Map<String, Long>> result = SolarisFileSystem.parseDfgInodes(lines);
        Map<String, Long> freeMap = result.getA();
        Map<String, Long> totalMap = result.getB();
        assertThat(freeMap, is(aMapWithSize(1)));
        assertThat(freeMap.get("/export/home"), is(450000L));
        assertThat(totalMap.get("/export/home"), is(500000L));
    }
}
