/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.software.os.ApplicationInfo;

class PkgInfoInstalledAppsTest {

    @Test
    void testParsePkgInfoOpenBsd() {
        // Captured on a vmactions OpenBSD 7.9 VM, then a constructed flavored vim line, per packages-specs(7)
        List<String> lines = """
                amd-firmware-20250708 microcode update binaries for AMD CPUs
                bzip2-1.0.8p0       block-sorting file compressor, unencumbered
                gettext-runtime-1.0 GNU gettext runtime libraries and programs
                libb2-0.98.1v0      library providing BLAKE2b, BLAKE2s, BLAKE2bp, BLAKE2sp
                py3-cryptography-46.0.7 cryptographic recipes and primitives for Python
                updatedb-0p0        pkg_add speed up cache
                vim-9.1.1-no_x11    vi clone, many additional features
                """.lines().toList();
        Map<String, Long> dirs = Map.of("bzip2-1.0.8p0", 1_792_461_180_000L);
        List<ApplicationInfo> apps = PkgInfoInstalledApps.parsePkgInfo(lines, true, pkg -> dirs.getOrDefault(pkg, 0L));
        assertThat(apps.stream().map(ApplicationInfo::getName).toList(),
                contains("amd-firmware", "bzip2", "gettext-runtime", "libb2", "py3-cryptography", "updatedb", "vim"));
        assertThat(apps.stream().map(ApplicationInfo::getVersion).toList(),
                contains("20250708", "1.0.8p0", "1.0", "0.98.1v0", "46.0.7", "0p0", "9.1.1-no_x11"));
        assertThat(apps.get(1).getTimestamp(), is(1_792_461_180_000L));
        assertThat(apps.get(0).getTimestamp(), is(0L));
        assertThat(apps.get(1).getAdditionalInfo().get("description"),
                is("block-sorting file compressor, unencumbered"));
        assertThat(apps.get(1).getVendor(), is(""));
    }

    @Test
    void testParsePkgInfoNetBsd() {
        // Captured on a vmactions NetBSD 10.1 VM, then a constructed font-adobe-100dpi line
        List<String> lines = """
                glib2-2.88.3nb1     Some useful routines for C programming (glib2)
                fuse-sshfs-3.7.5nb2 File system client based on the SFTP protocol
                libidn2-2.3.7       Convert internationalized domain names to/from ASCII Encoding
                curl-8.23.0         Client that groks URLs
                font-adobe-100dpi-1.0.4 Adobe 100dpi bitmap fonts
                """.lines().toList();
        List<ApplicationInfo> apps = PkgInfoInstalledApps.parsePkgInfo(lines, false, pkg -> 0L);
        // pkgsrc versions never hold a dash, so a name may hold a dash followed by a digit
        assertThat(apps.stream().map(ApplicationInfo::getName).toList(),
                contains("glib2", "fuse-sshfs", "libidn2", "curl", "font-adobe-100dpi"));
        assertThat(apps.stream().map(ApplicationInfo::getVersion).toList(),
                contains("2.88.3nb1", "3.7.5nb2", "2.3.7", "8.23.0", "1.0.4"));
        assertThat(apps.get(3).getAdditionalInfo().get("description"), is("Client that groks URLs"));
    }

    @Test
    void testParsePkgInfoEmpty() {
        assertThat(PkgInfoInstalledApps.parsePkgInfo(Collections.emptyList(), true, pkg -> 0L), is(empty()));
        assertThat(PkgInfoInstalledApps.parsePkgInfo(List.of("", "   "), false, pkg -> 0L), is(empty()));
    }
}
