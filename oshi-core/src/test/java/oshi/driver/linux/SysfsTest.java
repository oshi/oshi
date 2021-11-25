/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import com.sun.jna.Platform;

import oshi.TestConstants;

public class SysfsTest {

    @Test
    public void testQuerySystemVendor() {
        if (Platform.isLinux()) {
            final String sysVendor = Sysfs.querySystemVendor();
            if (sysVendor != null) {
                assertThat("Test Sysfs querySystemVendor", sysVendor, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryProductModel() {
        if (Platform.isLinux()) {
            final String sysProductModel = Sysfs.queryProductModel();
            if (sysProductModel != null) {
                assertThat("Test Sysfs queryProductModel", sysProductModel, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryProductSerial() {
        if (Platform.isLinux()) {
            final String serialNumber = Sysfs.queryProductSerial();
            if (serialNumber != null) {
                assertThat("Test Sysfs queryProductSerial", serialNumber, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryUUID() {
        if (Platform.isLinux()) {
            final String uuid = Sysfs.queryUUID();
            if (uuid != null) {
                assertThat("Test Sysfs queryUUID", uuid, not(emptyString()));
                assertThat("Test Sysfs queryUUID", uuid, matchesRegex(TestConstants.UUID_REGEX));
            }
        }
    }

    @Test
    public void testQueryBoardVendor() {
        if (Platform.isLinux()) {
            final String boardVendor = Sysfs.queryBoardVendor();
            if (boardVendor != null) {
                assertThat("Test Sysfs queryBoardVendor", boardVendor, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBoardModel() {
        if (Platform.isLinux()) {
            final String boardModel = Sysfs.queryBoardModel();
            if (boardModel != null) {
                assertThat("Test Sysfs queryBoardModel", boardModel, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBoardVersion() {
        if (Platform.isLinux()) {
            final String boardVersion = Sysfs.queryBoardVersion();
            if (boardVersion != null) {
                assertThat("Test Sysfs queryBoardVersion", boardVersion, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBoardSerial() {
        if (Platform.isLinux()) {
            final String boardSerial = Sysfs.queryBoardSerial();
            if (boardSerial != null) {
                assertThat("Test Sysfs queryBoardSerial", boardSerial, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBiosVendor() {
        if (Platform.isLinux()) {
            final String biosVendor = Sysfs.queryBiosVendor();
            if (biosVendor != null) {
                assertThat("Test Sysfs queryBiosVendor", biosVendor, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBiosDescription() {
        if (Platform.isLinux()) {
            final String biosDescription = Sysfs.queryBiosDescription();
            if (biosDescription != null) {
                assertThat("Test Sysfs queryBiosDescription", biosDescription, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBiosVersionEmptyBiosRevision() {
        if (Platform.isLinux()) {
            final String biosRevision = Sysfs.queryBiosVersion("");
            if (biosRevision != null) {
                assertThat("Test Sysfs queryBiosVersion", biosRevision, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryBiosVersion() {
        if (Platform.isLinux()) {
            final String biosRevisionSuffix = "biosRevision";
            final String biosRevision = Sysfs.queryBiosVersion(biosRevisionSuffix);
            if (biosRevision != null) {
                assertThat("Test Sysfs queryBiosVersion", biosRevision, not(emptyString()));
                assertThat("Test Sysfs queryBiosVersion with biosRevision", biosRevision,
                        containsString(biosRevisionSuffix));
            }
        }
    }

    @Test
    public void testQueryBiosReleaseDate() {
        if (Platform.isLinux()) {
            final String biosReleaseDate = Sysfs.queryBiosReleaseDate();
            if (biosReleaseDate != null) {
                assertThat("Test Sysfs queryBiosReleaseDate", biosReleaseDate, not(emptyString()));
            }
        }
    }

}
