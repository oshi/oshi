/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Triplet;

class NetBsdFirmwareTest {

    @Test
    void testParseDmesg() {
        // Representative NetBSD dmesg bios0 output
        List<String> dmesg = Arrays.asList(//
                "bios0 at mainbus0: SMBIOS rev. 2.7 @ 0xdcc0e000 (67 entries)", //
                "bios0: vendor LENOVO version \"GLET90WW (2.44 )\" date 09/13/2017", //
                "bios0: LENOVO 20AWA08J00");
        Triplet<String, String, String> fw = NetBsdFirmware.parseDmesg(dmesg);
        assertThat(fw.getA(), is("LENOVO"));
        assertThat(fw.getB(), is("GLET90WW (2.44 )"));
        assertThat(fw.getC(), is("2017-09-13"));
    }

    @Test
    void testParseDmesgEmpty() {
        Triplet<String, String, String> fw = NetBsdFirmware.parseDmesg(Collections.emptyList());
        assertThat(fw.getA(), is(nullValue()));
        assertThat(fw.getB(), is(nullValue()));
        assertThat(fw.getC(), is(emptyString()));
    }

    @Test
    void testParseDmesgMultiWordVendor() {
        List<String> dmesg = Arrays.asList(//
                "bios0: vendor Phoenix Technologies LTD version \"6.00\" date 04/14/2014");
        Triplet<String, String, String> fw = NetBsdFirmware.parseDmesg(dmesg);
        assertThat(fw.getA(), is("Phoenix Technologies LTD"));
        assertThat(fw.getB(), is("6.00"));
        assertThat(fw.getC(), is("2014-04-14"));
    }

    @Test
    void testParseDmesgNoMatch() {
        List<String> dmesg = Arrays.asList(//
                "cpu0 at mainbus0: AMD EPYC 7313P", //
                "some other line");
        Triplet<String, String, String> fw = NetBsdFirmware.parseDmesg(dmesg);
        assertThat(fw.getA(), is(nullValue()));
        assertThat(fw.getB(), is(nullValue()));
        assertThat(fw.getC(), is(emptyString()));
    }
}
