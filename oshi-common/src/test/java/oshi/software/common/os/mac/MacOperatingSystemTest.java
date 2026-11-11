/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.OSService;
import oshi.software.os.OSService.State;

class MacOperatingSystemTest {

    @Test
    void testResolveVersionUsesSwVersWhenNonEmpty() {
        assertThat(MacOperatingSystem.resolveVersion("10.16", "11.2.3"), is("11.2.3"));
    }

    @Test
    void testResolveVersionFallsBackWhenSwVersEmpty() {
        assertThat(MacOperatingSystem.resolveVersion("10.16", ""), is("10.16"));
    }

    @Test
    void testParseLaunchctlPrint() {
        List<String> lines = """
                system = {
                \ttype = system
                \tservices = {
                \t\t       0      - \tcom.apple.rpmuxd
                \t\t   72143      - \tcom.apple.lskdd
                \t\t   63915   (pe) \tcom.apple.kernelmanager_helper
                \t\t       0     78 \tcom.apple.wifiFirmwareLoader
                \t}
                \tendpoints = {
                \t\t       0      - \tcom.apple.not.a.service
                \t}
                }
                """.lines().toList();
        List<OSService> services = MacOperatingSystem.parseLaunchctlPrint(lines);
        assertThat(services.stream().map(OSService::getName).toList(), contains("com.apple.rpmuxd", "com.apple.lskdd",
                "com.apple.kernelmanager_helper", "com.apple.wifiFirmwareLoader"));
        assertThat(services.stream().map(OSService::getProcessID).toList(), contains(0, 72143, 63915, 0));
        assertThat(services.stream().map(OSService::getState).toList(),
                contains(State.STOPPED, State.RUNNING, State.RUNNING, State.STOPPED));
    }

    @Test
    void testParseLaunchctlList() {
        List<String> lines = """
                PID\tStatus\tLabel
                -\t0\tcom.apple.SafariHistoryServiceAgent
                95678\t-9\tcom.apple.progressd
                851\t0\tapplication.com.apple.Preview.1152921500311898404.1152921500311898409
                """.lines().toList();
        List<OSService> services = MacOperatingSystem.parseLaunchctlList(lines);
        assertThat(services.stream().map(OSService::getName).toList(),
                contains("com.apple.SafariHistoryServiceAgent", "com.apple.progressd"));
        assertThat(services.stream().map(OSService::getProcessID).toList(), contains(0, 95678));
        assertThat(services.stream().map(OSService::getState).toList(), contains(State.STOPPED, State.RUNNING));
    }
}
