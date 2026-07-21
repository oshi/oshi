/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class UptimeAndBootTimeTest {

    @Test
    void testParseUpTime() {
        // 18:36pm up 10 days 8:11, 2 users, load average: 3.14, 2.74, 2.41
        long ms = Uptime.parseUpTime("18:36pm up 10 days 8:11, 2 users, load average: 3.14, 2.74, 2.41");
        assertThat(ms, is((10L * 86_400_000L) + (8L * 3_600_000L) + (11L * 60_000L)));
        // minutes-only form
        assertThat(Uptime.parseUpTime("12:00pm up 5 mins, 1 user, load average: 0.00, 0.00, 0.00"), is(5L * 60_000L));
        // unparseable input yields 0
        assertThat(Uptime.parseUpTime(""), is(0L));
        assertThat(Uptime.parseUpTime("not an uptime line"), is(0L));
    }

    @Test
    void testParseBootTime() {
        // system boot 2020-06-16 09:12
        long ms = Who.parseBootTime("   .        system boot 2020-06-16 09:12");
        assertThat(ms, is(greaterThan(0L)));
        // matches the same zone-aware conversion the parser performs
        long expected = LocalDateTime
                .parse("2020-06-16 09:12", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertThat(ms, is(expected));
        // unparseable input yields 0
        assertThat(Who.parseBootTime(""), is(0L));
        assertThat(Who.parseBootTime("no timestamp here"), is(0L));
    }

    @Test
    @EnabledOnOs(OS.AIX)
    void testQueryBootTime() {
        long msSinceEpoch = Who.queryBootTime();
        // Possible to return 0 if there is no year information in the command
        assertThat("Boot time should be after the epoch", msSinceEpoch, greaterThanOrEqualTo(0L));
        assertThat("Boot time should be before now", msSinceEpoch, lessThan(System.currentTimeMillis()));

        long msSinceBoot = Uptime.queryUpTime();
        assertThat("Up time should be positive", msSinceBoot, greaterThan(0L));
        assertThat("Boot time plus uptime should be just before now", msSinceBoot + msSinceEpoch,
                lessThanOrEqualTo(System.currentTimeMillis()));
    }
}
