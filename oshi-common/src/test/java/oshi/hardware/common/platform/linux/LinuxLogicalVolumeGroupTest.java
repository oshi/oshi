/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.platform.linux.LinuxLogicalVolumeGroup.UdevBlockDevice;
import oshi.util.linux.DevPath;

// parsePhysicalVolumes dereferences DevPath.DEV, whose static initializer validates that the configured /dev path
// exists. That holds on Linux, macOS, and the BSD/illumos CI hosts, but not on Windows, where class init throws.
@DisabledOnOs(OS.WINDOWS)
class LinuxLogicalVolumeGroupTest {

    @Test
    void testParsePhysicalVolumes() {
        // pvs -o vg_name,pv_name output: header row, then "vg pv" rows (leading whitespace)
        List<String> pvs = List.of("  VG   PV", "  vg0  /dev/sda2", "  vg0  /dev/sdb1", "  vg1  /dev/sdc1");
        Map<String, Set<String>> map = LinuxLogicalVolumeGroup.parsePhysicalVolumes(pvs);
        assertThat(map.keySet(), containsInAnyOrder("vg0", "vg1"));
        assertThat(map.get("vg0"), containsInAnyOrder("/dev/sda2", "/dev/sdb1"));
        assertThat(map.get("vg1"), contains("/dev/sdc1"));
    }

    @Test
    void testParsePhysicalVolumesSkipsNonDeviceAndMalformed() {
        // A single-token line (length != 2) and a second token that is not a /dev path are both ignored
        List<String> pvs = List.of("  novg", "  vg2  notadevice", "  vg3  /dev/mapper/pv");
        Map<String, Set<String>> map = LinuxLogicalVolumeGroup.parsePhysicalVolumes(pvs);
        assertThat(map.keySet(), contains("vg3"));
        assertThat(map.get("vg3"), contains("/dev/mapper/pv"));
    }

    @Test
    void testParsePhysicalVolumesEmpty() {
        assertThat(LinuxLogicalVolumeGroup.parsePhysicalVolumes(Collections.emptyList()), anEmptyMap());
    }

    // -- volume group assembly --

    /** A concrete group so the factory has something to build. */
    private static final class TestLvg extends LinuxLogicalVolumeGroup {
        private TestLvg(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
            super(name, lvMap, pvSet);
        }
    }

    // Creates a sysfs-like directory with a "slaves" entry per physical volume, returning its path.
    private static String syspathWithSlaves(Path root, String name, String... slaves) throws IOException {
        Path dir = Files.createDirectories(root.resolve(name).resolve("slaves"));
        for (String slave : slaves) {
            Files.createFile(dir.resolve(slave));
        }
        return root.resolve(name).toString();
    }

    private static UdevBlockDevice lvmDevice(String syspath, String vgName, String lvName) {
        return new UdevBlockDevice(syspath, DevPath.DM + "0", "LVM-abcdef", vgName, lvName);
    }

    private static List<LogicalVolumeGroup> build(List<UdevBlockDevice> devices) {
        return LinuxLogicalVolumeGroup.buildLogicalVolumeGroups(devices, new HashMap<>(), TestLvg::new);
    }

    @Test
    void testAssemblesGroupFromSlavesDirectory(@TempDir Path tmp) throws IOException {
        String syspath = syspathWithSlaves(tmp, "dm-0", "sda2", "sdb1");
        List<LogicalVolumeGroup> lvgs = build(List.of(lvmDevice(syspath, "vg0", "root")));

        assertThat(lvgs, hasSize(1));
        assertThat(lvgs.get(0).getName(), is("vg0"));
        assertThat("Each slaves entry becomes a /dev physical volume", lvgs.get(0).getPhysicalVolumes(),
                containsInAnyOrder("/dev/sda2", "/dev/sdb1"));
        assertThat(lvgs.get(0).getLogicalVolumes().get("root"), containsInAnyOrder("/dev/sda2", "/dev/sdb1"));
    }

    @Test
    void testTwoLogicalVolumesShareOneGroup(@TempDir Path tmp) throws IOException {
        List<UdevBlockDevice> devices = List.of(lvmDevice(syspathWithSlaves(tmp, "dm-0", "sda2"), "vg0", "root"),
                lvmDevice(syspathWithSlaves(tmp, "dm-1", "sdb1"), "vg0", "swap"));
        List<LogicalVolumeGroup> lvgs = build(devices);

        assertThat("Both volumes belong to one group", lvgs, hasSize(1));
        assertThat(lvgs.get(0).getLogicalVolumes().keySet(), containsInAnyOrder("root", "swap"));
        assertThat(lvgs.get(0).getLogicalVolumes().get("root"), contains("/dev/sda2"));
        assertThat(lvgs.get(0).getLogicalVolumes().get("swap"), contains("/dev/sdb1"));
        assertThat("The group's physical volumes are the union", lvgs.get(0).getPhysicalVolumes(),
                containsInAnyOrder("/dev/sda2", "/dev/sdb1"));
    }

    @Test
    void testNonLvmDevicesAreFiltered(@TempDir Path tmp) throws IOException {
        String syspath = syspathWithSlaves(tmp, "dev", "sda1");
        List<UdevBlockDevice> devices = List.of(
                // not a device-mapper node
                new UdevBlockDevice(syspath, "/dev/sda", "LVM-abcdef", "vg0", "root"),
                // device-mapper, but owned by something other than LVM, e.g. LUKS
                new UdevBlockDevice(syspath, DevPath.DM + "0", "CRYPT-LUKS2-abcdef", "vg0", "root"),
                // device-mapper LVM, but udev reported no volume group or volume name
                new UdevBlockDevice(syspath, DevPath.DM + "1", "LVM-abcdef", null, "root"),
                new UdevBlockDevice(syspath, DevPath.DM + "2", "LVM-abcdef", "vg0", ""),
                // udev reported no properties at all
                new UdevBlockDevice(syspath, null, null, null, null));
        assertThat("Nothing here is an LVM volume", build(devices), is(empty()));
    }

    @Test
    void testMissingSlavesDirectoryStillReportsTheGroup(@TempDir Path tmp) {
        // A volume whose sysfs entry has no slaves directory is still a volume group, just with no physical volumes.
        List<LogicalVolumeGroup> lvgs = build(List.of(lvmDevice(tmp.resolve("absent").toString(), "vg0", "root")));
        assertThat(lvgs, hasSize(1));
        assertThat(lvgs.get(0).getName(), is("vg0"));
        assertThat(lvgs.get(0).getPhysicalVolumes(), is(empty()));
        assertThat(lvgs.get(0).getLogicalVolumes(), anEmptyMap());
    }

    @Test
    void testPhysicalVolumesFromPvsAreMergedWithSlaves(@TempDir Path tmp) throws IOException {
        // pvs already reported a volume for this group; the slaves scan adds to that set rather than replacing it.
        Map<String, Set<String>> fromPvs = new HashMap<>();
        fromPvs.put("vg0", new HashSet<>(List.of("/dev/sdc1")));
        List<LogicalVolumeGroup> lvgs = LinuxLogicalVolumeGroup.buildLogicalVolumeGroups(
                List.of(lvmDevice(syspathWithSlaves(tmp, "dm-0", "sda2"), "vg0", "root")), fromPvs, TestLvg::new);

        assertThat(lvgs, hasSize(1));
        assertThat(lvgs.get(0).getPhysicalVolumes(), containsInAnyOrder("/dev/sdc1", "/dev/sda2"));
    }

    @Test
    void testNoDevicesYieldsNoGroups() {
        assertThat(build(Collections.emptyList()), is(empty()));
    }
}
