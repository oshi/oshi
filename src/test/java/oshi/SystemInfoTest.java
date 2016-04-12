/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.EdidUtil;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * The Class SystemInfoTest.
 *
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfoTest {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SystemInfoTest.class);

    /**
     * Test get version.
     */
    @Test
    public void testGetVersion() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os);
        OperatingSystemVersion version = os.getVersion();
        assertNotNull(version);
        assertTrue(os.toString().length() > 0);
    }

    /**
     * Test get processors.
     */
    @Test
    public void testGetProcessor() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        assertTrue(hal.getProcessor().getLogicalProcessorCount() > 0);
    }

    /**
     * Test get memory.
     */
    @Test
    public void testGetMemory() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        GlobalMemory memory = hal.getMemory();
        assertNotNull(memory);
        assertTrue(memory.getTotal() > 0);
        assertTrue(memory.getAvailable() >= 0);
        assertTrue(memory.getAvailable() <= memory.getTotal());
    }

    /**
     * Test cpu load.
     */
    @Test
    public void testCpuLoad() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        assertTrue(hal.getProcessor().getSystemCpuLoadBetweenTicks() >= 0
                && hal.getProcessor().getSystemCpuLoadBetweenTicks() <= 1);
    }

    /**
     * Test cpu load ticks.
     */
    @Test
    public void testCpuLoadTicks() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        assertEquals(4, hal.getProcessor().getSystemCpuLoadTicks().length);
    }

    /**
     * Test processor cpu load.
     */
    @Test
    public void testProcCpuLoad() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        for (int cpu = 0; cpu < hal.getProcessor().getLogicalProcessorCount(); cpu++) {
            assertTrue(hal.getProcessor().getProcessorCpuLoadBetweenTicks()[cpu] >= 0
                    && hal.getProcessor().getProcessorCpuLoadBetweenTicks()[cpu] <= 1);
        }
    }

    /**
     * Test processor cpu load ticks.
     */
    @Test
    public void testProcCpuLoadTicks() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        assertEquals(4, hal.getProcessor().getProcessorCpuLoadTicks()[0].length);
    }

    /**
     * Test system cpu load.
     */
    @Test
    public void testSystemCpuLoad() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        double cpuLoad = hal.getProcessor().getSystemCpuLoad();
        assertTrue(cpuLoad >= 0.0 && cpuLoad <= 1.0);
    }

    /**
     * Test system load average.
     */
    @Test
    public void testSystemLoadAverage() {
        if (Platform.isMac() || Platform.isLinux()) {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            assertTrue(hal.getProcessor().getSystemLoadAverage() >= 0.0);
        }
    }

    /**
     * Test processor counts.
     */
    @Test
    public void testProcessorCounts() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        assertTrue(hal.getProcessor().getPhysicalProcessorCount() >= 1);
        assertTrue(hal.getProcessor().getLogicalProcessorCount() >= hal.getProcessor().getPhysicalProcessorCount());
    }

    /**
     * Test cpu vendor freq.
     */
    @Test
    public void testCpuVendorFreq() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        assertTrue(hal.getProcessor().getVendorFreq() == -1 || hal.getProcessor().getVendorFreq() > 0);
    }

    /**
     * Test power source.
     */
    @Test
    public void testPowerSource() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        if (hal.getPowerSources().length > 1) {
            assertTrue(hal.getPowerSources()[0].getRemainingCapacity() >= 0
                    && hal.getPowerSources()[0].getRemainingCapacity() <= 1);
            double epsilon = 1E-6;
            assertTrue(hal.getPowerSources()[0].getTimeRemaining() > 0
                    || Math.abs(hal.getPowerSources()[0].getTimeRemaining() - -1) < epsilon
                    || Math.abs(hal.getPowerSources()[0].getTimeRemaining() - -2) < epsilon);
        }
    }

    /**
     * Test file system.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testFileSystem() throws IOException {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        if (hal.getFileStores().length > 1) {
            assertTrue(hal.getFileStores()[0].getTotalSpace() >= 0);
            assertTrue(hal.getFileStores()[0].getUsableSpace() <= hal.getFileStores()[0].getTotalSpace());
        }
        // Hack to extract path from FileStore.toString() is undocumented,
        // this test will fail if toString format changes
        if (Platform.isLinux()) {
            FileStore store = Files.getFileStore((new File("/")).toPath());
            assertEquals("/", store.toString().replace(" (" + store.name() + ")", ""));
        }
    }

    /**
     * Test system uptime.
     */
    @Test
    public void testSystemUptime() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        long uptime = hal.getProcessor().getSystemUptime();
        assertTrue(uptime >= 0);
    }

    /**
     * Test serial number
     */
    @Test
    public void testSerialNumber() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        String sn = hal.getProcessor().getSystemSerialNumber();
        assertTrue(sn.length() >= 0);
    }

    /**
     * Test displays
     */
    @Test
    public void testDisplay() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        Display[] displays = hal.getDisplays();
        if (displays.length > 0) {
            assertTrue(displays[0].getEdid().length >= 128);
        }
    }

    /**
     * The main method.
     *
     * @param args
     *            the arguments
     */
    public static void main(String[] args) {
        List<String> oshi = new ArrayList<>();

        logger.info("Initializing System...");
        SystemInfo si = new SystemInfo();

        // software
        // software: operating system
        OperatingSystem os = si.getOperatingSystem();
        oshi.add(os.toString());

        logger.info("Initializing Hardware...");
        // hardware
        HardwareAbstractionLayer hal = si.getHardware();

        // hardware: processors
        oshi.add(hal.getProcessor().toString());
        oshi.add(" " + hal.getProcessor().getPhysicalProcessorCount() + " physical CPU(s)");
        oshi.add(" " + hal.getProcessor().getLogicalProcessorCount() + " logical CPU(s)");

        oshi.add("Identifier: " + hal.getProcessor().getIdentifier());
        oshi.add("Serial Num: " + hal.getProcessor().getSystemSerialNumber());

        // hardware: memory
        logger.info("Checking Memory...");
        oshi.add("Memory: " + FormatUtil.formatBytes(hal.getMemory().getAvailable()) + "/"
                + FormatUtil.formatBytes(hal.getMemory().getTotal()));
        // uptime
        logger.info("Checking Uptime...");
        oshi.add("Uptime: " + FormatUtil.formatElapsedSecs(hal.getProcessor().getSystemUptime()));

        // CPU
        logger.info("Checking CPU...");
        long[] prevTicks = hal.getProcessor().getSystemCpuLoadTicks();
        oshi.add("CPU ticks @ 0 sec:" + Arrays.toString(prevTicks));
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = hal.getProcessor().getSystemCpuLoadTicks();
        oshi.add("CPU ticks @ 1 sec:" + Arrays.toString(ticks));
        long user = ticks[0] - prevTicks[0];
        long nice = ticks[1] - prevTicks[1];
        long sys = ticks[2] - prevTicks[2];
        long idle = ticks[3] - prevTicks[3];
        long totalCpu = user + nice + sys + idle;

        oshi.add(String.format("User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%%%n", 100d * user / totalCpu,
                100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu));
        oshi.add(String.format("CPU load: %.1f%% (counting ticks)%n",
                hal.getProcessor().getSystemCpuLoadBetweenTicks() * 100));
        oshi.add(String.format("CPU load: %.1f%% (OS MXBean)%n", hal.getProcessor().getSystemCpuLoad() * 100));
        double loadAverage = hal.getProcessor().getSystemLoadAverage();
        oshi.add("CPU load average: " + (loadAverage < 0 ? "N/A" : String.format("%.2f", loadAverage)));
        // per core CPU
        StringBuilder procCpu = new StringBuilder("CPU load per processor:");
        double[] load = hal.getProcessor().getProcessorCpuLoadBetweenTicks();
        for (int cpu = 0; cpu < load.length; cpu++) {
            procCpu.append(String.format(" %.1f%%", load[cpu] * 100));
        }
        oshi.add(procCpu.toString());
        // hardware: sensors
        logger.info("Checking Sensors...");
        oshi.add("Sensors:");
        oshi.add(String.format(" CPU Temperature: %.1f°C%n", hal.getSensors().getCpuTemperature()));
        oshi.add(" Fan Speeds:" + Arrays.toString(hal.getSensors().getFanSpeeds()));
        oshi.add(String.format(" CPU Voltagee: %.1fV%n", hal.getSensors().getCpuVoltage()));

        // hardware: power
        logger.info("Checking Power sources...");
        StringBuilder sb = new StringBuilder("Power: ");
        if (hal.getPowerSources().length == 0) {
            sb.append("Unknown");
        } else {
            double timeRemaining = hal.getPowerSources()[0].getTimeRemaining();
            if (timeRemaining < -1d)
                sb.append("Charging");
            else if (timeRemaining < 0d)
                sb.append("Calculating time remaining");
            else
                sb.append(String.format("%d:%02d remaining", (int) (timeRemaining / 3600),
                        (int) (timeRemaining / 60) % 60));
        }
        for (PowerSource pSource : hal.getPowerSources()) {
            sb.append(String.format("%n %s @ %.1f%%", pSource.getName(), pSource.getRemainingCapacity() * 100d));
        }
        oshi.add(sb.toString());

        // hardware: file system
        logger.info("Checking File System...");
        oshi.add("File System:");

        OSFileStore[] fsArray = hal.getFileStores();
        for (OSFileStore fs : fsArray) {
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            oshi.add(String.format(" %s (%s) %s of %s free (%.1f%%)%n", fs.getName(),
                    fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), FormatUtil.formatBytes(usable),
                    FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total));
        }

        // hardware: displays
        logger.info("Checking Displays...");
        oshi.add("Displays:");
        int i = 0;
        for (Display display : hal.getDisplays()) {
            oshi.add(" Display " + i + ":");
            byte[] edid = display.getEdid();
            oshi.add("  Manuf. ID=" + EdidUtil.getManufacturerID(edid) + ", Product ID="
                    + EdidUtil.getProductID(edid) + ", " + (EdidUtil.isDigital(edid) ? "Digital" : "Analog")
                    + ", Serial=" + EdidUtil.getSerialNo(edid) + ", ManufDate=" + (EdidUtil.getWeek(edid) * 12 / 52 + 1)
                    + "/" + EdidUtil.getYear(edid) + ", EDID v" + EdidUtil.getVersion(edid));
            int hSize = EdidUtil.getHcm(edid);
            int vSize = EdidUtil.getVcm(edid);
            oshi.add(String.format("  %d x %d cm (%.1f x %.1f in)%n", hSize, vSize, hSize / 2.54, vSize / 2.54));
            byte[][] desc = EdidUtil.getDescriptors(edid);
            for (int d = 0; d < desc.length; d++) {
                switch (EdidUtil.getDescriptorType(desc[d])) {
                case 0xff:
                    oshi.add("  Serial Number: " + EdidUtil.getDescriptorText(desc[d]));
                    break;
                case 0xfe:
                    oshi.add("  Unspecified Text: " + EdidUtil.getDescriptorText(desc[d]));
                    break;
                case 0xfd:
                    oshi.add("  Range Limits: " + EdidUtil.getDescriptorRangeLimits(desc[d]));
                    break;
                case 0xfc:
                    oshi.add("  Monitor Name: " + EdidUtil.getDescriptorText(desc[d]));
                    break;
                case 0xfb:
                    oshi.add("  White Point Data: " + EdidUtil.getDescriptorHex(desc[d]));
                    break;
                case 0xfa:
                    oshi.add("  Standard Timing ID: " + EdidUtil.getDescriptorHex(desc[d]));
                    break;
                default:
                    if (EdidUtil.getDescriptorType(desc[d]) <= 0x0f && EdidUtil.getDescriptorType(desc[d]) >= 0x00) {
                        oshi.add("  Manufacturer Data: " + EdidUtil.getDescriptorHex(desc[d]));
                    } else {
                        oshi.add("  Preferred Timing: " + EdidUtil.getTimingDescriptor(desc[d]));
                    }
                }
            }
            i++;
        }

        String output = null;
        for (i = 0; i < oshi.size(); i++) {
            output = output + oshi.get(i);
            if (!"\n".equals(oshi.get(i))) {
                output = output + "\n";
            }
        }
        logger.info("Printing List: {}", output);

        logger.info("Printing JSON:");
        // Compact JSON
        // logger.info(si.toJSON().toString());

        // Pretty JSON
        logger.info(ParseUtil.jsonPrettyPrint(si.toJSON()));
    }
}
