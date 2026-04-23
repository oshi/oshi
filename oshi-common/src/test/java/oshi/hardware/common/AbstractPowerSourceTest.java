/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.PowerSource;

class AbstractPowerSourceTest {

    private static AbstractPowerSource createSource(String name) {
        return createSource(name, 0.75, -2.0, 3600.0, 15.0, 12.0, 500.0, true, true, false,
                PowerSource.CapacityUnits.MWH, 7500, 10000, 10500, 42, "Li-ion", LocalDate.of(2024, 1, 15), "TestMfg",
                "SN123", 35.5);
    }

    private static AbstractPowerSource createSource(String name, double remainPct, double timeEst, double timeInst,
            double power, double voltage, double amperage, boolean onLine, boolean charging, boolean discharging,
            PowerSource.CapacityUnits units, int current, int max, int design, int cycles, String chemistry,
            LocalDate mfgDate, String mfg, String serial, double temp) {
        return new AbstractPowerSource(name, "Device-" + name, remainPct, timeEst, timeInst, power, voltage, amperage,
                onLine, charging, discharging, units, current, max, design, cycles, chemistry, mfgDate, mfg, serial,
                temp) {
            @Override
            protected List<PowerSource> queryPowerSources() {
                return Collections.emptyList();
            }
        };
    }

    @Test
    void testGetters() {
        AbstractPowerSource ps = createSource("BAT0");
        assertThat(ps.getName(), is("BAT0"));
        assertThat(ps.getDeviceName(), is("Device-BAT0"));
        assertThat(ps.getRemainingCapacityPercent(), is(0.75));
        assertThat(ps.getTimeRemainingEstimated(), is(-2.0));
        assertThat(ps.getTimeRemainingInstant(), is(3600.0));
        assertThat(ps.getPowerUsageRate(), is(15.0));
        assertThat(ps.getVoltage(), is(12.0));
        assertThat(ps.getAmperage(), is(500.0));
        assertThat(ps.isPowerOnLine(), is(true));
        assertThat(ps.isCharging(), is(true));
        assertThat(ps.isDischarging(), is(false));
        assertThat(ps.getCapacityUnits(), is(PowerSource.CapacityUnits.MWH));
        assertThat(ps.getCurrentCapacity(), is(7500));
        assertThat(ps.getMaxCapacity(), is(10000));
        assertThat(ps.getDesignCapacity(), is(10500));
        assertThat(ps.getCycleCount(), is(42));
        assertThat(ps.getChemistry(), is("Li-ion"));
        assertThat(ps.getManufactureDate(), is(LocalDate.of(2024, 1, 15)));
        assertThat(ps.getManufacturer(), is("TestMfg"));
        assertThat(ps.getSerialNumber(), is("SN123"));
        assertThat(ps.getTemperature(), is(35.5));
    }

    @Test
    void testToStringWithVoltageAndTemperature() {
        String s = createSource("BAT0").toString();
        assertThat(s, containsString("Name: BAT0"));
        assertThat(s, containsString("12.0V"));
        assertThat(s, containsString("35.5C"));
        assertThat(s, containsString("Charging"));
    }

    @Test
    void testToStringUnknownVoltageAndTemp() {
        AbstractPowerSource ps = createSource("BAT1", 0.5, 7200.0, -0.5, 10.0, -1.0, 100.0, false, false, true,
                PowerSource.CapacityUnits.MAH, 5000, 10000, 10000, 10, "NiMH", null, "Mfg", "SN", -1.0);
        String s = ps.toString();
        // Voltage <= 0 should show "unknown"
        assertThat(s, containsString("Voltage: unknown"));
        // Null manufacture date
        assertThat(s, containsString("Manufacture Date: unknown"));
        // timeRemainingEstimated = 7200 -> "2:00"
        assertThat(s, containsString("2:00"));
        // timeRemainingInstant between -1.5 and 0 -> "Unknown"
        assertThat(s, containsString("Time Remaining Instant: Unknown"));
        // Temperature <= 0 should show "unknown"
        assertThat(s, containsString("Temperature: unknown"));
    }

    @Test
    void testUpdateAttributesNotFound() {
        AbstractPowerSource ps = createSource("BAT0");
        // queryPowerSources returns empty, so update should return false
        assertThat(ps.updateAttributes(), is(false));
    }

    @Test
    void testUpdateAttributesFound() {
        AbstractPowerSource ps = new AbstractPowerSource("BAT0", "Dev", 0.5, -1.0, -1.0, 0, 0, 0, false, false, false,
                PowerSource.CapacityUnits.MWH, 0, 0, 0, 0, "", null, "", "", 0) {
            @Override
            protected List<PowerSource> queryPowerSources() {
                return Collections.singletonList(createSource("BAT0"));
            }
        };
        assertThat(ps.updateAttributes(), is(true));
        // After update, values should match the source returned by queryPowerSources
        assertThat(ps.getRemainingCapacityPercent(), is(0.75));
        assertThat(ps.getVoltage(), is(12.0));
    }
}
