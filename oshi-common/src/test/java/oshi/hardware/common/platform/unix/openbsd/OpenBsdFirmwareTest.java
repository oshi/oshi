/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Triplet;

class OpenBsdFirmwareTest {

    @Test
    void testParseDmesg() {
        // Representative OpenBSD dmesg bios0 output with multi-word vendor
        List<String> dmesg = Arrays.asList(//
                "bios0 at mainbus0: SMBIOS rev. 2.8 @ 0xe9cb0 (53 entries)", //
                "bios0: vendor American Megatrends Inc. version \"F5\" date 03/18/2016");
        Triplet<String, String, String> fw = OpenBsdFirmware.parseDmesg(dmesg);
        assertThat(fw.getA(), is("American Megatrends Inc."));
        assertThat(fw.getB(), is("F5"));
        assertThat(fw.getC(), is("2016-03-18"));
    }

    @Test
    void testParseDmesgEmpty() {
        Triplet<String, String, String> fw = OpenBsdFirmware.parseDmesg(Collections.emptyList());
        assertThat(fw.getA(), is(nullValue()));
        assertThat(fw.getB(), is(nullValue()));
        assertThat(fw.getC(), is(emptyString()));
    }
}
