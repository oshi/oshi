/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.ApplicationInfo;

class SolarisInstalledAppsTest {

    @Test
    void testParsePkgList() {
        // Solaris 11.4 SPARC (cfarm216), then OpenIndiana from a vmactions VM
        List<String> lines = """
                pkg://solaris/archiver/gnu-tar@1.35-11.4.95.0.1.227.0:20260810T173115Z       i--
                pkg://solaris/compliance/benchmark/ehc-solaris-policy@11.4-11.4.96.0.1.227.1:20261001T032447Z i--
                pkg://openindiana.org/SUNWcs@0.5.11-2026.0.0.23349:20261101T115717Z          i--
                pkg://openindiana.org/consolidation/userland/userland-incorporation@0.5.11-2026.0.0.35309:20261103T012511Z i--
                """
                .lines().toList();
        List<ApplicationInfo> apps = SolarisInstalledApps.parsePkgList(lines);
        assertThat(apps, hasSize(4));
        ApplicationInfo tar = apps.get(0);
        assertThat(tar.getName(), is("archiver/gnu-tar"));
        assertThat(tar.getVersion(), is("1.35-11.4.95.0.1.227.0"));
        assertThat(tar.getVendor(), is("solaris"));
        // 2026-08-10T17:31:15Z
        assertThat(tar.getTimestamp(), is(1_786_383_075_000L));
        assertThat(tar.getAdditionalInfo().get("state"), is("i--"));
        assertThat(tar.getAdditionalInfo().get("fmri"),
                is("pkg://solaris/archiver/gnu-tar@1.35-11.4.95.0.1.227.0:20260810T173115Z"));
        assertThat(apps.get(2).getName(), is("SUNWcs"));
        assertThat(apps.get(2).getVendor(), is("openindiana.org"));
        assertThat(apps.get(3).getName(), is("consolidation/userland/userland-incorporation"));
    }

    @Test
    void testParsePkgListMalformed() {
        assertThat(SolarisInstalledApps.parsePkgList(List.of("pkg list: no packages installed", "")), is(empty()));
        assertThat(SolarisInstalledApps.parsePkgList(Collections.emptyList()), is(empty()));
    }
}
