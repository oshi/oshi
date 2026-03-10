/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.jna.platform.windows.WindowsDxgi;

class WindowsDxgiTest {

    // -------------------------------------------------------------------------
    // Unit tests for findMatch — platform-independent
    // -------------------------------------------------------------------------

    @Test
    void testFindMatchByVendorAndDeviceId() {
        DxgiAdapterInfo arc = new DxgiAdapterInfo("Intel(R) Arc(TM) A770 Graphics", 0x8086, 0x56A0,
                16L * 1024 * 1024 * 1024);
        DxgiAdapterInfo igpu = new DxgiAdapterInfo("Intel(R) UHD Graphics 770", 0x8086, 0x4680, 128L * 1024 * 1024);
        List<DxgiAdapterInfo> adapters = Arrays.asList(arc, igpu);

        DxgiAdapterInfo match = WindowsDxgi.findMatch(adapters, 0x8086, 0x56A0, "Intel Arc A770");
        assertThat("Should match Arc A770 by vendor+device ID", match, is(notNullValue()));
        assertThat(match.getDedicatedVideoMemory(), is(16L * 1024 * 1024 * 1024));
    }

    @Test
    void testFindMatchByVendorAndDeviceIdPicksFirst() {
        // Two adapters with same IDs (multi-GPU) — first one wins
        DxgiAdapterInfo first = new DxgiAdapterInfo("NVIDIA GeForce RTX 4090", 0x10DE, 0x2684,
                24L * 1024 * 1024 * 1024);
        DxgiAdapterInfo second = new DxgiAdapterInfo("NVIDIA GeForce RTX 4090", 0x10DE, 0x2684,
                24L * 1024 * 1024 * 1024);
        List<DxgiAdapterInfo> adapters = Arrays.asList(first, second);

        DxgiAdapterInfo match = WindowsDxgi.findMatch(adapters, 0x10DE, 0x2684, "NVIDIA GeForce RTX 4090");
        assertThat(match, is(first));
    }

    @Test
    void testFindMatchByNormalizedName() {
        // No vendor/device IDs available — fall back to name matching
        DxgiAdapterInfo adapter = new DxgiAdapterInfo("Intel(R) Arc(TM) A770 Graphics", 0x8086, 0x56A0,
                16L * 1024 * 1024 * 1024);
        List<DxgiAdapterInfo> adapters = Collections.singletonList(adapter);

        // Registry name may differ slightly in (R)/(TM) decoration
        DxgiAdapterInfo match = WindowsDxgi.findMatch(adapters, 0, 0, "Intel(R) Arc(TM) A770 Graphics");
        assertThat("Should match by normalized name", match, is(notNullValue()));
        assertThat(match.getDedicatedVideoMemory(), is(16L * 1024 * 1024 * 1024));
    }

    @Test
    void testFindMatchNoMatchReturnsNull() {
        DxgiAdapterInfo adapter = new DxgiAdapterInfo("AMD Radeon RX 7900 XTX", 0x1002, 0x744C,
                24L * 1024 * 1024 * 1024);
        List<DxgiAdapterInfo> adapters = Collections.singletonList(adapter);

        DxgiAdapterInfo match = WindowsDxgi.findMatch(adapters, 0x8086, 0x56A0, "Intel Arc A770");
        assertThat("Different vendor+device should not match", match, is(nullValue()));
    }

    @Test
    void testFindMatchEmptyListReturnsNull() {
        DxgiAdapterInfo match = WindowsDxgi.findMatch(Collections.emptyList(), 0x8086, 0x56A0, "Intel Arc A770");
        assertThat(match, is(nullValue()));
    }

    @Test
    void testFindMatchZeroIdsUsesNameFallback() {
        DxgiAdapterInfo adapter = new DxgiAdapterInfo("AMD Radeon RX 7900 XTX", 0x1002, 0x744C,
                24L * 1024 * 1024 * 1024);
        List<DxgiAdapterInfo> adapters = Collections.singletonList(adapter);

        // vendorId=0 and deviceId=0 should skip ID matching and try name
        DxgiAdapterInfo match = WindowsDxgi.findMatch(adapters, 0, 0, "AMD Radeon RX 7900 XTX");
        assertThat("Should fall through to name match when IDs are zero", match, is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // Unit tests for normalizeName
    // -------------------------------------------------------------------------

    @Test
    void testNormalizeNameStripsTrademarks() {
        assertThat(WindowsDxgi.normalizeName("Intel(R) Arc(TM) A770 Graphics"), is("intel arc a770 graphics"));
    }

    @Test
    void testNormalizeNameCollapsesWhitespace() {
        assertThat(WindowsDxgi.normalizeName("NVIDIA  GeForce   RTX  4090"), is("nvidia geforce rtx 4090"));
    }

    @Test
    void testNormalizeNameNull() {
        assertThat(WindowsDxgi.normalizeName(null), is(""));
    }

    // -------------------------------------------------------------------------
    // Unit tests for registryValueToVram
    // -------------------------------------------------------------------------

    @Test
    void testRegistryValueToVramLong() {
        long expected = 16L * 1024 * 1024 * 1024;
        assertThat(WindowsDxgi.registryValueToVram(expected), is(expected));
    }

    @Test
    void testRegistryValueToVramInteger() {
        // 0x80000000 would overflow signed int; test unsigned widening
        assertThat(WindowsDxgi.registryValueToVram(0x80000000), is(0x80000000L));
    }

    @Test
    void testRegistryValueToVramByteArrayLittleEndian() {
        // 16 GB = 0x0000000400000000 in little-endian bytes
        byte[] bytes = { 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00 };
        assertThat(WindowsDxgi.registryValueToVram(bytes), is(0x0000000400000000L));
    }

    @Test
    void testRegistryValueToVramNull() {
        assertThat(WindowsDxgi.registryValueToVram(null), is(0L));
    }

    // -------------------------------------------------------------------------
    // Windows-only integration test
    // -------------------------------------------------------------------------

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testQueryAdaptersOnWindows() {
        List<DxgiAdapterInfo> adapters = WindowsDxgi.queryAdapters();
        // On any Windows system with a display, there should be at least one adapter
        assertThat("Should find at least one DXGI adapter", adapters.size(), is(greaterThan(0)));
        for (DxgiAdapterInfo a : adapters) {
            assertThat("Description should not be null", a.getDescription(), is(notNullValue()));
            assertThat("VendorId should be positive", a.getVendorId(), is(greaterThan(0)));
            assertThat("DedicatedVideoMemory should be non-negative", a.getDedicatedVideoMemory(),
                    is(greaterThanOrEqualTo(0L)));
        }
    }
}
