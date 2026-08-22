/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.util.FileUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

class LinuxVirtualMemoryTest {

    @Test
    void testParseMemInfoSwap(@TempDir Path tmp) throws IOException {
        // kB values are multiplied by 1024; used swap = total - free
        Path meminfo = tmp.resolve("meminfo");
        Files.writeString(meminfo, """
                MemTotal:       16000000 kB
                SwapTotal:       2000000 kB
                SwapFree:         500000 kB
                CommitLimit:     8000000 kB
                """);
        Triplet<Long, Long, Long> t = LinuxVirtualMemory.parseMemInfo(FileUtil.readFile(meminfo.toString()));
        assertThat(t.getA(), is(1_500_000L * 1024));
        assertThat(t.getB(), is(2_000_000L * 1024));
        assertThat(t.getC(), is(8_000_000L * 1024));
    }

    @Test
    void testParseMemInfoEmpty() {
        Triplet<Long, Long, Long> t = LinuxVirtualMemory.parseMemInfo(Collections.emptyList());
        assertThat(t.getA(), is(0L));
        assertThat(t.getB(), is(0L));
        assertThat(t.getC(), is(0L));
    }

    @Test
    void testParseVmStat(@TempDir Path tmp) throws IOException {
        Path vmstat = tmp.resolve("vmstat");
        Files.writeString(vmstat, """
                pgpgin 12345
                pswpin 100
                pswpout 200
                pgfault 999
                """);
        Pair<Long, Long> p = LinuxVirtualMemory.parseVmStat(FileUtil.readFile(vmstat.toString()));
        assertThat(p.getA(), is(100L));
        assertThat(p.getB(), is(200L));
    }

    @Test
    void testParseVmStatEmpty() {
        Pair<Long, Long> p = LinuxVirtualMemory.parseVmStat(Collections.emptyList());
        assertThat(p.getA(), is(0L));
        assertThat(p.getB(), is(0L));
    }
}
