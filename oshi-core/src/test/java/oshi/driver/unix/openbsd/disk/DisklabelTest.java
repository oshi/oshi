/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.openbsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.HWPartition;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Quartet;

@EnabledOnOs(OS.OPENBSD)
class DisklabelTest {
    @Test
    void testDisklabel() {
        String[] devices = OpenBsdSysctlUtil.sysctl("hw.disknames", "").split(",");
        for (String device : devices) {
            String diskName = device.split(":")[0];
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            // First 3 only available with elevation
            if (new OpenBsdOperatingSystem().isElevated()) {
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
