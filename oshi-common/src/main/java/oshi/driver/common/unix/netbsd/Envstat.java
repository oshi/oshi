/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.netbsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Reads NetBSD's environmental sensors ({@code envsys(4)}) through {@code envstat -x}, which prints the kernel's sensor
 * dictionary as an XML property list and needs no privileges.
 * <p>
 * Values are raw: temperatures in microkelvin, voltages in microvolts, power in microwatts, current in microamperes,
 * energy in microwatt-hours or microampere-hours, fans in RPM, and indicators as 0 or 1.
 */
@ThreadSafe
public final class Envstat {

    // envstat lives in /usr/sbin, which an unprivileged user's PATH often omits
    private static final String ENVSTAT_X = "/usr/sbin/envstat -x";

    private static final Pattern KEY = Pattern.compile("<key>(.*)</key>");
    private static final Pattern VALUE = Pattern.compile("<(integer|string)>(.*)</\\1>");

    private static final double MICRO = 1_000_000d;
    private static final double ZERO_CELSIUS_KELVIN = 273.15;

    /** The {@code type} of a temperature sensor. */
    public static final String TEMPERATURE = "Temperature";
    /** The {@code type} of a fan speed sensor. */
    public static final String FAN = "Fan";
    /** The {@code type} of a DC voltage sensor. */
    public static final String VOLTAGE_DC = "Voltage DC";
    /** The {@code type} of a power sensor. */
    public static final String WATTS = "Watts";
    /** The {@code type} of a current sensor. */
    public static final String AMPERE = "Ampere";
    /** The {@code type} of an energy sensor measured in watt-hours. */
    public static final String WATT_HOUR = "Watt hour";
    /** The {@code type} of a charge sensor measured in ampere-hours. */
    public static final String AMPERE_HOUR = "Ampere hour";
    /** The {@code type} of a true/false sensor. */
    public static final String INDICATOR = "Indicator";
    /** The {@code type} of a battery's charging indicator. */
    public static final String BATTERY_CHARGE = "Battery charge";

    private Envstat() {
    }

    /**
     * One sensor reading.
     */
    @Immutable
    public static final class Sensor {
        private final String device;
        private final String description;
        private final String type;
        private final long value;
        private final boolean valid;

        Sensor(String device, String description, String type, long value, boolean valid) {
            this.device = device;
            this.description = description;
            this.type = type;
            this.value = value;
            this.valid = valid;
        }

        /**
         * Gets the device that reports the sensor.
         *
         * @return the device name, such as {@code acpibat0}
         */
        public String getDevice() {
            return device;
        }

        /**
         * Gets the sensor description.
         *
         * @return the description, such as {@code cpu0 temperature} or {@code charge}
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets the sensor type.
         *
         * @return the type, such as {@link Envstat#TEMPERATURE}
         */
        public String getType() {
            return type;
        }

        /**
         * Gets the raw value, in the units described for {@link Envstat}.
         *
         * @return the value
         */
        public long getValue() {
            return value;
        }

        /**
         * Gets whether the sensor has a current value. Sensors whose state is {@code invalid} or {@code unknown} do
         * not.
         *
         * @return whether the value is usable
         */
        public boolean isValid() {
            return valid;
        }
    }

    /**
     * Runs {@code envstat -x} and parses every sensor it reports.
     *
     * @return the sensors, empty if there are none or {@code envstat} is unavailable
     */
    public static List<Sensor> querySensors() {
        return parseSensors(ExecutingCommand.runNative(ENVSTAT_X));
    }

    /**
     * Parses the XML property list printed by {@code envstat -x}: a dictionary of device names, each holding an array
     * of sensor dictionaries followed by a {@code device-properties} dictionary.
     *
     * @param lines the lines of {@code envstat -x} output
     * @return the sensors, in the order printed
     */
    public static List<Sensor> parseSensors(List<String> lines) {
        List<Sensor> sensors = new ArrayList<>();
        String device = "";
        String key = null;
        String lastKey = "";
        Map<String, String> dict = new HashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            Matcher k = KEY.matcher(line);
            if (k.matches()) {
                key = unescape(k.group(1));
                lastKey = key;
                continue;
            }
            if ("<array>".equals(line)) {
                // A device's sensor array follows its name
                device = lastKey;
            } else if ("<dict>".equals(line)) {
                dict.clear();
            } else if ("</dict>".equals(line)) {
                String type = dict.get("type");
                if (type != null) {
                    String state = dict.getOrDefault("state", "");
                    sensors.add(new Sensor(device, dict.getOrDefault("description", ""), type,
                            ParseUtil.parseLongOrDefault(dict.getOrDefault("cur-value", ""), 0L),
                            "valid".equals(state) || state.startsWith("warning") || state.startsWith("critical")));
                }
                dict.clear();
            } else if (key != null) {
                Matcher v = VALUE.matcher(line);
                if (v.matches()) {
                    dict.put(key, unescape(v.group(2)));
                }
            }
            key = null;
        }
        return Collections.unmodifiableList(sensors);
    }

    /**
     * Summarizes sensors as {@code Sensors} reports them.
     *
     * @param sensors the parsed sensors
     * @return the CPU temperature in degrees Celsius (the average of sensors described as {@code cpu...}, or of all
     *         temperature sensors if none are), the fan speeds in RPM, and the CPU core voltage in volts (from a DC
     *         voltage sensor described as a core voltage), with 0 for any not reported
     */
    public static Triplet<Double, int[], Double> summarize(List<Sensor> sensors) {
        double cpuTempSum = 0d;
        int cpuTempCount = 0;
        double allTempSum = 0d;
        int allTempCount = 0;
        List<Integer> fans = new ArrayList<>();
        double volts = 0d;
        for (Sensor s : sensors) {
            if (!s.isValid()) {
                continue;
            }
            String description = s.getDescription().toLowerCase(Locale.ROOT);
            switch (s.getType()) {
                case TEMPERATURE:
                    double celsius = s.getValue() / MICRO - ZERO_CELSIUS_KELVIN;
                    allTempSum += celsius;
                    allTempCount++;
                    if (description.startsWith("cpu")) {
                        cpuTempSum += celsius;
                        cpuTempCount++;
                    }
                    break;
                case FAN:
                    fans.add((int) s.getValue());
                    break;
                case VOLTAGE_DC:
                    if (volts == 0d && description.contains("core")) {
                        volts = s.getValue() / MICRO;
                    }
                    break;
                default:
                    break;
            }
        }
        double temp = cpuTempCount > 0 ? cpuTempSum / cpuTempCount : allTempCount > 0 ? allTempSum / allTempCount : 0d;
        int[] fanSpeeds = fans.stream().mapToInt(Integer::intValue).toArray();
        return new Triplet<>(temp, fanSpeeds, volts);
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
