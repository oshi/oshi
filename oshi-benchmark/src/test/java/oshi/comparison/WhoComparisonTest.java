/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.driver.unix.freebsd.Who;
import oshi.driver.unix.freebsd.WhoFFM;
import oshi.software.os.OSSession;

/**
 * Compares JNA {@link Who} against FFM {@link WhoFFM} on FreeBSD. Both walk the utmpx database and must return the same
 * set of active sessions.
 */
@EnabledOnOs(OS.FREEBSD)
class WhoComparisonTest {

    @Test
    void queryUtxent() {
        List<OSSession> jna = Who.queryUtxent();
        List<OSSession> ffm = WhoFFM.queryUtxent();
        assertThat(jna).as("JNA Who.queryUtxent()").isNotNull();
        assertThat(ffm).as("FFM WhoFFM.queryUtxent()").isNotNull();
        assertThat(ffm).hasSameSizeAs(jna);
        if (jna.isEmpty()) {
            return;
        }
        // utmpx walks return entries in the same order across back-to-back reads. Match by (user, terminalDevice).
        var ffmByKey = ffm.stream()
                .collect(Collectors.toMap(s -> s.getUserName() + "|" + s.getTerminalDevice(), Function.identity()));
        for (OSSession j : jna) {
            String key = j.getUserName() + "|" + j.getTerminalDevice();
            OSSession f = ffmByKey.get(key);
            assertThat(f).as("session %s", key).isNotNull();
            assertThat(f.getUserName()).as("session[%s].user", key).isEqualTo(j.getUserName());
            assertThat(f.getTerminalDevice()).as("session[%s].device", key).isEqualTo(j.getTerminalDevice());
            assertThat(f.getHost()).as("session[%s].host", key).isEqualTo(j.getHost());
            assertThat(f.getLoginTime()).as("session[%s].loginTime", key).isEqualTo(j.getLoginTime());
        }
    }
}
