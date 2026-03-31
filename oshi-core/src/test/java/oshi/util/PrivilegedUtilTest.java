/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

@Execution(SAME_THREAD)
class PrivilegedUtilTest {

    @BeforeEach
    void setUp() {
        GlobalConfig.clear();
    }

    @Test
    void testParseAllowlistEmpty() {
        Set<String> result = PrivilegedUtil.parseAllowlist("");
        assertThat(result, is(empty()));

        result = PrivilegedUtil.parseAllowlist(null);
        assertThat(result, is(empty()));

        result = PrivilegedUtil.parseAllowlist("   ");
        assertThat(result, is(empty()));
    }

    @Test
    void testParseAllowlistSingleEntry() {
        Set<String> result = PrivilegedUtil.parseAllowlist("dmidecode");
        assertThat(result, hasSize(1));
        assertThat(result, hasItem("dmidecode"));
    }

    @Test
    void testParseAllowlistMultipleEntries() {
        Set<String> result = PrivilegedUtil.parseAllowlist("dmidecode, lshw , /usr/sbin/lsblk");
        assertThat(result, hasSize(3));
        assertThat(result, hasItem("dmidecode"));
        assertThat(result, hasItem("lshw"));
        assertThat(result, hasItem("/usr/sbin/lsblk"));
    }

    @Test
    void testParseAllowlistWithWhitespace() {
        Set<String> result = PrivilegedUtil.parseAllowlist("  dmidecode  ,  lshw  ");
        assertThat(result, hasSize(2));
        assertThat(result, hasItem("dmidecode"));
        assertThat(result, hasItem("lshw"));
    }

    @Test
    void testIsCommandAllowedExactMatch() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("dmidecode");
        allowlist.add("/usr/sbin/lshw");

        // Exact name match
        assertThat(PrivilegedUtil.isCommandAllowed("dmidecode -t system", allowlist), is(true));

        // Exact path match
        assertThat(PrivilegedUtil.isCommandAllowed("/usr/sbin/lshw -C system", allowlist), is(true));

        // Not in allowlist
        assertThat(PrivilegedUtil.isCommandAllowed("lsblk", allowlist), is(false));
    }

    @Test
    void testIsCommandAllowedPathExtraction() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("dmidecode");

        // Full path command should match bare name in allowlist
        assertThat(PrivilegedUtil.isCommandAllowed("/usr/sbin/dmidecode -t system", allowlist), is(true));
    }

    @Test
    void testIsCommandAllowedBareNameMatchesPath() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("/usr/sbin/dmidecode");

        // Bare name command should match full path in allowlist
        assertThat(PrivilegedUtil.isCommandAllowed("dmidecode -t system", allowlist), is(true));
    }

    @Test
    void testIsCommandAllowedNullAndEmpty() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("dmidecode");

        assertThat(PrivilegedUtil.isCommandAllowed(null, allowlist), is(false));
        assertThat(PrivilegedUtil.isCommandAllowed("", allowlist), is(false));
        assertThat(PrivilegedUtil.isCommandAllowed("   ", allowlist), is(false));
        assertThat(PrivilegedUtil.isCommandAllowed("dmidecode", null), is(false));
        assertThat(PrivilegedUtil.isCommandAllowed("dmidecode", new HashSet<>()), is(false));
    }

    @Test
    void testIsFileAllowedExactMatch() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("/sys/devices/virtual/dmi/id/product_serial");

        assertThat(PrivilegedUtil.isFileAllowed("/sys/devices/virtual/dmi/id/product_serial", allowlist), is(true));
        assertThat(PrivilegedUtil.isFileAllowed("/sys/devices/virtual/dmi/id/product_uuid", allowlist), is(false));
    }

    @Test
    void testIsFileAllowedGlobPattern() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("/proc/*/io");
        allowlist.add("/proc/*/environ");

        // Glob * should match any single path segment (including non-numeric)
        assertThat(PrivilegedUtil.isFileAllowed("/proc/1234/io", allowlist), is(true));
        assertThat(PrivilegedUtil.isFileAllowed("/proc/1/io", allowlist), is(true));
        assertThat(PrivilegedUtil.isFileAllowed("/proc/99999/environ", allowlist), is(true));
        assertThat(PrivilegedUtil.isFileAllowed("/proc/abc/io", allowlist), is(true));

        // Non-matching paths (different filename)
        assertThat(PrivilegedUtil.isFileAllowed("/proc/1234/status", allowlist), is(false));
    }

    @Test
    void testIsFileAllowedNullAndEmpty() {
        Set<String> allowlist = new HashSet<>();
        allowlist.add("/proc/*/io");

        assertThat(PrivilegedUtil.isFileAllowed(null, allowlist), is(false));
        assertThat(PrivilegedUtil.isFileAllowed("", allowlist), is(false));
        assertThat(PrivilegedUtil.isFileAllowed("/proc/1234/io", null), is(false));
        assertThat(PrivilegedUtil.isFileAllowed("/proc/1234/io", new HashSet<>()), is(false));
    }

    @Test
    void testGetPrefixDefault() {
        // Without config, prefix should be empty
        assertThat(PrivilegedUtil.getPrefix(), is(""));
    }

    @Test
    void testGetPrefixConfigured() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        assertThat(PrivilegedUtil.getPrefix(), is("sudo -n"));
    }

    @Test
    void testReadFilePrivilegedReadableFile() {
        // Reading a file that exists and is readable should work without fallback
        // Using a file that should exist on most systems
        java.util.List<String> result = PrivilegedUtil.readFilePrivileged("/etc/hostname");
        // We can't assert specific content, but the method should not throw
        // and should return either content or empty list
        assertThat(result != null, is(true));
    }

    @Test
    void testReadFilePrivilegedNonExistent() {
        // Non-existent file should return empty list
        java.util.List<String> result = PrivilegedUtil.readFilePrivileged("/nonexistent/path/file");
        assertThat(result, is(empty()));
    }

    @Test
    void testGetStringFromFilePrivilegedReadable() {
        // Test with a file that should exist
        String result = PrivilegedUtil.getStringFromFilePrivileged("/etc/hostname");
        // Should return either content or empty string, not throw
        assertThat(result != null, is(true));
    }

    @Test
    void testGetStringFromFilePrivilegedNonExistent() {
        String result = PrivilegedUtil.getStringFromFilePrivileged("/nonexistent/path/file");
        assertThat(result, is(""));
    }

    @Test
    void testReadAllBytesPrivilegedNonExistent() {
        byte[] result = PrivilegedUtil.readAllBytesPrivileged("/nonexistent/path/file", false);
        assertThat(result.length, is(0));
    }

    @Test
    void testCommandAllowlistCaching() {
        // Memoized suppliers cache based on time expiration, not config changes
        // Just verify that multiple calls return consistent results
        Set<String> allowlist1 = PrivilegedUtil.getCommandAllowlist();
        Set<String> allowlist2 = PrivilegedUtil.getCommandAllowlist();
        assertThat(allowlist1 == allowlist2 || allowlist1.equals(allowlist2), is(true));
    }

    @Test
    void testFileAllowlistCaching() {
        // Memoized suppliers cache based on time expiration, not config changes
        // Just verify that multiple calls return consistent results
        Set<String> allowlist1 = PrivilegedUtil.getFileAllowlist();
        Set<String> allowlist2 = PrivilegedUtil.getFileAllowlist();
        assertThat(allowlist1 == allowlist2 || allowlist1.equals(allowlist2), is(true));
    }
}
