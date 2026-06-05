/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.bsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * Tests for the shared BSD {@link Disklabel} parser. The {@code parseDiskParams} and {@code parseDfFallback} methods
 * are exercised with sample command output so they can be covered on any OS; live-system smoke tests are gated to
 * OpenBSD/NetBSD.
 */
class DisklabelTest {

    /** Returns deterministic (major,minor) without calling stat(1). */
    private static final BiFunction<String, String, Pair<Integer, Integer>> ZERO_MAJOR_MINOR = (d, n) -> new Pair<>(0,
            0);

    @Test
    void testParseDiskParamsExtractsHeaderAndPartitions() {
        // Representative output of `disklabel -n sd0` on OpenBSD/NetBSD.
        List<String> disklabelOut = Arrays.asList("# /dev/rsd0c:", "type: SCSI", "disk: SCSI disk",
                "label: Storage Device", "duid: 0000000000000000", "flags:", "bytes/sector: 512", "sectors/track: 63",
                "tracks/cylinder: 255", "sectors/cylinder: 16065", "cylinders: 976", "total sectors: 15693824",
                "boundstart: 0", "boundend: 15693824", "drivedata: 0", "", "16 partitions:",
                "#                size           offset  fstype [fsize bsize   cpg]",
                "  a:          2097152             1024  4.2BSD   2048 16384 12958 # /",
                "  b:         17023368          2098176    swap                    # none",
                "  c:        500118192                0  unused", "  i:              960               64   MSDOS");

        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDiskParams("sd0", disklabelOut,
                ZERO_MAJOR_MINOR);

        assertThat("label", result.getA(), is("Storage Device"));
        assertThat("duid", result.getB(), is("0000000000000000"));
        // total sectors x bytes/sector
        assertThat("size in bytes", result.getC(), is(15693824L * 512L));
        // Only `a` and `b` keep enough columns to be considered partitions. The single-letter rows for `c` (unused) and
        // `i` (MSDOS) have <=4 whitespace fields and are deliberately filtered out by `split.length > 4`.
        assertThat(result.getD(), hasSize(2));

        HWPartition root = result.getD().get(0);
        assertThat(root.getIdentification(), is("sd0a"));
        assertThat(root.getName(), is("a"));
        assertThat(root.getType(), is("4.2BSD"));
        assertThat(root.getUuid(), is("0000000000000000.a"));
        assertThat(root.getSize(), is(2097152L * 512L));
        assertThat(root.getMountPoint(), is("/"));

        HWPartition swap = result.getD().get(1);
        assertThat(swap.getName(), is("b"));
        assertThat(swap.getType(), is("swap"));
        assertThat(swap.getMountPoint(), is("none"));
    }

    @Test
    void testParseDiskParamsLargeDiskWithoutIntOverflow() {
        // 5 TiB disk at 512-byte sectors = 10737418240 sectors, > Integer.MAX_VALUE.
        long sectors = 10_737_418_240L;
        List<String> disklabelOut = Arrays.asList("label: Big Disk", "duid: deadbeefdeadbeef", "bytes/sector: 512",
                "total sectors: " + sectors);

        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDiskParams("sd1", disklabelOut,
                ZERO_MAJOR_MINOR);

        assertThat("size must not overflow", result.getC(), is(sectors * 512L));
    }

    @Test
    void testParseDiskParamsEmptyInputReturnsDefaultsAndEmptyPartitions() {
        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDiskParams("sd0",
                Collections.emptyList(), ZERO_MAJOR_MINOR);

        assertThat(result.getA(), is(""));
        assertThat(result.getB(), is(""));
        assertThat(result.getC(), is(1L)); // default 1 sector x 1 byte
        assertThat(result.getD(), is(empty()));
    }

    @Test
    void testParseDiskParamsUsesMajorMinorLookup() {
        List<String> disklabelOut = Arrays.asList("label: Test", "duid: aaaa", "bytes/sector: 512",
                "total sectors: 100", "  a:  100  0  4.2BSD  2048  16384 12958 # /");
        BiFunction<String, String, Pair<Integer, Integer>> lookup = (disk, name) -> new Pair<>(7, 42);

        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDiskParams("sd0", disklabelOut,
                lookup);

        assertThat(result.getD(), hasSize(1));
        assertThat(result.getD().get(0).getMajor(), is(7));
        assertThat(result.getD().get(0).getMinor(), is(42));
    }

    @Test
    void testParseDfFallbackBuildsUnknownLabeledQuartet() {
        // `df` columns: device 1k-blocks used avail capacity mounted
        List<String> dfOut = Arrays.asList("Filesystem  1K-blocks    Used   Avail Capacity  Mounted on",
                "/dev/sd0a     1024000  500000  524000     49%  /",
                "/dev/sd0d     2048000 1000000 1048000     49%  /var",
                "tmpfs           65536       0   65536      0%  /tmp");

        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDfFallback("sd0", dfOut,
                ZERO_MAJOR_MINOR);

        assertThat(result.getA(), is(Constants.UNKNOWN));
        assertThat(result.getB(), is(Constants.UNKNOWN));
        assertThat(result.getC(), is(0L));
        assertThat(result.getD(), hasSize(2));
        HWPartition rootPart = result.getD().get(0);
        // df fallback names use the suffix after "/dev/" (e.g. "sd0a"), not just the partition letter.
        assertThat(rootPart.getName(), is("sd0a"));
        assertThat(rootPart.getMountPoint(), is("/"));
        // 1024000 1K blocks x 512 (df fallback assumes 512-byte units)
        assertThat(rootPart.getSize(), is(1024000L * 512L));
    }

    @Test
    void testParseDfFallbackIgnoresOtherDisks() {
        List<String> dfOut = Collections.singletonList("/dev/sd1a   1000   500   500   50%  /home");
        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDfFallback("sd0", dfOut,
                ZERO_MAJOR_MINOR);
        assertThat(result.getD(), is(empty()));
    }

    @Test
    void testParseDiskParamsRowWithoutFstypeIsSkipped() {
        // A partition row with fewer than 5 columns (no fstype) is discarded by the parser.
        List<String> disklabelOut = Arrays.asList("label: X", "duid: x", "bytes/sector: 512", "total sectors: 10",
                "  a:  10  0  ");
        Quartet<String, String, Long, List<HWPartition>> result = Disklabel.parseDiskParams("sd0", disklabelOut,
                ZERO_MAJOR_MINOR);
        assertThat(result.getD(), is(empty()));
    }

    // ---- Live-system smoke tests gated to OpenBSD and NetBSD ----

    @Test
    @EnabledOnOs(OS.OPENBSD)
    void testGetDiskParamsLiveOpenBsd() {
        String disknames = ExecutingCommand.getFirstAnswer("sysctl -n hw.disknames");
        String[] devices = disknames.isEmpty() ? new String[0] : disknames.split(",");
        boolean elevated = "0".equals(ExecutingCommand.getFirstAnswer("id -u"));
        for (String device : devices) {
            String diskName = device.split(":")[0];
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            if (elevated) {
                assertThat("Disk label is not null", diskdata.getA(), is(not(nullValue())));
                assertThat("Disk duid is not null", diskdata.getB(), is(not(nullValue())));
                assertThat("Disk size is nonnegative", diskdata.getC(), is(greaterThanOrEqualTo(0L)));
                for (HWPartition part : diskdata.getD()) {
                    assertTrue(part.getIdentification().startsWith(diskName), "Partition ID starts with disk");
                }
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
    void testGetDiskParamsLiveNetBsd() {
        String disknames = ExecutingCommand.getFirstAnswer("sysctl -n hw.disknames");
        String[] devices = disknames.isEmpty() ? new String[0] : disknames.trim().split("\\s+");
        boolean elevated = "0".equals(ExecutingCommand.getFirstAnswer("id -u"));
        for (String device : devices) {
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(device);
            if (elevated) {
                assertThat("Disk label is not null", diskdata.getA(), is(not(nullValue())));
                assertThat("Disk duid is not null", diskdata.getB(), is(not(nullValue())));
                assertThat("Disk size is nonnegative", diskdata.getC(), is(greaterThanOrEqualTo(0L)));
            }
        }
    }
}
