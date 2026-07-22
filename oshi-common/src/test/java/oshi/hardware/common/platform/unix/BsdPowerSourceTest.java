/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.driver.common.unix.bsd.Systat;
import oshi.driver.common.unix.bsd.Systat.BatteryFields;
import oshi.hardware.PowerSource;
import oshi.hardware.PowerSource.CapacityUnits;

class BsdPowerSourceTest {

    // Real `systat -ab sensors` battery rows (watt-hour battery): voltage 11.10 V, current 0.5 A, temp 30 degC,
    // remaining/full/design capacity 12/24/36 Wh -> current/max/design 12000/24000/36000 mWh.
    private static final List<String> SYSTAT_WATTHOUR = Arrays.asList("acpibat0.volt0 11.10 V DC",
            "acpibat0.current0 0.500 A", "acpibat0.temp0 30.0 degC", "acpibat0.watthour0 12.00 Wh (remaining capacity)",
            "acpibat0.watthour1 24.00 Wh (last full capacity)", "acpibat0.watthour2 36.00 Wh (design capacity)");

    private static BatteryFields watthourFields() {
        return Systat.parseBatteryFields("acpibat0", SYSTAT_WATTHOUR);
    }

    @Test
    void testDischarging() {
        // apm -b state 1 (low) -> discharging & on-line; apm -m 90 minutes; apm -l 75 percent
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0", watthourFields(), 1, 90, 75);
        // "acpi" prefix is stripped from the name
        assertThat(ps.getName(), is("bat0"));
        assertThat(ps.isPowerOnLine(), is(true));
        assertThat(ps.isDischarging(), is(true));
        assertThat(ps.isCharging(), is(false));
        // time is in minutes -> seconds
        assertThat(ps.getTimeRemainingEstimated(), is(closeTo(60d * 90, 1e-9)));
        assertThat(ps.getRemainingCapacityPercent(), is(closeTo(0.75, 1e-9)));
        // power derived from P = IV = 0.5 A * 11.10 V
        assertThat(ps.getPowerUsageRate(), is(closeTo(0.5 * 11.10, 1e-9)));
        assertThat(ps.getVoltage(), is(closeTo(11.10, 1e-9)));
        assertThat(ps.getAmperage(), is(closeTo(0.5, 1e-9)));
        assertThat(ps.getCurrentCapacity(), is(12000));
        assertThat(ps.getMaxCapacity(), is(24000));
        assertThat(ps.getDesignCapacity(), is(36000));
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.MWH));
    }

    @Test
    void testDischargingUnknownTime() {
        // apm -b state 2 (critical); apm -m returns -1 (unknown) -> time remaining unknown, still discharging
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0", watthourFields(), 2, -1, 50);
        assertThat(ps.isDischarging(), is(true));
        assertThat(ps.isPowerOnLine(), is(true));
        assertThat(ps.getTimeRemainingEstimated(), is(closeTo(-1d, 1e-9)));
        assertThat(ps.getRemainingCapacityPercent(), is(closeTo(0.50, 1e-9)));
    }

    @Test
    void testCharging() {
        // apm -b state 3 (charging): on-line & charging, not discharging; time remaining stays unknown
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0", watthourFields(), 3, -1, 80);
        assertThat(ps.isPowerOnLine(), is(true));
        assertThat(ps.isCharging(), is(true));
        assertThat(ps.isDischarging(), is(false));
        assertThat(ps.getTimeRemainingEstimated(), is(closeTo(-1d, 1e-9)));
        assertThat(ps.getRemainingCapacityPercent(), is(closeTo(0.80, 1e-9)));
    }

    @Test
    void testAbsentNoPowerNoLife() {
        // apm -b state 4 (absent): not on-line, not charging/discharging; empty sensors -> no voltage so no power
        // rate; apm -l -1 -> remaining capacity stays at the 1.0 default
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0",
                Systat.parseBatteryFields("acpibat0", Collections.<String>emptyList()), 4, -1, -1);
        assertThat(ps.isPowerOnLine(), is(false));
        assertThat(ps.isCharging(), is(false));
        assertThat(ps.isDischarging(), is(false));
        assertThat(ps.getTimeRemainingEstimated(), is(closeTo(-1d, 1e-9)));
        assertThat(ps.getRemainingCapacityPercent(), is(closeTo(1.0, 1e-9)));
        assertThat(ps.getPowerUsageRate(), is(closeTo(0d, 1e-9)));
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.RELATIVE));
    }

    @Test
    void testUnknownStateBehavesAsOffline() {
        // apm -b returns 255 (unknown / unparseable) -> treated like absent (state >= 4)
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0", watthourFields(), 255, -1, -1);
        assertThat(ps.isPowerOnLine(), is(false));
        assertThat(ps.isCharging(), is(false));
        assertThat(ps.isDischarging(), is(false));
    }

    @Test
    void testMaxCapacityRaisedToDesign() {
        // remaining/full/design 30/10/20 Wh: max (10) < design (20) and max < current (30) -> max raised to design
        List<String> systat = Arrays.asList("acpibat0.watthour0 30.00 Wh (remaining capacity)",
                "acpibat0.watthour1 10.00 Wh (last full capacity)", "acpibat0.watthour2 20.00 Wh (design capacity)");
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0", Systat.parseBatteryFields("acpibat0", systat), 1,
                10, 90);
        assertThat(ps.getCurrentCapacity(), is(30000));
        assertThat(ps.getMaxCapacity(), is(20000));
        assertThat(ps.getDesignCapacity(), is(20000));
    }

    @Test
    void testDesignCapacityRaisedToMax() {
        // remaining/full/design 30/25/5 Wh: design (5) < max (25) and design < current (30) -> design raised to max
        List<String> systat = Arrays.asList("acpibat0.watthour0 30.00 Wh (remaining capacity)",
                "acpibat0.watthour1 25.00 Wh (last full capacity)", "acpibat0.watthour2 5.00 Wh (design capacity)");
        PowerSource ps = BsdPowerSource.buildPowerSource("acpibat0", Systat.parseBatteryFields("acpibat0", systat), 1,
                10, 90);
        assertThat(ps.getMaxCapacity(), is(25000));
        assertThat(ps.getDesignCapacity(), is(25000));
    }

    @Test
    void testNonAcpiNameKept() {
        // A name that does not start with "acpi" is used verbatim
        PowerSource ps = BsdPowerSource.buildPowerSource("bat0", watthourFields(), 3, -1, 100);
        assertThat(ps.getName(), is("bat0"));
    }
}
