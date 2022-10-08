/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import com.sun.jna.platform.mac.IOKit.IOConnect;

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
