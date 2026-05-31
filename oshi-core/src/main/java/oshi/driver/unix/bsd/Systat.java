/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.bsd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource.CapacityUnits;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Utility class parsing output from the BSD {@code systat -ab sensors} command, shared by OpenBSD and NetBSD which
 * produce the same output format.
 */
@ThreadSafe
public final class Systat {

    private static final String SYSTAT_AB_SENSORS = "systat -ab sensors";

    private Systat() {
    }

    /**
     * Runs {@code systat -ab sensors} and returns its raw output. Callers that need multiple parsed views of the same
     * snapshot (e.g. power-source names plus battery fields for several batteries) should invoke this once and pass the
     * result to the {@code parse*} methods to avoid spawning the command repeatedly.
     *
     * @return the raw lines returned by {@code systat -ab sensors}
     */
    public static List<String> querySensorLines() {
        return ExecutingCommand.runNative(SYSTAT_AB_SENSORS);
    }

    /**
     * Runs {@code systat -ab sensors} and returns CPU temperature, fan speeds, and CPU voltage.
     *
     * @return a Triplet of (cpu temperature in °C, fan RPMs, cpu voltage in V).
     */
    public static Triplet<Double, int[], Double> querySensors() {
        return parseSensors(querySensorLines());
    }

    /**
     * Parses output from {@code systat -ab sensors} into CPU temperature, fan speeds, and CPU voltage.
     *
     * @param systatLines the raw lines returned by the command
     * @return a Triplet of (cpu temperature in °C, fan RPMs, cpu voltage in V). If no CPU-specific temperature is
     *         present, the average of any other temperature sensors is returned in its place.
     */
    public static Triplet<Double, int[], Double> parseSensors(List<String> systatLines) {
        double volts = 0d;
        List<Double> cpuTemps = new ArrayList<>();
        List<Double> allTemps = new ArrayList<>();
        List<Integer> fanRPMs = new ArrayList<>();
        for (String line : systatLines) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 1) {
                if (split[0].contains("cpu")) {
                    if (split[0].contains("temp0")) {
                        cpuTemps.add(ParseUtil.parseDoubleOrDefault(split[1], Double.NaN));
                    } else if (split[0].contains("volt0")) {
                        volts = ParseUtil.parseDoubleOrDefault(split[1], 0d);
                    }
                } else if (split[0].contains("temp0")) {
                    allTemps.add(ParseUtil.parseDoubleOrDefault(split[1], Double.NaN));
                } else if (split[0].contains("fan")) {
                    fanRPMs.add(ParseUtil.parseIntOrDefault(split[1], 0));
                }
            }
        }
        // Prefer cpu temps
        double temp = cpuTemps.isEmpty() ? listAverage(allTemps) : listAverage(cpuTemps);
        int[] fans = new int[fanRPMs.size()];
        for (int i = 0; i < fans.length; i++) {
            fans[i] = fanRPMs.get(i);
        }
        return new Triplet<>(temp, fans, volts);
    }

    /**
     * Runs {@code systat -ab sensors} and returns the set of distinct power-source device prefixes (the substring
     * preceding {@code .amphour}/{@code .watthour}).
     *
     * @return a Set of power-source device names (e.g. {@code "acpibat0"}).
     */
    public static Set<String> queryPowerSourceNames() {
        return parsePowerSourceNames(querySensorLines());
    }

    /**
     * Parses output from {@code systat -ab sensors} and returns the set of distinct power-source device prefixes.
     *
     * @param systatLines the raw lines returned by the command
     * @return a Set of power-source device names (e.g. {@code "acpibat0"}).
     */
    public static Set<String> parsePowerSourceNames(List<String> systatLines) {
        Set<String> psNames = new HashSet<>();
        for (String line : systatLines) {
            if (line.contains(".amphour") || line.contains(".watthour")) {
                int dot = line.indexOf('.');
                psNames.add(line.substring(0, dot));
            }
        }
        return psNames;
    }

    /**
     * Runs {@code systat -ab sensors} and returns the battery sensor data for the named power source.
     *
     * @param name the power source name (e.g. {@code "acpibat0"})
     * @return parsed battery fields with sentinel defaults for any sensor not present.
     */
    public static BatteryFields queryBatteryFields(String name) {
        return parseBatteryFields(name, querySensorLines());
    }

    /**
     * Parses {@code systat -ab sensors} output to extract battery readings for a single power source.
     *
     * @param name        the power source name to match (e.g. {@code "acpibat0"})
     * @param systatLines the raw lines returned by {@code systat -ab sensors}
     * @return parsed battery fields with sentinel defaults for any sensor not present.
     */
    public static BatteryFields parseBatteryFields(String name, List<String> systatLines) {
        double voltage = -1d;
        double amperage = 0d;
        double temperature = 0d;
        CapacityUnits capacityUnits = CapacityUnits.RELATIVE;
        int currentCapacity = 0;
        int maxCapacity = 1;
        int designCapacity = 1;
        for (String line : systatLines) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 1 && split[0].startsWith(name)) {
                if (split[0].contains("volt0") || (split[0].contains("volt") && line.contains("current"))) {
                    voltage = ParseUtil.parseDoubleOrDefault(split[1], -1d);
                } else if (split[0].contains("current0")) {
                    amperage = ParseUtil.parseDoubleOrDefault(split[1], 0d);
                } else if (split[0].contains("temp0")) {
                    temperature = ParseUtil.parseDoubleOrDefault(split[1], 0d);
                } else if (split[0].contains("watthour") || split[0].contains("amphour")) {
                    capacityUnits = split[0].contains("watthour") ? CapacityUnits.MWH : CapacityUnits.MAH;
                    if (line.contains("remaining")) {
                        currentCapacity = (int) (1000d * ParseUtil.parseDoubleOrDefault(split[1], 0d));
                    } else if (line.contains("full")) {
                        maxCapacity = (int) (1000d * ParseUtil.parseDoubleOrDefault(split[1], 0d));
                    } else if (line.contains("new") || line.contains("design")) {
                        designCapacity = (int) (1000d * ParseUtil.parseDoubleOrDefault(split[1], 0d));
                    }
                }
            }
        }
        return new BatteryFields(voltage, amperage, temperature, capacityUnits, currentCapacity, maxCapacity,
                designCapacity);
    }

    /**
     * Returns the average of the non-NaN values in {@code doubles}, or {@code 0} if none are present.
     *
     * @param doubles the values to average; NaN entries are skipped
     * @return the arithmetic mean of non-NaN values, or {@code 0} if {@code doubles} contains no non-NaN entries
     */
    static double listAverage(List<Double> doubles) {
        double sum = 0d;
        int count = 0;
        for (Double d : doubles) {
            if (!d.isNaN()) {
                sum += d;
                count++;
            }
        }
        return count > 0 ? sum / count : 0d;
    }

    /**
     * Immutable holder for battery sensor readings parsed from {@code systat -ab sensors}.
     */
    @ThreadSafe
    public static final class BatteryFields {
        private final double voltage;
        private final double amperage;
        private final double temperature;
        private final CapacityUnits capacityUnits;
        private final int currentCapacity;
        private final int maxCapacity;
        private final int designCapacity;

        BatteryFields(double voltage, double amperage, double temperature, CapacityUnits capacityUnits,
                int currentCapacity, int maxCapacity, int designCapacity) {
            this.voltage = voltage;
            this.amperage = amperage;
            this.temperature = temperature;
            this.capacityUnits = capacityUnits;
            this.currentCapacity = currentCapacity;
            this.maxCapacity = maxCapacity;
            this.designCapacity = designCapacity;
        }

        public double getVoltage() {
            return voltage;
        }

        public double getAmperage() {
            return amperage;
        }

        public double getTemperature() {
            return temperature;
        }

        public CapacityUnits getCapacityUnits() {
            return capacityUnits;
        }

        public int getCurrentCapacity() {
            return currentCapacity;
        }

        public int getMaxCapacity() {
            return maxCapacity;
        }

        public int getDesignCapacity() {
            return designCapacity;
        }
    }
}
