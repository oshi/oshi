/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSSession;

@DisabledOnOs(OS.WINDOWS)
class WhoTest {
    @Test
    void testQueryWho() {
        for (OSSession session : Who.queryWho()) {
            assertThat("Session login time should be greater than 0", session.getLoginTime(), is(greaterThan(0L)));
            assertThat("Session login time should be less than current time", session.getLoginTime(),
                    is(lessThan(System.currentTimeMillis())));
            assertThat("User should be non-empty", session.getUserName(), is(not(emptyOrNullString())));
            assertThat("Devices should be non-empty", session.getTerminalDevice(), is(not(emptyOrNullString())));
        }
    }
}
