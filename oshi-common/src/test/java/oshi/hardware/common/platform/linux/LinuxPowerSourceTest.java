/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.PowerSource;
import oshi.hardware.PowerSource.CapacityUnits;

class LinuxPowerSourceTest {

    // Tolerance for floating-point comparisons
    private static final double EPS = 1e-6;

    private static Map<LinuxPowerSource.Prop, String> sampleProps() {
        Map<LinuxPowerSource.Prop, String> props = new EnumMap<>(LinuxPowerSource.Prop.class);
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_NAME, "BAT0");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_STATUS, "Discharging");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_PRESENT, "1");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_TECHNOLOGY, "Li-ion");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CYCLE_COUNT, "481");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_VOLTAGE_NOW, "7400000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_POWER_NOW, "9361000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_ENERGY_FULL_DESIGN, "48248000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_ENERGY_FULL, "40877000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_ENERGY_NOW, "20712000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CAPACITY, "50");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_MODEL_NAME, "UX32-65");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_MANUFACTURER, "ASUSTeK");
        return props;
    }

    @Test
    void testSampleUeventDischarging() {
        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", sampleProps());

        assertThat(ps.getName(), is("BAT0"));
        assertThat(ps.getDeviceName(), is("UX32-65"));
        assertThat(ps.getChemistry(), is("Li-ion"));
        assertThat(ps.getManufacturer(), is("ASUSTeK"));
        assertThat(ps.getCycleCount(), is(481));

        assertThat(ps.getRemainingCapacityPercent(), closeTo(0.50, EPS));
        assertThat(ps.isCharging(), is(false));
        assertThat(ps.isDischarging(), is(true));

        // Voltage: 7400000 µV → 7.4 V
        assertThat(ps.getVoltage(), closeTo(7.4, EPS));

        // ENERGY_* present → MWh units
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.MWH));

        // ENERGY_NOW: 20712000 µWh → 20712 mWh
        assertThat(ps.getCurrentCapacity(), is(20712));
        // ENERGY_FULL: 40877000 µWh → 40877 mWh
        assertThat(ps.getMaxCapacity(), is(40877));
        // ENERGY_FULL_DESIGN: 48248000 µWh → 48248 mWh
        assertThat(ps.getDesignCapacity(), is(48248));

        // POWER_NOW: 9361000 µW → 9361 mW, negated because discharging
        assertThat(ps.getPowerUsageRate(), closeTo(-9361.0, EPS));
        assertThat(ps.getPowerUsageRate(), lessThan(0d));

        // No CURRENT_NOW → amperage derived: P/V = -9361 mW / 7.4 V ≈ -1265 mA
        assertThat(ps.getAmperage(), closeTo(-9361.0 / 7.4, 1e-3));
    }

    @Test
    void testChargingSignConvention() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_STATUS, "Charging");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.isCharging(), is(true));
        assertThat(ps.isDischarging(), is(false));
        // Power and amperage should be positive when charging
        assertThat(ps.getPowerUsageRate(), closeTo(9361.0, EPS));
        assertThat(ps.getAmperage(), closeTo(9361.0 / 7.4, 1e-3));
    }

    @Test
    void testChargeNowFallback() {
        // No ENERGY_* keys → should fall back to CHARGE_* and use MAH units
        Map<LinuxPowerSource.Prop, String> props = new EnumMap<>(LinuxPowerSource.Prop.class);
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_STATUS, "Discharging");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CHARGE_NOW, "3000000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CHARGE_FULL, "5000000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CHARGE_FULL_DESIGN, "6000000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_VOLTAGE_NOW, "7400000");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CURRENT_NOW, "1500000");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getCapacityUnits(), is(CapacityUnits.MAH));
        assertThat(ps.getCurrentCapacity(), is(3000));
        assertThat(ps.getMaxCapacity(), is(5000));
        assertThat(ps.getDesignCapacity(), is(6000));

        // CURRENT_NOW: 1500000 µA → 1500 mA, negated because discharging
        assertThat(ps.getAmperage(), closeTo(-1500.0, EPS));
        // No POWER_NOW → derived: I * V = -1500 mA * 7.4 V = -11100 mW
        assertThat(ps.getPowerUsageRate(), closeTo(-1500.0 * 7.4, 1e-3));
    }

    @Test
    void testBothPowerAndCurrentPresent() {
        // When both POWER_NOW and CURRENT_NOW are present, both are used directly
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_CURRENT_NOW, "1200000");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getPowerUsageRate(), closeTo(-9361.0, EPS));
        assertThat(ps.getAmperage(), closeTo(-1200.0, EPS));
    }

    @Test
    void testParseErrorSentinelNotZero() {
        // A corrupt value should leave capacity as -1, not silently become 0
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_ENERGY_NOW, "corrupt");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        // ENERGY_NOW fails to parse → falls back to CHARGE_NOW (absent) → -1
        assertThat(ps.getCurrentCapacity(), is(-1));
        assertThat(ps.getCapacityUnits(), is(CapacityUnits.MAH));
    }

    @Test
    void testTemperature() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        // TEMP is in tenths of degrees Celsius: 293 → 29.3°C
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_TEMP, "293");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getTemperature(), closeTo(29.3, EPS));
    }

    @Test
    void testManufactureDate() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_MANUFACTURE_YEAR, "2021");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_MANUFACTURE_MONTH, "6");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_MANUFACTURE_DAY, "15");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getManufactureDate().getYear(), is(2021));
        assertThat(ps.getManufactureDate().getMonthValue(), is(6));
        assertThat(ps.getManufactureDate().getDayOfMonth(), is(15));
    }

    @Test
    void testManufactureDatePartiallyMissing() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_MANUFACTURE_YEAR, "2021");
        // month and day absent → date should be null

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getManufactureDate(), is(nullValue()));
    }

    @Test
    void testTimeRemainingDischarging() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        // TIME_TO_EMPTY_NOW in seconds
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_TIME_TO_EMPTY_NOW, "3600");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getTimeRemainingInstant(), closeTo(3600.0, EPS));
    }

    @Test
    void testTimeRemainingCharging() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_STATUS, "Charging");
        props.put(LinuxPowerSource.Prop.POWER_SUPPLY_TIME_TO_FULL_NOW, "1800");

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getTimeRemainingInstant(), closeTo(1800.0, EPS));
    }

    @Test
    void testFallbackNameWhenNameKeyAbsent() {
        Map<LinuxPowerSource.Prop, String> props = sampleProps();
        props.remove(LinuxPowerSource.Prop.POWER_SUPPLY_NAME);

        LinuxPowerSource ps = LinuxPowerSource.buildPowerSource("BAT0", props);

        assertThat(ps.getName(), is("BAT0"));
    }

    @Test
    void testCopyConstructor() {
        LinuxPowerSource original = LinuxPowerSource.buildPowerSource("BAT0", sampleProps());
        LinuxPowerSource copy = new LinuxPowerSource(original);

        assertThat(copy.getName(), is(original.getName()));
        assertThat(copy.getDeviceName(), is(original.getDeviceName()));
        assertThat(copy.getRemainingCapacityPercent(), closeTo(original.getRemainingCapacityPercent(), EPS));
        assertThat(copy.getPowerUsageRate(), closeTo(original.getPowerUsageRate(), EPS));
        assertThat(copy.getVoltage(), closeTo(original.getVoltage(), EPS));
        assertThat(copy.getAmperage(), closeTo(original.getAmperage(), EPS));
        assertThat(copy.isCharging(), is(original.isCharging()));
        assertThat(copy.isDischarging(), is(original.isDischarging()));
        assertThat(copy.getCapacityUnits(), is(original.getCapacityUnits()));
        assertThat(copy.getCurrentCapacity(), is(original.getCurrentCapacity()));
        assertThat(copy.getMaxCapacity(), is(original.getMaxCapacity()));
        assertThat(copy.getDesignCapacity(), is(original.getDesignCapacity()));
        assertThat(copy.getCycleCount(), is(original.getCycleCount()));
        assertThat(copy.getChemistry(), is(original.getChemistry()));
        assertThat(copy.getManufacturer(), is(original.getManufacturer()));
        assertThat(copy.getSerialNumber(), is(original.getSerialNumber()));
    }

    @Test
    void testGetPowerSourcesNonexistentPath(@TempDir Path tempDir) {
        List<PowerSource> result = LinuxPowerSource.getPowerSources(tempDir.resolve("power_supply_missing").toString());
        assertThat(result, hasSize(0));
    }

    @Test
    void testGetPowerSourcesEmptyDir(@TempDir Path tempDir) {
        List<PowerSource> result = LinuxPowerSource.getPowerSources(tempDir.toString());
        assertThat(result, hasSize(0));
    }

    @Test
    void testGetPowerSourcesFiltersAdapters(@TempDir Path tempDir) throws IOException {
        for (String name : new String[] { "ADP1", "AC0", "USBC-charger" }) {
            Files.createDirectories(tempDir.resolve(name));
        }
        List<PowerSource> result = LinuxPowerSource.getPowerSources(tempDir.toString());
        assertThat(result, hasSize(0));
    }

    @Test
    void testGetPowerSourcesUeventParsing(@TempDir Path tempDir) throws IOException {
        Path bat = tempDir.resolve("BAT0");
        Files.createDirectories(bat);
        String uevent = "POWER_SUPPLY_NAME=BAT0\n" + "POWER_SUPPLY_STATUS=Discharging\n" + "POWER_SUPPLY_PRESENT=1\n"
                + "POWER_SUPPLY_TECHNOLOGY=Li-ion\n" + "POWER_SUPPLY_VOLTAGE_NOW=7400000\n"
                + "POWER_SUPPLY_ENERGY_NOW=20712000\n" + "POWER_SUPPLY_ENERGY_FULL=40877000\n"
                + "POWER_SUPPLY_ENERGY_FULL_DESIGN=48248000\n" + "POWER_SUPPLY_POWER_NOW=9361000\n"
                + "POWER_SUPPLY_CAPACITY=50\n" + "IGNORED_KEY=should_be_skipped\n" + "MALFORMED_LINE\n";
        Files.write(bat.resolve("uevent"), uevent.getBytes(StandardCharsets.UTF_8));

        List<PowerSource> result = LinuxPowerSource.getPowerSources(tempDir.toString());

        assertThat(result, hasSize(1));
        PowerSource ps = result.get(0);
        assertThat(ps.getName(), is("BAT0"));
        assertThat(ps.isDischarging(), is(true));
        assertThat(ps.getChemistry(), is("Li-ion"));
        assertThat(ps.getCurrentCapacity(), is(20712));
        assertThat(ps.getMaxCapacity(), is(40877));
    }

    @Test
    void testGetPowerSourcesSkipsPresentZero(@TempDir Path tempDir) throws IOException {
        Path bat = tempDir.resolve("BAT0");
        Files.createDirectories(bat);
        Files.write(bat.resolve("uevent"),
                "POWER_SUPPLY_NAME=BAT0\nPOWER_SUPPLY_PRESENT=0\n".getBytes(StandardCharsets.UTF_8));

        List<PowerSource> result = LinuxPowerSource.getPowerSources(tempDir.toString());
        assertThat(result, hasSize(0));
    }

    @Test
    void testGetPowerSourcesCopyRoundTrip(@TempDir Path tempDir) throws IOException {
        Path bat = tempDir.resolve("BAT0");
        Files.createDirectories(bat);
        String uevent = "POWER_SUPPLY_NAME=BAT0\nPOWER_SUPPLY_STATUS=Charging\n"
                + "POWER_SUPPLY_ENERGY_NOW=20000000\nPOWER_SUPPLY_ENERGY_FULL=40000000\n";
        Files.write(bat.resolve("uevent"), uevent.getBytes(StandardCharsets.UTF_8));

        List<PowerSource> sources = LinuxPowerSource.getPowerSources(tempDir.toString());
        assertThat(sources, hasSize(1));
        LinuxPowerSource ps = (LinuxPowerSource) sources.get(0);

        // queryPowerSources() delegates to getPowerSources(SysPath.POWER_SUPPLY) on real
        // systems; here we verify the copy constructor round-trip used by that path
        LinuxPowerSource copy = new LinuxPowerSource(ps);
        assertThat(copy.getName(), is(ps.getName()));
        assertThat(copy.isCharging(), is(true));
    }
}
