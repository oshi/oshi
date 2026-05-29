/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.netbsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import oshi.hardware.HWPartition;
import oshi.software.os.unix.netbsd.NetBsdOperatingSystem;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;
import oshi.util.tuples.Quartet;

@EnabledIfSystemProperty(named = "os.name", matches = "(?i)netbsd")
class DisklabelTest {
    @Test
    void testDisklabel() {
        String[] devices = NetBsdSysctlUtil.sysctl("hw.disknames", "").split(",");
        for (String device : devices) {
            String diskName = device.split(":")[0];
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            // First 3 only available with elevation
            if (new NetBsdOperatingSystem().isElevated()) {
                assertThat("Disk label is not null", diskdata.getA(), not(nullValue()));
                assertThat("Disk duid is not null", diskdata.getB(), not(nullValue()));
                assertThat("Disk size is nonnegative", diskdata.getC().longValue(), greaterThanOrEqualTo(0L));
                for (HWPartition part : diskdata.getD()) {
                    assertTrue(part.getIdentification().startsWith(diskName), "Partition ID starts with disk");
                }
            }
        }
    }
}
