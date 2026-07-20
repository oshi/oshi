/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class LinuxLogicalVolumeGroupTest {

    @Test
    void testParsePhysicalVolumes() {
        // pvs -o vg_name,pv_name output: header row, then "vg pv" rows (leading whitespace)
        List<String> pvs = Arrays.asList("  VG   PV", "  vg0  /dev/sda2", "  vg0  /dev/sdb1", "  vg1  /dev/sdc1");
        Map<String, Set<String>> map = LinuxLogicalVolumeGroup.parsePhysicalVolumes(pvs);
        assertThat(map.keySet(), containsInAnyOrder("vg0", "vg1"));
        assertThat(map.get("vg0"), containsInAnyOrder("/dev/sda2", "/dev/sdb1"));
        assertThat(map.get("vg1"), contains("/dev/sdc1"));
    }

    @Test
    void testParsePhysicalVolumesSkipsNonDeviceAndMalformed() {
        // A single-token line (length != 2) and a second token that is not a /dev path are both ignored
        List<String> pvs = Arrays.asList("  novg", "  vg2  notadevice", "  vg3  /dev/mapper/pv");
        Map<String, Set<String>> map = LinuxLogicalVolumeGroup.parsePhysicalVolumes(pvs);
        assertThat(map.keySet(), contains("vg3"));
        assertThat(map.get("vg3"), contains("/dev/mapper/pv"));
    }

    @Test
    void testParsePhysicalVolumesEmpty() {
        assertThat(LinuxLogicalVolumeGroup.parsePhysicalVolumes(Collections.emptyList()), anEmptyMap());
    }
}
