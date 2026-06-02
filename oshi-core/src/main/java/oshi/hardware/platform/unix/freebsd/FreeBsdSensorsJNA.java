/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.Locale;

import com.sun.jna.Memory;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdSensors;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;

/**
 * JNA-backed FreeBSD sensors. The only native read is the per-CPU temperature from the {@code coretemp} kld module
 * via {@code sysctlbyname("dev.cpu.%d.temperature")}. Fan speeds and CPU voltage have no FreeBSD-supported source
 * and return the empty/zero defaults inherited from {@link FreeBsdSensors}.
 */
@ThreadSafe
public class FreeBsdSensorsJNA extends FreeBsdSensors {

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
                while (0 == FreeBsdLibc.INSTANCE.sysctlbyname(String.format(Locale.ROOT, name, cpu), p, size, null,
                        size_t.ZERO)) {
                    sumTemp += p.getInt(0) / 10d - 273.15;
                    cpu++;
                }
            }
            return cpu > 0 ? sumTemp / cpu : Double.NaN;
        }
    }
}
