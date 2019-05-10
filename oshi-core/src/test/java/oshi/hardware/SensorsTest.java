/**
 * OSHI (https://github.com/oshi/oshi)
 * <p>
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import org.junit.Test;
import oshi.SystemInfo;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Test Sensors
 */
public class SensorsTest {


    private SystemInfo si = new SystemInfo();

    private Sensors s = si.getHardware().getSensors();

    private boolean hasThermalZonePath() {

        File f = new File("/sys/class/thermal/thermal_zone0");
        return f.isDirectory();
    }

    /**
     * Test sensors with /sys/class/thermal/thermal_zone
     */
    @Test
    public void testSensorsWithThermalZone() {
        assumeTrue(hasThermalZonePath());
        assertTrue(s.getCpuTemperature() >= 0d && s.getCpuTemperature() <= 100d);
        assertTrue(s.getCpuVoltage() >= 0);
    }

    /**
     * Test sensors with /sys/class/hwmon/hwmon
     */
    @Test
    public void testSensorsWithHWMon() {
        assumeFalse(hasThermalZonePath());
        assertTrue(s.getCpuTemperature() > 0d && s.getCpuTemperature() <= 100d);
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
