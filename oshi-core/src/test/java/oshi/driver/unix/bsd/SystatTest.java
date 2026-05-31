/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.bsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.driver.unix.bsd.Systat.BatteryFields;
import oshi.hardware.PowerSource.CapacityUnits;
import oshi.util.tuples.Triplet;

/**
 * Tests for {@link Systat} parsing methods. Each parse method is exercised with sample {@code systat -ab sensors}
 * output drawn from real OpenBSD/NetBSD systems so the tests can run on any platform.
 */
class SystatTest {

    @Test
    void testParseSensorsExtractsCpuTempVoltsAndFans() {
        // CPU rows go to cpuTemps/volts; non-cpu temp rows go to allTemps; "fan" rows to fanRPMs.
        List<String> systat = Arrays.asList("cpu0.temp0    55.00 degC", "cpu0.volt0    1.25 V DC",
                "acpitz0.temp0 40.00 degC", "acpitz1.temp0 42.00 degC", "it0.fan0    1500 RPM", "it0.fan1    2000 RPM");

        Triplet<Double, int[], Double> r = Systat.parseSensors(systat);
        // cpuTemps non-empty so the average of cpu temps wins over allTemps.
        assertThat(r.getA(), is(closeTo(55.0, 1e-9)));
        assertThat("fan count", r.getB().length, is(2));
        assertThat(r.getB()[0], is(1500));
        assertThat(r.getB()[1], is(2000));
        assertThat(r.getC(), is(closeTo(1.25, 1e-9)));
    }

    @Test
    void testParseSensorsFallsBackToAllTempsWhenNoCpuTemp() {
        List<String> systat = Arrays.asList("acpitz0.temp0 30.00 degC", "acpitz1.temp0 50.00 degC");
        Triplet<Double, int[], Double> r = Systat.parseSensors(systat);
        // Average of acpitz0 and acpitz1.
        assertThat(r.getA(), is(closeTo(40.0, 1e-9)));
        assertThat("no fans", r.getB().length, is(0));
        assertThat(r.getC(), is(closeTo(0.0, 1e-9)));
    }

    @Test
    void testParseSensorsAveragesMultipleCpuTemps() {
        List<String> systat = Arrays.asList("cpu0.temp0 50.00 degC", "cpu1.temp0 70.00 degC");
        Triplet<Double, int[], Double> r = Systat.parseSensors(systat);
        assertThat(r.getA(), is(closeTo(60.0, 1e-9)));
    }

    @Test
    void testParseSensorsIgnoresMalformedRowsAndEmptyInput() {
        // Single-token rows (no value) and blank lines must be skipped without NaN propagation.
        List<String> systat = Arrays.asList("", "cpu0.temp0", "junk", "cpu0.temp0 not-a-number");
        Triplet<Double, int[], Double> r = Systat.parseSensors(systat);
        // "not-a-number" parses to NaN and is excluded by listAverage; result is 0.
        assertThat(r.getA(), is(closeTo(0.0, 1e-9)));
        assertThat("no fans", r.getB().length, is(0));

        Triplet<Double, int[], Double> empty = Systat.parseSensors(Collections.<String>emptyList());
        assertThat(empty.getA(), is(closeTo(0.0, 1e-9)));
        assertThat("no fans on empty", empty.getB().length, is(0));
        assertThat(empty.getC(), is(closeTo(0.0, 1e-9)));
    }

    @Test
    void testParsePowerSourceNamesExtractsPrefixesAndDedupes() {
        List<String> systat = Arrays.asList("acpibat0.watthour0 12.0 Wh", "acpibat0.amphour1 5.0 Ah",
                "acpibat1.watthour0 10.0 Wh", "cpu0.temp0 55 degC", // not a power source row
                "noise without dot");

        assertThat(Systat.parsePowerSourceNames(systat), containsInAnyOrder("acpibat0", "acpibat1"));
    }

    @Test
    void testParsePowerSourceNamesEmpty() {
        assertThat(Systat.parsePowerSourceNames(Collections.<String>emptyList()), is(empty()));
    }

    @Test
    void testParseBatteryFieldsWatthour() {
        List<String> systat = Arrays.asList("acpibat0.volt0 11.10 V DC", "acpibat0.current0 0.500 A",
                "acpibat0.temp0 30.0 degC", "acpibat0.watthour0 12.00 Wh (remaining capacity)",
                "acpibat0.watthour1 24.00 Wh (last full capacity)", "acpibat0.watthour2 36.00 Wh (design capacity)",
                "acpibat1.volt0 99.0 V DC"); // different name — ignored

        BatteryFields b = Systat.parseBatteryFields("acpibat0", systat);
        assertThat(b.getVoltage(), is(closeTo(11.10, 1e-9)));
        assertThat(b.getAmperage(), is(closeTo(0.500, 1e-9)));
        assertThat(b.getTemperature(), is(closeTo(30.0, 1e-9)));
        assertThat(b.getCapacityUnits(), is(CapacityUnits.MWH));
        // x1000 conversion to mWh
        assertThat(b.getCurrentCapacity(), is(12000));
        assertThat(b.getMaxCapacity(), is(24000));
        assertThat(b.getDesignCapacity(), is(36000));
    }

    @Test
    void testParseBatteryFieldsAmphour() {
        List<String> systat = Arrays.asList("acpibat0.amphour0 1.50 Ah (remaining capacity)",
                "acpibat0.amphour1 3.00 Ah (last full capacity)", "acpibat0.amphour2 4.00 Ah (new capacity)");

        BatteryFields b = Systat.parseBatteryFields("acpibat0", systat);
        assertThat(b.getCapacityUnits(), is(CapacityUnits.MAH));
        assertThat(b.getCurrentCapacity(), is(1500));
        assertThat(b.getMaxCapacity(), is(3000));
        assertThat(b.getDesignCapacity(), is(4000));
    }

    @Test
    void testParseBatteryFieldsDefaultsWhenAbsent() {
        BatteryFields b = Systat.parseBatteryFields("acpibat0", Collections.<String>emptyList());
        assertThat(b.getVoltage(), is(closeTo(-1.0, 1e-9)));
        assertThat(b.getAmperage(), is(closeTo(0.0, 1e-9)));
        assertThat(b.getTemperature(), is(closeTo(0.0, 1e-9)));
        assertThat(b.getCapacityUnits(), is(CapacityUnits.RELATIVE));
        assertThat(b.getCurrentCapacity(), is(0));
        assertThat(b.getMaxCapacity(), is(1));
        assertThat(b.getDesignCapacity(), is(1));
    }

    @Test
    void testParseBatteryFieldsVoltMatchesWhenRowDescribesCurrent() {
        // Some sensors report "volt" without trailing 0 but include "current" in the line text.
        List<String> systat = Collections.singletonList("acpibat0.volt 12.00 V (current voltage)");
        BatteryFields b = Systat.parseBatteryFields("acpibat0", systat);
        assertThat(b.getVoltage(), is(closeTo(12.0, 1e-9)));
    }

    @Test
    void testListAverageSkipsNaNs() {
        assertThat(Systat.listAverage(Arrays.asList(10.0, Double.NaN, 20.0)), is(closeTo(15.0, 1e-9)));
        assertThat(Systat.listAverage(Arrays.asList(Double.NaN, Double.NaN)), is(closeTo(0.0, 1e-9)));
        assertThat(Systat.listAverage(Collections.<Double>emptyList()), is(closeTo(0.0, 1e-9)));
    }

    @Test
    void testParseSensorsFanZeroDefaultForNonNumeric() {
        // Fan row with a non-numeric value should default the parsed RPM to 0 rather than throw.
        List<String> systat = Arrays.asList("it0.fan0 broken RPM");
        Triplet<Double, int[], Double> r = Systat.parseSensors(systat);
        assertThat("one fan parsed", r.getB().length, is(1));
        assertThat(r.getB()[0], is(0));
    }
}
