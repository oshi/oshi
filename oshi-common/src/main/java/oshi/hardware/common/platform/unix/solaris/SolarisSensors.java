/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

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
public final class SolarisSensors extends AbstractSensors {

    @Override
    public double queryCpuTemperature() {
        return parseCpuTemperature(ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c temperature-sensor"));
    }

    /**
     * Parses the output of {@code prtpicl -v -c temperature-sensor} to find the maximum CPU temperature.
     *
     * @param prtpicl the lines emitted by {@code prtpicl -v -c temperature-sensor}
     * @return the maximum temperature found (divided by 1000 if in millidegrees), or 0 if none
     */
    static double parseCpuTemperature(List<String> prtpicl) {
        double maxTemp = 0d;
        // Return max found temp
        for (String line : prtpicl) {
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
        return parseFanSpeeds(ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c fan"));
    }

    /**
     * Parses the output of {@code prtpicl -v -c fan} to extract fan speeds.
     *
     * @param prtpicl the lines emitted by {@code prtpicl -v -c fan}
     * @return an array of fan speeds
     */
    static int[] parseFanSpeeds(List<String> prtpicl) {
        List<Integer> speedList = new ArrayList<>();
        // Return max found temp
        for (String line : prtpicl) {
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
        return parseVoltage(ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c voltage-sensor"));
    }

    /**
     * Parses the output of {@code prtpicl -v -c voltage-sensor} to find the first voltage reading.
     *
     * @param prtpicl the lines emitted by {@code prtpicl -v -c voltage-sensor}
     * @return the first voltage found, or 0 if none
     */
    static double parseVoltage(List<String> prtpicl) {
        double voltage = 0d;
        for (String line : prtpicl) {
            if (line.trim().startsWith("Voltage:")) {
                voltage = ParseUtil.parseDoubleOrDefault(line.replace("Voltage:", "").trim(), 0d);
                break;
            }
        }
        return voltage;
    }
}
