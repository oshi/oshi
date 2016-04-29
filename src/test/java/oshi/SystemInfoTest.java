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
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 *
 * Contributors:
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
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

import oshi.hardware.CentralProcessor;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.stores.HWNetworkStore;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * The Class SystemInfoTest.
 *
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfoTest {

    /**
     * Test central processor.
     */
    @Test
    public void testCentralProcessor() {
        SystemInfo si = new SystemInfo();
        CentralProcessor p = si.getHardware().getProcessor();

        assertNotNull(p.getVendor());
        assertTrue(p.getVendorFreq() == -1 || p.getVendorFreq() > 0);
        p.setVendor("v");
        assertEquals(p.getVendor(), "v");

        assertNotNull(p.getName());
        p.setName("n");
        assertEquals(p.getName(), "n");

        assertNotNull(p.getIdentifier());
        p.setIdentifier("i");
        assertEquals(p.getIdentifier(), "i");

        p.setCpu64(true);
        assertTrue(p.isCpu64bit());

        assertNotNull(p.getStepping());
        p.setStepping("s");
        assertEquals(p.getStepping(), "s");

        assertNotNull(p.getModel());
        p.setModel("m");
        assertEquals(p.getModel(), "m");

        assertNotNull(p.getFamily());
        p.setFamily("f");
        assertEquals(p.getFamily(), "f");

        assertTrue(p.getSystemCpuLoadBetweenTicks() >= 0 && p.getSystemCpuLoadBetweenTicks() <= 1);
        assertEquals(p.getSystemCpuLoadTicks().length, 4);
        assertTrue(p.getSystemIOWaitTicks() >= 0);
        assertEquals(p.getSystemIrqTicks().length, 2);

        Util.sleep(500);
        assertTrue(p.getSystemCpuLoad() >= 0.0 && p.getSystemCpuLoad() <= 1.0);
        assertEquals(p.getSystemLoadAverage(3).length, 3);
        if (Platform.isMac() || Platform.isLinux()) {
            assertTrue(p.getSystemLoadAverage() >= 0.0);
        }

        assertEquals(p.getProcessorCpuLoadBetweenTicks().length, p.getLogicalProcessorCount());
        for (int cpu = 0; cpu < p.getLogicalProcessorCount(); cpu++) {
            assertTrue(p.getProcessorCpuLoadBetweenTicks()[cpu] >= 0 && p.getProcessorCpuLoadBetweenTicks()[cpu] <= 1);
            assertEquals(p.getProcessorCpuLoadTicks()[cpu].length, 4);
        }

        assertTrue(p.getSystemUptime() > 0);
        assertNotNull(p.getSystemSerialNumber());
        assertTrue(p.getLogicalProcessorCount() >= p.getPhysicalProcessorCount());
        assertTrue(p.getPhysicalProcessorCount() > 0);
        assertTrue(p.getProcessCount() >= 1);
        assertTrue(p.getThreadCount() >= p.getProcessCount());
    }

    /**
     * Test disks extraction.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testDisks() throws IOException {
        SystemInfo si = new SystemInfo();

        for (HWDiskStore disk : si.getHardware().getDiskStores()) {
            // NOTE: for now, only tests are for getting disk informations
            assertNotNull(disk.getName());
            assertNotNull(disk.getModel());
            assertNotNull(disk.getSerial());
            assertNotNull(disk.getSize());
            assertNotNull(disk.getReads());
            assertNotNull(disk.getWrites());
        }
    }

    /**
     * Test network interfaces extraction.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testNetworkInterfaces() throws IOException {
        SystemInfo si = new SystemInfo();

        for (HWNetworkStore net : si.getHardware().getNetworkStores()) {
            assertNotNull(net.getName());
            assertNotNull(net.getMacaddr());
            // FIXME: this test is wrong (IP address cannot be assigned)
            assertNotNull(net.getIpaddr());
            // FIXME: this test is wrong (IPv6 address cannot be assigned)
            assertNotNull(net.getIpv6addr());
            assertTrue(net.getBytesRecv() >= 0);
            assertTrue(net.getBytesSent() >= 0);
            assertTrue(net.getPacketsRecv()>= 0);
            assertTrue(net.getPacketsSent()>= 0);
        }
    }
    
    /**
     * Test displays
     */
    @Test
    public void testDisplay() {
        SystemInfo si = new SystemInfo();
        Display[] displays = si.getHardware().getDisplays();
        for (Display d : displays) {
            assertTrue(d.getEdid().length >= 128);
        }
    }

    /**
     * Test GlobalMemory.
     */
    @Test
    public void testGlobalMemory() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        GlobalMemory memory = hal.getMemory();
        assertNotNull(memory);

        // RAM tests
        assertTrue(memory.getTotal() > 0);
        assertTrue(memory.getAvailable() >= 0);
        assertTrue(memory.getAvailable() <= memory.getTotal());

        // Swap tests
        assertTrue(memory.getSwapTotal() >= 0);
        assertTrue(memory.getSwapUsed() >= 0);
        assertTrue(memory.getSwapUsed() <= memory.getSwapTotal());
    }

    /**
     * Test power source.
     */
    @Test
    public void testPowerSource() {
        SystemInfo si = new SystemInfo();
        PowerSource[] psArr = si.getHardware().getPowerSources();
        for (PowerSource ps : psArr) {
            assertTrue(ps.getRemainingCapacity() >= 0 && ps.getRemainingCapacity() <= 1);
            double epsilon = 1E-6;
            assertTrue(ps.getTimeRemaining() > 0 || Math.abs(ps.getTimeRemaining() - -1) < epsilon
                    || Math.abs(ps.getTimeRemaining() - -2) < epsilon);
        }
    }

    /**
     * Test sensors
     */
    @Test
    public void testSensors() {
        SystemInfo si = new SystemInfo();
        Sensors s = si.getHardware().getSensors();
        assertTrue(s.getCpuTemperature() >= 0d && s.getCpuTemperature() <= 100d);
        int[] speeds = s.getFanSpeeds();
        for (int fan = 0; fan < speeds.length; fan++) {
            assertTrue(speeds[fan] >= 0);
        }
        assertTrue(s.getCpuVoltage() >= 0);
    }

    /**
     * Test get version.
     */
    @Test
    public void testOSVersion() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os.getFamily());
        assertNotNull(os.getManufacturer());
        OperatingSystemVersion version = os.getVersion();
        assertNotNull(version);
        assertNotNull(version.getVersion());
        assertNotNull(version.getCodeName());
        assertNotNull(version.getBuildNumber());
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
        OSFileStore[] fs = si.getHardware().getFileStores();
        for (int f = 0; f < fs.length; f++) {
            assertNotNull(fs[f].getName());
            assertNotNull(fs[f].getDescription());
            assertNotNull(fs[f].getType());
            assertTrue(fs[f].getTotalSpace() >= 0);
            assertTrue(fs[f].getUsableSpace() <= fs[f].getTotalSpace());
        }
        // Hack to extract path from FileStore.toString() is undocumented,
        // this test will fail if toString format changes
        if (Platform.isLinux()) {
            FileStore store = Files.getFileStore((new File("/")).toPath());
            assertEquals("/", store.toString().replace(" (" + store.name() + ")", ""));
        }
    }

    /**
     * The main method.
     *
     * @param args
     *            the arguments
     */
    public static void main(String[] args) {
        // Options: ERROR > WARN > INFO > DEBUG > TRACE
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        Logger LOG = LoggerFactory.getLogger(SystemInfoTest.class);

        LOG.info("Initializing System...");
        SystemInfo si = new SystemInfo();
        // software
        // software: operating system
        OperatingSystem os = si.getOperatingSystem();
        System.out.println(os);

        LOG.info("Initializing Hardware...");
        // hardware
        HardwareAbstractionLayer hal = si.getHardware();

        // hardware: processors
        System.out.println(hal.getProcessor());
        System.out.println(" " + hal.getProcessor().getPhysicalProcessorCount() + " physical CPU(s)");
        System.out.println(" " + hal.getProcessor().getLogicalProcessorCount() + " logical CPU(s)");

        System.out.println("Identifier: " + hal.getProcessor().getIdentifier());
        System.out.println("Serial Num: " + hal.getProcessor().getSystemSerialNumber());

        // hardware: memory
        LOG.info("Checking Memory...");
        System.out.println("Memory: " + FormatUtil.formatBytes(hal.getMemory().getAvailable()) + "/"
                + FormatUtil.formatBytes(hal.getMemory().getTotal()));
        System.out.println("Swap used: " + FormatUtil.formatBytes(hal.getMemory().getSwapUsed()) + "/"
                + FormatUtil.formatBytes(hal.getMemory().getSwapTotal()));
        // uptime
        LOG.info("Checking Uptime...");
        System.out.println("Uptime: " + FormatUtil.formatElapsedSecs(hal.getProcessor().getSystemUptime()));

        // CPU
        LOG.info("Checking CPU...");
        long[] prevTicks = hal.getProcessor().getSystemCpuLoadTicks();
        System.out.println("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks) + ", "
                + hal.getProcessor().getSystemIOWaitTicks() + ", "
                + Arrays.toString(hal.getProcessor().getSystemIrqTicks()));
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = hal.getProcessor().getSystemCpuLoadTicks();
        System.out.println("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks) + ", "
                + hal.getProcessor().getSystemIOWaitTicks() + ", "
                + Arrays.toString(hal.getProcessor().getSystemIrqTicks()));
        long user = ticks[0] - prevTicks[0];
        long nice = ticks[1] - prevTicks[1];
        long sys = ticks[2] - prevTicks[2];
        long idle = ticks[3] - prevTicks[3];
        long totalCpu = user + nice + sys + idle;

        System.out.format("User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%%%n", 100d * user / totalCpu,
                100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu);
        System.out.format("CPU load: %.1f%% (counting ticks)%n",
                hal.getProcessor().getSystemCpuLoadBetweenTicks() * 100);
        System.out.format("CPU load: %.1f%% (OS MXBean)%n", hal.getProcessor().getSystemCpuLoad() * 100);
        double[] loadAverage = hal.getProcessor().getSystemLoadAverage(3);
        System.out.println("CPU load averages:" + (loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0]))
                + (loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1]))
                + (loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])));
        // per core CPU
        StringBuilder procCpu = new StringBuilder("CPU load per processor:");
        double[] load = hal.getProcessor().getProcessorCpuLoadBetweenTicks();
        for (int cpu = 0; cpu < load.length; cpu++) {
            procCpu.append(String.format(" %.1f%%", load[cpu] * 100));
        }
        System.out.println(procCpu.toString());
        System.out.println("Processes: " + hal.getProcessor().getProcessCount() + ", Threads: "
                + hal.getProcessor().getThreadCount());
        // hardware: sensors
        LOG.info("Checking Sensors...");
        System.out.println("Sensors:");
        System.out.format(" CPU Temperature: %.1f°C%n", hal.getSensors().getCpuTemperature());
        System.out.println(" Fan Speeds:" + Arrays.toString(hal.getSensors().getFanSpeeds()));
        System.out.format(" CPU Voltage: %.1fV%n", hal.getSensors().getCpuVoltage());

        // hardware: power
        LOG.info("Checking Power sources...");
        StringBuilder sb = new StringBuilder("Power: ");
        if (hal.getPowerSources().length == 0) {
            sb.append("Unknown");
        } else {
            double timeRemaining = hal.getPowerSources()[0].getTimeRemaining();
            if (timeRemaining < -1d) {
                sb.append("Charging");
            } else if (timeRemaining < 0d) {
                sb.append("Calculating time remaining");
            } else {
                sb.append(String.format("%d:%02d remaining", (int) (timeRemaining / 3600),
                        (int) (timeRemaining / 60) % 60));
            }
        }
        for (PowerSource pSource : hal.getPowerSources()) {
            sb.append(String.format("%n %s @ %.1f%%", pSource.getName(), pSource.getRemainingCapacity() * 100d));
        }
        System.out.println(sb.toString());

        // hardware: file system
        LOG.info("Checking File System...");
        System.out.println("File System:");

        OSFileStore[] fsArray = hal.getFileStores();
        for (OSFileStore fs : fsArray) {
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            System.out.format(" %s (%s) [%s] %s of %s free (%.1f%%)%n", fs.getName(),
                    fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), fs.getType(),
                    FormatUtil.formatBytes(usable), FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total);
        }

        // hardware: disks
        LOG.info("Checking Disks...");
        System.out.println("Disks:");

        HWDiskStore[] dskArray = hal.getDiskStores();
        for (HWDiskStore dsk : dskArray) {
            System.out.format(" %s: (model: %s - S/N: %s) size: %s, reads: %s, writes: %s %n", dsk.getName(),
                    dsk.getModel(), dsk.getSerial(),
                    dsk.getSize() > 0 ? FormatUtil.formatBytesDecimal(dsk.getSize()) : "?",
                    dsk.getReads() > 0 ? FormatUtil.formatBytes(dsk.getReads()) : "?",
                    dsk.getWrites() > 0 ? FormatUtil.formatBytes(dsk.getWrites()) : "?");
        }

        // hardware: displays
        LOG.info("Checking Displays...");
        System.out.println("Displays:");
        int i = 0;
        for (Display display : hal.getDisplays()) {
            System.out.println(" Display " + i + ":");
            System.out.println(display.toString());
            i++;
        }
        LOG.info("Printing JSON:");
        // Compact JSON
        // System.out.println(si.toJSON().toString());

        // Pretty JSON
        System.out.println(ParseUtil.jsonPrettyPrint(si.toJSON()));
    }
}
