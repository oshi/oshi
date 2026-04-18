/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static oshi.comparison.ComparisonAssertions.assertWithinRatio;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.driver.windows.wmi.Win32LogicalDiskFFM;
import oshi.driver.windows.wmi.Win32LogicalDiskJNA;
import oshi.util.PlatformEnum;
import oshi.util.platform.windows.WbemcliUtilFFM;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtilFFM;

/**
 * Compares JNA and FFM WMI driver implementations to verify they return equivalent results.
 */
@DisabledIf("isNotWindows")
class WmiComparisonTest {

    @Test
    void testLogicalDiskUnfiltered() {
        WmiResult<LogicalDiskProperty> jna = Win32LogicalDiskJNA.queryLogicalDisk(null, false);
        WbemcliUtilFFM.WmiResult<LogicalDiskProperty> ffm = Win32LogicalDiskFFM.queryLogicalDisk(null, false);

        assertThat(ffm.getResultCount()).as("LogicalDisk result count").isEqualTo(jna.getResultCount());

        // Build maps keyed by NAME since WMI row order is nondeterministic
        var jnaByName = buildJnaMap(jna);
        var ffmByName = buildFfmMap(ffm);

        assertThat(ffmByName.keySet()).as("LogicalDisk names").containsExactlyInAnyOrderElementsOf(jnaByName.keySet());

        for (String name : jnaByName.keySet()) {
            int ji = jnaByName.get(name);
            int fi = ffmByName.get(name);
            for (LogicalDiskProperty key : LogicalDiskProperty.values()) {
                String desc = "LogicalDisk " + key + " [" + name + "]";
                switch (key) {
                    case NAME, DESCRIPTION, FILESYSTEM, VOLUMENAME, PROVIDERNAME -> assertThat(
                            WmiUtilFFM.getString(ffm, key, fi)).as(desc).isEqualTo(WmiUtil.getString(jna, key, ji));
                    case DRIVETYPE -> assertThat(WmiUtilFFM.getUint32(ffm, key, fi)).as(desc)
                            .isEqualTo(WmiUtil.getUint32(jna, key, ji));
                    case ACCESS -> assertThat(WmiUtilFFM.getUint16(ffm, key, fi)).as(desc)
                            .isEqualTo(WmiUtil.getUint16(jna, key, ji));
                    case SIZE -> assertThat(WmiUtilFFM.getUint64(ffm, key, fi)).as(desc)
                            .isEqualTo(WmiUtil.getUint64(jna, key, ji));
                    default -> assertWithinRatio(WmiUtilFFM.getUint64(ffm, key, fi), WmiUtil.getUint64(jna, key, ji),
                            0.05, desc);
                }
            }
        }
    }

    @Test
    void testLogicalDiskLocalOnly() {
        WmiResult<LogicalDiskProperty> jna = Win32LogicalDiskJNA.queryLogicalDisk(null, true);
        WbemcliUtilFFM.WmiResult<LogicalDiskProperty> ffm = Win32LogicalDiskFFM.queryLogicalDisk(null, true);

        assertThat(ffm.getResultCount()).as("LogicalDisk local-only result count").isEqualTo(jna.getResultCount());

        var jnaByName = buildJnaMap(jna);
        var ffmByName = buildFfmMap(ffm);

        assertThat(ffmByName.keySet()).as("LogicalDisk local names")
                .containsExactlyInAnyOrderElementsOf(jnaByName.keySet());

        for (String name : jnaByName.keySet()) {
            int fi = ffmByName.get(name);
            int driveType = WmiUtilFFM.getUint32(ffm, LogicalDiskProperty.DRIVETYPE, fi);
            assertThat(driveType).as("LogicalDisk local DRIVETYPE [%s]", name).isIn(2, 3, 6);
        }
    }

    @Test
    void testLogicalDiskNameFilter() {
        WmiResult<LogicalDiskProperty> all = Win32LogicalDiskJNA.queryLogicalDisk(null, false);
        Assumptions.assumeTrue(all.getResultCount() > 0, "No logical disks found");
        String firstName = WmiUtil.getString(all, LogicalDiskProperty.NAME, 0);

        WmiResult<LogicalDiskProperty> jna = Win32LogicalDiskJNA.queryLogicalDisk(firstName, false);
        WbemcliUtilFFM.WmiResult<LogicalDiskProperty> ffm = Win32LogicalDiskFFM.queryLogicalDisk(firstName, false);

        assertThat(ffm.getResultCount()).as("LogicalDisk filtered result count").isEqualTo(jna.getResultCount());
        assertThat(jna.getResultCount()).as("LogicalDisk filtered should find match").isGreaterThan(0);

        assertThat(WmiUtilFFM.getString(ffm, LogicalDiskProperty.NAME, 0)).as("LogicalDisk filtered NAME")
                .isEqualTo(firstName);
    }

    private static Map<String, Integer> buildJnaMap(WmiResult<LogicalDiskProperty> result) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < result.getResultCount(); i++) {
            map.put(WmiUtil.getString(result, LogicalDiskProperty.NAME, i), i);
        }
        return map;
    }

    private static Map<String, Integer> buildFfmMap(WbemcliUtilFFM.WmiResult<LogicalDiskProperty> result) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < result.getResultCount(); i++) {
            map.put(WmiUtilFFM.getString(result, LogicalDiskProperty.NAME, i), i);
        }
        return map;
    }

    static boolean isNotWindows() {
        return PlatformEnum.getCurrentPlatform() != PlatformEnum.WINDOWS;
    }
}
