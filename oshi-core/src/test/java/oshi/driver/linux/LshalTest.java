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
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;

import com.sun.jna.Platform;
import org.junit.Test;
import oshi.TestConstants;

public class LshalTest {

    @Test
    public void testQuerySerialNumber() {
        if (Platform.isLinux()) {
            final String serialNumber = Lshal.querySerialNumber();
            if (serialNumber != null) {
                assertThat("Test Lshal querySerialNumber", serialNumber, not(emptyString()));
            }
        }
    }

    @Test
    public void testQueryUUID() {
        if (Platform.isLinux()) {
            final String uuid = Lshal.queryUUID();
            if (uuid != null) {
                assertThat("Test Lshal queryUUID", uuid, not(emptyString()));
                assertThat("Test Lshal queryUUID format", uuid, matchesRegex(TestConstants.UUID_REGEX));
            }
        }
    }

}
