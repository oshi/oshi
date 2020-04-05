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
package oshi.hardware.platform.mac;

import com.sun.jna.platform.mac.IOKit.IOConnect; // NOSONAR squid:S1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.platform.mac.SmcUtil;

/**
 * Sensors from SMC
 */
@ThreadSafe
final class MacSensors extends AbstractSensors {

    // This shouldn't change once determined
    private int numFans = 0;

    @Override
    public double queryCpuTemperature() {
        IOConnect conn = SmcUtil.smcOpen();
        double temp = SmcUtil.smcGetFloat(conn, SmcUtil.SMC_KEY_CPU_TEMP);
        SmcUtil.smcClose(conn);
        if (temp > 0d) {
            return temp;
        }
        return 0d;
    }

    @Override
    public int[] queryFanSpeeds() {
        // If we don't have fan # try to get it
        IOConnect conn = SmcUtil.smcOpen();
        if (this.numFans == 0) {
            this.numFans = (int) SmcUtil.smcGetLong(conn, SmcUtil.SMC_KEY_FAN_NUM);
        }
        int[] fanSpeeds = new int[this.numFans];
        for (int i = 0; i < this.numFans; i++) {
            fanSpeeds[i] = (int) SmcUtil.smcGetFloat(conn, String.format(SmcUtil.SMC_KEY_FAN_SPEED, i));
        }
        SmcUtil.smcClose(conn);
        return fanSpeeds;
    }

    @Override
    public double queryCpuVoltage() {
        IOConnect conn = SmcUtil.smcOpen();
        double volts = SmcUtil.smcGetFloat(conn, SmcUtil.SMC_KEY_CPU_VOLTAGE) / 1000d;
        SmcUtil.smcClose(conn);
        return volts;
    }
}
