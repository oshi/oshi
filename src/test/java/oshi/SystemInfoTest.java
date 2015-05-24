/**
 * Copyright (c) Daniel Doubrovkine, 2010 dblock[at]dblock[dot]org All Rights
 * Reserved Eclipse Public License (EPLv1) http://oshi.codeplex.com/license
 */
package oshi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.FormatUtil;

/**
 * @author dblock[at]dblock[dot]org
 */
public class SystemInfoTest {

	@Test
	public void testGetVersion() {
		SystemInfo si = new SystemInfo();
		OperatingSystem os = si.getOperatingSystem();
		assertNotNull(os);
		OperatingSystemVersion version = os.getVersion();
		assertNotNull(version);
		assertTrue(os.toString().length() > 0);
	}

	@Test
	public void testGetProcessors() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertTrue(hal.getProcessors().length > 0);
	}

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

	@Test
	public void testCpuLoad() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		assertTrue(hal.getProcessors()[0].getLoad() >= 0
				&& hal.getProcessors()[0].getLoad() <= 100);
	}
    
    @Test
    public void testCpuVendorFreq() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
		assertTrue(hal.getProcessors()[0].getVendorFreq() == -1
				|| hal.getProcessors()[0].getVendorFreq() > 0);
    }

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
		System.out.println("CPU load: " + hal.getProcessors()[0].getLoad()
				+ "%");
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
	}
}
