/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Quintet;

class FreeBsdComputerSystemTest {

    @Test
    void testParseDmiDecode() {
        // Representative `dmidecode -t system` (DMI type 1) block.
        List<String> dmidecode = Arrays.asList(//
                "Handle 0x0001, DMI type 1, 27 bytes", //
                "System Information", //
                "\tManufacturer: Dell Inc.", //
                "\tProduct Name: PowerEdge R640", //
                "\tVersion: Not Specified", //
                "\tSerial Number: 7XY1234", //
                "\tUUID: 4c4c4544-0058-5910-8034-c4c04f313233", //
                "\tWake-up Type: Power Switch", //
                "\tSKU Number: SKU=NotProvided", //
                "\tFamily: PowerEdge");
        Quintet<String, String, String, String, String> dmi = FreeBsdComputerSystem.parseDmiDecode(dmidecode);
        assertThat(dmi.getA(), is("Dell Inc."));
        assertThat(dmi.getB(), is("PowerEdge R640"));
        assertThat(dmi.getC(), is("7XY1234"));
        assertThat(dmi.getD(), is("4c4c4544-0058-5910-8034-c4c04f313233"));
        assertThat(dmi.getE(), is("Not Specified"));
    }

    @Test
    void testParseDmiDecodeEmpty() {
        // No output (e.g. dmidecode not available / not root): every field is null so the caller can fall back.
        Quintet<String, String, String, String, String> dmi = FreeBsdComputerSystem
                .parseDmiDecode(Collections.emptyList());
        assertThat(dmi.getA(), is(nullValue()));
        assertThat(dmi.getC(), is(nullValue()));
        assertThat(dmi.getD(), is(nullValue()));
    }
}
