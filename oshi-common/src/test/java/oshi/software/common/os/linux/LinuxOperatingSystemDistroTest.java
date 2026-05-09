/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.Constants;
import oshi.util.tuples.Triplet;

/**
 * Tests LinuxOperatingSystem release file parsing with fixtures from various distributions.
 */
@EnabledOnOs(OS.LINUX)
class LinuxOperatingSystemDistroTest {

    // --- os-release fixtures ---

    private static final List<String> OS_RELEASE_FEDORA = Arrays.asList("NAME=\"Fedora Linux\"",
            "VERSION=\"39 (Workstation Edition)\"", "ID=fedora", "VERSION_ID=39",
            "PRETTY_NAME=\"Fedora Linux 39 (Workstation Edition)\"");

    private static final List<String> OS_RELEASE_ARCH = Arrays.asList("NAME=\"Arch Linux\"", "ID=arch",
            "PRETTY_NAME=\"Arch Linux\"", "BUILD_ID=rolling");

    private static final List<String> OS_RELEASE_DEBIAN = Arrays.asList("NAME=\"Debian GNU/Linux\"",
            "VERSION=\"12 (bookworm)\"", "VERSION_ID=\"12\"", "ID=debian",
            "PRETTY_NAME=\"Debian GNU/Linux 12 (bookworm)\"");

    private static final List<String> OS_RELEASE_ALPINE = Arrays.asList("NAME=\"Alpine Linux\"", "ID=alpine",
            "VERSION_ID=3.19.0", "PRETTY_NAME=\"Alpine Linux v3.19\"");

    private static final List<String> OS_RELEASE_RHEL = Arrays.asList("NAME=\"Red Hat Enterprise Linux\"",
            "VERSION=\"9.3 (Plow)\"", "ID=\"rhel\"", "VERSION_ID=\"9.3\"",
            "PRETTY_NAME=\"Red Hat Enterprise Linux 9.3 (Plow)\"");

    private static final List<String> OS_RELEASE_OPENSUSE = Arrays.asList("NAME=\"openSUSE Leap\"", "VERSION=\"15.5\"",
            "ID=\"opensuse-leap\"", "VERSION_ID=\"15.5\"", "PRETTY_NAME=\"openSUSE Leap 15.5\"");

    @Test
    void testReadOsReleaseFedora() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_FEDORA);
        assertThat(result.getA(), is("Fedora Linux"));
        assertThat(result.getB(), is("39"));
        assertThat(result.getC(), is("Workstation Edition"));
    }

    @Test
    void testReadOsReleaseArch() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_ARCH);
        assertThat(result.getA(), is("Arch Linux"));
        // Arch has no VERSION or VERSION_ID
        assertThat(result.getB(), is(Constants.UNKNOWN));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    @Test
    void testReadOsReleaseDebian() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_DEBIAN);
        assertThat(result.getA(), is("Debian GNU/Linux"));
        assertThat(result.getB(), is("12"));
        assertThat(result.getC(), is("bookworm"));
    }

    @Test
    void testReadOsReleaseAlpine() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_ALPINE);
        assertThat(result.getA(), is("Alpine Linux"));
        assertThat(result.getB(), is("3.19.0"));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    @Test
    void testReadOsReleaseRHEL() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_RHEL);
        assertThat(result.getA(), is("Red Hat Enterprise Linux"));
        assertThat(result.getB(), is("9.3"));
        assertThat(result.getC(), is("Plow"));
    }

    @Test
    void testReadOsReleaseOpenSUSE() {
        Triplet<String, String, String> result = LinuxOperatingSystem.readOsRelease(OS_RELEASE_OPENSUSE);
        assertThat(result.getA(), is("openSUSE Leap"));
        assertThat(result.getB(), is("15.5"));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    // --- distrib-release fixtures ---

    @Test
    void testReadDistribReleaseAmazonLinux() {
        List<String> lines = Arrays.asList("Amazon Linux release 2023 (Amazon Linux)");
        Triplet<String, String, String> result = LinuxOperatingSystem.readDistribRelease(lines);
        assertThat(result.getA(), is("Amazon Linux"));
        assertThat(result.getB(), is("2023"));
        assertThat(result.getC(), is("Amazon Linux"));
    }

    @Test
    void testReadDistribReleaseOracle() {
        List<String> lines = Arrays.asList("Oracle Linux Server release 8.9");
        Triplet<String, String, String> result = LinuxOperatingSystem.readDistribRelease(lines);
        assertThat(result.getA(), is("Oracle Linux Server"));
        assertThat(result.getB(), is("8.9"));
        assertThat(result.getC(), is(Constants.UNKNOWN));
    }

    @Test
    void testReadDistribReleaseRocky() {
        List<String> lines = Arrays.asList("Rocky Linux release 9.3 (Blue Onyx)");
        Triplet<String, String, String> result = LinuxOperatingSystem.readDistribRelease(lines);
        assertThat(result.getA(), is("Rocky Linux"));
        assertThat(result.getB(), is("9.3"));
        assertThat(result.getC(), is("Blue Onyx"));
    }

    // --- lsb-release fixtures ---

    @Test
    void testReadLsbReleaseFedora() {
        List<String> lines = Arrays.asList("DISTRIB_ID=Fedora", "DISTRIB_RELEASE=39", "DISTRIB_CODENAME=ThirtyNine");
        Triplet<String, String, String> result = LinuxOperatingSystem.readLsbRelease(lines);
        assertThat(result.getA(), is("Fedora"));
        assertThat(result.getB(), is("39"));
        assertThat(result.getC(), is("ThirtyNine"));
    }

    // --- execLsbRelease fixtures ---

    @Test
    void testExecLsbReleaseDebian() {
        List<String> lines = Arrays.asList("Distributor ID:\tDebian", "Description:\tDebian GNU/Linux 12 (bookworm)",
                "Release:\t12", "Codename:\tbookworm");
        Triplet<String, String, String> result = LinuxOperatingSystem.execLsbRelease(lines);
        assertThat(result.getA(), is("Debian"));
        assertThat(result.getB(), is("12"));
        assertThat(result.getC(), is("bookworm"));
    }

    @Test
    void testExecLsbReleaseNoCodename() {
        List<String> lines = Arrays.asList("Distributor ID:\tArch", "Description:\tArch Linux", "Release:\trolling",
                "Codename:\tn/a");
        Triplet<String, String, String> result = LinuxOperatingSystem.execLsbRelease(lines);
        assertThat(result.getA(), is("Arch"));
        assertThat(result.getB(), is("rolling"));
    }

    // --- filenameToFamily ---

    @Test
    void testFilenameToFamilyKnownDistros() {
        assertThat(LinuxOperatingSystem.filenameToFamily("redhat"), is("Red Hat Linux"));
        assertThat(LinuxOperatingSystem.filenameToFamily("yellowdog"), is("Yellow Dog"));
        assertThat(LinuxOperatingSystem.filenameToFamily("centos"), is("Centos"));
    }
}
