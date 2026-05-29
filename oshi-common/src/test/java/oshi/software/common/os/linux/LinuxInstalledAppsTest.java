/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.ApplicationInfo;

@EnabledOnOs(OS.LINUX)
class LinuxInstalledAppsTest {

    @Test
    void testValidDpkgLineWithAllFields() {
        List<String> lines = Collections
                .singletonList("firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(1));
        ApplicationInfo app = result.get(0);
        assertThat(app.getName(), is("firefox"));
        assertThat(app.getVersion(), is("120.0"));
        assertThat(app.getVendor(), is("Mozilla Team"));
        assertThat(app.getTimestamp(), is(1700000000L));
        assertThat(app.getAdditionalInfo().get("architecture"), is("amd64"));
        assertThat(app.getAdditionalInfo().get("installedSize"), is("250000"));
        assertThat(app.getAdditionalInfo().get("source"), is("firefox-source"));
        assertThat(app.getAdditionalInfo().get("homepage"), is("https://firefox.com"));
    }

    @Test
    void testMultipleValidLines() {
        List<String> lines = Arrays.asList(
                "firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com",
                "vim|9.0|amd64|3500|1690000000|Vim Maintainers|vim-source|https://vim.org");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getName(), is("firefox"));
        assertThat(result.get(1).getName(), is("vim"));
    }

    @Test
    void testLineWithFewerThanEightFieldsIsSkipped() {
        List<String> lines = Arrays.asList("firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source",
                "vim|9.0|amd64|3500|1690000000|Vim Maintainers|vim-source|https://vim.org");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getName(), is("vim"));
    }

    @Test
    void testLineWithEmptyFieldsUsesUnknown() {
        List<String> lines = Collections.singletonList("|||0|||| ");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(1));
        ApplicationInfo app = result.get(0);
        assertThat(app.getName(), is("unknown"));
        assertThat(app.getVersion(), is("unknown"));
        assertThat(app.getVendor(), is("unknown"));
        assertThat(app.getTimestamp(), is(0L));
        assertThat(app.getAdditionalInfo().get("architecture"), is("unknown"));
        assertThat(app.getAdditionalInfo().get("installedSize"), is("0"));
        assertThat(app.getAdditionalInfo().get("source"), is("unknown"));
    }

    @Test
    void testEmptyListReturnsEmptyResult() {
        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(Collections.emptyList());

        assertThat(result, is(empty()));
    }

    @Test
    void testDuplicateEntriesAreDeduplicated() {
        List<String> lines = Arrays.asList(
                "firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com",
                "firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getName(), is("firefox"));
    }
}
