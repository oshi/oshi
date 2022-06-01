/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.windows.perfmon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.Util;

@EnabledOnOs(OS.WINDOWS)
class LoadAverageTest {

    @Test
    void testQueryLoadAverage() {
        double[] loadAverage = LoadAverage.queryLoadAverage(3);
        assertThat("1m load average should be negative", loadAverage[0], lessThan(0d));
        assertThat("5m load average should be negative", loadAverage[1], lessThan(0d));
        assertThat("15m load average should be negative", loadAverage[2], lessThan(0d));
        LoadAverage.startDaemon();
        Util.sleep(11000L);
        loadAverage = LoadAverage.queryLoadAverage(3);
        assertThat("1m load average should be positive", loadAverage[0], greaterThan(0d));
        assertThat("5m load average should be positive", loadAverage[1], greaterThan(0d));
        assertThat("15m load average should be positive", loadAverage[2], greaterThan(0d));
        LoadAverage.stopDaemon();
    }
}
