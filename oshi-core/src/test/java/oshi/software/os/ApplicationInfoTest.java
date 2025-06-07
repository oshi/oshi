/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        additionalInfo = new LinkedHashMap<>();
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

    @Test
    public void testEqualsAndHashCode_sameValues_shouldBeEqual() {
        Map<String, String> info1 = new LinkedHashMap<>();
        info1.put("installLocation", null);
        info1.put("installSource",
                "C:\\ProgramData\\Package Cache\\{FE8C7838-D3E6-4CEA-87BE-216E42391827}v20.2.37.0\\");

        ApplicationInfo app1 = new ApplicationInfo("SQL Server Management Studio", "20.2.37.0", "Microsoft Corp.",
                1746576000000L, info1);
        ApplicationInfo app2 = new ApplicationInfo("SQL Server Management Studio", "20.2.37.0", "Microsoft Corp.",
                1746576000000L, new LinkedHashMap<>(info1));

        assertEquals(app1, app2);
        assertEquals(app1.hashCode(), app2.hashCode());
    }

    @Test
    public void testEqualsAndHashCode_differentVersion_shouldNotBeEqual() {
        ApplicationInfo app1 = new ApplicationInfo("SQL Server Management Studio", "20.2.37.0", "Microsoft Corp.",
                1746576000000L, new LinkedHashMap<>());
        ApplicationInfo app2 = new ApplicationInfo("SQL Server Management Studio", "20.3.37.0", "Microsoft Corp.",
                1746576000000L, new LinkedHashMap<>());

        assertNotEquals(app1, app2);
    }

    @Test
    public void testSetBehavior() {
        Set<ApplicationInfo> set = new LinkedHashSet<>();
        ApplicationInfo app = new ApplicationInfo("SQL Server Management Studio", "20.3.37.0", "Microsoft Corp.",
                1746576000000L, new LinkedHashMap<>());
        set.add(app);

        // Should not add duplicate
        assertFalse(set.add(new ApplicationInfo("SQL Server Management Studio", "20.3.37.0", "Microsoft Corp.",
                1746576000000L, new LinkedHashMap<>())));
        assertEquals(1, set.size());
    }

}
