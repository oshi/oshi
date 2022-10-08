/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.OSDesktopWindow;

@EnabledOnOs(OS.MAC)
class WindowInfoTest {
    @Test
    void testQueryDesktopWindows() {
        final List<OSDesktopWindow> osDesktopWindows = WindowInfo.queryDesktopWindows(true);
        final List<OSDesktopWindow> allOsDesktopWindows = WindowInfo.queryDesktopWindows(false);
        final Set<Integer> windowOrders = new HashSet<>();
        final Set<Long> windowIds = new HashSet<>();

        assertThat("Desktop should have at least one window.", osDesktopWindows.size(), is(greaterThan(0)));
        assertThat("The number of all desktop windows should be greater than the number of visible desktop windows",
                allOsDesktopWindows.size(), is(greaterThan(osDesktopWindows.size())));
        for (OSDesktopWindow window : osDesktopWindows) {
            assertThat("Window IDs are not unique.", windowIds.contains(window.getWindowId()), is(false));
            windowOrders.add(window.getOrder());
            windowIds.add(window.getWindowId());
            assertThat("Windows should have a title.", window.getTitle(), is(not(emptyOrNullString())));
            assertThat("Window should be visible", window.isVisible(), is(true));
        }
        assertThat("Number of layers must be less than or equal to 20.", windowOrders.size(),
                is(lessThanOrEqualTo(20)));
    }
}
