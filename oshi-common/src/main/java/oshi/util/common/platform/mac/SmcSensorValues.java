/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Connection-independent interpretation of the values macOS SMC sensor keys report.
 * <p>
 * These methods take a reading rather than an SMC connection, so the JNA and FFM backends share them and so they can be
 * tested without Mac hardware. Locating the keys themselves is {@link SmcKeyIndex}.
 */
@ThreadSafe
public final class SmcSensorValues {

    /** The SMC data type reporting a value as fixed point with two fractional bits. */
    private static final String DATATYPE_FPE2 = "fpe2";

    /**
     * The lowest reading accepted as a genuine CPU voltage, in volts.
     * <p>
     * No CPU core rail runs this low. The lowest confirmed reading is 0.748 V, from {@code VP0C} on an M3 Pro, and the
     * nearest neighbour on that machine is 0.864 V.
     * <p>
     * What this rejects is a value scaled by the wrong factor: an {@code fpe2} reading treated as volts rather than
     * millivolts yields about 0.0012 V where 1.2 V was meant. That is two orders of magnitude below the floor, and the
     * floor is two orders of magnitude below a real reading, so it sits in an empty band with margin either way.
     * <p>
     * Unlike temperature there is no known voltage sentinel: an idle-gated sensor parks its temperature below ambient,
     * but nothing comparable has been observed for the voltage keys. This is a scaling and garbage guard rather than a
     * sentinel filter.
     */
    public static final double MIN_PLAUSIBLE_VOLTAGE = 0.2;

    private SmcSensorValues() {
    }

    /**
     * Converts a fan speed reading to rpm.
     * <p>
     * A reading of zero is preserved rather than discarded. A fan that is stopped genuinely reads zero: on an idle M3
     * Pro both {@code F0Ac} and {@code F1Ac} read 0.0 while {@code F0Mn} and {@code F0Mx} report 1350 and 5349, so the
     * fans are present and readable but not spinning. Dropping those readings would turn two stopped fans into no fans,
     * and {@link oshi.hardware.Sensors#getFanSpeeds()} distinguishes the two: an empty array means no fans were
     * detected, while a zero entry means a fan whose speed is zero or could not be measured.
     *
     * @param reading the raw reading
     * @return the speed in rpm, never negative
     */
    public static int toRpm(double reading) {
        if (Double.isNaN(reading) || reading <= 0d) {
            return 0;
        }
        // Round rather than truncate: rpm is reported as a float but is integral in nature, and truncating biases low.
        return (int) Math.min(Integer.MAX_VALUE, Math.round(reading));
    }

    /**
     * Tests whether a reading is plausible as a CPU voltage.
     *
     * @param volts the reading to test, in volts
     * @return true if the reading is at least {@link #MIN_PLAUSIBLE_VOLTAGE}
     */
    public static boolean isPlausibleVoltage(double volts) {
        return volts >= MIN_PLAUSIBLE_VOLTAGE;
    }

    /**
     * Converts a voltage reading to volts, according to the units its SMC data type implies.
     * <p>
     * An {@code fpe2} value is fourteen integer bits and two fractional bits, so it resolves to a quarter of a unit
     * over a range of 0 to 16383.75. Read as volts that would express a 1.2 V core rail only as 1.00 or 1.25, which is
     * too coarse to be the intent; read as millivolts it resolves to 0.25 mV over 0 to 16.4 V, which is what a voltage
     * sensor wants. Millivolts is the only self-consistent reading, so an {@code fpe2} value is scaled.
     * <p>
     * Every other type is already in volts, including the {@code flt} that Apple Silicon reports. Scaling from the type
     * rather than from the key name means a key whose encoding differs from the one expected still reads correctly.
     *
     * @param raw      the decoded reading
     * @param dataType the key's SMC data type, which may be empty if that read failed
     * @return the reading in volts
     */
    public static double scaleVoltage(double raw, String dataType) {
        return DATATYPE_FPE2.equals(dataType) ? raw / 1000d : raw;
    }
}
