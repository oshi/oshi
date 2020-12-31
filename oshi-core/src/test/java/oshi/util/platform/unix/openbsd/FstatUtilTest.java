/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.util.platform.unix.openbsd;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import oshi.PlatformEnum;
import oshi.SystemInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test general utility methods for {@link FstatUtil}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FstatUtilTest {
    private static int pid;

    @BeforeAll
    void getPid(){
        assumeTrue(SystemInfo.getCurrentPlatformEnum().equals(PlatformEnum.OPENBSD));
        pid = new SystemInfo().getOperatingSystem().getProcessId();
    }

    @Test
    void testGetOpenFiles() {
        assertThat("Number of open files must be nonnegative", FstatUtil.getOpenFiles(pid), is(greaterThanOrEqualTo(0L)));
    }

    @Test
    void testGetCwd() {
        assertThat("Cwd should not be empty", FstatUtil.getCwd(pid), is(not(emptyString())));
    }
}
