/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.gpu;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DxgiUtil} PCI parsing and LUID prefix building.
 */
class DxgiUtilTest {

    private static final String VALID_LOCATION = "PCI bus 3, device 0, function 0";

    @Test
    void testParsePciBusNumberValid() {
        assertThat("bus number from valid location", DxgiUtil.parsePciBusNumber(VALID_LOCATION), is(3));
    }

    @Test
    void testParsePciBusNumberNull() {
        assertThat("bus number from null", DxgiUtil.parsePciBusNumber(null), is(-1));
    }

    @Test
    void testParsePciBusNumberEmpty() {
        assertThat("bus number from empty string", DxgiUtil.parsePciBusNumber(""), is(-1));
    }

    @Test
    void testParsePciBusNumberNoPciBus() {
        assertThat("bus number with no PCI bus prefix", DxgiUtil.parsePciBusNumber("some other string"), is(-1));
    }

    @Test
    void testParsePciDeviceValid() {
        assertThat("device number from valid location", DxgiUtil.parsePciDevice(VALID_LOCATION), is(0));
    }

    @Test
    void testParsePciDeviceNull() {
        assertThat("device number from null", DxgiUtil.parsePciDevice(null), is(-1));
    }

    @Test
    void testParsePciDeviceEmpty() {
        assertThat("device number from empty string", DxgiUtil.parsePciDevice(""), is(-1));
    }

    @Test
    void testParsePciDeviceNoDeviceKeyword() {
        assertThat("device number with no device keyword", DxgiUtil.parsePciDevice("PCI bus 3, function 0"), is(-1));
    }

    @Test
    void testParsePciFunctionValid() {
        assertThat("function number from valid location", DxgiUtil.parsePciFunction(VALID_LOCATION), is(0));
    }

    @Test
    void testParsePciFunctionNull() {
        assertThat("function number from null", DxgiUtil.parsePciFunction(null), is(-1));
    }

    @Test
    void testParsePciFunctionEmpty() {
        assertThat("function number from empty string", DxgiUtil.parsePciFunction(""), is(-1));
    }

    @Test
    void testParsePciFunctionNoFunctionKeyword() {
        assertThat("function number with no function keyword", DxgiUtil.parsePciFunction("PCI bus 3, device 0"),
                is(-1));
    }

    @Test
    void testBuildPciBusIdValid() {
        assertThat("PCI bus ID from valid location", DxgiUtil.buildPciBusId(VALID_LOCATION), is("0000:03:00.0"));
    }

    @Test
    void testBuildPciBusIdHigherNumbers() {
        String location = "PCI bus 255, device 31, function 7";
        assertThat("PCI bus ID with higher numbers", DxgiUtil.buildPciBusId(location), is("0000:ff:1f.7"));
    }

    @Test
    void testBuildPciBusIdUnparseable() {
        assertThat("PCI bus ID from unparseable input", DxgiUtil.buildPciBusId("not a valid location"), is(""));
    }

    @Test
    void testBuildPciBusIdNull() {
        assertThat("PCI bus ID from null", DxgiUtil.buildPciBusId(null), is(""));
    }

    @Test
    void testBuildLuidPrefixNonZero() {
        DxgiAdapterInfo adapter = new DxgiAdapterInfo("Test GPU", 0x10de, 0x1234, 4096L, 0x0000d3a7, 0x00000000);
        assertThat("LUID prefix for non-zero LUID", DxgiUtil.buildLuidPrefix(adapter),
                is("luid_0x00000000_0x0000d3a7_phys_0"));
    }

    @Test
    void testBuildLuidPrefixBothPartsNonZero() {
        DxgiAdapterInfo adapter = new DxgiAdapterInfo("Test GPU", 0x10de, 0x1234, 4096L, 0x0000abcd, 0x00000012);
        assertThat("LUID prefix with both parts non-zero", DxgiUtil.buildLuidPrefix(adapter),
                is("luid_0x00000012_0x0000abcd_phys_0"));
    }

    @Test
    void testBuildLuidPrefixZero() {
        DxgiAdapterInfo adapter = new DxgiAdapterInfo("Test GPU", 0x10de, 0x1234, 4096L, 0, 0);
        assertThat("LUID prefix for zero LUID", DxgiUtil.buildLuidPrefix(adapter), is(""));
    }
}
