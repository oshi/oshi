/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.util.platform.mac.SmcUtilFFM.SMC_KEYS_CPU_TEMP_AGGREGATE_AS;
import static oshi.ffm.util.platform.mac.SmcUtilFFM.SMC_KEYS_CPU_TEMP_AS;
import static oshi.ffm.util.platform.mac.SmcUtilFFM.SMC_KEY_CPU_TEMP;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.mac.SmcUtilFFM;
import oshi.hardware.common.AbstractSensors;
import oshi.util.common.platform.mac.SmcSensorValues;

/**
 * Sensors from SMC
 */
@ThreadSafe
final class MacSensorsFFM extends AbstractSensors {

    @Override
    public double queryCpuTemperature() {
        int conn = SmcUtilFFM.smcOpen();
        if (conn == 0) {
            return 0d;
        }
        try {
            // Prefer the chip-independent CPU-die aggregate keys (TCMb average, TCMz max), which the firmware
            // computes even on chips whose per-core keys are named differently or absent (e.g. the M3 Pro).
            double temp = SmcUtilFFM.smcGetFirstTemperature(conn, SMC_KEYS_CPU_TEMP_AGGREGATE_AS);
            if (temp <= 0d) {
                temp = SmcUtilFFM.smcGetFirstTemperature(conn, SMC_KEYS_CPU_TEMP_AS);
            }
            if (temp <= 0d) {
                // Intel fallback, held to the same plausibility floor
                double intelTemp = SmcUtilFFM.smcGetFloat(conn, SMC_KEY_CPU_TEMP);
                temp = SmcUtilFFM.isPlausibleTemperature(intelTemp) ? intelTemp : 0d;
            }
            return temp;
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }

    @Override
    public int[] queryFanSpeeds() {
        // Resolve the keys before opening a connection: discovery opens its own, and nesting two would leak a handle.
        List<String> keys = SmcUtilFFM.getFanSpeedKeys();
        int conn = SmcUtilFFM.smcOpen();
        // On open failure return an array sized to the known key count rather than empty, so the length stays stable
        // across polls. An empty array means "no fans detected"; a zero entry means "a fan reading zero or unmeasured".
        if (conn == 0) {
            return new int[keys.size()];
        }
        try {
            int[] fanSpeeds = new int[keys.size()];
            for (int i = 0; i < keys.size(); i++) {
                fanSpeeds[i] = SmcSensorValues.toRpm(SmcUtilFFM.smcGetFloat(conn, keys.get(i)));
            }
            return fanSpeeds;
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }

    @Override
    public double queryCpuVoltage() {
        int conn = SmcUtilFFM.smcOpen();
        if (conn == 0) {
            return 0d;
        }
        try {
            return SmcUtilFFM.smcGetFirstVoltage(conn, SmcUtilFFM.getCpuVoltageKeys());
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }
}
