/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
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
    // Bounds each read, and the whole body: a server that trickles bytes is abandoned after this long in total
    private static final int READ_TIMEOUT_MS = 2000;
    // A real response is tens of kilobytes; anything far larger is not a LibreHardwareMonitor server
    private static final int MAX_BODY_CHARS = 1 << 20;

    // lhm_cpu_temperature_celsius {"sensorName"="CPU Package", ..., "hardwareId"="/intelcpu/0", ...} 45.5
    // A timestamp in milliseconds follows the value in 0.9.5, which lists up to five samples per sensor, and in
    // later versions when the request asks for one.
    private static final Pattern SAMPLE = Pattern.compile("^(lhm_\\w++)\\s*+\\{(.*)\\}\\s++(\\S++)(?:\\s++(\\d++))?$");
    private static final Pattern LABEL = Pattern.compile("\"(\\w++)\"=\"([^\"]*+)\"");

    private LhmMetrics() {
    }

    /**
     * Fetches and parses the {@code /metrics} endpoint of a LibreHardwareMonitor web server.
     *
     * @param baseUrl the server's address, such as {@code http://127.0.0.1:8085}; a URL already ending in
     *                {@code /metrics} is used as given
     * @return the readings, empty if the server could not be reached, is not an {@code http} or {@code https} address,
     *         or returned none
     */
    public static List<Reading> query(String baseUrl) {
        String url = metricsUrl(baseUrl);
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                LOG.debug("Not an http or https address for LibreHardwareMonitor: {}", url);
                return Collections.emptyList();
            }
            // The server is local, or on a host the user named; a proxy configured for the JVM is not meant for it
            conn = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                LOG.debug("LibreHardwareMonitor at {} returned HTTP {}", url, status);
                return Collections.emptyList();
            }
            try (Reader reader = new InputStreamReader(conn.getInputStream(), UTF_8)) {
                String body = readBounded(reader, MAX_BODY_CHARS,
                        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(READ_TIMEOUT_MS));
                if (body == null) {
                    LOG.debug("LibreHardwareMonitor at {} sent more than {} characters or took too long", url,
                            MAX_BODY_CHARS);
                    return Collections.emptyList();
                }
                return parse(Arrays.asList(body.split("\n", -1)));
            }
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
     * Reads a response body, giving up once it exceeds a size or a deadline.
     *
     * @param reader        the body
     * @param maxChars      the largest body to accept
     * @param deadlineNanos the {@link System#nanoTime()} by which the body must be complete
     * @return the body, or {@code null} if it was too large or too slow
     * @throws IOException if reading fails
     */
    static @Nullable String readBounded(Reader reader, int maxChars, long deadlineNanos) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
            if (sb.length() > maxChars || System.nanoTime() - deadlineNanos > 0) {
                return null;
            }
        }
        return sb.toString();
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
     * Parses the {@code /metrics} response. Comment lines and samples whose value is not a number are skipped. Where a
     * sensor appears more than once, as in 0.9.5, which lists its recent history, only the sample with the latest
     * timestamp is kept.
     *
     * @param lines the response body, one sample per line
     * @return one reading per sensor, in the order each sensor first appears
     */
    public static List<Reading> parse(List<String> lines) {
        Map<String, Reading> readings = new LinkedHashMap<>();
        Map<String, Long> timestamps = new HashMap<>();
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
            String sensorId = "";
            String sensorName = "";
            Matcher label = LABEL.matcher(sample.group(2));
            while (label.find()) {
                switch (label.group(1)) {
                    case "hardwareId":
                        hardwareId = label.group(2);
                        break;
                    case "sensorId":
                        sensorId = label.group(2);
                        break;
                    case "sensorName":
                        sensorName = label.group(2);
                        break;
                    default:
                        break;
                }
            }
            String key = sample.group(1) + ' ' + hardwareId + ' ' + sensorId + ' ' + sensorName;
            long timestamp = ParseUtil.parseLongOrDefault(sample.group(4), -1L);
            Long previous = timestamps.get(key);
            if (previous == null || timestamp > previous) {
                readings.put(key, new Reading(sample.group(1), hardwareId, sensorName, value));
                timestamps.put(key, timestamp);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(readings.values()));
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
