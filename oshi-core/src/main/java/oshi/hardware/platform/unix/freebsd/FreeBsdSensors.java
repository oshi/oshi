/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import com.sun.jna.Memory;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;

/**
 * Sensors from coretemp
 */
@ThreadSafe
final class FreeBsdSensors extends AbstractSensors {

    @Override
    public double queryCpuTemperature() {
        return queryKldloadCoretemp();
    }

    /*
     * If user has loaded coretemp module via kldload coretemp, sysctl call will return temperature
     *
     * @return Temperature if successful, otherwise NaN
     */
    private static double queryKldloadCoretemp() {
        String name = "dev.cpu.%d.temperature";
        try (CloseableSizeTByReference size = new CloseableSizeTByReference(FreeBsdLibc.INT_SIZE)) {
            int cpu = 0;
            double sumTemp = 0d;
            try (Memory p = new Memory(size.longValue())) {
                while (0 == FreeBsdLibc.INSTANCE.sysctlbyname(String.format(name, cpu), p, size, null, size_t.ZERO)) {
                    sumTemp += p.getInt(0) / 10d - 273.15;
                    cpu++;
                }
            }
            return cpu > 0 ? sumTemp / cpu : Double.NaN;
        }
    }

    @Override
    public int[] queryFanSpeeds() {
        // Nothing known on FreeBSD for this.
        return new int[0];
    }

    @Override
    public double queryCpuVoltage() {
        // Nothing known on FreeBSD for this.
        return 0d;
    }
}
