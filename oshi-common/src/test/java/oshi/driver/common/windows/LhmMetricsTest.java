/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.LhmMetrics.Reading;

class LhmMetricsTest {

    // The first two samples and the two comment lines are from LibreHardwareMonitor 0.9.6 on a GitHub windows-latest
    // runner. That virtual machine has no processor temperature, fan or voltage sensors; the last four samples are
    // written in the same format, following the 0.9.6 source, with the hardware identifiers of real chips.
    private static final String METRICS = """
            # TYPE lhm_cpu_load_percent gauge
            lhm_cpu_load_percent {"sensorName"="CPU Total", "sensorAlias"="CPU Total (/load/0)", \
            "hardwareName"="INTEL XEON PLATINUM 8573C", "hardwareAlias"="INTEL XEON PLATINUM 8573C (/intelcpu/0)", \
            "sensorId"="/load/0", "hardwareId"="/intelcpu/0", "host"="runnervmm7ud1"} 15.2224998474121
            # TYPE lhm_storage_temperature_celsius gauge
            lhm_storage_temperature_celsius {"sensorName"="Composite Temperature", \
            "sensorAlias"="Composite Temperature (/temperature/0)", "hardwareName"="MSFT NVMe Accelerator v1.0", \
            "hardwareAlias"="MSFT NVMe Accelerator v1.0 (/nvme/0)", "sensorId"="/temperature/0", \
            "hardwareId"="/nvme/0", "host"="runnervmm7ud1"} 37.5
            lhm_cpu_temperature_celsius {"sensorName"="CPU Package", "sensorAlias"="CPU Package (/temperature/0)", \
            "hardwareName"="Intel Core i7-12700K", "hardwareAlias"="Intel Core i7-12700K (/intelcpu/0)", \
            "sensorId"="/temperature/0", "hardwareId"="/intelcpu/0", "host"="desktop"} 52.0
            lhm_motherboard_fan_rpm {"sensorName"="Fan 2", "sensorAlias"="Fan 2 (/fan/1)", \
            "hardwareName"="ASUS PRIME Z690-P", "hardwareAlias"="ASUS PRIME Z690-P (/lpc/nct6798d)", \
            "sensorId"="/fan/1", "hardwareId"="/lpc/nct6798d", "host"="desktop"} 1123
            lhm_motherboard_voltage_volts {"sensorName"="Vcore", "sensorAlias"="Vcore (/voltage/0)", \
            "hardwareName"="ASUS PRIME Z690-P", "hardwareAlias"="ASUS PRIME Z690-P (/lpc/nct6798d)", \
            "sensorId"="/voltage/0", "hardwareId"="/lpc/nct6798d", "host"="desktop"} 1.2 1763170762000
            lhm_cpu_temperature_celsius {"sensorName"="Core #1", "hardwareId"="/intelcpu/0"} NaN
            """;

    @Test
    void testParse() {
        List<Reading> readings = LhmMetrics.parse(METRICS.lines().toList());
        assertThat("Comment lines and the NaN sample are skipped", readings, hasSize(5));

        Reading load = readings.get(0);
        assertThat(load.getMetric(), is("lhm_cpu_load_percent"));
        assertThat(load.getHardwareId(), is("/intelcpu/0"));
        assertThat(load.getSensorName(), is("CPU Total"));
        assertThat(load.getValue(), closeTo(15.2225, 1e-4));

        assertThat(readings.get(1).getValue(), closeTo(37.5, 1e-9));
        assertThat(readings.get(2).getMetric(), is(LhmMetrics.CPU_TEMPERATURE));
        assertThat(readings.get(3).getMetric(), is(LhmMetrics.MOTHERBOARD_FAN));
        assertThat(readings.get(3).getHardwareId(), is("/lpc/nct6798d"));
        Reading vcore = readings.get(4);
        assertThat(vcore.getMetric(), is(LhmMetrics.MOTHERBOARD_VOLTAGE));
        assertThat("A trailing timestamp is not taken as the value", vcore.getValue(), closeTo(1.2, 1e-9));
    }

    @Test
    void testParseEmptyAndMalformed() {
        assertThat(LhmMetrics.parse(List.of()), is(empty()));
        assertThat(LhmMetrics.parse(List.of("", "<html>401 Unauthorized</html>", "lhm_cpu_load_percent 5")),
                is(empty()));
    }

    @Test
    void testParseKeepsTheLatestSampleOfEachSensor() {
        // 0.9.5 lists up to five samples per sensor, newest first, each with a timestamp in milliseconds
        List<String> history = List.of(
                "lhm_motherboard_fan_rpm {\"sensorName\"=\"Fan 1\", \"sensorId\"=\"/fan/0\", \"hardwareId\"=\"/lpc/nct6798d\"}"
                        + " 1100 1763170763000",
                "lhm_motherboard_fan_rpm {\"sensorName\"=\"Fan 1\", \"sensorId\"=\"/fan/0\", \"hardwareId\"=\"/lpc/nct6798d\"}"
                        + " 1200 1763170762000",
                "lhm_motherboard_fan_rpm {\"sensorName\"=\"Fan 2\", \"sensorId\"=\"/fan/1\", \"hardwareId\"=\"/lpc/nct6798d\"}"
                        + " 800 1763170761000",
                "lhm_motherboard_fan_rpm {\"sensorName\"=\"Fan 2\", \"sensorId\"=\"/fan/1\", \"hardwareId\"=\"/lpc/nct6798d\"}"
                        + " 850 1763170763000");
        List<Reading> readings = LhmMetrics.parse(history);
        assertThat("One reading per sensor", readings, hasSize(2));
        assertThat(readings.get(0).getSensorName(), is("Fan 1"));
        assertThat("The newest sample, listed first", readings.get(0).getValue(), closeTo(1100, 1e-9));
        assertThat("The newest sample, listed last", readings.get(1).getValue(), closeTo(850, 1e-9));
    }

    @Test
    void testReadBounded() throws IOException {
        long later = System.nanoTime() + 60_000_000_000L;
        assertThat(LhmMetrics.readBounded(new StringReader("a\nb\n"), 100, later), is("a\nb\n"));
        assertThat("Too large", LhmMetrics.readBounded(new StringReader("x".repeat(101)), 100, later), is(nullValue()));
        assertThat("Too slow", LhmMetrics.readBounded(new StringReader("a"), 100, System.nanoTime() - 1L),
                is(nullValue()));
        assertThat("Too slow, checked at the end of the body too",
                LhmMetrics.readBounded(new StringReader(""), 100, System.nanoTime() - 1L), is(nullValue()));
    }

    @Test
    void testMetricsUrl() {
        assertThat(LhmMetrics.metricsUrl("http://127.0.0.1:8085"), is("http://127.0.0.1:8085/metrics"));
        assertThat(LhmMetrics.metricsUrl(" http://127.0.0.1:8085/ "), is("http://127.0.0.1:8085/metrics"));
        assertThat(LhmMetrics.metricsUrl("http://host:8085/metrics"), is("http://host:8085/metrics"));
    }

    @Test
    void testQueryReadsTheServer() throws IOException, InterruptedException {
        // Bound to the IPv4 address the client names; the JVM's loopback address may be ::1
        try (ServerSocket server = new ServerSocket(0, 2, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }))) {
            Thread responder = new Thread(() -> respond(server, 2));
            responder.setDaemon(true);
            responder.start();
            String base = "http://127.0.0.1:" + server.getLocalPort();
            assertThat(LhmMetrics.query(base), hasSize(5));
            assertThat("A non-200 status reads as no data", LhmMetrics.query(base + "/denied/metrics"), is(empty()));
            responder.join(5000L);
        }
    }

    /** Answers {@code count} requests: 401 for a path under {@code /denied}, otherwise the fixture. */
    private static void respond(ServerSocket server, int count) {
        for (int i = 0; i < count; i++) {
            try (Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                    OutputStream out = socket.getOutputStream()) {
                String requestLine = in.readLine();
                // Skip the request headers
                String line = in.readLine();
                while (line != null && !line.isEmpty()) {
                    line = in.readLine();
                }
                byte[] body = METRICS.getBytes(UTF_8);
                String head = requestLine != null && requestLine.contains("/denied")
                        ? "HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                        : "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + body.length
                                + "\r\nConnection: close\r\n\r\n";
                out.write(head.getBytes(UTF_8));
                if (head.startsWith("HTTP/1.1 200")) {
                    out.write(body);
                }
                out.flush();
            } catch (IOException e) {
                return;
            }
        }
    }

    @Test
    void testQueryUnreachableOrInvalid() {
        long start = System.nanoTime();
        // Port 9 (discard) is not served on any CI host
        assertThat(LhmMetrics.query("http://127.0.0.1:9"), is(empty()));
        assertThat("The connect timeout bounds a refused connection", (System.nanoTime() - start) / 1_000_000L,
                is(lessThan(5000L)));
        assertThat(LhmMetrics.query("not a url"), is(empty()));
        assertThat("Only http and https are requested", LhmMetrics.query("file:///C:/lhm"), is(empty()));
        assertThat(LhmMetrics.query("ftp://127.0.0.1:8085"), is(empty()));
    }
}
