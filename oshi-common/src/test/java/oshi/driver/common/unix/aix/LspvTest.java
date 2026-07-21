/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWPartition;
import oshi.util.tuples.Pair;

class LspvTest {

    @Test
    void testParsePpSize() {
        // lspv -L: an active volume with a 128 MB physical-partition size, returned in bytes
        long ppSize = Lspv.parsePpSize(Arrays.asList(//
                "PHYSICAL VOLUME:    hdisk0                   VOLUME GROUP:     rootvg", //
                "PV STATE:           active", //
                "PP SIZE:            128 megabyte(s)          LOGICAL VOLUMES:  12", //
                "TOTAL PPs:          271 (34688 megabytes)    VG DESCRIPTORS:   2"));
        assertThat(ppSize, is(128L << 20));
    }

    @Test
    void testParsePpSizeInactiveOrEmpty() {
        // A non-active PV yields 0 (no partitions to enumerate)
        assertThat(
                Lspv.parsePpSize(Arrays.asList("PV STATE:           missing", "PP SIZE:            128 megabyte(s)")),
                is(0L));
        assertThat(Lspv.parsePpSize(Collections.emptyList()), is(0L));
    }

    @Test
    void testParsePartitions() {
        List<String> lspvP = Arrays.asList(//
                "hdisk0:", //
                "PP RANGE  STATE   REGION        LV NAME             TYPE       MOUNT POINT", //
                "1-1     used    outer edge    hd5                 boot       N/A", //
                "2-55    free    outer edge", // free rows are skipped
                "56-59    used    outer middle  hd6                 paging     N/A", //
                "111-112   used    center        hd4                 jfs2       /");
        Map<String, Pair<Integer, Integer>> majMin = Collections.singletonMap("hd5", new Pair<>(10, 5));
        long ppSize = 128L << 20;
        Map<String, HWPartition> byName = Lspv.parsePartitions(lspvP, ppSize, majMin).stream()
                .collect(Collectors.toMap(HWPartition::getName, Function.identity()));

        // hd5: 1 PP, type/mount from the row, major/minor from the supplied map
        assertThat(byName.get("hd5").getType(), is("boot"));
        assertThat(byName.get("hd5").getMountPoint(), is("")); // N/A is normalized to empty
        assertThat(byName.get("hd5").getSize(), is(ppSize));
        assertThat(byName.get("hd5").getMajor(), is(10));
        assertThat(byName.get("hd5").getMinor(), is(5));
        // hd6: 4 PPs (56-59); not in the map, so major/minor fall back to the first int in the name
        assertThat(byName.get("hd6").getType(), is("paging"));
        assertThat(byName.get("hd6").getSize(), is(ppSize * 4));
        assertThat(byName.get("hd6").getMajor(), is(6));
        // hd4: 2 PPs (111-112), mounted at /
        assertThat(byName.get("hd4").getMountPoint(), is("/"));
        assertThat(byName.get("hd4").getSize(), is(ppSize * 2));
    }

    @Test
    void testParsePartitionsEmpty() {
        assertThat(Lspv.parsePartitions(Collections.emptyList(), 128L << 20, Collections.emptyMap()), is(empty()));
    }
}
