/*
 * MIT License
 *
 * Copyright (c) 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

import com.sun.jna.Platform;

import oshi.software.os.OSDesktopWindow;

class WindowInfoTest {
    @Test
    void testQueryDesktopWindows() {
        if (Platform.isMac()) {
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
}
