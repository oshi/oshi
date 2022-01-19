/*
 * MIT License
 *
 * Copyright (c) 2021-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Platform;

@EnabledOnOs(OS.LINUX)
class SysfsTest {

    @Test
    void testQuerySystemVendor() {
        assertDoesNotThrow(Sysfs::querySystemVendor);
        assertDoesNotThrow(Sysfs::queryProductModel);
        assertDoesNotThrow(Sysfs::queryProductSerial);
        assertDoesNotThrow(Sysfs::queryUUID);
        assertDoesNotThrow(Sysfs::queryBoardVendor);
        assertDoesNotThrow(Sysfs::queryBoardModel);
        assertDoesNotThrow(Sysfs::queryBoardVersion);
        assertDoesNotThrow(Sysfs::queryBoardSerial);
        assertDoesNotThrow(Sysfs::queryBiosVendor);
        assertDoesNotThrow(Sysfs::queryBiosDescription);
        assertDoesNotThrow(Sysfs::queryBiosReleaseDate);
        assertDoesNotThrow(this::queryBiosVersionEmpty);
    }

    private void queryBiosVersionEmpty() {
        Sysfs.queryBiosVersion("");
    }

    @Test
    void testQueryBiosVersion() {
        if (Platform.isLinux()) {
            assertDoesNotThrow(Sysfs::querySystemVendor);
            final String biosRevisionSuffix = "biosRevision";
            final String biosRevision = Sysfs.queryBiosVersion(biosRevisionSuffix);
            if (biosRevision != null) {
                assertThat("Test Sysfs queryBiosVersion with biosRevision", biosRevision,
                        containsString(biosRevisionSuffix));
            }
        }
    }
}
