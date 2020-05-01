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
package oshi.hardware;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test Power Source
 */
public class PowerSourceTest {
    /**
     * Test power source.
     */
    @Test
    public void testPowerSource() {
        SystemInfo si = new SystemInfo();
        List<PowerSource> psArr = si.getHardware().getPowerSources();
        for (PowerSource ps : psArr) {
            assertTrue(ps.getRemainingCapacityPercent() >= 0);
            double epsilon = 1E-6;
            assertTrue(ps.getTimeRemainingEstimated() > 0 || Math.abs(ps.getTimeRemainingEstimated() - -1) < epsilon
                    || Math.abs(ps.getTimeRemainingEstimated() - -2) < epsilon);
        }
    }
}
