/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.hardware.PowerSource;
import oshi.hardware.PowerSource.CapacityUnits;
import oshi.util.Constants;

class FreeBsdPowerSourceTest {

    /** A concrete power source for the factory; it never queries anything. */
    private static final class TestPowerSource extends FreeBsdPowerSource {
        TestPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
                double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate,
                double psVoltage, double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
                CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
                int psCycleCount, String psChemistry, @Nullable LocalDate psManufactureDate, String psManufacturer,
                String psSerialNumber, double psTemperature) {
            super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                    psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                    psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                    psManufacturer, psSerialNumber, psTemperature);
        }

        @Override
        protected List<PowerSource> queryPowerSources() {
            return Collections.emptyList();
        }
    }

    // acpiconf -i 0 for a battery reporting in mW, laid out by acpi_battinfo() in usr.sbin/acpi/acpiconf/acpiconf.c
    private static final String MW_DISCHARGING = """
            Design capacity:\t57000 mWh
            Last full capacity:\t51300 mWh
            Technology:\t\tsecondary (rechargeable)
            Design voltage:\t\t11400 mV
            Capacity (warn):\t2565 mWh
            Capacity (low):\t\t200 mWh
            Cycle Count:\t\t142
            Measurement Accuracy:\t0%
            Max Average Interval:\t0 ms
            Min Average Interval:\t0 ms
            Low/warn granularity:\t10 mWh
            Warn/full granularity:\t10 mWh
            Model number:\t\t5B10W13930
            Serial number:\t\t1234
            Type:\t\t\tLION
            OEM info:\t\tSMP
            State:\t\t\tdischarging
            Remaining capacity:\t80%
            Remaining time:\t\t2:15
            Present rate:\t\t9120 mW
            Present voltage:\t12160 mV
            """;

    // A battery reporting in mA, which acpiconf converts to mW alongside
    private static final String MA_CHARGING = """
            Design capacity:\t4000 mAh
            Last full capacity:\t3800 mAh
            Technology:\t\tsecondary (rechargeable)
            Design voltage:\t\t7600 mV
            Capacity (warn):\t190 mAh
            Capacity (low):\t\t40 mAh
            Low/warn granularity:\t40 mAh
            Warn/full granularity:\t40 mAh
            Model number:\t\tDELL 1234
            Serial number:\t\t
            Type:\t\t\tLiP
            OEM info:\t\tSamsung SDI
            State:\t\t\tcharging
            Remaining capacity:\t50%
            Remaining time:\t\tunknown
            Present rate:\t\t1500 mA (12000 mW)
            Present voltage:\t8000 mV
            """;

    private static final String NOT_PRESENT = """
            Design capacity:\tunknown
            Last full capacity:\tunknown
            Technology:\t\tsecondary (rechargeable)
            Design voltage:\t\tunknown
            Model number:\t\t
            Serial number:\t\t
            Type:\t\t\t
            OEM info:\t\t
            State:\t\t\tnot present
            Present voltage:\tunknown
            """;

    private static @Nullable TestPowerSource parse(String acpiconf, boolean acline, int state, int time, int life) {
        return FreeBsdPowerSource.parseBattery("BAT0", acpiconf.lines().toList(), acline, state, time, life,
                TestPowerSource::new);
    }

    @Test
    void testMilliwattBatteryDischarging() {
        TestPowerSource ps = parse(MW_DISCHARGING, false, -1, -1, -1);
        assertNotNull(ps);
        assertThat(ps.getName(), is("BAT0"));
        assertThat(ps.getDeviceName(), is("5B10W13930"));
        assertThat(ps.getSerialNumber(), is("1234"));
        assertThat(ps.getChemistry(), is("LION"));
        assertThat(ps.getManufacturer(), is("SMP"));
        assertThat(ps.isDischarging(), is(true));
        assertThat(ps.isCharging(), is(false));
        assertThat(ps.isPowerOnLine(), is(false));
        assertThat(ps.getRemainingCapacityPercent(), closeTo(0.8, 1e-9));
        assertThat(ps.getTimeRemainingInstant(), closeTo(8100d, 1e-9));
        assertThat(ps.getTimeRemainingEstimated(), closeTo(8100d, 1e-9));
        assertThat(ps.getPowerUsageRate(), closeTo(-9120d, 1e-9));
        assertThat(ps.getVoltage(), closeTo(12.16, 1e-9));
        // 9120 mW / 12.16 V = 750 mA
        assertThat(ps.getAmperage(), closeTo(-750d, 1e-9));
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.MWH));
        assertThat(ps.getDesignCapacity(), is(57000));
        assertThat(ps.getMaxCapacity(), is(51300));
        assertThat(ps.getCurrentCapacity(), is(41040));
        assertThat(ps.getCycleCount(), is(142));
    }

    @Test
    void testMilliampBatteryCharging() {
        TestPowerSource ps = parse(MA_CHARGING, true, -1, -1, -1);
        assertNotNull(ps);
        assertThat(ps.getSerialNumber(), is(Constants.UNKNOWN));
        assertThat(ps.isCharging(), is(true));
        assertThat(ps.isDischarging(), is(false));
        assertThat(ps.isPowerOnLine(), is(true));
        assertThat(ps.getTimeRemainingInstant(), closeTo(-1d, 1e-9));
        assertThat(ps.getPowerUsageRate(), closeTo(12000d, 1e-9));
        assertThat(ps.getAmperage(), closeTo(1500d, 1e-9));
        assertThat(ps.getVoltage(), closeTo(8d, 1e-9));
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.MAH));
        assertThat(ps.getMaxCapacity(), is(3800));
        assertThat(ps.getCurrentCapacity(), is(1900));
        assertThat(ps.getCycleCount(), is(-1));
    }

    @Test
    void testNotPresent() {
        assertNull(parse(NOT_PRESENT, true, -1, -1, -1));
    }

    @Test
    void testSysctlTotalsWhenAcpiconfFails() {
        // state 1 = discharging, 95 minutes, 64%
        TestPowerSource ps = parse("", false, 1, 95, 64);
        assertNotNull(ps);
        assertThat(ps.isDischarging(), is(true));
        assertThat(ps.getRemainingCapacityPercent(), closeTo(0.64, 1e-9));
        assertThat(ps.getTimeRemainingEstimated(), closeTo(5700d, 1e-9));
        assertThat(ps.getTimeRemainingInstant(), closeTo(5700d, 1e-9));
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.RELATIVE));
        assertThat(ps.getDeviceName(), is(Constants.UNKNOWN));
        assertThat(ps.getVoltage(), closeTo(-1d, 1e-9));
    }

    @Test
    void testNoBattery() {
        // hw.acpi.battery.units is missing, so the accessor returns its default
        List<PowerSource> batteries = FreeBsdPowerSource.queryBatteries((name, def) -> def, TestPowerSource::new);
        assertThat(batteries, is(empty()));
    }
}
