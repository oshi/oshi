/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;

class AixComputerSystemTest {

    // lsattr -El sys0: attr, value, description, user-settable columns (whitespace-separated)
    private static final List<String> LSATTR = Arrays.asList(//
            "fwversion       IBM,RG080425_d79e22_r                Firmware version and revision levels   False", //
            "modelname       IBM,9114-275                         Machine name                           False", //
            "os_uuid         789f930f-b15c-4639-b842-b42603862704 N/A                                     True", //
            "systemid        IBM,0110ACFDE                        Hardware system identifier             False");

    // lsmcode -c
    private static final List<String> LSMCODE = Arrays.asList(//
            "Platform Firmware level is 3F080425", //
            "System Firmware level is RG080425_d79e22_regatta");

    @Test
    void testParseLsattr() {
        AixComputerSystem.LsattrStrings s = AixComputerSystem.parseLsattr(LSATTR, LSMCODE);
        // The "IBM," prefix is split off manufacturer/vendor; the remaining first token is model/version
        assertThat(s.manufacturer(), is("IBM"));
        assertThat(s.model(), is("9114-275"));
        assertThat(s.serialNumber(), is("IBM,0110ACFDE"));
        assertThat(s.uuid(), is("789f930f-b15c-4639-b842-b42603862704"));
        assertThat(s.biosVendor(), is("IBM"));
        assertThat(s.biosVersion(), is("RG080425_d79e22_r"));
        assertThat(s.biosPlatformVersion(), is("3F080425"));
    }

    @Test
    void testParseLsattrEmpty() {
        // No lsattr/lsmcode output: vendor/manufacturer keep their IBM defaults, everything else is UNKNOWN
        AixComputerSystem.LsattrStrings s = AixComputerSystem.parseLsattr(Collections.emptyList(),
                Collections.emptyList());
        assertThat(s.manufacturer(), is("IBM"));
        assertThat(s.biosVendor(), is("IBM"));
        assertThat(s.model(), is(Constants.UNKNOWN));
        assertThat(s.serialNumber(), is(Constants.UNKNOWN));
        assertThat(s.uuid(), is(Constants.UNKNOWN));
        assertThat(s.biosVersion(), is(Constants.UNKNOWN));
        assertThat(s.biosPlatformVersion(), is(Constants.UNKNOWN));
    }
}
