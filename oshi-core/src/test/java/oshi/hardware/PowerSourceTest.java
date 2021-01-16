/**
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
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test Power Source
 */
class PowerSourceTest {
    /**
     * Test power source.
     */
    @Test
    void testPowerSource() {
        SystemInfo si = new SystemInfo();
        List<PowerSource> psArr = si.getHardware().getPowerSources();
        for (PowerSource ps : psArr) {
            assertThat("Power Source's remaining capacity shouldn't be negative", ps.getRemainingCapacityPercent(),
                    is(greaterThanOrEqualTo(0d)));
            double epsilon = 1E-6;
            assertThat(
                    "Power Source's estimated remaining time should be greater than zero or within error margin of -1 or within error margin of -2",
                    ps.getTimeRemainingEstimated(),
                    is(either(greaterThan(0d)).or(closeTo(-1d, epsilon)).or(closeTo(-2d, epsilon))));
        }
    }
}
