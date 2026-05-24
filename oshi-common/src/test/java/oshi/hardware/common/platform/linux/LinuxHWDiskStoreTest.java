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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.HWPartition;

@EnabledOnOs(OS.LINUX)
class LinuxHWDiskStoreTest {

    @TempDir
    private Path tempDir;

    @Test
    void testGetModelForDmDevice() {
        assertThat(LinuxHWDiskStore.getModelForDmDevice("LVM-test"), is(LinuxHWDiskStore.LOGICAL_VOLUME_GROUP));
        assertThat(LinuxHWDiskStore.getModelForDmDevice("CRYPT-LUKS2-test"), is(LinuxHWDiskStore.ENCRYPTED_VOLUME));
        assertThat(LinuxHWDiskStore.getModelForDmDevice("DMRAID-test"), is(LinuxHWDiskStore.DEVICE_MAPPER));
        assertThat(LinuxHWDiskStore.getModelForDmDevice(null), is(LinuxHWDiskStore.DEVICE_MAPPER));
    }

    @Test
    void testDeviceMapperUuidTypeChecks() {
        assertThat(LinuxHWDiskStore.isLogicalVolume("LVM-test"), is(true));
        assertThat(LinuxHWDiskStore.isLogicalVolume("CRYPT-LUKS2-test"), is(false));
        assertThat(LinuxHWDiskStore.isLogicalVolume(null), is(false));

        assertThat(LinuxHWDiskStore.isEncryptedVolume("CRYPT-LUKS2-test"), is(true));
        assertThat(LinuxHWDiskStore.isEncryptedVolume("LVM-test"), is(false));
        assertThat(LinuxHWDiskStore.isEncryptedVolume(null), is(false));
    }

    @Test
    void testGetDmDevicePath() {
        assertThat(LinuxHWDiskStore.getDmDevicePath("cryptroot", "/dev/dm-0"), is("/dev/mapper/cryptroot"));
        assertThat(LinuxHWDiskStore.getDmDevicePath(null, "/dev/dm-0"), is("/dev/dm-0"));
        assertThat(LinuxHWDiskStore.getDmDevicePath("", "/dev/dm-0"), is("/dev/dm-0"));
    }

    @Test
    void testGetMountPointForDmDevice() {
        Map<String, String> mountsMap = new HashMap<>();
        mountsMap.put("/dev/mapper/cryptroot", "/");
        mountsMap.put("/dev/dm-0", "/mnt/fallback");

        assertThat(LinuxHWDiskStore.getMountPointForDmDevice(mountsMap, "cryptroot", "/dev/dm-0", "/sys/block/dm-0"),
                is("/"));
        assertThat(LinuxHWDiskStore.getMountPointForDmDevice(mountsMap, "missing", "/dev/dm-0", "/sys/block/dm-0"),
                is("/mnt/fallback"));
    }

    @Test
    void testGetMountPointForDmDeviceFallsBackToHoldersDirectory() throws IOException {
        Map<String, String> mountsMap = new HashMap<>();
        Path sysPath = tempDir.resolve("sys/block/dm-0");
        Files.createDirectories(sysPath.resolve("holders"));
        Files.createFile(sysPath.resolve("holders/dm-1"));

        assertThat(LinuxHWDiskStore.getMountPointForDmDevice(mountsMap, "missing", "/dev/dm-0", sysPath.toString()),
                is("dm-1"));
    }

    @Test
    void testAddDeviceMapperPartitionForLogicalVolume() {
        TestLinuxHWDiskStore store = new TestLinuxHWDiskStore();

        LinuxHWDiskStore.addDeviceMapperPartition(store, new HashMap<>(), "LVM-test", "vg", "lv", null, "/dev/dm-0",
                "dm-0", "/sys/block/dm-0", "ext4", "uuid", "label", 1024L, 253, 0);

        HWPartition partition = store.getPartitions().get(0);
        assertThat(store.getPartitions().size(), is(1));
        assertThat(partition.getIdentification(), is("/dev/vg/lv"));
        assertThat(partition.getName(), is("dm-0"));
        assertThat(partition.getType(), is("ext4"));
        assertThat(partition.getUuid(), is("uuid"));
        assertThat(partition.getLabel(), is("label"));
        assertThat(partition.getSize(), is(1024L));
        assertThat(partition.getMajor(), is(253));
        assertThat(partition.getMinor(), is(0));
        assertThat(partition.getMountPoint(), is("/dev/mapper/vg-lv"));
    }

    @Test
    void testAddDeviceMapperPartitionSkipsLogicalVolumeWithoutNames() {
        TestLinuxHWDiskStore store = new TestLinuxHWDiskStore();

        LinuxHWDiskStore.addDeviceMapperPartition(store, new HashMap<>(), "LVM-test", null, "lv", null, "/dev/dm-0",
                "dm-0", "/sys/block/dm-0", null, null, null, 1024L, 253, 0);

        assertThat(store.getPartitions().isEmpty(), is(true));
    }

    @Test
    void testAddDeviceMapperPartitionForEncryptedVolumeWithMapperName() {
        TestLinuxHWDiskStore store = new TestLinuxHWDiskStore();
        Map<String, String> mountsMap = new HashMap<>();
        mountsMap.put("/dev/mapper/cryptroot", "/");

        LinuxHWDiskStore.addDeviceMapperPartition(store, mountsMap, "CRYPT-LUKS2-test", null, null, "cryptroot",
                "/dev/dm-0", "dm-0", "/sys/block/dm-0", null, null, null, 2048L, 253, 1);

        HWPartition partition = store.getPartitions().get(0);
        assertThat(store.getPartitions().size(), is(1));
        assertThat(partition.getIdentification(), is("/dev/mapper/cryptroot"));
        assertThat(partition.getType(), is("partition"));
        assertThat(partition.getUuid(), is(""));
        assertThat(partition.getLabel(), is(""));
        assertThat(partition.getMountPoint(), is("/"));
    }

    @Test
    void testAddDeviceMapperPartitionForEncryptedVolumeFallsBackToDevnode() {
        TestLinuxHWDiskStore store = new TestLinuxHWDiskStore();
        Map<String, String> mountsMap = new HashMap<>();
        mountsMap.put("/dev/dm-0", "/mnt/crypt");

        LinuxHWDiskStore.addDeviceMapperPartition(store, mountsMap, "CRYPT-LUKS2-test", null, null, null, "/dev/dm-0",
                "dm-0", "/sys/block/dm-0", "xfs", "uuid", "label", 2048L, 253, 1);

        HWPartition partition = store.getPartitions().get(0);
        assertThat(store.getPartitions().size(), is(1));
        assertThat(partition.getIdentification(), is("/dev/dm-0"));
        assertThat(partition.getType(), is("xfs"));
        assertThat(partition.getMountPoint(), is("/mnt/crypt"));
    }

    @Test
    void testAddDeviceMapperPartitionSkipsGenericDeviceMapper() {
        TestLinuxHWDiskStore store = new TestLinuxHWDiskStore();

        LinuxHWDiskStore.addDeviceMapperPartition(store, new HashMap<>(), "DMRAID-test", null, null, "dmraid",
                "/dev/dm-0", "dm-0", "/sys/block/dm-0", null, null, null, 1024L, 253, 0);

        assertThat(store.getPartitions().isEmpty(), is(true));
    }

    private static final class TestLinuxHWDiskStore extends LinuxHWDiskStore {
        private TestLinuxHWDiskStore() {
            super("/dev/dm-0", "model", "serial", 4096L, "Virtual");
        }

        @Override
        public boolean updateAttributes() {
            return false;
        }
    }
}
