/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.LogicalVolumeGroup;

class MacLogicalVolumeGroupTest {

    // Fixture: typical diskutil cs list output with one CoreStorage volume group
    private static final List<String> DISKUTIL_CS_OUTPUT = Arrays.asList("CoreStorage logical volume groups (1 found)",
            "|", "+-- Logical Volume Group B6D1A2C3-4E5F-6789-ABCD-EF0123456789",
            "    =========================================================", "    Name:         Macintosh HD",
            "    Status:       Online", "    Size:         499248103424 B (499.2 GB)",
            "    Free Space:   12345678 B (12.3 MB)", "    |",
            "    +-< Physical Volume 0A1B2C3D-4E5F-6789-ABCD-EF0123456789",
            "    |   ----------------------------------------------------", "    |   Index:    0",
            "    |   Disk:     disk0s2", "    |   Status:   Online", "    |   Size:     499248103424 B (499.2 GB)",
            "    |", "    +-> Logical Volume Family 1A2B3C4D-5E6F-7890-ABCD-EF0123456789",
            "        ----------------------------------------------------------",
            "        +-> Logical Volume 2A3B4C5D-6E7F-8901-ABCD-EF0123456789",
            "            ---------------------------------------------------",
            "            Disk:                  disk1", "            Status:                Online",
            "            Size (Total):          498877931520 B (498.9 GB)");

    @Test
    void testParseDiskutilCsListSingleGroup() {
        List<LogicalVolumeGroup> groups = MacLogicalVolumeGroup.parseDiskutilCsList(DISKUTIL_CS_OUTPUT);
        assertThat(groups, hasSize(1));

        LogicalVolumeGroup group = groups.get(0);
        assertThat(group.getName(), is("Macintosh HD"));
        assertThat(group.getPhysicalVolumes(), hasItem("disk0s2"));
        assertThat(group.getLogicalVolumes(), hasKey("disk1"));
    }

    @Test
    void testParseDiskutilCsListEmpty() {
        List<LogicalVolumeGroup> groups = MacLogicalVolumeGroup.parseDiskutilCsList(Collections.emptyList());
        assertThat(groups, is(empty()));
    }

    @Test
    void testParseDiskutilCsListNoGroups() {
        List<String> noGroups = Arrays.asList("No CoreStorage logical volume groups found");
        List<LogicalVolumeGroup> groups = MacLogicalVolumeGroup.parseDiskutilCsList(noGroups);
        assertThat(groups, is(empty()));
    }

    @Test
    void testParseDiskutilCsListMultipleGroups() {
        List<String> twoGroups = Arrays.asList("+-- Logical Volume Group AAA", "    Name:         Group One",
                "    +-< Physical Volume PV1", "    |   Disk:     disk0s2", "    +-> Logical Volume LV1",
                "            Disk:                  disk1", "+-- Logical Volume Group BBB",
                "    Name:         Group Two", "    +-< Physical Volume PV2", "    |   Disk:     disk2s1",
                "    +-> Logical Volume LV2", "            Disk:                  disk3");
        List<LogicalVolumeGroup> groups = MacLogicalVolumeGroup.parseDiskutilCsList(twoGroups);
        assertThat(groups, hasSize(2));
        assertThat(groups.get(0).getName(), is("Group One"));
        assertThat(groups.get(1).getName(), is("Group Two"));
        assertThat(groups.get(0).getPhysicalVolumes(), hasItem("disk0s2"));
        assertThat(groups.get(1).getPhysicalVolumes(), hasItem("disk2s1"));
    }

    @Test
    void testParseDiskutilCsListMultiplePVsAndLVs() {
        List<String> multiPvLv = Arrays.asList("+-- Logical Volume Group CCC", "    Name:         Fusion Drive",
                "    +-< Physical Volume PV1", "    |   Disk:     disk0s2", "    +-< Physical Volume PV2",
                "    |   Disk:     disk1s1", "    +-> Logical Volume LV1", "            Disk:                  disk2",
                "    +-> Logical Volume LV2", "            Disk:                  disk3");
        List<LogicalVolumeGroup> groups = MacLogicalVolumeGroup.parseDiskutilCsList(multiPvLv);
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getPhysicalVolumes(), hasItem("disk0s2"));
        assertThat(groups.get(0).getPhysicalVolumes(), hasItem("disk1s1"));
        assertThat(groups.get(0).getLogicalVolumes(), hasKey("disk2"));
        assertThat(groups.get(0).getLogicalVolumes(), hasKey("disk3"));
    }
}
