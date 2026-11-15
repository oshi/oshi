/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;

/**
 * Reads sensor values from the web server of a running LibreHardwareMonitor application.
 * <p>
 * LibreHardwareMonitor 0.9.5 and later no longer publish the {@code ROOT\LibreHardwareMonitor} WMI namespace; their
 * built-in web server replaces it. The server is off by default. A user turns it on in the application (Options, Remote
 * Web Server, Run) and gives OSHI its address in {@link GlobalConfig#OSHI_OS_WINDOWS_LHM_URL}.
 * <p>
 * The {@code /metrics} endpoint is read rather than {@code /data.json}. It is one line per sensor, it gives every value
 * in base units with an invariant decimal point, and its format is the same in every release that has it. The raw value
 * in {@code data.json} changed from a localized string with units in 0.9.6 to a number afterward.
 */
@ThreadSafe
public final class LhmMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(LhmMetrics.class);

    /** The processor's temperature sensors, in degrees Celsius. */
    public static final String CPU_TEMPERATURE = "lhm_cpu_temperature_celsius";
    /** The processor's voltage sensors, in volts. */
    public static final String CPU_VOLTAGE = "lhm_cpu_voltage_volts";
    /**
     * Fan sensors on the motherboard, in RPM. A Super I/O chip is a child of the motherboard, and the server names its
     * sensors after the parent's hardware type.
     */
    public static final String MOTHERBOARD_FAN = "lhm_motherboard_fan_rpm";
    /** Voltage sensors on the motherboard, including the Super I/O chip's {@code Vcore}, in volts. */
    public static final String MOTHERBOARD_VOLTAGE = "lhm_motherboard_voltage_volts";

    private static final String ENDPOINT = "/metrics";

    // Short, because a refused connection to a closed port on Windows retries for two seconds before failing
    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int READ_TIMEOUT_MS = 2000;

    // lhm_cpu_temperature_celsius {"sensorName"="CPU Package", ..., "hardwareId"="/intelcpu/0", ...} 45.5
    // An optional trailing timestamp follows the value when the request asks for one.
    private static final Pattern SAMPLE = Pattern.compile("^(lhm_\\w++)\\s*+\\{(.*)\\}\\s++(\\S++)(?:\\s++\\d++)?$");
    private static final Pattern LABEL = Pattern.compile("\"(\\w++)\"=\"([^\"]*+)\"");

    private LhmMetrics() {
    }

    /**
     * Fetches and parses the {@code /metrics} endpoint of a LibreHardwareMonitor web server.
     *
     * @param baseUrl the server's address, such as {@code http://127.0.0.1:8085}; a URL already ending in
     *                {@code /metrics} is used as given
     * @return the readings, empty if the server could not be reached or returned none
     */
    public static List<Reading> query(String baseUrl) {
        String url = metricsUrl(baseUrl);
        HttpURLConnection conn = null;
        try {
            // The server is local, or on a host the user named; a proxy configured for the JVM is not meant for it
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection(Proxy.NO_PROXY);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                LOG.debug("LibreHardwareMonitor at {} returned HTTP {}", url, status);
                return Collections.emptyList();
            }
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return parse(lines);
        } catch (IOException | IllegalArgumentException e) {
            LOG.debug("Unable to read LibreHardwareMonitor at {}", url, e);
            return Collections.emptyList();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Builds the endpoint address from the configured one.
     *
     * @param baseUrl the configured address
     * @return the address of the {@code /metrics} endpoint
     */
    static String metricsUrl(String baseUrl) {
        String url = baseUrl.trim();
        if (url.endsWith(ENDPOINT)) {
            return url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + ENDPOINT;
    }

    /**
     * Parses the {@code /metrics} response. Comment lines and samples whose value is not a number are skipped.
     *
     * @param lines the response body, one sample per line
     * @return the readings, in response order
     */
    public static List<Reading> parse(List<String> lines) {
        List<Reading> readings = new ArrayList<>();
        for (String line : lines) {
            Matcher sample = SAMPLE.matcher(line.trim());
            if (!sample.matches()) {
                continue;
            }
            double value = ParseUtil.parseDoubleOrDefault(sample.group(3), Double.NaN);
            if (Double.isNaN(value)) {
                continue;
            }
            String hardwareId = "";
            String sensorName = "";
            Matcher label = LABEL.matcher(sample.group(2));
            while (label.find()) {
                if ("hardwareId".equals(label.group(1))) {
                    hardwareId = label.group(2);
                } else if ("sensorName".equals(label.group(1))) {
                    sensorName = label.group(2);
                }
            }
            readings.add(new Reading(sample.group(1), hardwareId, sensorName, value));
        }
        return Collections.unmodifiableList(readings);
    }

    /**
     * One sensor reading from the {@code /metrics} endpoint.
     */
    @Immutable
    public static final class Reading {
        private final String metric;
        private final String hardwareId;
        private final String sensorName;
        private final double value;

        /**
         * Creates a reading.
         *
         * @param metric     the metric name, which encodes the hardware type, the sensor type and the unit
         * @param hardwareId the hardware identifier, such as {@code /intelcpu/0}
         * @param sensorName the sensor name, such as {@code CPU Package}
         * @param value      the value, in the unit the metric name states
         */
        public Reading(String metric, String hardwareId, String sensorName, double value) {
            this.metric = metric;
            this.hardwareId = hardwareId;
            this.sensorName = sensorName;
            this.value = value;
        }

        /**
         * Gets the metric name.
         *
         * @return the metric name, such as {@link LhmMetrics#CPU_TEMPERATURE}
         */
        public String getMetric() {
            return metric;
        }

        /**
         * Gets the hardware identifier.
         *
         * @return the identifier of the hardware carrying the sensor, such as {@code /intelcpu/0}
         */
        public String getHardwareId() {
            return hardwareId;
        }

        /**
         * Gets the sensor name.
         *
         * @return the sensor name, such as {@code CPU Package}
         */
        public String getSensorName() {
            return sensorName;
        }

        /**
         * Gets the value.
         *
         * @return the value, in the unit the metric name states
         */
        public double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return metric + '{' + hardwareId + ' ' + sensorName + "}=" + value;
        }
    }
}
