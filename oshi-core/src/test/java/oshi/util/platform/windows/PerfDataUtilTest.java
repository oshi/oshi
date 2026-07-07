/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.event.Level;

import com.sun.jna.platform.win32.PdhMsg;

@EnabledOnOs(OS.WINDOWS)
class PerfDataUtilTest {

    @Test
    void testHandleErrorBooleanDefault() {
        assertThat(PerfDataUtil.handleError(PdhMsg.PDH_INVALID_HANDLE, ERROR, "Failed to open PDH Query.", false),
                is(false));
    }

    @Test
    void testHandleErrorLongDefault() {
        assertThat(PerfDataUtil.handleError(PdhMsg.PDH_INVALID_HANDLE, WARN, "Failed to get counter.",
                (long) PdhMsg.PDH_INVALID_HANDLE), is((long) PdhMsg.PDH_INVALID_HANDLE));
    }

    @Test
    void testHandleErrorStringDefault() {
        assertThat(PerfDataUtil.handleError(PdhMsg.PDH_INVALID_HANDLE, WARN, "Failed to get counter.", "default"),
                is("default"));
    }

    @Test
    void testHandleErrorAtEveryLevel() {
        for (Level level : Level.values()) {
            assertThat(PerfDataUtil.handleError(PdhMsg.PDH_INVALID_HANDLE, level, "Failed.", "default"), is("default"));
        }
    }
}
