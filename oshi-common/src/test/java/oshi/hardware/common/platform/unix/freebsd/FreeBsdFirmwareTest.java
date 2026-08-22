/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

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
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = FreeBsdFirmware.parseDmiDecode(dmidecode);
        assertThat(fw.getA(), is("Parallels Software International Inc."));
        assertThat(fw.getB(), is("11.2.1 (32626)"));
        assertThat(fw.getC(), is("2016-07-15"));
    }

    @Test
    void testParseDmiDecodeEmpty() {
        // No output (e.g. dmidecode not available / not root): the parser returns raw absent values (null
        // manufacturer/version, empty date); the BsdFirmware base applies the Constants.UNKNOWN fallbacks.
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = FreeBsdFirmware
                .parseDmiDecode(Collections.emptyList());
        assertThat(fw.getA(), is(nullValue()));
        assertThat(fw.getB(), is(nullValue()));
        assertThat(fw.getC(), is(emptyString()));
    }

    @Test
    void testDmiDecodeOverridesTheDmesgDefault() {
        // BsdFirmware defaults to the dmesg banner; FreeBSD is the one BSD that reads dmidecode instead. Deleting its
        // override would silently switch it to dmesg while the parseDmiDecode tests above kept passing.
        assertDoesNotThrow(() -> FreeBsdFirmware.class.getDeclaredMethod("readFirmware"),
                "FreeBsdFirmware must declare its own readFirmware, not inherit the dmesg default");
    }
}
