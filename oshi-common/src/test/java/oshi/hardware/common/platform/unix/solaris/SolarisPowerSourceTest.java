/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.unix.solaris.SolarisPowerSource.BatteryReadings;

/**
 * Tests that {@link SolarisPowerSource} reports a battery only when the battery kstats were read.
 */
class SolarisPowerSourceTest {

    private static List<PowerSource> build(BatteryReadings readings) {
        return SolarisPowerSource.buildPowerSources("BAT0", readings,
                (name, device, percent, estimated, instant, rate, voltage, amperage, online, charging, discharging,
                        units, current, max, design, cycles, chemistry, date, manufacturer, serial,
                        temperature) -> new SolarisPowerSource(name, device, percent, estimated, instant, rate, voltage,
                                amperage, online, charging, discharging, units, current, max, design, cycles, chemistry,
                                date, manufacturer, serial, temperature) {
                            @Override
                            protected List<PowerSource> queryPowerSources() {
                                return Collections.emptyList();
                            }
                        });
    }

    @Test
    void testNoBatteryKstatsMeansNoPowerSource() {
        // A machine with no battery, such as a SPARC server, has neither the battery nor the acpi_drv kstat module
        assertThat(build(new BatteryReadings()), is(empty()));
    }

    @Test
    void testBatteryKstatsMakeOnePowerSource() {
        BatteryReadings readings = new BatteryReadings();
        readings.bstValid = true;
        readings.bstRemCap = 50_000L;
        readings.bstVoltage = 12_000L;
        List<PowerSource> sources = build(readings);
        assertThat(sources, hasSize(1));
        assertThat(sources.get(0).getName(), is("BAT0"));

        BatteryReadings infoOnly = new BatteryReadings();
        infoOnly.bifValid = true;
        infoOnly.bifDesignCap = 60_000L;
        assertThat(build(infoOnly), hasSize(1));
    }
}
