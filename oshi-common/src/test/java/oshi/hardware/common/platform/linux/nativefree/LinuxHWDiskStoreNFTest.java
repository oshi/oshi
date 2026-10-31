/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.DevPath;
import oshi.util.linux.ProcPath;

/**
 * Reads disk statistics from a sysfs block tree built in a temporary directory. Linux only, because loading the disk
 * store requires {@code /proc}.
 */
@EnabledOnOs(OS.LINUX)
class LinuxHWDiskStoreNFTest {

    @TempDir
    private Path sysBlock;

    /**
     * Writes a {@code stat} file with the given leading fields, padded with zeros to the field count of this kernel's
     * {@code /proc/diskstats}, which is the count the parser expects, or to the 11 fields it assumes without one.
     */
    private void writeStat(String sysfsName, long... fields) throws IOException {
        String diskstats = FileUtil.getStringFromFile(ProcPath.DISKSTATS);
        int length = diskstats.isEmpty() ? 11 : ParseUtil.countStringToLongArray(diskstats, ' ');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(length, fields.length); i++) {
            sb.append(' ').append(i < fields.length ? fields[i] : 0L);
        }
        Path dir = Files.createDirectories(sysBlock.resolve(sysfsName));
        Files.writeString(dir.resolve("stat"), sb.append('\n').toString());
    }

    @Test
    void testUpdateAttributesReadsStat() throws IOException {
        // reads, merged, sectors read, ms reading, writes, merged, sectors written, ms writing, in flight, ms active
        writeStat("sda", 100, 1, 2000, 30, 50, 2, 4000, 60, 3, 700);
        LinuxHWDiskStoreNF disk = new LinuxHWDiskStoreNF(DevPath.DEV + "sda", "model", "serial", 1L, "SSD");
        assertThat(disk.updateAttributes(sysBlock.toString() + "/"), is(true));
        assertThat(disk.getReads(), is(100L));
        assertThat(disk.getReadBytes(), is(2000L * 512));
        assertThat(disk.getWrites(), is(50L));
        assertThat(disk.getWriteBytes(), is(4000L * 512));
        assertThat(disk.getCurrentQueueLength(), is(3L));
        assertThat(disk.getTransferTime(), is(700L));
        assertThat(disk.getTimeStamp(), is(greaterThan(0L)));
    }

    @Test
    void testUpdateAttributesEncodesSlashInName() throws IOException {
        writeStat("cciss!c0d0", 7);
        LinuxHWDiskStoreNF disk = new LinuxHWDiskStoreNF(DevPath.DEV + "cciss/c0d0", "model", "serial", 1L, "HDD");
        assertThat(disk.updateAttributes(sysBlock.toString() + "/"), is(true));
        assertThat(disk.getReads(), is(7L));
    }

    @Test
    void testUpdateAttributesUsesSysPath() {
        // A disk with no sysfs entry exercises the public method on any host, including one with no disks
        LinuxHWDiskStoreNF missing = new LinuxHWDiskStoreNF(DevPath.DEV + "oshi-no-such-disk", "model", "serial", 1L,
                "SSD");
        assertThat(missing.updateAttributes(), is(false));
    }

    @Test
    void testUpdateAttributesMissingDisk() {
        LinuxHWDiskStoreNF disk = new LinuxHWDiskStoreNF(DevPath.DEV + "nvme0n1", "model", "serial", 1L, "SSD");
        assertThat(disk.updateAttributes(sysBlock.toString() + "/"), is(false));
        assertThat(disk.getTimeStamp(), is(0L));
    }
}
