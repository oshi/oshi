/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
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
import java.util.Arrays;

import org.junit.Test;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.FormatUtil;
import oshi.util.Util;

import com.sun.jna.Platform;

/**
 * The Class SystemInfoTest.
 *
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfoTest {

	/**
	 * Test get processor number.
	 */
	@Test
	public void testGetProcessorNumber() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		for (int i = 0; i < hal.getProcessors().length; i++) {
			assertEquals(i, hal.getProcessors()[i].getProcessorNumber());
		}
	}

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
	public void testGetProcessors() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertTrue(hal.getProcessors().length > 0);
	}

	/**
	 * Test get memory.
	 */
	@Test
	public void testGetMemory() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		Memory memory = hal.getMemory();
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
		assertTrue(hal.getProcessors()[0].getSystemCpuLoadBetweenTicks() >= 0
				&& hal.getProcessors()[0].getSystemCpuLoadBetweenTicks() <= 1);
	}

	/**
	 * Test cpu load ticks.
	 */
	@Test
	public void testCpuLoadTicks() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertEquals(4, hal.getProcessors()[0].getSystemCpuLoadTicks().length);
	}

	/**
	 * Test processor cpu load.
	 */
	@Test
	public void testProcCpuLoad() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertTrue(hal.getProcessors()[0].getProcessorCpuLoadBetweenTicks() >= 0
				&& hal.getProcessors()[0].getProcessorCpuLoadBetweenTicks() <= 1);
	}

	/**
	 * Test processor cpu load ticks.
	 */
	@Test
	public void testProcCpuLoadTicks() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertEquals(4,
				hal.getProcessors()[0].getProcessorCpuLoadTicks().length);
	}

	/**
	 * Test system cpu load.
	 */
	@Test
	public void testSystemCpuLoad() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		double cpuLoad = hal.getProcessors()[0].getSystemCpuLoad();
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
			assertTrue(hal.getProcessors()[0].getSystemLoadAverage() >= 0.0);
		}
	}

	/**
	 * Test cpu vendor freq.
	 */
	@Test
	public void testCpuVendorFreq() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertTrue(hal.getProcessors()[0].getVendorFreq() == -1
				|| hal.getProcessors()[0].getVendorFreq() > 0);
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
					|| Math.abs(hal.getPowerSources()[0].getTimeRemaining()
							- -1) < epsilon
					|| Math.abs(hal.getPowerSources()[0].getTimeRemaining()
							- -2) < epsilon);
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
			assertTrue(hal.getFileStores()[0].getName().length() > 0);
			assertTrue(hal.getFileStores()[0].getTotalSpace() >= 0);
			assertTrue(hal.getFileStores()[0].getUsableSpace() <= hal
					.getFileStores()[0].getTotalSpace());
		}
		// Hack to extract path from FileStore.toString() is undocumented,
		// this test will fail if toString format changes
		if (Platform.isLinux()) {
			FileStore store = Files.getFileStore((new File("/")).toPath());
			assertEquals("/",
					store.toString().replace(" (" + store.name() + ")", ""));
		}
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		SystemInfo si = new SystemInfo();
		// software
		// software: operating system
		OperatingSystem os = si.getOperatingSystem();
		System.out.println(os);

		// hardware
		HardwareAbstractionLayer hal = si.getHardware();
		// hardware: processors
		System.out.println(hal.getProcessors().length + " CPU(s):");
		for (Processor cpu : hal.getProcessors()) {
			System.out.println(" " + cpu);
		}
		System.out.println("Identifier: "
				+ hal.getProcessors()[0].getIdentifier());
		// hardware: memory
		System.out.println("Memory: "
				+ FormatUtil.formatBytes(hal.getMemory().getAvailable()) + "/"
				+ FormatUtil.formatBytes(hal.getMemory().getTotal()));
		// CPU
		long[] prevTicks = hal.getProcessors()[0].getSystemCpuLoadTicks();
		System.out.println("CPU ticks @ 0 sec:" + Arrays.toString(prevTicks));
		// Wait a second...
		Util.sleep(1000);
		long[] ticks = hal.getProcessors()[0].getSystemCpuLoadTicks();
		System.out.println("CPU ticks @ 1 sec:" + Arrays.toString(ticks));
		long user = ticks[0] - prevTicks[0];
		long nice = ticks[1] - prevTicks[1];
		long sys = ticks[2] - prevTicks[2];
		long idle = ticks[3] - prevTicks[3];
		long totalCpu = user + nice + sys + idle;

		System.out.format(
				"User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%%%n", 100d
						* user / totalCpu, 100d * nice / totalCpu, 100d * sys
						/ totalCpu, 100d * idle / totalCpu);
		System.out.format("CPU load: %.1f%% (counting ticks)%n",
				hal.getProcessors()[0].getSystemCpuLoadBetweenTicks() * 100);
		System.out.format("CPU load: %.1f%% (OS MXBean)%n",
				hal.getProcessors()[0].getSystemCpuLoad() * 100);
		double loadAverage = hal.getProcessors()[0].getSystemLoadAverage();
		System.out
				.println("CPU load average: "
						+ (loadAverage < 0 ? "N/A" : String.format("%.2f",
								loadAverage)));
		// per core CPU
		StringBuilder procCpu = new StringBuilder("CPU load per processor:");
		for (int cpu = 0; cpu < hal.getProcessors().length; cpu++) {
			procCpu.append(String.format(
					" %.1f%%",
					hal.getProcessors()[cpu].getProcessorCpuLoadBetweenTicks() * 100));
		}
		System.out.println(procCpu.toString());
		// hardware: power
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
				sb.append(String.format("%d:%02d remaining",
						(int) (timeRemaining / 3600),
						(int) (timeRemaining / 60) % 60));
		}
		for (PowerSource pSource : hal.getPowerSources()) {
			sb.append(String.format("%n %s @ %.1f%%", pSource.getName(),
					pSource.getRemainingCapacity() * 100d));
		}
		System.out.println(sb.toString());
		// hardware: file system
		System.out.println("File System:");
		OSFileStore[] fsArray = hal.getFileStores();
		for (OSFileStore fs : fsArray) {
			long usable = fs.getUsableSpace();
			long total = fs.getTotalSpace();
			System.out.format(" %s (%s) %s of %s free (%.1f%%)%n",
					fs.getName(), fs.getDescription().isEmpty() ? "file system"
							: fs.getDescription(), FormatUtil
							.formatBytes(usable), FormatUtil.formatBytes(fs
							.getTotalSpace()), 100d * usable / total);
		}
	}
}
