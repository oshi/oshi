/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static oshi.comparison.ComparisonAssertions.assertWithinRatio;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.unix.freebsd.FreeBsdLibcFunctions;
import oshi.jna.platform.unix.FreeBsdLibc;

/**
 * Compares JNA {@link FreeBsdLibc} calls against FFM {@link FreeBsdLibcFunctions} on FreeBSD. Runs only on FreeBSD.
 */
@EnabledOnOs(OS.FREEBSD)
class FreeBsdLibcComparisonTest {

    /**
     * Compares {@code getloadavg} between JNA and FFM. Load averages can fluctuate slightly between back-to-back calls,
     * so values are compared within a tight ratio rather than exact equality.
     */
    @Test
    void getloadavg() throws Throwable {
        int nelem = 3;
        double[] jnaAvg = new double[nelem];
        int jnaCount = FreeBsdLibc.INSTANCE.getloadavg(jnaAvg, nelem);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ffmAvg = arena.allocate(JAVA_DOUBLE, nelem);
            int ffmCount = FreeBsdLibcFunctions.getloadavg(ffmAvg, nelem);

            assertThat(ffmCount).isEqualTo(jnaCount).isEqualTo(nelem);
            for (int i = 0; i < nelem; i++) {
                double ffmVal = ffmAvg.getAtIndex(JAVA_DOUBLE, i);
                assertThat(ffmVal).as("loadAverage[%d] non-negative", i).isGreaterThanOrEqualTo(0.0);
                assertWithinRatio(ffmVal, jnaAvg[i], 0.25, "loadAverage[" + i + "]");
            }
        }
    }
}
