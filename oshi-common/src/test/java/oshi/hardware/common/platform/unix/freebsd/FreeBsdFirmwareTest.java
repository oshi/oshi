/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;
import oshi.util.tuples.Triplet;

class FreeBsdFirmwareTest {

    @Test
    void testParseDmiDecode() {
        // Representative `dmidecode -t bios` (DMI type 0) block; MM/DD/YYYY date is normalized to ISO.
        List<String> dmidecode = Arrays.asList(//
                "Handle 0x0000, DMI type 0, 24 bytes", //
                "BIOS Information", //
                "\tVendor: Parallels Software International Inc.", //
                "\tVersion: 11.2.1 (32626)", //
                "\tRelease Date: 07/15/2016", //
                "\tBIOS Revision: 11.2", //
                "\tFirmware Revision: 11.2");
        Triplet<String, String, String> fw = FreeBsdFirmware.parseDmiDecode(dmidecode);
        assertThat(fw.getA(), is("Parallels Software International Inc."));
        assertThat(fw.getB(), is("11.2.1 (32626)"));
        assertThat(fw.getC(), is("2016-07-15"));
    }

    @Test
    void testParseDmiDecodeEmpty() {
        // No output (e.g. dmidecode not available / not root): every field is UNKNOWN.
        Triplet<String, String, String> fw = FreeBsdFirmware.parseDmiDecode(Collections.emptyList());
        assertThat(fw.getA(), is(Constants.UNKNOWN));
        assertThat(fw.getB(), is(Constants.UNKNOWN));
        assertThat(fw.getC(), is(Constants.UNKNOWN));
    }
}
