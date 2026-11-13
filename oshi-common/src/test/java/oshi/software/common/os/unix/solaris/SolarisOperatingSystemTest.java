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

import oshi.software.os.OSService;
import oshi.software.os.OSService.State;

class SolarisOperatingSystemTest {

    @Test
    void testParseSvcsTypical() {
        // Solaris 11.4 format; illumos and older releases print a bare HH:mm:ss STIME
        List<String> svcs = """
                STATE          STIME               FMRI
                legacy_run     2026-10-20T23:59:58 lrc:/etc/rc2_d/S47pppd
                online         2026-10-20T23:59:04 svc:/system/svc/restarter:default
                               2026-10-20T23:59:04        14 svc.startd
                online         2026-10-20T23:59:05 svc:/network/loopback:default
                online         2026-10-20T23:59:11 svc:/network/ssh:default
                               2026-10-20T23:59:11       812 sshd
                               2026-11-05T21:31:58     23051 sshd
                """.lines().toList();
        List<String> legacySvcs = List.of("S47pppd", "S89PRESERVE");

        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, legacySvcs);
        assertThat(services, hasSize(4));

        assertThat(services.get(0).getName(), is("S47pppd"));
        assertThat(services.get(0).getProcessID(), is(0));
        assertThat(services.get(0).getState(), is(State.STOPPED));

        // An online service takes the PID of its process
        assertThat(services.get(1).getName(), is("/system/svc/restarter"));
        assertThat(services.get(1).getProcessID(), is(14));
        assertThat(services.get(1).getState(), is(State.RUNNING));

        // An online transient service has no process but is still running
        assertThat(services.get(2).getName(), is("/network/loopback"));
        assertThat(services.get(2).getProcessID(), is(0));
        assertThat(services.get(2).getState(), is(State.RUNNING));

        // With several processes, the first is reported
        assertThat(services.get(3).getName(), is("/network/ssh"));
        assertThat(services.get(3).getProcessID(), is(812));
        assertThat(services.get(3).getState(), is(State.RUNNING));
    }

    @Test
    void testParseSvcsBareTime() {
        List<String> svcs = """
                online         23:56:25 svc:/system/svc/restarter:default
                                        23:56:24       13 svc.startd
                """.lines().toList();
        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, Collections.emptyList());
        assertThat(services, hasSize(1));
        assertThat(services.get(0).getProcessID(), is(13));
        assertThat(services.get(0).getState(), is(State.RUNNING));
    }

    @Test
    void testParseSvcsProcessNameWithSpaces() {
        List<String> svcs = """
                online         23:56:25 svc:/network/smtp:sendmail
                                        23:56:24       13 my mail daemon
                """.lines().toList();
        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, Collections.emptyList());
        assertThat(services, hasSize(1));
        assertThat(services.get(0).getProcessID(), is(13));
    }

    @Test
    void testParseSvcsEmpty() {
        List<OSService> services = SolarisOperatingSystem.parseSvcs(Collections.emptyList(), Collections.emptyList());
        assertThat(services, is(empty()));
    }

    @Test
    void testParseSvcsLegacyNoMatch() {
        List<String> svcs = List.of("legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd");
        List<String> legacySvcs = List.of("S89PRESERVE");

        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, legacySvcs);
        assertThat(services, is(empty()));
    }

    @Test
    void testParseSvcsOnlineWithoutDefaultSuffix() {
        List<String> svcs = List.of("online         23:56:25 svc:/system/manifest-import:refresh");
        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, Collections.emptyList());
        assertThat(services, hasSize(1));
        // No :default suffix, so name includes everything after the last :/
        assertThat(services.get(0).getName(), is("/system/manifest-import:refresh"));
    }
}
