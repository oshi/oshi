/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSService;
import oshi.software.os.OSService.State;

class SolarisOperatingSystemTest {

    @Test
    void testParseSvcsTypical() {
        List<String> svcs = Arrays.asList("online         23:56:25 svc:/system/svc/restarter:default",
                "                        23:56:24       13 svc.startd",
                "legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd",
                "online         23:56:25 svc:/network/loopback:default");
        List<String> legacySvcs = Arrays.asList("S47pppd", "S89PRESERVE");

        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, legacySvcs);

        // "online" lines produce STOPPED services (named from svc path)
        // lines starting with space produce RUNNING services (with PID)
        // "legacy_run" lines matching legacySvcs produce STOPPED services
        assertThat(services, hasSize(4));

        // First: online svc:/system/svc/restarter:default -> name "/system/svc/restarter", state STOPPED
        assertThat(services.get(0).getName(), is("/system/svc/restarter"));
        assertThat(services.get(0).getState(), is(State.STOPPED));

        // Second: space-prefixed line with pid 13
        assertThat(services.get(1).getName(), is("svc.startd"));
        assertThat(services.get(1).getProcessID(), is(13));
        assertThat(services.get(1).getState(), is(State.RUNNING));

        // Third: legacy_run matching S47pppd
        assertThat(services.get(2).getName(), is("S47pppd"));
        assertThat(services.get(2).getState(), is(State.STOPPED));

        // Fourth: online svc:/network/loopback:default -> name "/network/loopback"
        assertThat(services.get(3).getName(), is("/network/loopback"));
        assertThat(services.get(3).getState(), is(State.STOPPED));
    }

    @Test
    void testParseSvcsEmpty() {
        List<OSService> services = SolarisOperatingSystem.parseSvcs(Collections.emptyList(), Collections.emptyList());
        assertThat(services, is(empty()));
    }

    @Test
    void testParseSvcsLegacyNoMatch() {
        List<String> svcs = Arrays.asList("legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd");
        List<String> legacySvcs = Arrays.asList("S89PRESERVE");

        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, legacySvcs);
        assertThat(services, is(empty()));
    }

    @Test
    void testParseSvcsOnlineWithoutDefaultSuffix() {
        List<String> svcs = Arrays.asList("online         23:56:25 svc:/system/manifest-import:refresh");
        List<OSService> services = SolarisOperatingSystem.parseSvcs(svcs, Collections.emptyList());
        assertThat(services, hasSize(1));
        // No :default suffix, so name includes everything after the last :/
        assertThat(services.get(0).getName(), is("/system/manifest-import:refresh"));
    }
}
