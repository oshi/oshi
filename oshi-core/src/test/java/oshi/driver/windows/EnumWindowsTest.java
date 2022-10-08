/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSDesktopWindow;

@EnabledOnOs(OS.WINDOWS)
class EnumWindowsTest {
    @Test
    void testQueryDesktopWindows() {
        List<OSDesktopWindow> allWindows = EnumWindows.queryDesktopWindows(false);
        List<OSDesktopWindow> visibleWindows = EnumWindows.queryDesktopWindows(true);
        assertThat("All windows should be no smaller than visible windows", allWindows.size(),
                greaterThanOrEqualTo(visibleWindows.size()));
    }
}
