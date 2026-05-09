/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MacInstalledAppsTest {

    // Fixture: minimal system_profiler -xml SPApplicationsDataType output
    private static final String PLIST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<plist version=\"1.0\"><array><dict>" + "<key>_items</key><array>" + "<dict>"
            + "<key>_name</key><string>Safari</string>" + "<key>version</key><string>17.0</string>"
            + "<key>obtained_from</key><string>apple</string>"
            + "<key>path</key><string>/Applications/Safari.app</string>"
            + "<key>lastModified</key><date>2024-01-15T10:30:00Z</date>"
            + "<key>arch_kind</key><string>arch_arm_i64</string>"
            + "<key>signed_by</key><array><string>Apple Root CA</string></array>" + "</dict>" + "<dict>"
            + "<key>_name</key><string>Firefox</string>" + "<key>version</key><string>120.0</string>"
            + "<key>obtained_from</key><string>identified_developer</string>"
            + "<key>path</key><string>/Applications/Firefox.app</string>"
            + "<key>signed_by</key><array><string>Developer ID Application: Mozilla Corporation</string></array>"
            + "</dict>" + "</array></dict></array></plist>";

    // --- parseItems ---

    @Test
    void testParseItemsValidPlist() {
        List<Map<String, String>> items = MacInstalledApps.parseItems(PLIST_XML);
        assertThat(items, hasSize(2));
        assertThat(items.get(0), hasEntry("_name", "Safari"));
        assertThat(items.get(0), hasEntry("version", "17.0"));
        assertThat(items.get(0), hasEntry("obtained_from", "apple"));
        assertThat(items.get(0), hasEntry("path", "/Applications/Safari.app"));
        assertThat(items.get(1), hasEntry("_name", "Firefox"));
        assertThat(items.get(1), hasEntry("version", "120.0"));
    }

    @Test
    void testParseItemsNull() {
        assertThat(MacInstalledApps.parseItems(null), is(empty()));
    }

    @Test
    void testParseItemsNoItemsKey() {
        String xml = "<plist><dict><key>other</key><string>value</string></dict></plist>";
        assertThat(MacInstalledApps.parseItems(xml), is(empty()));
    }

    @Test
    void testParseItemsNoArray() {
        String xml = "<key>_items</key><string>not an array</string>";
        assertThat(MacInstalledApps.parseItems(xml), is(empty()));
    }

    @Test
    void testParseItemsEmptyArray() {
        String xml = "<key>_items</key><array></array>";
        assertThat(MacInstalledApps.parseItems(xml), is(empty()));
    }

    // --- parseDict ---

    @Test
    void testParseDictStringValues() {
        String dictInner = "<key>name</key><string>MyApp</string>" + "<key>version</key><string>1.0</string>";
        Map<String, String> result = MacInstalledApps.parseDict(dictInner);
        assertThat(result, hasEntry("name", "MyApp"));
        assertThat(result, hasEntry("version", "1.0"));
    }

    @Test
    void testParseDictDateValue() {
        String dictInner = "<key>lastModified</key><date>2024-06-15T08:00:00Z</date>";
        Map<String, String> result = MacInstalledApps.parseDict(dictInner);
        assertThat(result, hasEntry("lastModified", "2024-06-15T08:00:00Z"));
    }

    @Test
    void testParseDictArrayValue() {
        String dictInner = "<key>signed_by</key><array><string>Apple Root CA</string></array>";
        Map<String, String> result = MacInstalledApps.parseDict(dictInner);
        assertThat(result, hasEntry("signed_by", "Apple Root CA"));
    }

    @Test
    void testParseDictEmptyArray() {
        String dictInner = "<key>signed_by</key><array></array>";
        Map<String, String> result = MacInstalledApps.parseDict(dictInner);
        assertThat(result, hasEntry("signed_by", ""));
    }

    @Test
    void testParseDictSkipsUnknownTags() {
        String dictInner = "<key>count</key><integer>42</integer>" + "<key>name</key><string>App</string>";
        Map<String, String> result = MacInstalledApps.parseDict(dictInner);
        // integer tag is skipped, string is parsed
        assertThat(result, hasEntry("name", "App"));
        assertThat(result.containsKey("count"), is(false));
    }

    @Test
    void testParseDictEmpty() {
        Map<String, String> result = MacInstalledApps.parseDict("");
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void testParseDictWithEscapedEntities() {
        String dictInner = "<key>name</key><string>Tom &amp; Jerry&apos;s App</string>";
        Map<String, String> result = MacInstalledApps.parseDict(dictInner);
        assertThat(result, hasEntry("name", "Tom & Jerry's App"));
    }

    // --- parseStringArray ---

    @Test
    void testParseStringArraySingleElement() {
        assertThat(MacInstalledApps.parseStringArray("<string>Hello</string>"), is("Hello"));
    }

    @Test
    void testParseStringArrayEmpty() {
        assertThat(MacInstalledApps.parseStringArray(""), is(nullValue()));
    }

    @Test
    void testParseStringArrayNoStringTag() {
        assertThat(MacInstalledApps.parseStringArray("<integer>42</integer>"), is(nullValue()));
    }

    // --- unescape ---

    @Test
    void testUnescapeAllEntities() {
        assertThat(MacInstalledApps.unescape("&amp;&lt;&gt;&quot;&apos;"), is("&<>\"'"));
    }

    @Test
    void testUnescapeNoEntities() {
        assertThat(MacInstalledApps.unescape("plain text"), is("plain text"));
    }

    @Test
    void testUnescapeMixed() {
        assertThat(MacInstalledApps.unescape("a &amp; b &lt; c"), is("a & b < c"));
    }
}
