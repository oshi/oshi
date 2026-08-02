/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSService;

class AixOperatingSystemTest {

    @Test
    void testParseServices() {
        // lssrc -a: header row, active subsystems (with/without a Group column), and an inoperative one
        List<OSService> services = AixOperatingSystem.parseServices(Arrays.asList(//
                "Subsystem         Group            PID          Status", //
                " syslogd          ras              4194320      active", //
                " inetd            5218374          active", // 3-token active form (no group column)
                " portmap          portmap                       inoperative"));
        Map<String, OSService> byName = services.stream()
                .collect(Collectors.toMap(OSService::getName, Function.identity()));

        assertThat(byName.get("syslogd").getProcessID(), is(4194320));
        assertThat(byName.get("syslogd").getState(), is(OSService.State.RUNNING));
        assertThat(byName.get("inetd").getProcessID(), is(5218374));
        assertThat(byName.get("inetd").getState(), is(OSService.State.RUNNING));
        assertThat(byName.get("portmap").getProcessID(), is(0));
        assertThat(byName.get("portmap").getState(), is(OSService.State.STOPPED));
    }

    @Test
    void testParseServicesHeaderOnlyOrEmpty() {
        assertThat(AixOperatingSystem.parseServices(Collections.singletonList("Subsystem Group PID Status")),
                is(empty()));
        assertThat(AixOperatingSystem.parseServices(Collections.emptyList()), is(empty()));
    }

    @Test
    void testResolveBootTimeMillis() {
        long now = 1_600_000_000_000L;
        long whoBootTime = 1_500_000_000_000L;
        long upTime = 10_000_000L;

        // The up time duration is unambiguous, so it wins even when who -b also parsed
        assertThat(AixOperatingSystem.resolveBootTimeMillis(whoBootTime, upTime, now), is(now - upTime));
        assertThat(AixOperatingSystem.resolveBootTimeMillis(0L, upTime, now), is(now - upTime));
        // who -b is consulted only when the uptime command gave nothing
        assertThat(AixOperatingSystem.resolveBootTimeMillis(whoBootTime, 0L, now), is(whoBootTime));
    }

    @Test
    void testResolveBootTimeMillisTreatsZeroUpTimeAsUnknown() {
        long now = 1_600_000_000_000L;
        // who -b unparseable (its AIX output carries no year) and the uptime command failed. Reporting now - 0 would
        // claim the system booted this instant and pin uptime to zero, so both failing must yield "unknown" instead.
        assertThat(AixOperatingSystem.resolveBootTimeMillis(0L, 0L, now), is(0L));
        // A who -b value below the 1000 ms floor is treated as unusable rather than as a boot time in 1970
        assertThat(AixOperatingSystem.resolveBootTimeMillis(999L, 0L, now), is(0L));
    }
}
