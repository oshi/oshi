/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.util.Constants;
import oshi.util.tuples.Triplet;

/**
 * Tests the dmesg firmware parse shared by the BSDs, and the blank-to-unknown substitution the base applies to every
 * BSD. NetBSD and OpenBSD print the same {@code bios0} banner, so both platforms' fixtures are exercised here.
 */
class BsdFirmwareTest {

    /** Exposes the parse and the substitution without running a native command. */
    private static final class TestFirmware extends BsdFirmware {
        private final List<String> dmesg;

        private TestFirmware(List<String> dmesg) {
            this.dmesg = dmesg;
        }

        @Override
        protected Triplet<@Nullable String, @Nullable String, @Nullable String> readFirmware() {
            return parseDmesg(dmesg);
        }
    }

    private static Triplet<@Nullable String, @Nullable String, @Nullable String> parse(List<String> dmesg) {
        return BsdFirmware.parseDmesg(dmesg);
    }

    @Test
    void testParseDmesgNetBsd() {
        // Representative NetBSD dmesg bios0 output
        List<String> dmesg = """
                bios0 at mainbus0: SMBIOS rev. 2.7 @ 0xdcc0e000 (67 entries)
                bios0: vendor LENOVO version "GLET90WW (2.44 )" date 09/13/2017
                bios0: LENOVO 20AWA08J00
                """.lines().toList();
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = parse(dmesg);
        assertThat(fw.getA(), is("LENOVO"));
        assertThat(fw.getB(), is("GLET90WW (2.44 )"));
        assertThat(fw.getC(), is("2017-09-13"));
    }

    @Test
    void testParseDmesgOpenBsd() {
        // Representative OpenBSD dmesg bios0 output with multi-word vendor
        List<String> dmesg = Arrays.asList(//
                "bios0 at mainbus0: SMBIOS rev. 2.8 @ 0xe9cb0 (53 entries)", //
                "bios0: vendor American Megatrends Inc. version \"F5\" date 03/18/2016");
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = parse(dmesg);
        assertThat(fw.getA(), is("American Megatrends Inc."));
        assertThat(fw.getB(), is("F5"));
        assertThat(fw.getC(), is("2016-03-18"));
    }

    @Test
    void testParseDmesgMultiWordVendor() {
        List<String> dmesg = Arrays.asList("bios0: vendor Phoenix Technologies LTD version \"6.00\" date 04/14/2014");
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = parse(dmesg);
        assertThat(fw.getA(), is("Phoenix Technologies LTD"));
        assertThat(fw.getB(), is("6.00"));
        assertThat(fw.getC(), is("2014-04-14"));
    }

    @Test
    void testParseDmesgEmpty() {
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = parse(Collections.emptyList());
        assertThat(fw.getA(), is(nullValue()));
        assertThat(fw.getB(), is(nullValue()));
        assertThat(fw.getC(), is(emptyString()));
    }

    @Test
    void testParseDmesgNoMatch() {
        List<String> dmesg = Arrays.asList(//
                "cpu0 at mainbus0: AMD EPYC 7313P", //
                "some other line");
        Triplet<@Nullable String, @Nullable String, @Nullable String> fw = parse(dmesg);
        assertThat(fw.getA(), is(nullValue()));
        assertThat(fw.getB(), is(nullValue()));
        assertThat(fw.getC(), is(emptyString()));
    }

    @Test
    void testBlankAttributesBecomeUnknown() {
        // The parse leaves absent fields null or blank; the base is what turns them into the documented sentinel.
        BsdFirmware fw = new TestFirmware(Collections.emptyList());
        assertThat(fw.getManufacturer(), is(Constants.UNKNOWN));
        assertThat(fw.getVersion(), is(Constants.UNKNOWN));
        assertThat(fw.getReleaseDate(), is(Constants.UNKNOWN));
    }

    @Test
    void testParsedAttributesArePassedThrough() {
        BsdFirmware fw = new TestFirmware(Arrays.asList("bios0: vendor LENOVO version \"GLET90WW\" date 09/13/2017"));
        assertThat(fw.getManufacturer(), is("LENOVO"));
        assertThat(fw.getVersion(), is("GLET90WW"));
        assertThat(fw.getReleaseDate(), is("2017-09-13"));
    }

    @Test
    void testNameAndDescriptionKeepTheAbstractDefaults() {
        // dmesg and dmidecode supply neither, so the BSDs must not start reporting something else for them.
        BsdFirmware fw = new TestFirmware(Collections.emptyList());
        assertThat(fw.getName(), is(Constants.UNKNOWN));
        assertThat(fw.getDescription(), is(Constants.UNKNOWN));
    }
}
