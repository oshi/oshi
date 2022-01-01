/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.util.platform.unix.freebsd.ProcstatUtil;

/**
 * Test general utility methods
 */
class ProcstatUtilTest {

    @Test
    void testProcstat() {
        if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.FREEBSD)) {
            int pid = new SystemInfo().getOperatingSystem().getProcessId();

            assertThat("Open files must be nonnegative", ProcstatUtil.getOpenFiles(pid), is(greaterThanOrEqualTo(0L)));

            assertThat("CwdMap should have at least one element", ProcstatUtil.getCwdMap(-1), is(not(anEmptyMap())));

            assertThat("CwdMap with pid should have at least one element", ProcstatUtil.getCwdMap(pid),
                    is(not(anEmptyMap())));

            assertThat("Cwd should be nonempty", ProcstatUtil.getCwd(pid), is(not(emptyString())));
        }
    }
}
