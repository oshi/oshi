/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.lang.foreign.MemorySegment;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.platform.mac.SmcUtilFFM;

import static oshi.util.platform.mac.SmcUtilFFM.SMC_KEY_CPU_TEMP;
import static oshi.util.platform.mac.SmcUtilFFM.SMC_KEY_CPU_VOLTAGE;
import static oshi.util.platform.mac.SmcUtilFFM.SMC_KEY_CPU_VOLTAGE_AS;
import static oshi.util.platform.mac.SmcUtilFFM.SMC_KEY_FAN_NUM;
import static oshi.util.platform.mac.SmcUtilFFM.SMC_KEY_FAN_SPEED;
import static oshi.util.platform.mac.SmcUtilFFM.SMC_KEYS_CPU_TEMP_AS;

/**
 * Sensors from SMC
 */
@ThreadSafe
final class MacSensorsFFM extends AbstractSensors {

    private int numFans = 0;

    @Override
    public double queryCpuTemperature() {
        MemorySegment conn = SmcUtilFFM.smcOpen();
        if (conn == null) {
            return 0d;
        }
        try {
            double temp = SmcUtilFFM.smcGetFirstFloat(conn, SMC_KEYS_CPU_TEMP_AS);
            if (temp <= 0d) {
                temp = SmcUtilFFM.smcGetFloat(conn, SMC_KEY_CPU_TEMP);
            }
            return temp;
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }

    @Override
    public int[] queryFanSpeeds() {
        MemorySegment conn = SmcUtilFFM.smcOpen();
        if (conn == null) {
            return new int[this.numFans];
        }
        try {
            if (this.numFans == 0) {
                this.numFans = (int) SmcUtilFFM.smcGetLong(conn, SMC_KEY_FAN_NUM);
            }
            int[] fanSpeeds = new int[this.numFans];
            for (int i = 0; i < this.numFans; i++) {
                fanSpeeds[i] = (int) SmcUtilFFM.smcGetFloat(conn, String.format(Locale.ROOT, SMC_KEY_FAN_SPEED, i));
            }
            return fanSpeeds;
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }

    @Override
    public double queryCpuVoltage() {
        MemorySegment conn = SmcUtilFFM.smcOpen();
        if (conn == null) {
            return 0d;
        }
        try {
            double volts = SmcUtilFFM.smcGetFloat(conn, SMC_KEY_CPU_VOLTAGE_AS);
            if (volts > 0d) {
                return volts;
            }
            return SmcUtilFFM.smcGetFloat(conn, SMC_KEY_CPU_VOLTAGE) / 1000d;
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }
}
