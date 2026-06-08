/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaDoubleOrDefault;
import static oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions.SIZE_T;

import java.lang.foreign.MemorySegment;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdSensors;

/**
 * FFM-backed FreeBSD sensors. The only native read is the per-CPU temperature from the {@code coretemp} kld module via
 * {@code sysctlbyname("dev.cpu.%d.temperature")}. Fan speeds and CPU voltage have no FreeBSD-supported source and
 * return the empty/zero defaults inherited from {@link FreeBsdSensors}.
 */
@ThreadSafe
public class FreeBsdSensorsFFM extends FreeBsdSensors {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdSensorsFFM.class);

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
        return callInArenaDoubleOrDefault(arena -> {
            MemorySegment p = arena.allocate(JAVA_INT);
            MemorySegment size = arena.allocateFrom(SIZE_T, JAVA_INT.byteSize());
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            int cpu = 0;
            double sumTemp = 0d;
            while (true) {
                MemorySegment name = arena.allocateFrom(String.format(Locale.ROOT, "dev.cpu.%d.temperature", cpu));
                if (0 != FreeBsdLibcFunctions.sysctlbyname(callState, name, p, size, MemorySegment.NULL, 0L)) {
                    break;
                }
                sumTemp += p.get(JAVA_INT, 0) / 10d - 273.15;
                cpu++;
            }
            return cpu > 0 ? sumTemp / cpu : Double.NaN;
        }, LOG, Level.WARN, "Failed reading CPU temperature", Double.NaN);
    }
}
