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
}
