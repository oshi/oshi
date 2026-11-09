/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class UdevDataTest {

    // Shape of /run/udev/data/b8:1 on an Ubuntu host: symlinks, tags and timestamps around the E: properties
    private static final String PARTITION_RECORD = """
            S:disk/by-uuid/ABCD-1234
            S:disk/by-partuuid/0f1e2d3c-01
            W:12
            I:1731000000000000
            E:ID_FS_TYPE=vfat
            E:ID_FS_UUID=ABCD-1234
            E:ID_FS_LABEL=EFI
            E:ID_PART_ENTRY_NAME=EFI System Partition
            E:ID_MODEL=Samsung SSD 980 PRO=2TB
            G:systemd
            Q:systemd
            V:1
            """;

    @Test
    void testParseProperties() {
        var properties = UdevData.parseProperties(PARTITION_RECORD.lines().toList());
        assertThat(properties, aMapWithSize(5));
        assertThat(properties, hasEntry("ID_FS_TYPE", "vfat"));
        assertThat("A value keeps spaces", properties, hasEntry("ID_PART_ENTRY_NAME", "EFI System Partition"));
        assertThat("Only the first = separates key and value", properties,
                hasEntry("ID_MODEL", "Samsung SSD 980 PRO=2TB"));
    }

    @Test
    void testParsePropertiesIgnoresMalformed() {
        assertThat(UdevData.parseProperties(List.of("E:", "E:=value", "E:NOEQUALS", "S:link", "")), anEmptyMap());
    }

    @Test
    // A block device's record is named major:minor, and Windows does not allow a colon in a file name
    @DisabledOnOs(OS.WINDOWS)
    void testQueryByDeviceNumberAndIfindex(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b8:1"), PARTITION_RECORD);
        Files.writeString(dir.resolve("n2"), "E:ID_MODEL_FROM_DATABASE=I225-V\n");
        String data = dir + "/";
        assertThat(UdevData.queryBlockDevice(data, 8, 1), hasEntry("ID_FS_UUID", "ABCD-1234"));
        assertThat(UdevData.queryNetworkInterface(data, 2), hasEntry("ID_MODEL_FROM_DATABASE", "I225-V"));
        // No record, and the numbers a failed sysfs read yields, never touch a file
        assertThat(UdevData.queryBlockDevice(data, 8, 2), anEmptyMap());
        assertThat(UdevData.queryBlockDevice(data, 0, 0), anEmptyMap());
        assertThat(UdevData.queryNetworkInterface(data, 0), anEmptyMap());
    }

    @Test
    void testDefaultDatabaseRejectsUnsetNumbers() {
        assertThat(UdevData.queryBlockDevice(0, 0), anEmptyMap());
        assertThat(UdevData.queryNetworkInterface(0), anEmptyMap());
        assertThat(UdevData.queryDevice("pci", ""), anEmptyMap());
        assertThat(UdevData.queryDevice("", "0000:01:00.0"), anEmptyMap());
    }
}
