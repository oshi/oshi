/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

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
        // dpkg and rpm print epoch seconds; the API is milliseconds
        assertThat(app.getTimestamp(), is(1700000000_000L));
        assertThat(app.getAdditionalInfo().get("architecture"), is("amd64"));
        assertThat(app.getAdditionalInfo().get("installedSize"), is("250000"));
        assertThat(app.getAdditionalInfo().get("source"), is("firefox-source"));
        assertThat(app.getAdditionalInfo().get("homepage"), is("https://firefox.com"));
    }

    @Test
    void testMultipleValidLines() {
        List<String> lines = List.of(
                "firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com",
                "vim|9.0|amd64|3500|1690000000|Vim Maintainers|vim-source|https://vim.org");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getName(), is("firefox"));
        assertThat(result.get(1).getName(), is("vim"));
    }

    @Test
    void testLineWithFewerThanEightFieldsIsSkipped() {
        List<String> lines = List.of("firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source",
                "vim|9.0|amd64|3500|1690000000|Vim Maintainers|vim-source|https://vim.org");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getName(), is("vim"));
    }

    @Test
    void testLineWithEmptyFieldsUsesUnknown() {
        List<String> lines = List.of("|||0|||| ");

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
        List<String> lines = List.of(
                "firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com",
                "firefox|120.0|amd64|250000|1700000000|Mozilla Team|firefox-source|https://firefox.com");

        List<ApplicationInfo> result = LinuxInstalledApps.parseLinuxAppInfo(lines);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getName(), is("firefox"));
    }

    // Trimmed from /lib/apk/db/installed on Alpine 3.24 (the eclipse-temurin:25-jdk-alpine image)
    private static final String APK_DB = """
            C:Q1abcdefghijklmnopqrstuvwxyz=
            P:alpine-baselayout-data
            V:3.7.2-r1
            A:aarch64
            S:11583
            I:19034
            T:Alpine base dir structure and init scripts
            U:https://gitlab.alpinelinux.org/alpine/aports/-/tree/master/main/alpine-baselayout
            L:GPL-2.0-only
            o:alpine-baselayout
            m:Natanael Copa <ncopa@alpinelinux.org>
            t:1774381254
            F:etc
            R:fstab

            C:Q1zyxwvutsrqponmlkjihgfedcba=
            P:musl
            V:1.2.5-r10
            A:aarch64
            I:678912
            o:musl
            m:Timo Teras <timo.teras@iki.fi>
            U:https://musl.libc.org/
            """;

    @Test
    void testParseApkInstalled() {
        List<ApplicationInfo> apps = LinuxInstalledApps.parseApkInstalled(APK_DB.lines().toList());
        assertThat(apps, hasSize(2));
        ApplicationInfo base = apps.get(0);
        assertThat(base.getName(), is("alpine-baselayout-data"));
        assertThat(base.getVersion(), is("3.7.2-r1"));
        assertThat(base.getVendor(), is("Natanael Copa <ncopa@alpinelinux.org>"));
        assertThat("apk records no install time, and t: is the build time", base.getTimestamp(), is(0L));
        assertThat(base.getAdditionalInfo().get("architecture"), is("aarch64"));
        assertThat(base.getAdditionalInfo().get("installedSize"), is("19034"));
        assertThat(base.getAdditionalInfo().get("source"), is("alpine-baselayout"));
        // The last record has no trailing blank line
        assertThat(apps.get(1).getName(), is("musl"));
        assertThat(apps.get(1).getAdditionalInfo().get("homepage"), is("https://musl.libc.org/"));
    }

    // From /var/lib/pacman/local/bash-5.3.20-1/desc in the archlinux image
    private static final String PACMAN_DESC = """
            %NAME%
            bash

            %VERSION%
            5.3.20-1

            %BASE%
            bash

            %DESC%
            The GNU Bourne Again shell

            %URL%
            https://www.gnu.org/software/bash/bash.html

            %ARCH%
            x86_64

            %BUILDDATE%
            1789501639

            %INSTALLDATE%
            1793491200

            %PACKAGER%
            Tobias Powalowski <tpowa@archlinux.org>

            %SIZE%
            10055840

            %LICENSE%
            GPL-3.0-or-later

            %DEPENDS%
            readline
            libreadline.so=8-64
            glibc
            """;

    @Test
    void testParsePacmanDesc() {
        ApplicationInfo bash = LinuxInstalledApps.parsePacmanDesc(PACMAN_DESC.lines().toList());
        assertNotNull(bash);
        assertThat(bash.getName(), is("bash"));
        assertThat(bash.getVersion(), is("5.3.20-1"));
        assertThat(bash.getVendor(), is("Tobias Powalowski <tpowa@archlinux.org>"));
        assertThat("pacman prints seconds", bash.getTimestamp(), is(1793491200_000L));
        assertThat(bash.getAdditionalInfo().get("architecture"), is("x86_64"));
        assertThat(bash.getAdditionalInfo().get("installedSize"), is("10055840"));
        assertThat(bash.getAdditionalInfo().get("homepage"), is("https://www.gnu.org/software/bash/bash.html"));
        assertThat(LinuxInstalledApps.parsePacmanDesc(List.of("%VERSION%", "1.0")), is(nullValue()));
    }

    @Test
    void testQueryPackageDatabasesApk(@TempDir Path tmp) throws IOException {
        Path installed = Files.writeString(tmp.resolve("installed"), APK_DB);
        assertThat(LinuxInstalledApps.queryPackageDatabases(installed.toString(), tmp.resolve("absent").toString()),
                is(LinuxInstalledApps.parseApkInstalled(APK_DB.lines().toList())));
    }

    @Test
    void testQueryPackageDatabasesPacman(@TempDir Path tmp) throws IOException {
        Path local = Files.createDirectories(tmp.resolve("local"));
        Files.writeString(Files.createDirectories(local.resolve("bash-5.3.20-1")).resolve("desc"), PACMAN_DESC);
        // A package directory whose desc has no name is skipped, and pacman's version file is not a package
        Files.writeString(Files.createDirectories(local.resolve("broken-1.0-1")).resolve("desc"), "%VERSION%\n1.0\n");
        Files.writeString(local.resolve("ALPM_DB_VERSION"), "9\n");
        List<ApplicationInfo> apps = LinuxInstalledApps.queryPackageDatabases(tmp.resolve("absent").toString(),
                local.toString());
        assertThat(apps, hasSize(1));
        assertThat(apps.get(0).getName(), is("bash"));
    }

    @Test
    void testQueryPackageDatabasesNeither(@TempDir Path tmp) {
        assertThat(LinuxInstalledApps.queryPackageDatabases(tmp.resolve("installed").toString(),
                tmp.resolve("local").toString()), is(empty()));
    }
}
