/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.util.platform.mac.SmcUtil.SMC_KEYS_CPU_TEMP_AGGREGATE_AS;
import static oshi.util.platform.mac.SmcUtil.SMC_KEYS_CPU_TEMP_AS;
import static oshi.util.platform.mac.SmcUtil.SMC_KEY_CPU_TEMP;

import java.util.List;

import com.sun.jna.platform.mac.IOKit.IOConnect;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.common.platform.mac.SmcSensorValues;
import oshi.util.platform.mac.SmcUtil;

/**
 * Sensors from SMC
 */
@ThreadSafe
final class MacSensorsJNA extends AbstractSensors {

    @Override
    public double queryCpuTemperature() {
        IOConnect conn = SmcUtil.smcOpen();
        if (conn == null) {
            return 0d;
        }
        try {
            // Prefer the chip-independent CPU-die aggregate keys (TCMb average, TCMz max), which the firmware
            // computes even on chips whose per-core keys are named differently or absent (e.g. the M3 Pro).
            double temp = SmcUtil.smcGetFirstTemperature(conn, SMC_KEYS_CPU_TEMP_AGGREGATE_AS);
            if (temp <= 0d) {
                temp = SmcUtil.smcGetFirstTemperature(conn, SMC_KEYS_CPU_TEMP_AS);
            }
            if (temp <= 0d) {
                // Intel fallback, held to the same plausibility floor
                double intelTemp = SmcUtil.smcGetFloat(conn, SMC_KEY_CPU_TEMP);
                temp = SmcUtil.isPlausibleTemperature(intelTemp) ? intelTemp : 0d;
            }
            return temp;
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    @Override
    public int[] queryFanSpeeds() {
        // Resolve the keys before opening a connection: discovery opens its own, and nesting two would leak a handle.
        List<String> keys = SmcUtil.getFanSpeedKeys();
        IOConnect conn = SmcUtil.smcOpen();
        // On open failure return an array sized to the known key count rather than empty, so the length stays stable
        // across polls. An empty array means "no fans detected"; a zero entry means "a fan reading zero or unmeasured".
        if (conn == null) {
            return new int[keys.size()];
        }
        try {
            int[] fanSpeeds = new int[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                fanSpeeds[i] = SmcSensorValues.toRpm(SmcUtil.smcGetFloat(conn, keys.get(i)));
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
            return SmcUtil.smcGetFirstVoltage(conn, SmcUtil.getCpuVoltageKeys());
        } finally {
            SmcUtil.smcClose(conn);
        }
    }
}
