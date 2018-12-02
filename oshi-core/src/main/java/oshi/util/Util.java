/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

/**
 * General utility methods
 *
 * @author widdis[at]gmail[dot]com
 */
public class Util {
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);

	private static final Map<String, String> vmIF_Mac = new HashMap<>();
	static {
		vmIF_Mac.put("00:50:56", "VMware ESX 3");
		vmIF_Mac.put("00:0C:29", "VMware ESX 3");
		vmIF_Mac.put("00:05:69", "VMware ESX 3");
		vmIF_Mac.put("00:03:FF", "Microsoft Hyper-V");
		vmIF_Mac.put("00:1C:42", "Parallels Desktop");
		vmIF_Mac.put("00:0F:4B", "Virtual Iron 4");
		vmIF_Mac.put("00:16:3E", "Xen or Oracle VM");
		vmIF_Mac.put("08:00:27", "Sun xVM VirtualBox");
	}

	private static final String[] vmModelArray = new String[] { "Linux KVM", "Linux lguest", "OpenVZ", "Qemu",
			"Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "OpenVZ Host", "VirtualBox",
			"Parallels", "Linux Containers", "LXC" };

	private Util() {
	}

	/**
	 * Sleeps for the specified number of milliseconds.
	 *
	 * @param ms How long to sleep
	 */
	public static void sleep(long ms) {
		try {
			LOG.trace("Sleeping for {} ms", ms);
			Thread.sleep(ms);
		} catch (InterruptedException e) { // NOSONAR squid:S2142
			LOG.warn("Interrupted while sleeping for {} ms: {}", ms, e);
		}
	}

	/**
	 * Sleeps for the specified number of milliseconds after the given system time
	 * in milliseconds. If that number of milliseconds has already elapsed, does
	 * nothing.
	 *
	 * @param startTime System time in milliseconds to sleep after
	 * @param ms        How long after startTime to sleep
	 */
	public static void sleepAfter(long startTime, long ms) {
		long now = System.currentTimeMillis();
		long until = startTime + ms;
		LOG.trace("Sleeping until {}", until);
		if (now < until) {
			sleep(until - now);
		}
	}

	/**
	 * Generates a Computer Identifier, which may be part of a strategy to construct
	 * a licence key. (The identifier may not be unique as in one case hashcode
	 * could be same for multiple values, and the result may differ based on whether
	 * the program is running with sudo/root permission.) The identifier string is
	 * based upon the processor serial number, vendor, processor identifier, and
	 * total processor count.
	 * 
	 * @return A string containing four hyphen-delimited fields representing the
	 *         processor; the first 3 are 32-bit hexadecimal values and the last one
	 *         is an integer value.
	 */
	public static String getComputerIdentifier() {
		SystemInfo systemInfo = new SystemInfo();
		OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
		CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
		ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();

		String vendor = operatingSystem.getManufacturer();
		String processorSerialNumber = computerSystem.getSerialNumber();
		String processorIdentifier = centralProcessor.getIdentifier();
		int processors = centralProcessor.getLogicalProcessorCount();

		String delimiter = "-";

		return String.format("%08x", vendor.hashCode()) + delimiter
				+ String.format("%08x", processorSerialNumber.hashCode()) + delimiter
				+ String.format("%08x", processorIdentifier.hashCode()) + delimiter + processors;
	}

	/**
	 * The function returns a string for VM identification, based on 2 checks so far:
	 *  	1. MAC address
	 *  	2. Computer model
	 * The checks are done against two constants defined above. More can be added to
	 * the constants as we encounter more types.
	 * 
	 * @return A string indicating if the machine is a VM or not. If yes, what type
	 */
	public static String identifyVM() {

		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hw = si.getHardware();

		// Try well known MAC addresses
		NetworkIF[] nifs = hw.getNetworkIFs();
		boolean isvm = false;
		for (NetworkIF nif : nifs) {
			String mac = nif.getMacaddr().substring(0, 8).toUpperCase();
			if (vmIF_Mac.containsKey(mac)) {
				isvm = true;
				return "On a VM: " + vmIF_Mac.get(mac);
			}
		}

		// Try well known models
		if (!isvm) {
			String model = hw.getComputerSystem().getModel();
			System.out.println(model);
			for (String vm : vmModelArray) {
				if (model.contains(vm)) {
					isvm = true;
					return "On a VM: " + vm;
				}
			}
		}
		return "Couldn't detect VM";
	}
}
