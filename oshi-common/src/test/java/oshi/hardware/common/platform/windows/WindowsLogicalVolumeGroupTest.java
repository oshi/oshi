/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_REFERENCE;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_STRING;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_BSTR;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.hardware.LogicalVolumeGroup;

/**
 * Tests the shared Storage Spaces correlation without Windows, by stubbing the four WMI results. Storage Spaces needs
 * pooled disks to be configured, so this path is otherwise hard to reach even on real hardware.
 */
class WindowsLogicalVolumeGroupTest {

    private static final String SP_GUID = "{11111111-1111-1111-1111-111111111111}";
    private static final String OTHER_SP_GUID = "{99999999-9999-9999-9999-999999999999}";
    private static final String PD1_GUID = "{aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa}";
    private static final String PD2_GUID = "{bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb}";
    private static final String VD_GUID = "{cccccccc-cccc-cccc-cccc-cccccccccccc}";

    // Builds the ObjectId form the production regexes expect.
    private static String objectId(String kind, String... guids) {
        StringBuilder sb = new StringBuilder("{1}\\\\HOST\\root/Microsoft/Windows/Storage/Providers_v2\\SPACES_");
        sb.append(kind).append(":ObjectId=").append(kind).append(':');
        for (String guid : guids) {
            sb.append(guid);
        }
        return sb.toString();
    }

    // A minimal single-column-per-property WmiResult stub.
    private static <T extends Enum<T>> WmiResult<T> result(List<Map<T, Object>> rows, int cimType) {
        return new WmiResult<T>() {
            @Override
            public int getResultCount() {
                return rows.size();
            }

            @Override
            public @Nullable Object getValue(T property, int index) {
                return rows.get(index).get(property);
            }

            @Override
            public int getVtType(T property) {
                return VT_BSTR;
            }

            @Override
            public int getCIMType(T property) {
                return cimType;
            }
        };
    }

    // The varargs of these helpers are fixed-size groups, so each loop is bounded on the last index it reads rather
    // than on the first: a caller that passes a partial group gets one fewer row instead of running off the end.
    private static WmiResult<StoragePoolProperty> pools(String... namesAndIds) {
        List<Map<StoragePoolProperty, Object>> rows = new ArrayList<>();
        for (int i = 0; i + 1 < namesAndIds.length; i += 2) {
            Map<StoragePoolProperty, Object> row = new EnumMap<>(StoragePoolProperty.class);
            row.put(StoragePoolProperty.FRIENDLYNAME, namesAndIds[i]);
            row.put(StoragePoolProperty.OBJECTID, objectId("SP", namesAndIds[i + 1]));
            rows.add(row);
        }
        return result(rows, CIM_STRING);
    }

    private static WmiResult<VirtualDiskProperty> virtualDisks(String... namesSpAndVd) {
        List<Map<VirtualDiskProperty, Object>> rows = new ArrayList<>();
        for (int i = 0; i + 2 < namesSpAndVd.length; i += 3) {
            Map<VirtualDiskProperty, Object> row = new EnumMap<>(VirtualDiskProperty.class);
            row.put(VirtualDiskProperty.FRIENDLYNAME, namesSpAndVd[i]);
            // A VD ObjectId carries the storage pool GUID followed by the virtual disk GUID
            row.put(VirtualDiskProperty.OBJECTID, objectId("VD", namesSpAndVd[i + 1], namesSpAndVd[i + 2]));
            rows.add(row);
        }
        return result(rows, CIM_STRING);
    }

    private static WmiResult<PhysicalDiskProperty> physicalDisks(String... nameLocationAndId) {
        List<Map<PhysicalDiskProperty, Object>> rows = new ArrayList<>();
        for (int i = 0; i + 2 < nameLocationAndId.length; i += 3) {
            Map<PhysicalDiskProperty, Object> row = new EnumMap<>(PhysicalDiskProperty.class);
            row.put(PhysicalDiskProperty.FRIENDLYNAME, nameLocationAndId[i]);
            row.put(PhysicalDiskProperty.PHYSICALLOCATION, nameLocationAndId[i + 1]);
            row.put(PhysicalDiskProperty.OBJECTID, objectId("PD", nameLocationAndId[i + 2]));
            rows.add(row);
        }
        return result(rows, CIM_STRING);
    }

    private static WmiResult<StoragePoolToPhysicalDiskProperty> poolToDisk(String... spAndPdGuids) {
        List<Map<StoragePoolToPhysicalDiskProperty, Object>> rows = new ArrayList<>();
        for (int i = 0; i + 1 < spAndPdGuids.length; i += 2) {
            Map<StoragePoolToPhysicalDiskProperty, Object> row = new EnumMap<>(StoragePoolToPhysicalDiskProperty.class);
            row.put(StoragePoolToPhysicalDiskProperty.STORAGEPOOL, objectId("SP", spAndPdGuids[i]));
            row.put(StoragePoolToPhysicalDiskProperty.PHYSICALDISK, objectId("PD", spAndPdGuids[i + 1]));
            rows.add(row);
        }
        // Association properties come back as CIM references, not plain strings
        return result(rows, CIM_REFERENCE);
    }

    private static List<LogicalVolumeGroup> build(WmiResult<StoragePoolProperty> sp, WmiResult<VirtualDiskProperty> vds,
            WmiResult<PhysicalDiskProperty> pds, WmiResult<StoragePoolToPhysicalDiskProperty> sppd) {
        return WindowsLogicalVolumeGroup.buildFromWmi(sp, vds, pds, sppd, TestLvg::new);
    }

    private static final class TestLvg extends WindowsLogicalVolumeGroup {
        private TestLvg(String name, Map<String, java.util.Set<String>> lvMap, java.util.Set<String> pvSet) {
            super(name, lvMap, pvSet);
        }
    }

    @Test
    void testCorrelatesPoolWithItsDisksAndVolumes() {
        List<LogicalVolumeGroup> lvgs = build(pools("Pool1", SP_GUID), virtualDisks("Volume1", SP_GUID, VD_GUID),
                physicalDisks("Disk1", "PCISlot1", PD1_GUID, "Disk2", "PCISlot2", PD2_GUID),
                poolToDisk(SP_GUID, PD1_GUID, SP_GUID, PD2_GUID));

        assertThat(lvgs, hasSize(1));
        LogicalVolumeGroup lvg = lvgs.get(0);
        assertThat(lvg.getName(), is("Pool1"));
        assertThat("Both pooled disks are physical volumes, named and located", lvg.getPhysicalVolumes(),
                containsInAnyOrder("Disk1 @ PCISlot1", "Disk2 @ PCISlot2"));
        assertThat(lvg.getLogicalVolumes().keySet(), contains("Volume1 " + VD_GUID));
        assertThat("A logical volume maps to the pool's physical volumes",
                lvg.getLogicalVolumes().get("Volume1 " + VD_GUID),
                containsInAnyOrder("Disk1 @ PCISlot1", "Disk2 @ PCISlot2"));
    }

    @Test
    void testDisksAndVolumesOfAnotherPoolAreNotMixedIn() {
        // The regression this correlation exists to prevent: object IDs are matched by substring, so a second pool
        // must not pick up the first pool's members.
        List<LogicalVolumeGroup> lvgs = build(pools("Pool1", SP_GUID, "Pool2", OTHER_SP_GUID),
                virtualDisks("Volume1", SP_GUID, VD_GUID),
                physicalDisks("Disk1", "PCISlot1", PD1_GUID, "Disk2", "PCISlot2", PD2_GUID),
                poolToDisk(SP_GUID, PD1_GUID, OTHER_SP_GUID, PD2_GUID));

        assertThat(lvgs, hasSize(2));
        LogicalVolumeGroup pool1 = lvgs.get(0);
        LogicalVolumeGroup pool2 = lvgs.get(1);
        assertThat(pool1.getName(), is("Pool1"));
        assertThat(pool1.getPhysicalVolumes(), contains("Disk1 @ PCISlot1"));
        assertThat(pool1.getLogicalVolumes().keySet(), contains("Volume1 " + VD_GUID));
        assertThat(pool2.getName(), is("Pool2"));
        assertThat(pool2.getPhysicalVolumes(), contains("Disk2 @ PCISlot2"));
        assertThat("Pool2 owns no virtual disk", pool2.getLogicalVolumes().keySet(), is(empty()));
    }

    @Test
    void testUnknownPhysicalDiskIsSkippedNotNulled() {
        // The pool references a disk absent from the physical disk query; it must be dropped rather than producing a
        // "null @ null" entry.
        List<LogicalVolumeGroup> lvgs = build(pools("Pool1", SP_GUID), virtualDisks(),
                physicalDisks("Disk1", "PCISlot1", PD1_GUID), poolToDisk(SP_GUID, PD1_GUID, SP_GUID, PD2_GUID));

        assertThat(lvgs, hasSize(1));
        assertThat(lvgs.get(0).getPhysicalVolumes(), contains("Disk1 @ PCISlot1"));
    }

    @Test
    void testNoPoolsYieldsNoGroups() {
        assertThat(build(pools(), virtualDisks(), physicalDisks(), poolToDisk()), is(empty()));
    }

    @Test
    void testUnparseableObjectIdsDoNotThrow() {
        // A future or unexpected ObjectId format must degrade to no correlation rather than an exception.
        List<Map<StoragePoolProperty, Object>> rows = new ArrayList<>();
        Map<StoragePoolProperty, Object> row = new EnumMap<>(StoragePoolProperty.class);
        row.put(StoragePoolProperty.FRIENDLYNAME, "Pool1");
        row.put(StoragePoolProperty.OBJECTID, "not-an-object-id");
        rows.add(row);

        List<LogicalVolumeGroup> lvgs = build(result(rows, CIM_STRING), virtualDisks("Volume1", SP_GUID, VD_GUID),
                physicalDisks("Disk1", "PCISlot1", PD1_GUID), poolToDisk(SP_GUID, PD1_GUID));
        assertThat(lvgs, hasSize(1));
        assertThat(lvgs.get(0).getName(), is("Pool1"));
        assertThat("An unparseable pool ID must correlate nothing, not everything", lvgs.get(0).getPhysicalVolumes(),
                is(empty()));
        assertThat(lvgs.get(0).getLogicalVolumes(), is(anEmptyMap()));
    }

    @Test
    void testMissingObjectIdCorrelatesNothing() {
        // WmiUtil maps an absent property to "", and every string contains "", so an unguarded substring match would
        // make a pool with no ObjectId absorb every physical and virtual disk on the system.
        List<Map<StoragePoolProperty, Object>> rows = new ArrayList<>();
        Map<StoragePoolProperty, Object> row = new EnumMap<>(StoragePoolProperty.class);
        row.put(StoragePoolProperty.FRIENDLYNAME, "Pool1");
        row.put(StoragePoolProperty.OBJECTID, null);
        rows.add(row);

        List<LogicalVolumeGroup> lvgs = build(result(rows, CIM_STRING), virtualDisks("Volume1", SP_GUID, VD_GUID),
                physicalDisks("Disk1", "PCISlot1", PD1_GUID), poolToDisk(SP_GUID, PD1_GUID));
        assertThat(lvgs, hasSize(1));
        assertThat("A pool with no ObjectId must not claim every disk", lvgs.get(0).getPhysicalVolumes(), is(empty()));
        assertThat("A pool with no ObjectId must not claim every volume", lvgs.get(0).getLogicalVolumes(),
                is(anEmptyMap()));
    }

    @Test
    void testMultipleVolumesInOnePool() {
        String vd2 = "{dddddddd-dddd-dddd-dddd-dddddddddddd}";
        List<LogicalVolumeGroup> lvgs = build(pools("Pool1", SP_GUID),
                virtualDisks("Volume1", SP_GUID, VD_GUID, "Volume2", SP_GUID, vd2),
                physicalDisks("Disk1", "PCISlot1", PD1_GUID), poolToDisk(SP_GUID, PD1_GUID));

        assertThat(lvgs, hasSize(1));
        assertThat(lvgs.get(0).getLogicalVolumes().keySet(),
                containsInAnyOrder("Volume1 " + VD_GUID, "Volume2 " + vd2));
    }

    @Test
    void testObjectIdFixtureMatchesTheProductionRegexes() {
        // Guards the fixture itself: if these strings stopped matching, every assertion above would still pass while
        // testing nothing, because an unmatched ObjectId is silently left as-is.
        assertThat("SP fixture must parse",
                build(pools("Pool1", SP_GUID), virtualDisks(), physicalDisks("Disk1", "PCISlot1", PD1_GUID),
                        poolToDisk(SP_GUID, PD1_GUID)).get(0).getPhysicalVolumes(),
                contains("Disk1 @ PCISlot1"));
        assertThat("VD fixture must parse into 'name guid'", List.of(
                build(pools("Pool1", SP_GUID), virtualDisks("Volume1", SP_GUID, VD_GUID), physicalDisks(), poolToDisk())
                        .get(0).getLogicalVolumes().keySet().iterator().next()),
                contains("Volume1 " + VD_GUID));
    }
}
