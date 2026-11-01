/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
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

    private void write(String path, String content) throws IOException {
        Path file = sysBlock.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content + "\n");
    }

    private static HWDiskStore disk(List<HWDiskStore> disks, String sysfsName) {
        HWDiskStore disk = disks.stream().filter(d -> d.getName().equals(DevPath.DEV + sysfsName)).findFirst()
                .orElse(null);
        assertNotNull(disk, sysfsName);
        return disk;
    }

    /**
     * An SSD whose second partition is an LVM physical volume, the logical volume on it, a LUKS mapping, an md array,
     * and the loop and RAM disks that are not listed.
     */
    private List<HWDiskStore> layeredDisks() throws IOException {
        write("sda/size", "2048");
        write("sda/device/model", "Fixture Disk");
        write("sda/device/serial", "S123");
        write("sda/removable", "0");
        write("sda/queue/rotational", "0");
        write("sda/sda1/size", "512");
        write("sda/sda1/dev", "8:1");
        write("sda/sda2/size", "1024");
        write("sda/sda2/dev", "8:2");
        write("sda/sda2/holders/dm-0", "");
        write("dm-0/size", "1000");
        write("dm-0/dev", "253:0");
        write("dm-0/dm/name", "my--vg-root");
        write("dm-0/dm/uuid", "LVM-vguuidlvuuid");
        write("dm-1/size", "500");
        write("dm-1/dev", "253:1");
        write("dm-1/dm/name", "cryptdata");
        write("dm-1/dm/uuid", "CRYPT-LUKS2-0123-cryptdata");
        write("md0/size", "4096");
        write("loop0/size", "8");
        write("ram0/size", "8");
        Map<String, String> mounts = new HashMap<>();
        mounts.put(DevPath.DEV + "sda1", "/boot");
        mounts.put(DevPath.MAPPER + "cryptdata", "/data");
        return LinuxHWDiskStoreNF.getDisks(sysBlock.toString() + "/", mounts);
    }

    @Test
    void testListsLayeredDevicesButNotLoopOrRam() throws IOException {
        assertThat(layeredDisks().stream().map(HWDiskStore::getName).sorted().collect(Collectors.toList()),
                contains(DevPath.DEV + "dm-0", DevPath.DEV + "dm-1", DevPath.DEV + "md0", DevPath.DEV + "sda"));
    }

    @Test
    void testPhysicalDiskAndItsPartitions() throws IOException {
        HWDiskStore sda = disk(layeredDisks(), "sda");
        assertThat(sda.getModel(), is("Fixture Disk"));
        assertThat(sda.getSerial(), is("S123"));
        assertThat(sda.getDiskType(), is("SSD"));
        assertThat(sda.getSize(), is(2048L * 512));
        List<HWPartition> parts = sda.getPartitions();
        assertThat(parts.stream().map(HWPartition::getIdentification).collect(Collectors.toList()),
                contains(DevPath.DEV + "sda1", DevPath.DEV + "sda2"));
        assertThat(parts.get(0).getMountPoint(), is("/boot"));
        assertThat(parts.get(0).getMajor(), is(8));
        assertThat(parts.get(0).getMinor(), is(1));
        // An unmounted physical volume names the device built on it, as the udev backends do
        assertThat(parts.get(1).getMountPoint(), is("dm-0"));
    }

    @Test
    void testLogicalVolume() throws IOException {
        HWDiskStore lv = disk(layeredDisks(), "dm-0");
        assertThat(lv.getModel(), is("Logical Volume Group"));
        assertThat(lv.getSerial(), is("LVM-vguuidlvuuid"));
        assertThat(lv.getDiskType(), is("Virtual"));
        assertThat(lv.getPartitions().size(), is(1));
        HWPartition part = lv.getPartitions().get(0);
        assertThat(part.getIdentification(), is(DevPath.DEV + "my-vg/root"));
        assertThat(part.getName(), is("dm-0"));
        assertThat(part.getSize(), is(1000L * 512));
        assertThat(part.getMajor(), is(253));
        assertThat(part.getMinor(), is(0));
    }

    @Test
    void testEncryptedVolume() throws IOException {
        HWDiskStore luks = disk(layeredDisks(), "dm-1");
        assertThat(luks.getModel(), is("Encrypted Volume"));
        assertThat(luks.getDiskType(), is("Virtual"));
        assertThat(luks.getPartitions().size(), is(1));
        HWPartition part = luks.getPartitions().get(0);
        assertThat(part.getIdentification(), is(DevPath.MAPPER + "cryptdata"));
        assertThat(part.getMountPoint(), is("/data"));
    }

    @Test
    void testMdArrayWithoutDeviceAttributes() throws IOException {
        HWDiskStore md = disk(layeredDisks(), "md0");
        assertThat(md.getModel(), is("unknown"));
        assertThat(md.getSerial(), is("unknown"));
        assertThat(md.getDiskType(), is("Unknown"));
        assertThat(md.getPartitions(), is(empty()));
    }
}
