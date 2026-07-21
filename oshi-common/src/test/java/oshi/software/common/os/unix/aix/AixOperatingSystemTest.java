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
}
