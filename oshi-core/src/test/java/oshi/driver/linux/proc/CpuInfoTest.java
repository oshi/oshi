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
package oshi.driver.linux.proc;

import com.sun.jna.Platform;
import org.junit.Test;
import oshi.util.tuples.Quartet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link CpuInfo}.
 */
public class CpuInfoTest {

    @Test
    public void testQueryCpuManufacturer() {
        if (Platform.isLinux()) {
            String cpuManufacturer = CpuInfo.queryCpuManufacturer();
            assertThat("CPU manufacturer should be a non-empty String or null", cpuManufacturer, anyOf(nullValue(), not(emptyString())));
        }
    }

    @Test
    public void testQueryBoardInfo() {
        if (Platform.isLinux()) {
            Quartet<String, String, String, String> boardInfo = CpuInfo.queryBoardInfo();
            String pcManufacturer = boardInfo.getA();
            assertThat("PC manufacturer should be a non-empty String or null", pcManufacturer, anyOf(nullValue(), not(emptyString())));
            String pcModel = boardInfo.getB();
            assertThat("PC model should be null, blank, empty or non-empty", pcModel, anyOf(blankOrNullString(), not(emptyString()), emptyString()));
            String pcVersion = boardInfo.getC();
            assertThat("PC version should be null, blank, empty or non-empty", pcVersion, anyOf(blankOrNullString(), not(emptyString()), emptyString()));
            String pcSerialNumber = boardInfo.getD();
            assertThat("PC serial number should be null, blank, empty or non-empty", pcSerialNumber, anyOf(blankOrNullString(), not(emptyString()), emptyString()));
        }
    }
}
