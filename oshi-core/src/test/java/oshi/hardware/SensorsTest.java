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

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test Sensors
 */
public class SensorsTest {
    private SystemInfo si = new SystemInfo();
    private Sensors s = si.getHardware().getSensors();

    /**
     * Test sensors
     */
    @Test
    public void testSensors() {
        assertTrue(s.getCpuTemperature() == Double.NaN || s.getCpuTemperature() >= 0d && s.getCpuTemperature() <= 100d);
        assertTrue(s.getCpuVoltage() >= 0);
    }

    @Test
    public void testFanSpeeds() {
        int[] speeds = s.getFanSpeeds();
        for (int speed : speeds) {
            assertTrue(speed >= 0);
        }
    }
}
