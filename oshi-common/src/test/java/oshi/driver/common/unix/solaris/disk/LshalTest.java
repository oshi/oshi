/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LshalTest {

    @Test
    void testParseLshalTypicalOutput() {
        List<String> lshal = Arrays.asList("udi = '/org/freedesktop/Hal/devices/storage_serial_VBOX_HARDDISK_VB12345'",
                "  block.major = 102  (0x66)  (int)", "  block.minor = 0  (0x0)  (int)",
                "  info.category = 'storage'  (string)", "",
                "udi = '/org/freedesktop/Hal/devices/storage_serial_disk1'", "  block.major = 91  (0x5b)  (int)",
                "  block.minor = 1  (0x1)  (int)");

        Map<String, Integer> result = Lshal.parseLshal(lshal);

        assertThat(result.size(), is(2));
        assertThat(result.get("storage_serial_VBOX_HARDDISK_VB12345"), is(102));
        assertThat(result.get("storage_serial_disk1"), is(91));
    }

    @Test
    void testParseLshalEmptyInput() {
        Map<String, Integer> result = Lshal.parseLshal(Collections.emptyList());
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    void testParseLshalNoBlockMajorLine() {
        List<String> lshal = Arrays.asList("udi = '/org/freedesktop/Hal/devices/computer'",
                "  info.category = 'computer'  (string)", "  info.product = 'Computer'  (string)");

        Map<String, Integer> result = Lshal.parseLshal(lshal);
        assertThat(result, is(anEmptyMap()));
    }
}
