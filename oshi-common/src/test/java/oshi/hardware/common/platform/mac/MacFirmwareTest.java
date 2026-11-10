/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

class MacFirmwareTest {

    @Test
    void testSplitSystemFirmwareVersion() {
        Pair<String, String> nv = MacFirmware.splitSystemFirmwareVersion("mBoot-18000.161.10");
        assertThat(nv.getA(), is("mBoot"));
        assertThat(nv.getB(), is("18000.161.10"));

        nv = MacFirmware.splitSystemFirmwareVersion("iBoot-11881.140.96 ");
        assertThat(nv.getA(), is("iBoot"));
        assertThat(nv.getB(), is("11881.140.96"));

        nv = MacFirmware.splitSystemFirmwareVersion("18000.161.10");
        assertThat(nv.getA(), is(""));
        assertThat(nv.getB(), is("18000.161.10"));

        nv = MacFirmware.splitSystemFirmwareVersion(null);
        assertThat(nv.getA(), is(""));
        assertThat(nv.getB(), is(""));
    }
}
