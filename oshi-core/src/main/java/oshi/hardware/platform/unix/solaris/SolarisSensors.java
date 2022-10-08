/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
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
