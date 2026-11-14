/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.ApplicationInfo;

class FreeBsdInstalledAppsTest {

    @Test
    void testParsePkgQuery() {
        // cfarm430 (FreeBSD 16.0-CURRENT, pkgbase), cfarm152 (DragonFly 6.4), then a comment holding the separator
        List<String> lines = """
                FreeBSD-acct|16.snap20261103180419|1794221260|base/FreeBSD-acct|116347|FreeBSD:16:amd64|https://www.FreeBSD.org|re@FreeBSD.org|System resource accounting
                autoconf-switch|20220527|1773585777|devel/autoconf-switch|524|dragonfly:6.4:*|UNKNOWN|tijl@FreeBSD.org|Wrapper script to switch between autoconf versions
                foo|1.0|1700000000|misc/foo|10|FreeBSD:14:amd64|https://example.org|me@example.org|pipes | in | comments
                """
                .lines().toList();
        List<ApplicationInfo> apps = FreeBsdInstalledApps.parsePkgQuery(lines);
        assertThat(apps, hasSize(3));
        ApplicationInfo acct = apps.get(0);
        assertThat(acct.getName(), is("FreeBSD-acct"));
        assertThat(acct.getVersion(), is("16.snap20261103180419"));
        assertThat(acct.getVendor(), is("re@FreeBSD.org"));
        assertThat(acct.getTimestamp(), is(1_794_221_260_000L));
        assertThat(acct.getAdditionalInfo().get("architecture"), is("FreeBSD:16:amd64"));
        assertThat(acct.getAdditionalInfo().get("installedSize"), is("116347"));
        assertThat(acct.getAdditionalInfo().get("source"), is("base/FreeBSD-acct"));
        assertThat(acct.getAdditionalInfo().get("homepage"), is("https://www.FreeBSD.org"));
        assertThat(acct.getAdditionalInfo().get("description"), is("System resource accounting"));
        assertThat(apps.get(1).getAdditionalInfo().get("homepage"), is("UNKNOWN"));
        assertThat(apps.get(2).getAdditionalInfo().get("description"), is("pipes | in | comments"));
    }

    @Test
    void testParsePkgQueryMalformed() {
        assertThat(FreeBsdInstalledApps.parsePkgQuery(List.of("pkg: not found", "", "a|b|c")), is(empty()));
        assertThat(FreeBsdInstalledApps.parsePkgQuery(Collections.emptyList()), is(empty()));
    }
}
