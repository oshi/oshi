/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
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
