/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
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

    @Test
    void testGetMissingDetailsFillsNameFromStat() {
        Map<String, String> status = new HashMap<>();
        status.put("Name", "");
        status.put("State", "S");
        LinuxOSProcess.getMissingDetails(status, "1234 (java) S 1 1234 1234 0");
        assertThat(status.get("Name"), is("java"));
    }

    @Test
    void testGetMissingDetailsFillsStateFromStat() {
        Map<String, String> status = new HashMap<>();
        status.put("Name", "java");
        status.put("State", "");
        LinuxOSProcess.getMissingDetails(status, "1234 (java) S 1 1234 1234 0");
        assertThat(status.get("State"), is("S"));
    }

    @Test
    void testGetMissingDetailsDoesNotOverwriteExisting() {
        Map<String, String> status = new HashMap<>();
        status.put("Name", "myproc");
        status.put("State", "R");
        LinuxOSProcess.getMissingDetails(status, "1234 (java) S 1 1234 1234 0");
        assertThat(status.get("Name"), is("myproc"));
        assertThat(status.get("State"), is("R"));
    }

    @Test
    void testGetMissingDetailsNullStatusNoException() {
        assertDoesNotThrow(() -> LinuxOSProcess.getMissingDetails(null, "1234 (java) S 1 1234 1234 0"));
    }

    @Test
    void testGetMissingDetailsCommWithParentheses() {
        Map<String, String> status = new HashMap<>();
        status.put("Name", "");
        status.put("State", "");
        LinuxOSProcess.getMissingDetails(status, "1234 (Web Content (pid 42)) S 1 1234 1234 0");
        assertThat(status.get("Name"), is("Web Content (pid 42)"));
        assertThat(status.get("State"), is("S"));
    }
}
