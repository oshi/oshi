/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ApplicationInfoTest {

    private ApplicationInfo appInfo;
    private final String name = "zstd";
    private final String version = "1.5.5+dfsg2-2build1";
    private final String vendor = "Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>";
    private final long timestamp = 1713886075L;
    private Map<String, String> additionalInfo;

    @BeforeEach
    void setUp() {
        additionalInfo = new HashMap<>();
        additionalInfo.put("source", "libzstd");
        additionalInfo.put("installedSize", "1798");
        additionalInfo.put("architecture", "amd64");
        additionalInfo.put("homepage", "https://github.com/facebook/zstd");

        appInfo = new ApplicationInfo(name, version, vendor, timestamp, additionalInfo);
    }

    @Test
    void testGetName() {
        assertEquals(name, appInfo.getName(), "Application name should match");
    }

    @Test
    void testGetVersion() {
        assertEquals(version, appInfo.getVersion(), "Version should match");
    }

    @Test
    void testGetVendor() {
        assertEquals(vendor, appInfo.getVendor(), "Vendor should match");
    }

    @Test
    void testGetTimestamp() {
        assertEquals(timestamp, appInfo.getTimestamp(), "Timestamp should match");
    }

    @Test
    void testGetAdditionalInfo() {
        assertNotNull(appInfo.getAdditionalInfo(), "Additional info map should not be null");
        assertEquals("libzstd", appInfo.getAdditionalInfo().get("source"));
        assertEquals("1798", appInfo.getAdditionalInfo().get("installedSize"));
        assertEquals("amd64", appInfo.getAdditionalInfo().get("architecture"));
        assertEquals("https://github.com/facebook/zstd", appInfo.getAdditionalInfo().get("homepage"));
    }

    @Test
    void testImmutableAdditionalInfo() {
        // Ensure modifying the original map doesn't affect the stored map
        additionalInfo.put("NewKey", "NewValue");
        assertFalse(appInfo.getAdditionalInfo().containsKey("NewKey"), "Stored map should remain unchanged");
    }
}
