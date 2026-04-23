/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AbstractLogicalVolumeGroupTest {

    @Test
    void testGetters() {
        Map<String, Set<String>> lvMap = new HashMap<>();
        lvMap.put("lv0", Collections.singleton("/dev/sda1"));
        Set<String> pvSet = Collections.singleton("/dev/sda1");

        AbstractLogicalVolumeGroup lvg = new AbstractLogicalVolumeGroup("vg0", lvMap, pvSet) {
        };
        assertThat(lvg.getName(), is("vg0"));
        assertThat(lvg.getLogicalVolumes(), hasEntry(is("lv0"), hasItem("/dev/sda1")));
        assertThat(lvg.getPhysicalVolumes(), hasItem("/dev/sda1"));
    }

    @Test
    void testToString() {
        Map<String, Set<String>> lvMap = new HashMap<>();
        lvMap.put("lv0", Collections.singleton("/dev/sda1"));
        Set<String> pvSet = Collections.singleton("/dev/sda1");

        String s = new AbstractLogicalVolumeGroup("vg0", lvMap, pvSet) {
        }.toString();
        assertThat(s, containsString("vg0"));
        assertThat(s, containsString("PVs:"));
        assertThat(s, containsString("LV: lv0"));
        assertThat(s, containsString("/dev/sda1"));
    }

    @Test
    void testToStringEmptyMappedPVs() {
        Map<String, Set<String>> lvMap = new HashMap<>();
        lvMap.put("lv_empty", Collections.emptySet());

        String s = new AbstractLogicalVolumeGroup("vg1", lvMap, Collections.emptySet()) {
        }.toString();
        assertThat(s, containsString("LV: lv_empty"));
        // Empty mapped PVs should not produce " --> "
        assertThat(s.contains(" --> "), is(false));
    }
}
