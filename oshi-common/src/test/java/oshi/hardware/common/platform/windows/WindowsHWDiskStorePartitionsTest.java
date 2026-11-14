/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.windows.WindowsHWDiskStore.PartitionMaps;

class WindowsHWDiskStorePartitionsTest {

    @Test
    void testUnmountedPartitionsAreListed() {
        PartitionMaps maps = new PartitionMaps();
        String disk = "\\\\.\\PHYSICALDRIVE0";
        WindowsHWDiskStore.mapDriveToPartition(maps,
                "\\\\HOST\\root\\cimv2:Win32_DiskDrive.DeviceID=\"\\\\\\\\.\\\\PHYSICALDRIVE0\"",
                "\\\\HOST\\root\\cimv2:Win32_DiskPartition.DeviceID=\"Disk #0, Partition #0\"");
        WindowsHWDiskStore.mapDriveToPartition(maps,
                "\\\\HOST\\root\\cimv2:Win32_DiskDrive.DeviceID=\"\\\\\\\\.\\\\PHYSICALDRIVE0\"",
                "\\\\HOST\\root\\cimv2:Win32_DiskPartition.DeviceID=\"Disk #0, Partition #1\"");
        // The EFI system partition has no drive letter
        WindowsHWDiskStore.mapUnmountedPartition(maps, "Disk #0, Partition #0", "Disk #0, Partition #0", "GPT: System",
                "GPT: System", 104_857_600L, 0, 0);
        maps.getPartitionMap().computeIfAbsent("Disk #0, Partition #1", x -> new ArrayList<>())
                .add(new HWPartition("Disk #0, Partition #1", "GPT: Basic Data", "GPT: Basic Data", "uuid", "Windows",
                        255_000_000_000L, 0, 1, "C:\\"));

        List<HWPartition> partitions = WindowsHWDiskStore.buildPartitionList(maps, disk);
        // Sorted by name, which Windows fills from the partition type
        assertThat(partitions.stream().map(HWPartition::getMountPoint).toList(), contains("C:\\", ""));
        HWPartition efi = partitions.get(1);
        assertThat(efi.getSize(), is(104_857_600L));
        assertThat(efi.getUuid(), is(""));
        assertThat(efi.getMajor(), is(0));
        assertThat(efi.getMinor(), is(0));
    }
}
