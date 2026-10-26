/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskMediaTypeProperty;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.WmiConstants;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.hardware.common.platform.windows.WindowsHWDiskStore.StorageProperties;

/**
 * Tests disk type classification from storage driver and WMI values. These are pure functions of the values a Windows
 * host supplies, so they are exercised on every platform.
 */
class WindowsDiskTypeTest {

    // STORAGE_BUS_TYPE values
    private static final int BUS_USB = 7;
    private static final int BUS_SATA = 11;
    private static final int BUS_FILE_BACKED_VIRTUAL = 15;
    private static final int BUS_SPACES = 16;
    private static final int BUS_NVME = 17;

    @Test
    void testWin32MediaTypeCannotIdentifyMediaTechnology() {
        // Win32_DiskDrive's MediaType is mapped from DISK_GEOMETRY.MediaType and reports only removability.
        // A fixed or external disk must not be guessed at: either could be an SSD.
        assertThat("Fixed media is not an HDD", WindowsHWDiskStore.parseWindowsMediaType("Fixed hard disk media"),
                is("Unknown"));
        assertThat("External media is not an HDD", WindowsHWDiskStore.parseWindowsMediaType("External hard disk media"),
                is("Unknown"));
        assertThat("Unknown format", WindowsHWDiskStore.parseWindowsMediaType("Format is unknown"), is("Unknown"));
        assertThat("Removable media", WindowsHWDiskStore.parseWindowsMediaType("Removable media other than floppy"),
                is("Removable"));
    }

    @Test
    void testWin32MediaTypeAbsentOrEmpty() {
        assertThat("Null media type", WindowsHWDiskStore.parseWindowsMediaType(null), is("Unknown"));
        assertThat("Empty media type", WindowsHWDiskStore.parseWindowsMediaType(""), is("Unknown"));
    }

    @Test
    void testClassifyFromSeekPenalty() {
        assertThat("No seek penalty is solid state", WindowsHWDiskStore.classifyDiskType(
                new StorageProperties(BUS_NVME, false, Boolean.FALSE), "Fixed hard disk media"), is("SSD"));
        assertThat("A seek penalty is rotating media", WindowsHWDiskStore.classifyDiskType(
                new StorageProperties(BUS_SATA, false, Boolean.TRUE), "Fixed hard disk media"), is("HDD"));
    }

    @Test
    void testRemovableTakesPrecedenceOverMediaTechnology() {
        // Matches Linux, which returns Removable without consulting queue/rotational
        assertThat("Removable flash drive",
                WindowsHWDiskStore.classifyDiskType(new StorageProperties(BUS_USB, true, Boolean.FALSE),
                        "Removable media other than floppy"),
                is("Removable"));
    }

    @Test
    void testVirtualBusTypes() {
        assertThat("File backed virtual disk", WindowsHWDiskStore.classifyDiskType(
                new StorageProperties(BUS_FILE_BACKED_VIRTUAL, false, null), "Fixed hard disk media"), is("Virtual"));
        assertThat("Storage Spaces disk", WindowsHWDiskStore.classifyDiskType(
                new StorageProperties(BUS_SPACES, false, Boolean.FALSE), "Fixed hard disk media"), is("Virtual"));
    }

    @Test
    void testExternalDriveIsClassifiedByTechnologyNotEnclosure() {
        // A USB-attached SSD reports fixed media on a removable-looking bus. The seek penalty decides.
        assertThat("USB SSD enclosure", WindowsHWDiskStore.classifyDiskType(
                new StorageProperties(BUS_USB, false, Boolean.FALSE), "External hard disk media"), is("SSD"));
    }

    @Test
    void testSeekPenaltyUnsupportedLeavesTypeUnknown() {
        // The descriptor was read but the driver does not implement the seek penalty property, so the
        // MSFT_PhysicalDisk fallback must get a chance rather than the WMI string being over-read.
        assertThat("Driver did not answer", WindowsHWDiskStore.classifyDiskType(
                new StorageProperties(BUS_SATA, false, null), "Fixed hard disk media"), is("Unknown"));
    }

    @Test
    void testDeviceUnreachableFallsBackToWmiString() {
        assertThat("Device could not be opened",
                WindowsHWDiskStore.classifyDiskType(null, "Removable media other than floppy"), is("Removable"));
        assertThat("Device could not be opened", WindowsHWDiskStore.classifyDiskType(null, "Fixed hard disk media"),
                is("Unknown"));
    }

    @Test
    void testMsftMediaTypeValues() {
        assertThat("MediaType 3", WindowsHWDiskStore.diskTypeFromMsftMediaType(3, 7200L), is("HDD"));
        assertThat("MediaType 4", WindowsHWDiskStore.diskTypeFromMsftMediaType(4, 0L), is("SSD"));
        assertThat("MediaType 5 is storage class memory", WindowsHWDiskStore.diskTypeFromMsftMediaType(5, 0L),
                is("SSD"));
    }

    @Test
    void testMsftSpindleSpeedFallback() {
        assertThat("Unspecified type, rotating", WindowsHWDiskStore.diskTypeFromMsftMediaType(0, 5400L), is("HDD"));
        assertThat("Unspecified type, rotating at unknown speed",
                WindowsHWDiskStore.diskTypeFromMsftMediaType(0, 0xFFFFFFFFL), is("HDD"));
        // A zero spindle speed is also what an absent property reads as, so it must not imply solid state
        assertThat("Unspecified type, no spindle speed", WindowsHWDiskStore.diskTypeFromMsftMediaType(0, 0L),
                is("Unknown"));
    }

    @Test
    void testBuildMsftDiskTypeMap() {
        Map<String, String> map = WindowsHWDiskStore.buildMsftDiskTypeMap(
                mediaTypeResult(new String[] { "0", "1", "2" }, new int[] { 4, 3, 0 }, new long[] { 0L, 7200L, 0L }));
        // DeviceId 2 is unclassifiable, so it is omitted rather than mapped to Unknown, leaving the
        // storage driver's own answer in place for that disk
        assertThat("Two disks classified", map.size(), is(2));
        assertThat("DeviceId 0", map.get("0"), is("SSD"));
        assertThat("DeviceId 1", map.get("1"), is("HDD"));
        assertThat("DeviceId 2 omitted", map.containsKey("2"), is(false));
    }

    @Test
    void testBuildMsftDiskTypeMapEmptyResult() {
        assertThat("No rows",
                WindowsHWDiskStore.buildMsftDiskTypeMap(WmiResult.<PhysicalDiskMediaTypeProperty>empty()).isEmpty(),
                is(true));
    }

    @Test
    void testBuildMsftDiskTypeMapSkipsBlankDeviceId() {
        Map<String, String> map = WindowsHWDiskStore
                .buildMsftDiskTypeMap(mediaTypeResult(new String[] { "" }, new int[] { 4 }, new long[] { 0L }));
        assertThat("Blank DeviceId skipped", map.isEmpty(), is(true));
    }

    /**
     * Builds a WmiResult standing in for an {@code MSFT_PhysicalDisk} query.
     *
     * @param deviceIds    the DeviceId of each row
     * @param mediaTypes   the MediaType of each row
     * @param spindleSpeed the SpindleSpeed of each row
     * @return the result
     */
    private static WmiResult<PhysicalDiskMediaTypeProperty> mediaTypeResult(String[] deviceIds, int[] mediaTypes,
            long[] spindleSpeed) {
        return new WmiResult<PhysicalDiskMediaTypeProperty>() {
            @Override
            public int getResultCount() {
                return deviceIds.length;
            }

            @Override
            public @Nullable Object getValue(PhysicalDiskMediaTypeProperty property, int index) {
                return switch (property) {
                    case DEVICEID -> deviceIds[index];
                    case MEDIATYPE -> mediaTypes[index];
                    // SpindleSpeed is a uint32, carried in a VT_I4 and read back as unsigned
                    case SPINDLESPEED -> (int) spindleSpeed[index];
                };
            }

            @Override
            public int getVtType(PhysicalDiskMediaTypeProperty property) {
                return property == PhysicalDiskMediaTypeProperty.DEVICEID ? WmiConstants.VT_BSTR : WmiConstants.VT_I4;
            }

            @Override
            public int getCIMType(PhysicalDiskMediaTypeProperty property) {
                return switch (property) {
                    case DEVICEID -> WmiConstants.CIM_STRING;
                    case MEDIATYPE -> WmiConstants.CIM_UINT16;
                    case SPINDLESPEED -> WmiConstants.CIM_UINT32;
                };
            }
        };
    }

    @Test
    void testClassifyDiskTypesOverAQueryResult() {
        WmiResult<DiskDriveProperty> vals = diskDriveResult(
                new String[] { "\\\\.\\PHYSICALDRIVE0", "\\\\.\\PHYSICALDRIVE1" },
                new String[] { "Fixed hard disk media", "Removable media other than floppy" }, new int[] { 0, 1 });
        // Resolver answers for drive 0 only, as a driver lacking the seek penalty property would
        String[] diskTypes = WindowsHWDiskStore.classifyDiskTypes(vals,
                name -> name.endsWith("0") ? new StorageProperties(BUS_NVME, false, Boolean.FALSE) : null);
        assertThat("Driver answered", diskTypes[0], is("SSD"));
        assertThat("Driver silent, WMI says removable", diskTypes[1], is("Removable"));
    }

    @Test
    void testFillUnknownDiskTypesOnlyFillsUnknowns() {
        WmiResult<DiskDriveProperty> vals = diskDriveResult(
                new String[] { "\\\\.\\PHYSICALDRIVE0", "\\\\.\\PHYSICALDRIVE1", "\\\\.\\PHYSICALDRIVE2" },
                new String[] { "Fixed hard disk media", "Fixed hard disk media", "Fixed hard disk media" },
                new int[] { 0, 1, 2 });
        // MSFT_PhysicalDisk claims HDD for all three, but only the Unknown may be overwritten
        String[] diskTypes = { "Removable", "Unknown", "SSD" };
        WindowsHWDiskStore.fillUnknownDiskTypes(diskTypes, vals, mediaTypeResult(new String[] { "0", "1", "2" },
                new int[] { 3, 3, 3 }, new long[] { 7200L, 7200L, 7200L }));
        assertThat("Removable is not relabeled by media technology", diskTypes[0], is("Removable"));
        assertThat("Unknown is filled from MSFT_PhysicalDisk", diskTypes[1], is("HDD"));
        assertThat("SSD is not relabeled", diskTypes[2], is("SSD"));
    }

    @Test
    void testFillUnknownDiskTypesJoinsOnDriveNumber() {
        // Win32_DiskDrive's Index and MSFT_PhysicalDisk's DeviceId are both the physical drive number,
        // and neither query is ordered by it, so the join must not rely on row position
        WmiResult<DiskDriveProperty> vals = diskDriveResult(
                new String[] { "\\\\.\\PHYSICALDRIVE3", "\\\\.\\PHYSICALDRIVE7" },
                new String[] { "Fixed hard disk media", "Fixed hard disk media" }, new int[] { 3, 7 });
        String[] diskTypes = { "Unknown", "Unknown" };
        WindowsHWDiskStore.fillUnknownDiskTypes(diskTypes, vals,
                mediaTypeResult(new String[] { "7", "3" }, new int[] { 3, 4 }, new long[] { 7200L, 0L }));
        assertThat("Drive 3 matched DeviceId 3", diskTypes[0], is("SSD"));
        assertThat("Drive 7 matched DeviceId 7", diskTypes[1], is("HDD"));
    }

    @Test
    void testFillUnknownDiskTypesLeavesUnmatchedDiskUnknown() {
        WmiResult<DiskDriveProperty> vals = diskDriveResult(new String[] { "\\\\.\\PHYSICALDRIVE0" },
                new String[] { "Fixed hard disk media" }, new int[] { 0 });
        String[] diskTypes = { "Unknown" };
        // A Storage Spaces virtual disk has no MSFT_PhysicalDisk row with its drive number
        WindowsHWDiskStore.fillUnknownDiskTypes(diskTypes, vals,
                mediaTypeResult(new String[] { "4" }, new int[] { 4 }, new long[] { 0L }));
        assertThat("No matching DeviceId", diskTypes[0], is("Unknown"));
    }

    @Test
    void testFillUnknownDiskTypesWithNoUsableRows() {
        WmiResult<DiskDriveProperty> vals = diskDriveResult(new String[] { "\\\\.\\PHYSICALDRIVE0" },
                new String[] { "Fixed hard disk media" }, new int[] { 0 });
        String[] diskTypes = { "Unknown" };
        // The Storage namespace is absent, as before Windows 8, so the query returned nothing
        WindowsHWDiskStore.fillUnknownDiskTypes(diskTypes, vals, WmiResult.<PhysicalDiskMediaTypeProperty>empty());
        assertThat("Empty result leaves the type alone", diskTypes[0], is("Unknown"));
    }

    /**
     * Builds a WmiResult standing in for a {@code Win32_DiskDrive} query.
     *
     * @param names      the Name of each row
     * @param mediaTypes the MediaType of each row
     * @param indices    the Index of each row
     * @return the result
     */
    private static WmiResult<DiskDriveProperty> diskDriveResult(String[] names, String[] mediaTypes, int[] indices) {
        return new WmiResult<DiskDriveProperty>() {
            @Override
            public int getResultCount() {
                return names.length;
            }

            @Override
            public @Nullable Object getValue(DiskDriveProperty property, int index) {
                return switch (property) {
                    case NAME -> names[index];
                    case MEDIATYPE -> mediaTypes[index];
                    case INDEX -> indices[index];
                    default -> null;
                };
            }

            @Override
            public int getVtType(DiskDriveProperty property) {
                return property == DiskDriveProperty.INDEX ? WmiConstants.VT_I4 : WmiConstants.VT_BSTR;
            }

            @Override
            public int getCIMType(DiskDriveProperty property) {
                return property == DiskDriveProperty.INDEX ? WmiConstants.CIM_UINT32 : WmiConstants.CIM_STRING;
            }
        };
    }

    @Test
    void testAnyDiskTypeUnknown() {
        assertThat("All known", WindowsHWDiskStore.anyDiskTypeUnknown(new String[] { "SSD", "HDD", "Removable" }),
                is(false));
        assertThat("One unknown", WindowsHWDiskStore.anyDiskTypeUnknown(new String[] { "SSD", "Unknown" }), is(true));
        assertThat("No disks", WindowsHWDiskStore.anyDiskTypeUnknown(new String[0]), is(false));
    }
}
