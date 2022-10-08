/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSDesktopWindow;

@DisabledOnOs({ OS.WINDOWS, OS.MAC })
class XwinInfoTest {
    @Test
    void testQueryXWindows() {
        List<OSDesktopWindow> allWindows = Xwininfo.queryXWindows(false);
        List<OSDesktopWindow> visibleWindows = Xwininfo.queryXWindows(true);
        assertThat("All windows should be no smaller than visible windows", allWindows.size(),
                greaterThanOrEqualTo(visibleWindows.size()));
    }
}
