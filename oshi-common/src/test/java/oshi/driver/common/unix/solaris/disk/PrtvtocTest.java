/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWPartition;
import oshi.util.Constants;

class PrtvtocTest {

    @Test
    void testParsePrtvtocTypicalOutput() {
        // Typical prtvtoc output for a Solaris disk
        List<String> prtvtoc = Arrays.asList("* /dev/dsk/c1t0d0s0 partition map", "*", "* Dimensions:",
                "*     512 bytes/sector", "*      63 sectors/track", "*      16 tracks/cylinder",
                "*    1008 sectors/cylinder", "*   16384 cylinders", "*   16382 accessible cylinders", "*", "* Flags:",
                "*   1: unmountable", "*  10: read-only", "*", "* Volume Name: MyVolume", "*",
                "*          First     Sector    Last",
                "* Partition  Tag  Flags  Sector     Count    Sector  Mount Directory",
                "       0      2    00       1008   14329440   14330447   /",
                "       1      3    01    14330448    2097648   16428095",
                "       2      5    00           0   16515072   16515071",
                "       3      7    00    16428096      86976   16515071   /var");

        List<HWPartition> result = Prtvtoc.parsePrtvtoc(prtvtoc, "c1t0d0", 102);

        // Partition 2 (backup/whole disk) is always skipped
        assertThat(result.size(), is(3));

        // Partition 0: tag=2 (root), flags=00 (wm)
        HWPartition p0 = result.get(0);
        assertThat(p0.getIdentification(), is("c1t0d0s0"));
        assertThat(p0.getName(), is("root"));
        assertThat(p0.getType(), is("wm"));
        assertThat(p0.getSize(), is(512L * 14329440L));
        assertThat(p0.getMajor(), is(102));
        assertThat(p0.getMinor(), is(0));
        assertThat(p0.getMountPoint(), is("/"));
        assertThat(p0.getLabel(), is("MyVolume"));

        // Partition 1: tag=3 (swap), flags=01 (wu)
        HWPartition p1 = result.get(1);
        assertThat(p1.getIdentification(), is("c1t0d0s1"));
        assertThat(p1.getName(), is("swap"));
        assertThat(p1.getType(), is("wu"));
        assertThat(p1.getSize(), is(512L * 2097648L));
        assertThat(p1.getMajor(), is(102));
        assertThat(p1.getMinor(), is(1));
        assertThat(p1.getMountPoint(), is(""));

        // Partition 3: tag=7 (var), flags=00 (wm)
        HWPartition p3 = result.get(2);
        assertThat(p3.getIdentification(), is("c1t0d0s3"));
        assertThat(p3.getName(), is("var"));
        assertThat(p3.getType(), is("wm"));
        assertThat(p3.getSize(), is(512L * 86976L));
        assertThat(p3.getMajor(), is(102));
        assertThat(p3.getMinor(), is(3));
        assertThat(p3.getMountPoint(), is("/var"));
    }

    @Test
    void testParsePrtvtocTagMappings() {
        // Test various tag values to verify name mappings
        List<String> prtvtoc = Arrays.asList("* header line", "*     512 bytes/sector",
                "       0      1    00       0   1000    999", "       1     24    00       0   1000    999",
                "       3      4    10       0   1000    999", "       4      6    00       0   1000    999",
                "       5      8    00       0   1000    999", "       6      9    00       0   1000    999",
                "       7     10    00       0   1000    999", "       8     11    00       0   1000    999",
                "       9     12    00       0   1000    999", "      10     14    00       0   1000    999",
                "      11     15    00       0   1000    999", "      12     99    00       0   1000    999");

        List<HWPartition> result = Prtvtoc.parsePrtvtoc(prtvtoc, "c0d0", 10);

        assertThat(result.size(), is(12));
        // tag 0x01 -> boot
        assertThat(result.get(0).getName(), is("boot"));
        // tag 0x18 (24) -> boot
        assertThat(result.get(1).getName(), is("boot"));
        // tag 0x04 -> usr
        assertThat(result.get(2).getName(), is("usr"));
        // tag 0x06 -> stand
        assertThat(result.get(3).getName(), is("stand"));
        // tag 0x08 -> home
        assertThat(result.get(4).getName(), is("home"));
        // tag 0x09 -> altsctr
        assertThat(result.get(5).getName(), is("altsctr"));
        // tag 0x0a (10) -> cache
        assertThat(result.get(6).getName(), is("cache"));
        // tag 0x0b (11) -> reserved
        assertThat(result.get(7).getName(), is("reserved"));
        // tag 0x0c (12) -> system
        assertThat(result.get(8).getName(), is("system"));
        // tag 0x0e (14) -> public region
        assertThat(result.get(9).getName(), is("public region"));
        // tag 0x0f (15) -> private region
        assertThat(result.get(10).getName(), is("private region"));
        // tag 99 -> unknown
        assertThat(result.get(11).getName(), is(Constants.UNKNOWN));
    }

    @Test
    void testParsePrtvtocFlagMappings() {
        // Test flag values: 00=wm, 10=rm, 01=wu, other=ru
        List<String> prtvtoc = Arrays.asList("* header", "*     512 bytes/sector",
                "       0      2    00       0   1000    999", "       1      2    10       0   1000    999",
                "       3      2    01       0   1000    999", "       4      2    11       0   1000    999");

        List<HWPartition> result = Prtvtoc.parsePrtvtoc(prtvtoc, "c0d0", 5);

        assertThat(result.size(), is(4));
        assertThat(result.get(0).getType(), is("wm"));
        assertThat(result.get(1).getType(), is("rm"));
        assertThat(result.get(2).getType(), is("wu"));
        assertThat(result.get(3).getType(), is("ru"));
    }

    @Test
    void testParsePrtvtocEmptyInput() {
        List<HWPartition> result = Prtvtoc.parsePrtvtoc(Collections.emptyList(), "c0d0", 10);
        assertThat(result, is(empty()));
    }

    @Test
    void testParsePrtvtocSingleLineInput() {
        // size <= 1 returns empty
        List<HWPartition> result = Prtvtoc.parsePrtvtoc(Collections.singletonList("* header"), "c0d0", 10);
        assertThat(result, is(empty()));
    }

    @Test
    void testParsePrtvtocNoBytesPerSectorIgnoresPartitions() {
        // If bytes/sector is never set, partition lines are ignored
        List<String> prtvtoc = Arrays.asList("* header line without bytes/sector info", "* another comment",
                "       0      2    00       0   1000    999");

        List<HWPartition> result = Prtvtoc.parsePrtvtoc(prtvtoc, "c0d0", 10);
        assertThat(result, is(empty()));
    }

    @Test
    void testParsePrtvtocVolumeNameUsedAsLabel() {
        List<String> prtvtoc = Arrays.asList("* header", "*     512 bytes/sector", "* Volume Name: TestLabel",
                "       0      2    00       0   2048    2047   /");

        List<HWPartition> result = Prtvtoc.parsePrtvtoc(prtvtoc, "c0d0", 5);

        assertThat(result.size(), is(1));
        // Volume name goes into the uuid/label field
        assertThat(result.get(0).getLabel(), is("TestLabel"));
    }
}
