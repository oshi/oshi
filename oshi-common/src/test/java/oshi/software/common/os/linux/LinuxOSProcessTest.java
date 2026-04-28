/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class LinuxOSProcessTest {

    @Test
    void testParseAffinityMaskHex() {
        assertThat(LinuxOSProcess.parseAffinityMask("pid 3283's current affinity mask: 3"), is(3L));
    }

    @Test
    void testParseAffinityMaskFullHex() {
        assertThat(LinuxOSProcess.parseAffinityMask("pid 9726's current affinity mask: f"), is(15L));
    }

    @Test
    void testParseAffinityMaskMultiCore() {
        assertThat(LinuxOSProcess.parseAffinityMask("pid 1234's current affinity mask: ff"), is(255L));
    }

    @Test
    void testParseAffinityMaskEmpty() {
        assertThat(LinuxOSProcess.parseAffinityMask(""), is(0L));
    }

    @Test
    void testParseAffinityMaskInvalid() {
        assertThat(LinuxOSProcess.parseAffinityMask("no mask here"), is(0L));
    }
}
