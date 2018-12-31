/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import java.util.Arrays;

import oshi.hardware.Sensors;
import oshi.jna.platform.mac.IOKit;
import oshi.util.platform.mac.SmcUtil;

public class MacSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    // Store some things to throttle SMC queries
    private double lastTemp = 0d;

    private long lastTempTime;

    private int numFans = 0;

    private int[] lastFanSpeeds = new int[0];

    private long lastFanSpeedsTime;

    private double lastVolts = 0d;

    private long lastVoltsTime;

    public MacSensors() {
        SmcUtil.smcOpen();
        // Do an initial read of temperature and fan speeds. This caches initial
        // dataInfo and improves success of future queries
        this.lastTemp = getCpuTemperature();
        this.lastFanSpeeds = getFanSpeeds();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                SmcUtil.smcClose();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        // Only update every second
        if (System.currentTimeMillis() - this.lastTempTime > 900) {
            double temp = SmcUtil.smcGetSp78(IOKit.SMC_KEY_CPU_TEMP, 50);
            if (temp > 0d) {
                this.lastTemp = temp;
                this.lastTempTime = System.currentTimeMillis();
            }
        }
        return this.lastTemp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        // Only update every second
        if (System.currentTimeMillis() - this.lastFanSpeedsTime > 900) {
            // If we don't have fan # try to get it
            if (this.numFans == 0) {
                this.numFans = (int) SmcUtil.smcGetLong(IOKit.SMC_KEY_FAN_NUM, 50);
                this.lastFanSpeeds = new int[this.numFans];
            }
            for (int i = 0; i < this.numFans; i++) {
                int speed = (int) SmcUtil.smcGetFpe2(String.format(IOKit.SMC_KEY_FAN_SPEED, i), 50);
                if (speed > 0) {
                    this.lastFanSpeeds[i] = speed;
                    this.lastFanSpeedsTime = System.currentTimeMillis();
                }
            }
        }
        // Make a copy to return
        return Arrays.copyOf(this.lastFanSpeeds, this.lastFanSpeeds.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        // Only update every second
        if (System.currentTimeMillis() - this.lastVoltsTime > 900) {
            double kiloVolts = SmcUtil.smcGetFpe2(IOKit.SMC_KEY_CPU_VOLTAGE, 50);
            if (kiloVolts > 0d) {
                this.lastVolts = kiloVolts / 1000d;
                this.lastVoltsTime = System.currentTimeMillis();
            }
        }
        return this.lastVolts;
    }
}
