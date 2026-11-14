/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static oshi.driver.common.unix.netbsd.EnvstatTest.envstatX;
import static oshi.driver.common.unix.netbsd.EnvstatTest.sensor;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.driver.common.unix.netbsd.Envstat;
import oshi.driver.common.unix.netbsd.EnvstatTest;
import oshi.hardware.PowerSource;
import oshi.hardware.PowerSource.CapacityUnits;

class NetBsdPowerSourceTest {

    @Test
    void testDischargingWattHourBattery() {
        List<PowerSource> sources = NetBsdPowerSource.parsePowerSources(Envstat.parseSensors(EnvstatTest.laptop()));
        // acpibat1 reports no battery present
        assertThat(sources, hasSize(1));
        PowerSource bat = sources.get(0);
        assertThat(bat.getName(), is("bat0"));
        assertThat(bat.getCapacityUnits(), is(CapacityUnits.MWH));
        assertThat(bat.getCurrentCapacity(), is(25_000));
        assertThat(bat.getMaxCapacity(), is(50_000));
        assertThat(bat.getDesignCapacity(), is(57_000));
        assertThat(bat.getRemainingCapacityPercent(), closeTo(0.5, 1e-9));
        assertThat(bat.getVoltage(), closeTo(12d, 1e-9));
        assertThat(bat.isPowerOnLine(), is(false));
        assertThat(bat.isCharging(), is(false));
        assertThat(bat.isDischarging(), is(true));
        // 10 W discharge, reported negative
        assertThat(bat.getPowerUsageRate(), closeTo(-10_000d, 1e-9));
        assertThat(bat.getAmperage(), closeTo(-10_000d / 12d, 1e-9));
        // 25 Wh at 10 W
        assertThat(bat.getTimeRemainingEstimated(), closeTo(9000d, 1e-9));
    }

    @Test
    void testChargingAmpereHourBattery() {
        List<String> lines = envstatX(new Object[] { "acpiacad0", sensor("connected", "Indicator", "1", "valid") },
                new Object[] { "acpibat0", sensor("present", "Indicator", "1", "valid"),
                        sensor("design cap", "Ampere hour", "4000000", "valid"),
                        sensor("last full cap", "Ampere hour", "3800000", "valid"),
                        sensor("voltage", "Voltage DC", "11000000", "valid"),
                        sensor("charge rate", "Ampere", "1500000", "valid"),
                        sensor("discharge rate", "Ampere", "", "invalid"),
                        sensor("charge", "Ampere hour", "1900000", "valid"),
                        sensor("charging", "Battery charge", "1", "valid") });
        List<PowerSource> sources = NetBsdPowerSource.parsePowerSources(Envstat.parseSensors(lines));
        assertThat(sources, hasSize(1));
        PowerSource bat = sources.get(0);
        assertThat(bat.getCapacityUnits(), is(CapacityUnits.MAH));
        assertThat(bat.getCurrentCapacity(), is(1900));
        assertThat(bat.getMaxCapacity(), is(3800));
        assertThat(bat.isPowerOnLine(), is(true));
        assertThat(bat.isCharging(), is(true));
        assertThat(bat.isDischarging(), is(false));
        assertThat(bat.getAmperage(), closeTo(1500d, 1e-9));
        assertThat(bat.getPowerUsageRate(), closeTo(16_500d, 1e-9));
        assertThat(bat.getTimeRemainingEstimated(), is(-2d));
    }

    @Test
    void testNoBattery() {
        List<String> lines = envstatX(
                new Object[] { "acpitz0", sensor("temperature", "Temperature", "313150000", "valid") });
        assertThat(NetBsdPowerSource.parsePowerSources(Envstat.parseSensors(lines)), is(empty()));
    }
}
