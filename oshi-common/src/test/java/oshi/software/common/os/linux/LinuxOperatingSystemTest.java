/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.Constants;
import oshi.util.tuples.Triplet;

@EnabledOnOs(OS.LINUX)
class LinuxOperatingSystemTest {

    // Fixture: /etc/os-release from Ubuntu 22.04
    private static final List<String> OS_RELEASE_UBUNTU = Arrays.asList("NAME=\"Ubuntu\"",
            "VERSION=\"22.04.3 LTS (Jammy Jellyfish)\"", "ID=ubuntu", "VERSION_ID=\"22.04\"",
            "PRETTY_NAME=\"Ubuntu 22.04.3 LTS\"");

    // Fixture: /etc/os-release from CentOS Stream (no codename in VERSION)
    private static final List<String> OS_RELEASE_CENTOS = Arrays.asList("NAME=\"CentOS Stream\"", "VERSION=\"9\"",
            "ID=\"centos\"", "VERSION_ID=\"9\"");

    // Fixture: lsb_release -a output
    private static final List<String> LSB_RELEASE = Arrays.asList(
            "LSB Version:\tcore-11.1.0ubuntu4-noarch:security-11.1.0ubuntu4-noarch", "Distributor ID:\tUbuntu",
            "Description:\tUbuntu 22.04.3 LTS", "Release:\t22.04", "Codename:\tjammy");

    @Test
    void testReadOsReleaseUbuntu() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_UBUNTU);
        assertThat(result.getA(), is("Ubuntu"));
        assertThat(result.getB(), is("22.04.3 LTS"));
        assertThat(result.getC(), is("Jammy Jellyfish"));
    }

    @Test
    void testReadOsReleaseCentOS() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_CENTOS);
        assertThat(result.getA(), is("CentOS Stream"));
        assertThat(result.getB(), is("9"));
    }

    @Test
    void testReadOsReleaseEmpty() {
        assertThat(LinuxOperatingSystem.readOsRelease(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testReadOsReleaseNoName() {
        List<String> noName = Arrays.asList("VERSION=\"1.0\"", "VERSION_ID=\"1\"");
        assertThat(LinuxOperatingSystem.readOsRelease(noName), is(nullValue()));
    }

    @Test
    void testExecLsbRelease() {
        Triplet<String, String, String> result = LinuxOperatingSystem.execLsbRelease(LSB_RELEASE);
        assertThat(result.getA(), is("Ubuntu"));
        assertThat(result.getB(), is("22.04"));
        assertThat(result.getC(), is("jammy"));
    }

    @Test
    void testExecLsbReleaseEmpty() {
        assertThat(LinuxOperatingSystem.execLsbRelease(Collections.emptyList()), is(nullValue()));
    }

    @Test
    void testExecLsbReleaseDescriptionOnly() {
        List<String> descOnly = Arrays.asList("Description:\tFedora release 38 (Thirty Eight)");
        Triplet<String, String, String> result = LinuxOperatingSystem.execLsbRelease(descOnly);
        assertThat(result.getA(), is("Fedora"));
        assertThat(result.getB(), is("38"));
        assertThat(result.getC(), is("Thirty Eight"));
    }

    // -------------------------------------------------------------------------
    // getParentPidFromStat — parses /proc/[pid]/stat
    // -------------------------------------------------------------------------

    @Test
    void testGetParentPidFromStat() {
        // Real /proc/[pid]/stat line: pid (comm) state ppid ...
        String stat = "1234 (bash) S 567 1234 1234 0 -1 4194304 1234 0 0 0 0 0 0 0 20 0 1 0 12345 67890 0 "
                + "18446744073709551615 0 0 0 0 0 0 0 0 65536 0 0 0 17 0 0 0 0 0 0 0 0 0 0 0 0 0 0";
        assertThat(LinuxOperatingSystem.getParentPidFromStat(stat), is(567));
    }

    @Test
    void testGetParentPidFromStatInit() {
        // PID 1 (init/systemd) has PPID 0
        String stat = "1 (systemd) S 0 1 1 0 -1 4194560 12345 0 0 0 0 0 0 0 20 0 1 0 1 67890 0 "
                + "18446744073709551615 0 0 0 0 0 0 0 0 65536 0 0 0 17 0 0 0 0 0 0 0 0 0 0 0 0 0 0";
        assertThat(LinuxOperatingSystem.getParentPidFromStat(stat), is(0));
    }

    @Test
    void testGetParentPidFromStatEmpty() {
        assertThat(LinuxOperatingSystem.getParentPidFromStat(""), is(0));
    }

    @Test
    void testGetParentPidFromStatCommWithSpaces() {
        // Process name can contain spaces and parentheses
        String stat = "42 (Web Content (pid 42)) S 100 42 42 0 -1 0 0 0 0 0 0 0 0 0 20 0 1 0 0 0 0 "
                + "18446744073709551615 0 0 0 0 0 0 0 0 0 0 0 0 17 0 0 0 0 0 0 0 0 0 0 0 0 0 0";
        assertThat(LinuxOperatingSystem.getParentPidFromStat(stat), is(100));
    }

    // -------------------------------------------------------------------------
    // readLsbRelease — parses /etc/lsb-release
    // -------------------------------------------------------------------------

    @Test
    void testReadLsbRelease() {
        List<String> lines = Arrays.asList("DISTRIB_ID=Ubuntu", "DISTRIB_RELEASE=20.04", "DISTRIB_CODENAME=focal",
                "DISTRIB_DESCRIPTION=\"Ubuntu 20.04.6 LTS\"");
        Triplet<String, String, String> result = LinuxOperatingSystem.readLsbRelease(lines);
        assertThat(result.getA(), is("Ubuntu"));
        assertThat(result.getB(), is("20.04"));
        assertThat(result.getC(), is("focal"));
    }

    @Test
    void testReadLsbReleaseDescriptionWithRelease() {
        List<String> lines = Arrays.asList("DISTRIB_DESCRIPTION=\"Fedora release 38 (Thirty Eight)\"");
        Triplet<String, String, String> result = LinuxOperatingSystem.readLsbRelease(lines);
        assertThat(result.getA(), is("Fedora"));
        assertThat(result.getB(), is("38"));
        assertThat(result.getC(), is("Thirty Eight"));
    }

    @Test
    void testReadLsbReleaseEmpty() {
        assertThat(LinuxOperatingSystem.readLsbRelease(Collections.emptyList()), is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // readDistribRelease — parses distrib-release files
    // -------------------------------------------------------------------------

    @Test
    void testReadDistribRelease() {
        List<String> lines = Arrays.asList("CentOS release 6.10 (Final)");
        Triplet<String, String, String> result = LinuxOperatingSystem.readDistribRelease(lines);
        assertThat(result.getA(), is("CentOS"));
        assertThat(result.getB(), is("6.10"));
        assertThat(result.getC(), is("Final"));
    }

    @Test
    void testReadDistribReleaseVersion() {
        List<String> lines = Arrays.asList("SUSE Linux Enterprise Server VERSION 15");
        Triplet<String, String, String> result = LinuxOperatingSystem.readDistribRelease(lines);
        assertThat(result.getA(), is("SUSE Linux Enterprise Server"));
        assertThat(result.getB(), is("15"));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    @Test
    void testReadDistribReleaseEmpty() {
        assertThat(LinuxOperatingSystem.readDistribRelease(Collections.emptyList()), is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // parseRelease — pure parsing logic
    // -------------------------------------------------------------------------

    @Test
    void testParseRelease() {
        Triplet<String, String, String> result = LinuxOperatingSystem
                .parseRelease("Red Hat Enterprise Linux release 8.5 (Ootpa)", " release ");
        assertThat(result.getA(), is("Red Hat Enterprise Linux"));
        assertThat(result.getB(), is("8.5"));
        assertThat(result.getC(), is("Ootpa"));
    }

    @Test
    void testParseReleaseNoCodename() {
        Triplet<String, String, String> result = LinuxOperatingSystem.parseRelease("Debian release 11", " release ");
        assertThat(result.getA(), is("Debian"));
        assertThat(result.getB(), is("11"));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    @Test
    void testParseReleaseNoVersion() {
        Triplet<String, String, String> result = LinuxOperatingSystem.parseRelease("MyLinux", " release ");
        assertThat(result.getA(), is("MyLinux"));
        assertThat(result.getB(), is(Constants.UNKNOWN));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    // -------------------------------------------------------------------------
    // filenameToFamily — maps release filename to distro family
    // -------------------------------------------------------------------------

    @Test
    void testFilenameToFamilyEmpty() {
        assertThat(LinuxOperatingSystem.filenameToFamily(""), is("Solaris"));
    }

    @Test
    void testFilenameToFamilyIssue() {
        assertThat(LinuxOperatingSystem.filenameToFamily("issue"), is("Unknown"));
    }

    @Test
    void testFilenameToFamilyUnknown() {
        // Unknown name gets capitalized
        assertThat(LinuxOperatingSystem.filenameToFamily("mylinux"), is("Mylinux"));
    }
}
