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
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Sensors from prtpicl
 */
@ThreadSafe
final class SolarisSensors extends AbstractSensors {

    @Override
    public double queryCpuTemperature() {
        double maxTemp = 0d;
        // Return max found temp
        for (String line : ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c temperature-sensor")) {
            if (line.trim().startsWith("Temperature:")) {
                int temp = ParseUtil.parseLastInt(line, 0);
                if (temp > maxTemp) {
                    maxTemp = temp;
                }
            }
        }
        // If it's in millidegrees:
        if (maxTemp > 1000) {
            maxTemp /= 1000;
        }
        return maxTemp;
    }

    @Override
    public int[] queryFanSpeeds() {
        List<Integer> speedList = new ArrayList<>();
        // Return max found temp
        for (String line : ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c fan")) {
            if (line.trim().startsWith("Speed:")) {
                speedList.add(ParseUtil.parseLastInt(line, 0));
            }
        }
        int[] fans = new int[speedList.size()];
        for (int i = 0; i < speedList.size(); i++) {
            fans[i] = speedList.get(i);
        }
        return fans;
    }

    @Override
    public double queryCpuVoltage() {
        double voltage = 0d;
        for (String line : ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c voltage-sensor")) {
            if (line.trim().startsWith("Voltage:")) {
                voltage = ParseUtil.parseDoubleOrDefault(line.replace("Voltage:", "").trim(), 0d);
                break;
            }
        }
        return voltage;
    }
}
