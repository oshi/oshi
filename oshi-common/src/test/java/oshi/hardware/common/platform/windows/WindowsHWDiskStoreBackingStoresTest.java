/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_REFERENCE;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_STRING;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT16;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT32;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_BSTR;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_I4;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.wmi.MSFTStorage.DiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskIdProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskIdProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Tests the shared Storage namespace correlation without Windows, by stubbing the four WMI results. The values are the
 * ones a GitHub Windows runner reports for a plain attached VHD and a Storage Space pooled from two more.
 */
class WindowsHWDiskStoreBackingStoresTest {

    private static final String SP_GUID = "{d9357415-de1d-4a36-a7c6-f72c0c603e5b}";
    private static final String VD_GUID = "{2d8ec33c-a9b8-4ffd-b126-0e027ff1a2cb}";
    private static final String PD1_GUID = "{33aa2bdf-5edb-2509-8406-0d5f72fafa25}";
    private static final String PD2_GUID = "{6b4d76ec-fa01-6c7c-7e26-2da40d1f4423}";
    private static final String SPACE_UNIQUE_ID = "3CC38E2DB8A9FD4FB1260E027FF1A2CB";

    // Stands in for GetVolumePathName on a path whose volume is its drive root
    private static String driveRoot(String path) {
        return path.substring(0, 3);
    }

    // The ObjectId form both the object IDs and the references to them carry
    private static String objectId(String kind, String... guids) {
        StringBuilder sb = new StringBuilder("{1}\\\\HOST\\root/Microsoft/Windows/Storage/Providers_v2\\SPACES_");
        sb.append(kind).append(".ObjectId=\"{5b46f354-cd98-11f1-98e6-806e6f6e6963}:").append(kind).append(':');
        for (String guid : guids) {
            sb.append(guid);
        }
        return sb.append('"').toString();
    }

    private static <T extends Enum<T>> WmiResult<T> result(List<Map<T, Object>> rows, Map<T, Integer> cimTypes) {
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
                int cim = getCIMType(property);
                return cim == CIM_UINT32 || cim == CIM_UINT16 ? VT_I4 : VT_BSTR;
            }

            @Override
            public int getCIMType(T property) {
                return cimTypes.getOrDefault(property, CIM_STRING);
            }
        };
    }

    private static WmiResult<DiskProperty> disks(Object... numberBusLocationUniqueId) {
        List<Map<DiskProperty, Object>> rows = new ArrayList<>();
        for (int i = 0; i + 3 < numberBusLocationUniqueId.length; i += 4) {
            Map<DiskProperty, Object> row = new EnumMap<>(DiskProperty.class);
            row.put(DiskProperty.NUMBER, numberBusLocationUniqueId[i]);
            row.put(DiskProperty.BUSTYPE, numberBusLocationUniqueId[i + 1]);
            row.put(DiskProperty.LOCATION, numberBusLocationUniqueId[i + 2]);
            row.put(DiskProperty.UNIQUEID, numberBusLocationUniqueId[i + 3]);
            rows.add(row);
        }
        Map<DiskProperty, Integer> types = new EnumMap<>(DiskProperty.class);
        types.put(DiskProperty.NUMBER, CIM_UINT32);
        types.put(DiskProperty.BUSTYPE, CIM_UINT16);
        return result(rows, types);
    }

    private static WmiResult<VirtualDiskIdProperty> virtualDisk() {
        Map<VirtualDiskIdProperty, Object> row = new EnumMap<>(VirtualDiskIdProperty.class);
        row.put(VirtualDiskIdProperty.UNIQUEID, SPACE_UNIQUE_ID);
        row.put(VirtualDiskIdProperty.OBJECTID, objectId("VD", SP_GUID, VD_GUID));
        List<Map<VirtualDiskIdProperty, Object>> rows = new ArrayList<>();
        rows.add(row);
        return result(rows, new EnumMap<>(VirtualDiskIdProperty.class));
    }

    private static WmiResult<VirtualDiskToPhysicalDiskProperty> members(String... pdGuids) {
        String[] refs = new String[pdGuids.length];
        for (int i = 0; i < pdGuids.length; i++) {
            refs[i] = objectId("PD", pdGuids[i]);
        }
        return memberRefs(refs);
    }

    private static WmiResult<VirtualDiskToPhysicalDiskProperty> memberRefs(String... pdRefs) {
        List<Map<VirtualDiskToPhysicalDiskProperty, Object>> rows = new ArrayList<>();
        for (String pd : pdRefs) {
            Map<VirtualDiskToPhysicalDiskProperty, Object> row = new EnumMap<>(VirtualDiskToPhysicalDiskProperty.class);
            row.put(VirtualDiskToPhysicalDiskProperty.VIRTUALDISK, objectId("VD", SP_GUID, VD_GUID));
            row.put(VirtualDiskToPhysicalDiskProperty.PHYSICALDISK, pd);
            rows.add(row);
        }
        Map<VirtualDiskToPhysicalDiskProperty, Integer> types = new EnumMap<>(VirtualDiskToPhysicalDiskProperty.class);
        types.put(VirtualDiskToPhysicalDiskProperty.VIRTUALDISK, CIM_REFERENCE);
        types.put(VirtualDiskToPhysicalDiskProperty.PHYSICALDISK, CIM_REFERENCE);
        return result(rows, types);
    }

    private static WmiResult<PhysicalDiskIdProperty> physicalDisks() {
        List<Map<PhysicalDiskIdProperty, Object>> rows = new ArrayList<>();
        String[][] pds = { { "2", PD1_GUID }, { "3", PD2_GUID } };
        for (String[] pd : pds) {
            Map<PhysicalDiskIdProperty, Object> row = new EnumMap<>(PhysicalDiskIdProperty.class);
            row.put(PhysicalDiskIdProperty.DEVICEID, pd[0]);
            row.put(PhysicalDiskIdProperty.OBJECTID, objectId("PD", pd[1]));
            rows.add(row);
        }
        return result(rows, new EnumMap<>(PhysicalDiskIdProperty.class));
    }

    private static Map<String, List<String>> runnerLayout() {
        return WindowsHWDiskStore.buildBackingStores(
                disks(0, 10,
                        "Integrated : Bus 0 : Device 63667 : Function 30746 : Adapter 0 : Port 0 : Target 0 : LUN 0",
                        "600224808EEA07B8362DCA3D18902747", 4, 15, "D:\\oshi3.vhdx", "60022480C89306D00CF0D175C5B764A1",
                        5, 16, "", SPACE_UNIQUE_ID),
                virtualDisk(), members(PD2_GUID, PD1_GUID), physicalDisks(),
                WindowsHWDiskStoreBackingStoresTest::driveRoot);
    }

    @Test
    void testPhysicalDiskIsNotLayered() {
        assertThat(runnerLayout().get("\\\\.\\PHYSICALDRIVE0"), is(empty()));
    }

    @Test
    void testFileBackedDiskNamesTheVolumeHoldingItsFile() {
        assertThat(runnerLayout().get("\\\\.\\PHYSICALDRIVE4"), contains("file:D:\\"));
    }

    @Test
    void testStorageSpaceNamesItsPhysicalDisksSorted() {
        assertThat(runnerLayout().get("\\\\.\\PHYSICALDRIVE5"),
                contains("disk:\\\\.\\PHYSICALDRIVE2", "disk:\\\\.\\PHYSICALDRIVE3"));
    }

    @Test
    void testUnresolvableDisksAreNotReported() {
        Map<String, List<String>> stores = WindowsHWDiskStore.buildBackingStores(
                disks(4, 15, "D:\\oshi3.vhdx", "", 6, 15, "", "", 5, 16, "", "NO-SUCH-VIRTUAL-DISK"), virtualDisk(),
                members(PD1_GUID), physicalDisks(), path -> "");
        // The volume could not be read, the location is missing, and the virtual disk is unknown
        assertThat(stores, is(anEmptyMap()));
    }

    @Test
    void testSpaceMembersWithoutADeviceIdAreSkipped() {
        // One member has no DeviceId, and one is a reference in no form the patterns know, so has no physical disk
        List<Map<PhysicalDiskIdProperty, Object>> rows = new ArrayList<>();
        Map<PhysicalDiskIdProperty, Object> row = new EnumMap<>(PhysicalDiskIdProperty.class);
        row.put(PhysicalDiskIdProperty.DEVICEID, "");
        row.put(PhysicalDiskIdProperty.OBJECTID, objectId("PD", PD1_GUID));
        rows.add(row);
        WmiResult<PhysicalDiskIdProperty> noDeviceId = result(rows, new EnumMap<>(PhysicalDiskIdProperty.class));
        Map<String, List<String>> stores = WindowsHWDiskStore.buildBackingStores(disks(5, 16, "", SPACE_UNIQUE_ID),
                virtualDisk(), memberRefs("not an object reference", objectId("PD", PD1_GUID)), noDeviceId,
                WindowsHWDiskStoreBackingStoresTest::driveRoot);
        // With no member resolvable to a physical drive the Space is not reported, rather than reported unlayered
        assertThat(stores, is(anEmptyMap()));
    }

    @Test
    void testUnexpectedPropertyTypeReportsNothing() {
        // A Number published as a string rather than a uint32 must not escape as an exception
        List<Map<DiskProperty, Object>> rows = new ArrayList<>();
        Map<DiskProperty, Object> row = new EnumMap<>(DiskProperty.class);
        row.put(DiskProperty.NUMBER, "0");
        rows.add(row);
        WmiResult<DiskProperty> badDisks = result(rows, new EnumMap<>(DiskProperty.class));
        assertThat(WindowsHWDiskStore.buildBackingStores(badDisks, virtualDisk(), members(), physicalDisks(),
                WindowsHWDiskStoreBackingStoresTest::driveRoot), is(anEmptyMap()));
        assertThat(runnerLayout(), not(hasKey("\\\\.\\PHYSICALDRIVE1")));
    }
}
