/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.netbsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

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
        boolean elevated = new NetBsdOperatingSystem().isElevated();
        String[] devices = NetBsdSysctlUtil.sysctl("hw.disknames", "").trim().split("\\s+");
        for (String device : devices) {
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(device);
            if (elevated) {
                assertThat("Disk label is not null", diskdata.getA(), not(nullValue()));
                assertThat("Disk duid is not null", diskdata.getB(), not(nullValue()));
                assertThat("Disk size is nonnegative", diskdata.getC().longValue(), greaterThanOrEqualTo(0L));
            }
        }
    }
}
