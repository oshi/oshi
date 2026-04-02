/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.Locale;

import com.sun.jna.platform.mac.IOKit.IOConnect;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.platform.mac.SmcUtil;
import static oshi.util.platform.mac.SmcUtil.SMC_KEY_CPU_TEMP;
import static oshi.util.platform.mac.SmcUtil.SMC_KEY_CPU_VOLTAGE;
import static oshi.util.platform.mac.SmcUtil.SMC_KEY_CPU_VOLTAGE_AS;
import static oshi.util.platform.mac.SmcUtil.SMC_KEY_FAN_NUM;
import static oshi.util.platform.mac.SmcUtil.SMC_KEY_FAN_SPEED;
import static oshi.util.platform.mac.SmcUtil.SMC_KEYS_CPU_TEMP_AS;

/**
 * Sensors from SMC
 */
@ThreadSafe
final class MacSensors extends AbstractSensors {

    private volatile int numFans = 0;

    @Override
    public double queryCpuTemperature() {
        IOConnect conn = SmcUtil.smcOpen();
        if (conn == null) {
            return 0d;
        }
        try {
            double temp = SmcUtil.smcGetFirstFloat(conn, SMC_KEYS_CPU_TEMP_AS);
            if (temp <= 0d) {
                temp = SmcUtil.smcGetFloat(conn, SMC_KEY_CPU_TEMP);
            }
            return temp;
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    @Override
    public int[] queryFanSpeeds() {
        IOConnect conn = SmcUtil.smcOpen();
        if (conn == null) {
            return new int[this.numFans];
        }
        try {
            int fans = this.numFans;
            if (fans == 0) {
                fans = (int) SmcUtil.smcGetLong(conn, SMC_KEY_FAN_NUM);
                this.numFans = fans;
            }
            int[] fanSpeeds = new int[fans];
            for (int i = 0; i < fans; i++) {
                fanSpeeds[i] = (int) SmcUtil.smcGetFloat(conn, String.format(Locale.ROOT, SMC_KEY_FAN_SPEED, i));
            }
            return fanSpeeds;
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    @Override
    public double queryCpuVoltage() {
        IOConnect conn = SmcUtil.smcOpen();
        if (conn == null) {
            return 0d;
        }
        try {
            // Apple Silicon: VP0C is flt already in volts, no scaling needed
            double volts = SmcUtil.smcGetFloat(conn, SMC_KEY_CPU_VOLTAGE_AS);
            if (volts > 0d) {
                return volts;
            }
            // Intel: VC0C is FPE2 in millivolts, divide by 1000 to get volts
            return SmcUtil.smcGetFloat(conn, SMC_KEY_CPU_VOLTAGE) / 1000d;
        } finally {
            SmcUtil.smcClose(conn);
        }
    }
}
