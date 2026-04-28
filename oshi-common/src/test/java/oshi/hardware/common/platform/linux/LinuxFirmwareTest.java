/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.common.platform.linux.LinuxFirmware.VcGenCmdStrings;
import oshi.util.Constants;

class LinuxFirmwareTest {

    // Fixture: typical vcgencmd version output from a Raspberry Pi
    private static final List<String> VCGENCMD_OUTPUT = Arrays.asList("Jan 13 2013 16:24:29",
            "Copyright (c) 2012 Broadcom", "version d292ce426b2d3fee875be2bfc220a76f3ef073fe (clean) (release)");

    @Test
    void testQueryVcGenCmdParsesDate() {
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(VCGENCMD_OUTPUT);
        assertThat(result.getReleaseDate(), is("2013-01-13"));
    }

    @Test
    void testQueryVcGenCmdParsesManufacturer() {
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(VCGENCMD_OUTPUT);
        assertThat(result.getManufacturer(), is("Broadcom"));
    }

    @Test
    void testQueryVcGenCmdParsesVersion() {
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(VCGENCMD_OUTPUT);
        assertThat(result.getVersion(), is("d292ce426b2d3fee875be2bfc220a76f3ef073fe (clean) (release)"));
    }

    @Test
    void testQueryVcGenCmdSetsNameAndDescription() {
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(VCGENCMD_OUTPUT);
        assertThat(result.getName(), is("RPi"));
        assertThat(result.getDescription(), is("Bootloader"));
    }

    @Test
    void testQueryVcGenCmdEmptyInput() {
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(Collections.emptyList());
        assertThat(result.getReleaseDate(), is(nullValue()));
        assertThat(result.getManufacturer(), is(nullValue()));
        assertThat(result.getVersion(), is(nullValue()));
        assertThat(result.getName(), is(nullValue()));
        assertThat(result.getDescription(), is(nullValue()));
    }

    @Test
    void testQueryVcGenCmdTooFewLines() {
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(Arrays.asList("Jan 13 2013 16:24:29"));
        assertThat(result.getReleaseDate(), is(nullValue()));
        assertThat(result.getManufacturer(), is(nullValue()));
        assertThat(result.getVersion(), is(nullValue()));
        assertThat(result.getName(), is(nullValue()));
        assertThat(result.getDescription(), is(nullValue()));
    }

    @Test
    void testQueryVcGenCmdInvalidDate() {
        List<String> badDate = Arrays.asList("not a date", "Copyright (c) 2012 Broadcom", "version abc123");
        VcGenCmdStrings result = LinuxFirmware.queryVcGenCmd(badDate);
        assertThat(result.getReleaseDate(), is(Constants.UNKNOWN));
        assertThat(result.getManufacturer(), is("Broadcom"));
        assertThat(result.getVersion(), is("abc123"));
    }
}
