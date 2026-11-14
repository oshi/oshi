/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.driver.common.unix.netbsd.Envstat.Sensor;
import oshi.util.tuples.Triplet;

public class EnvstatTest {

    /**
     * Builds {@code envstat -x} output in the layout NetBSD 10.1 prints (captured with the {@code swsensor} test
     * driver): keys sorted, one sensor dictionary per sensor, and a {@code device-properties} dictionary closing each
     * device's array. Each sensor is {description, type, value, state}.
     *
     * @param devices each a device name followed by its sensors
     * @return the output lines
     */
    public static List<String> envstatX(Object[]... devices) {
        List<String> out = new ArrayList<>(
                List.of("<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                        "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" "
                                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">",
                        "<plist version=\"1.0\">", "<dict>"));
        for (Object[] device : devices) {
            out.add("\t<key>" + device[0] + "</key>");
            out.add("\t<array>");
            for (int i = 1; i < device.length; i++) {
                String[] s = (String[]) device[i];
                out.add("\t\t<dict>");
                if (!s[2].isEmpty()) {
                    out.add("\t\t\t<key>cur-value</key>");
                    out.add("\t\t\t<integer>" + s[2] + "</integer>");
                }
                out.add("\t\t\t<key>description</key>");
                out.add("\t\t\t<string>" + s[0] + "</string>");
                out.add("\t\t\t<key>index</key>");
                out.add("\t\t\t<string>sensor" + (i - 1) + "</string>");
                out.add("\t\t\t<key>monitoring-supported</key>");
                out.add("\t\t\t<false/>");
                out.add("\t\t\t<key>state</key>");
                out.add("\t\t\t<string>" + s[3] + "</string>");
                out.add("\t\t\t<key>type</key>");
                out.add("\t\t\t<string>" + s[1] + "</string>");
                out.add("\t\t</dict>");
            }
            out.add("\t\t<dict>");
            out.add("\t\t\t<key>device-properties</key>");
            out.add("\t\t\t<dict>");
            out.add("\t\t\t\t<key>device-class</key>");
            out.add("\t\t\t\t<string>other</string>");
            out.add("\t\t\t\t<key>refresh-timeout</key>");
            out.add("\t\t\t\t<integer>0x1e</integer>");
            out.add("\t\t\t</dict>");
            out.add("\t\t</dict>");
            out.add("\t</array>");
        }
        out.add("</dict>");
        out.add("</plist>");
        return out;
    }

    public static String[] sensor(String description, String type, String value, String state) {
        return new String[] { description, type, value, state };
    }

    public static List<String> laptop() {
        return envstatX(new Object[] { "coretemp0", sensor("cpu0 temperature", "Temperature", "323150000", "valid") },
                new Object[] { "coretemp1", sensor("cpu1 temperature", "Temperature", "327150000", "valid") },
                new Object[] { "acpitz0", sensor("temperature", "Temperature", "313150000", "valid"),
                        sensor("FAN", "Fan", "2600", "valid") },
                new Object[] { "itesio0", sensor("VCORE A", "Voltage DC", "1150000", "valid"),
                        sensor("Fan 2", "Fan", "", "invalid") },
                new Object[] { "acpiacad0", sensor("connected", "Indicator", "0", "valid") },
                new Object[] { "acpibat0", sensor("present", "Indicator", "1", "valid"),
                        sensor("design cap", "Watt hour", "57000000", "valid"),
                        sensor("last full cap", "Watt hour", "50000000", "valid"),
                        sensor("design voltage", "Voltage DC", "11400000", "valid"),
                        sensor("voltage", "Voltage DC", "12000000", "valid"),
                        sensor("charge rate", "Watts", "", "invalid"),
                        sensor("discharge rate", "Watts", "10000000", "valid"),
                        sensor("charge", "Watt hour", "25000000", "warning-under"),
                        sensor("charging", "Battery charge", "0", "valid") },
                new Object[] { "acpibat1", sensor("present", "Indicator", "0", "valid"),
                        sensor("charge", "Watt hour", "", "invalid") });
    }

    @Test
    void testParseSensors() {
        List<Sensor> sensors = Envstat.parseSensors(laptop());
        assertThat(sensors, hasSize(18));
        Sensor first = sensors.get(0);
        assertThat(first.getDevice(), is("coretemp0"));
        assertThat(first.getDescription(), is("cpu0 temperature"));
        assertThat(first.getType(), is(Envstat.TEMPERATURE));
        assertThat(first.getValue(), is(323_150_000L));
        assertThat(first.isValid(), is(true));

        Sensor missingFan = sensors.get(5);
        assertThat(missingFan.getDevice(), is("itesio0"));
        assertThat(missingFan.getValue(), is(0L));
        assertThat(missingFan.isValid(), is(false));

        // A warning state still carries a current value
        Sensor charge = sensors.stream().filter(s -> s.getDescription().equals("charge")).findFirst().orElseThrow();
        assertThat(charge.isValid(), is(true));
    }

    @Test
    void testSummarize() {
        Triplet<Double, int[], Double> summary = Envstat.summarize(Envstat.parseSensors(laptop()));
        // Average of the two CPU sensors, ignoring the thermal zone
        assertThat(summary.getA(), closeTo(52d, 1e-9));
        // The invalid fan is skipped
        assertThat(summary.getB().length, is(1));
        assertThat(summary.getB()[0], is(2600));
        assertThat(summary.getC(), closeTo(1.15, 1e-9));
    }

    @Test
    void testSummarizeNoCpuSensor() {
        List<String> lines = envstatX(
                new Object[] { "acpitz0", sensor("temperature", "Temperature", "313150000", "valid") },
                new Object[] { "acpitz1", sensor("temperature", "Temperature", "317150000", "valid") });
        Triplet<Double, int[], Double> summary = Envstat.summarize(Envstat.parseSensors(lines));
        assertThat(summary.getA(), closeTo(42d, 1e-9));
        assertThat(summary.getB().length, is(0));
        assertThat(summary.getC(), is(0d));
    }

    @Test
    void testNoDrivers() {
        // NetBSD under QEMU prints an empty dictionary
        List<String> lines = List.of("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<plist version=\"1.0\">", "<dict/>",
                "</plist>");
        assertThat(Envstat.parseSensors(lines), is(empty()));
        assertThat(Envstat.parseSensors(List.of("envstat: no drivers registered")), is(empty()));
    }
}
