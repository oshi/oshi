/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class SolarisOSProcessTest {

    @Test
    void testParseOpenFileLimitSoft() {
        List<String> plimit = Arrays.asList("1234:", "   coredumpsize      0           unlimited",
                "   nofiles(descriptors)  256         65536", "   vmemorysize       unlimited   unlimited");
        assertThat(SolarisOSProcess.parseOpenFileLimit(plimit, 1), is(256L));
    }

    @Test
    void testParseOpenFileLimitHard() {
        List<String> plimit = Arrays.asList("1234:", "   nofiles(descriptors)  256         65536");
        assertThat(SolarisOSProcess.parseOpenFileLimit(plimit, 2), is(65536L));
    }

    @Test
    void testParseOpenFileLimitEmpty() {
        assertThat(SolarisOSProcess.parseOpenFileLimit(Collections.emptyList(), 1), is(-1L));
    }

    @Test
    void testParseOpenFileLimitNoNofilesLine() {
        List<String> plimit = Arrays.asList("1234:", "   coredumpsize      0           unlimited");
        assertThat(SolarisOSProcess.parseOpenFileLimit(plimit, 1), is(-1L));
    }

    @Test
    void testParseOpenFileLimitIndexOutOfBounds() {
        List<String> plimit = Arrays.asList("   nofiles(descriptors)  256         unlimited");
        // "unlimited" is non-numeric, so split yields only ["", "256"]
        assertThat(SolarisOSProcess.parseOpenFileLimit(plimit, 2), is(-1L));
    }

    @Test
    void testParsePbindOutputBound() {
        String cpuset = "pid 101048 strongly bound to processor(s) 0 1 2 3.";
        long mask = SolarisOSProcess.parsePbindOutput(cpuset);
        // bits 0,1,2,3 set = 0b1111 = 15
        assertThat(mask, is(15L));
    }

    @Test
    void testParsePbindOutputEmpty() {
        assertThat(SolarisOSProcess.parsePbindOutput(""), is(0L));
    }

    @Test
    void testParsePbindOutputNotBound() {
        // Some unexpected format that doesn't match the pattern
        assertThat(SolarisOSProcess.parsePbindOutput("pid 1234 not bound"), is(0L));
    }

    @Test
    void testParsePsrinfo() {
        List<String> psrinfo = Arrays.asList("0       on-line   since 01/01/2020 00:00:00",
                "1       on-line   since 01/01/2020 00:00:00", "4       on-line   since 01/01/2020 00:00:00");
        long mask = SolarisOSProcess.parsePsrinfo(psrinfo);
        // bits 0, 1, 4 set = 0b10011 = 19
        assertThat(mask, is(19L));
    }

    @Test
    void testParsePsrinfoEmpty() {
        assertThat(SolarisOSProcess.parsePsrinfo(Collections.emptyList()), is(0L));
    }
}
