/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

class LscfgTest {

    @Test
    void testParseModelSerial() {
        // lscfg -vl hdisk0: the "Machine Type and Model" line overrides the first-line description
        Pair<String, String> modSer = Lscfg.parseModelSerial(Arrays.asList(//
                "  hdisk0           U78CB.001.WZS00MP-P1-C2-T1-L0  MPIO IBM 2076 FC Disk", //
                "        Manufacturer................IBM", //
                "        Machine Type and Model......2076", //
                "        Serial Number...............0123456789AB", //
                "        EC Level....................D77161"), "hdisk0");
        assertThat(modSer.getA(), is("2076"));
        assertThat(modSer.getB(), is("0123456789AB"));
    }

    @Test
    void testParseModelSerialFallsBackToDescription() {
        // With no "Machine Type and Model" line, the model is the description after the location code
        Pair<String, String> modSer = Lscfg.parseModelSerial(
                Collections.singletonList("  cd0              U78CB.001.WZS00MP-P2-D1  SATA DVD-RAM Drive"), "cd0");
        assertThat(modSer.getA(), is("SATA DVD-RAM Drive"));
        assertThat(modSer.getB(), is(nullValue()));
    }

    @Test
    void testParseModelSerialEmpty() {
        Pair<String, String> modSer = Lscfg.parseModelSerial(Collections.emptyList(), "hdisk0");
        assertThat(modSer.getA(), is(nullValue()));
        assertThat(modSer.getB(), is(nullValue()));
    }
}
