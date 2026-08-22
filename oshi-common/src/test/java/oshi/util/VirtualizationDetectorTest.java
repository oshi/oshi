/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

/**
 * Tests virtualization signature matching against fixture data.
 */
class VirtualizationDetectorTest {

    private static Properties shippedVmProps() {
        return FileUtil.readPropertiesFromFilename("oshi.vm.properties");
    }

    private static Properties shippedMacProps() {
        return FileUtil.readPropertiesFromFilename("oshi.vmmacaddr.properties");
    }

    @Test
    void testShippedPropertiesLoadWithUnescapedKeys() {
        // A missing backslash before a space in a key truncates that key silently, so assert the unescaped forms
        Properties props = shippedVmProps();
        assertThat("oshi.vm.properties should be on the classpath", props.stringPropertyNames().size(),
                is(greaterThan(0)));
        assertThat(props.getProperty("cpuid.bhyve bhyve"), is("bhyve"));
        assertThat(props.getProperty("cpuid.Microsoft Hv"), is("Microsoft Hyper-V"));
        assertThat(props.getProperty("cpuid.lrpepyh vr"), is("Parallels"));
        assertThat(props.getProperty("model.Linux KVM"), is("KVM"));
        assertThat(props.getProperty("model.Microsoft Corporation Virtual Machine"), is("Microsoft Hyper-V"));
    }

    @Test
    void testShippedPropertiesAreWellFormed() {
        Properties props = shippedVmProps();
        for (String key : props.stringPropertyNames()) {
            assertThat("Unexpected prefix on key " + key, key.startsWith("cpuid.") || key.startsWith("model."),
                    is(true));
            assertFalse(key.contains("\\"), "Key retains a backslash, so it was over-escaped: " + key);
            String value = props.getProperty(key);
            assertThat("Blank value for key " + key, value, is(not(emptyString())));
            assertThat("Untrimmed value for key " + key, value, is(value.trim()));
        }
    }

    @Test
    void testShippedMacPropertiesAreWellFormed() {
        Properties props = shippedMacProps();
        assertThat("oshi.vmmacaddr.properties should be on the classpath", props.stringPropertyNames().size(),
                is(greaterThan(0)));
        for (String key : props.stringPropertyNames()) {
            assertThat("OUI key is not uppercase colon-delimited hex: " + key, key,
                    is(VirtualizationDetector.extractOui(key)));
        }
    }

    @Test
    void testMatchCpuid() {
        Properties props = shippedVmProps();
        assertThat(VirtualizationDetector.matchCpuid("VMwareVMware", props), is(Optional.of("VMware")));
        // The vendor string is trimmed before lookup
        assertThat(VirtualizationDetector.matchCpuid("  KVMKVMKVM  ", props), is(Optional.of("KVM")));
        assertThat(VirtualizationDetector.matchCpuid("bhyve bhyve", props), is(Optional.of("bhyve")));
        assertThat(VirtualizationDetector.matchCpuid("GenuineIntel", props), is(Optional.empty()));
        assertThat(VirtualizationDetector.matchCpuid("", props), is(Optional.empty()));
    }

    @Test
    void testMatchSystemAgainstRealDmiStrings() {
        List<Pair<String, String>> table = VirtualizationDetector.buildModelTable(shippedVmProps());
        // Manufacturer is part of the haystack, so a platform that names itself only there still matches
        assertThat(VirtualizationDetector.matchSystem("QEMU", "Standard PC (i440FX + PIIX, 1996)", table),
                is(Optional.of("QEMU")));
        assertThat(VirtualizationDetector.matchSystem("innotek GmbH", "VirtualBox", table),
                is(Optional.of("VirtualBox")));
        assertThat(VirtualizationDetector.matchSystem("VMware, Inc.", "VMware Virtual Platform", table),
                is(Optional.of("VMware")));
        assertThat(VirtualizationDetector.matchSystem("Microsoft Corporation", "Virtual Machine", table),
                is(Optional.of("Microsoft Hyper-V")));
        assertThat(VirtualizationDetector.matchSystem("Xen", "HVM domU", table), is(Optional.of("Xen")));
        // Apple Virtualization guest, as reported by the Apple Silicon GitHub Actions runners
        assertThat(VirtualizationDetector.matchSystem("Apple Inc.", "VirtualMac2,1", table),
                is(Optional.of("Apple Virtualization")));
        // The Hyper-V model carries a UEFI version suffix on Azure Linux, so this cannot be an equality test
        assertThat(
                VirtualizationDetector.matchSystem("Microsoft Corporation",
                        "Virtual Machine (version: Hyper-V UEFI Release v4.1)", table),
                is(Optional.of("Microsoft Hyper-V")));
        assertThat(VirtualizationDetector.matchSystem("Parallels Software International Inc.",
                "Parallels Virtual" + " Platform", table), is(Optional.of("Parallels")));
        // Physical hardware matches nothing
        assertThat(VirtualizationDetector.matchSystem("Dell Inc.", "PowerEdge R740", table), is(Optional.empty()));
        assertThat(VirtualizationDetector.matchSystem("Apple Inc.", "MacBookPro18,3", table), is(Optional.empty()));
        assertThat(VirtualizationDetector.matchSystem("Apple Inc.", "Macmini6,2", table), is(Optional.empty()));
        assertThat(VirtualizationDetector.matchSystem("", "", table), is(Optional.empty()));
    }

    @Test
    void testMatchSystemPrefersTheLongerSignature() {
        Properties props = new Properties();
        props.setProperty("model.Xen", "Xen");
        props.setProperty("model.Xen HVM domU", "Xen HVM");
        assertThat(VirtualizationDetector.matchSystem("Xen", "HVM domU", VirtualizationDetector.buildModelTable(props)),
                is(Optional.of("Xen HVM")));

        // Insertion order must not matter, since Properties iteration is unordered
        Properties reversed = new Properties();
        reversed.setProperty("model.Xen HVM domU", "Xen HVM");
        reversed.setProperty("model.Xen", "Xen");
        assertThat(
                VirtualizationDetector.matchSystem("Xen", "HVM domU", VirtualizationDetector.buildModelTable(reversed)),
                is(Optional.of("Xen HVM")));
    }

    @Test
    void testExtractOui() {
        assertThat(VirtualizationDetector.extractOui("08:00:27:aa:bb:cc"), is("08:00:27"));
        assertThat(VirtualizationDetector.extractOui("08-00-27-AA-BB-CC"), is("08:00:27"));
        assertThat(VirtualizationDetector.extractOui("080027aabbcc"), is("08:00:27"));
        assertThat(VirtualizationDetector.extractOui("00:50:56"), is("00:50:56"));
        // Constants.UNKNOWN and other unparseable values yield no OUI rather than throwing
        assertThat(VirtualizationDetector.extractOui(Constants.UNKNOWN), is(emptyString()));
        assertThat(VirtualizationDetector.extractOui(""), is(emptyString()));
        assertThat(VirtualizationDetector.extractOui("00:50"), is(emptyString()));
        assertThat(VirtualizationDetector.extractOui("zz:zz:zz:zz:zz:zz"), is(emptyString()));
    }

    @Test
    void testMatchMac() {
        Properties props = shippedMacProps();
        assertThat(VirtualizationDetector.matchMac(List.of("08:00:27:12:34:56"), props), is(Optional.of("VirtualBox")));
        // The library formats MAC addresses in lowercase; the table is keyed uppercase
        assertThat(VirtualizationDetector.matchMac(List.of("00:15:5d:aa:bb:cc"), props),
                is(Optional.of("Microsoft Hyper-V")));
        assertThat(VirtualizationDetector.matchMac(List.of("42:01:0a:80:00:01"), props),
                is(Optional.of("Google Cloud Platform")));
        // The first matching address wins, and unparseable entries are skipped rather than throwing
        assertThat(VirtualizationDetector.matchMac(List.of(Constants.UNKNOWN, "3c:22:fb:11:22:33", "08:00:27:12:34:56"),
                props), is(Optional.of("VirtualBox")));
        assertThat(VirtualizationDetector.matchMac(List.of("3c:22:fb:11:22:33"), props), is(Optional.empty()));
        assertThat(VirtualizationDetector.matchMac(Collections.<String>emptyList(), props), is(Optional.empty()));
    }
}
