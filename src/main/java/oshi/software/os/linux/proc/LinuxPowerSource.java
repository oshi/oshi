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
package oshi.software.os.linux.proc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oshi.hardware.PowerSource;
import oshi.util.FileUtil;

/**
 * A Power Source
 * 
 * @author widdis[at]gmail[dot]com
 */
public class LinuxPowerSource implements PowerSource {

	private static final String PS_PATH = "/sys/class/power_supply/";

	private String name;

	private double remainingCapacity;

	private double timeRemaining;

	public LinuxPowerSource(String name, double remainingCapacity,
			double timeRemaining) {
		this.name = name;
		this.remainingCapacity = remainingCapacity;
		this.timeRemaining = timeRemaining;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public double getRemainingCapacity() {
		return this.remainingCapacity;
	}

	@Override
	public double getTimeRemaining() {
		return this.timeRemaining;
	}

	/**
	 * Battery Information
	 */
	public static PowerSource[] getPowerSources() {
		// Get list of power source names
		File f = new File(PS_PATH);
		String[] psNames = f.list();
		// Empty directory will give null rather than empty array, so fix
		if (psNames == null)
			psNames = new String[0];
		List<LinuxPowerSource> psList = new ArrayList<>(
				psNames.length);
		// For each power source, output various info
		for (String psName : psNames) {
			// Skip if name is ADP* (AC power supply)
			if (psName.startsWith("ADP"))
				continue;
			// Skip if can't read uevent file
			List<String> psInfo;
			try {
				psInfo = FileUtil.readFile(PS_PATH + psName + "/uevent");
			} catch (IOException e) {
				continue;
			}
			// Initialize defaults
			boolean isPresent = false;
			boolean isCharging = false;
			String name = "Unknown";
			int energyNow = 0;
			int energyFull = 1;
			int powerNow = 1;
			for (String checkLine : psInfo) {
				if (checkLine.startsWith("POWER_SUPPLY_PRESENT")) {
					// Skip if not present
					String[] psSplit = checkLine.split("=");
					if (psSplit.length > 1)
						isPresent = Integer.parseInt(psSplit[1]) > 0;
					if (!isPresent)
						continue;
				} else if (checkLine.startsWith("POWER_SUPPLY_NAME")) {
					// Name
					String[] psSplit = checkLine.split("=");
					if (psSplit.length > 1)
						name = psSplit[1];
				} else if (checkLine.startsWith("POWER_SUPPLY_ENERGY_NOW")
						|| checkLine.startsWith("POWER_SUPPLY_CHARGE_NOW")) {
					// Remaining Capacity = energyNow / energyFull
					String[] psSplit = checkLine.split("=");
					if (psSplit.length > 1)
						energyNow = Integer.parseInt(psSplit[1]);
				} else if (checkLine.startsWith("POWER_SUPPLY_ENERGY_FULL")
						|| checkLine.startsWith("POWER_SUPPLY_CHARGE_FULL")) {
					String[] psSplit = checkLine.split("=");
					if (psSplit.length > 1)
						energyFull = Integer.parseInt(psSplit[1]);
				} else if (checkLine.startsWith("POWER_SUPPLY_STATUS")) {
					// Check if charging
					String[] psSplit = checkLine.split("=");
					if (psSplit.length > 1 && psSplit[1].equals("Charging"))
						isCharging = true;
				} else if (checkLine.startsWith("POWER_SUPPLY_POWER_NOW")
						|| checkLine.startsWith("POWER_SUPPLY_CURRENT_NOW")) {
					// Time Remaining = energyNow / powerNow (hours)
					String[] psSplit = checkLine.split("=");
					if (psSplit.length > 1)
						powerNow = Integer.parseInt(psSplit[1]);
					if (powerNow <= 0)
						isCharging = true;
				}
			}
			psList.add(new LinuxPowerSource(name, (double) energyNow
					/ energyFull, isCharging ? -2d : 3600d * energyNow
					/ powerNow));
		}

		return psList.toArray(new LinuxPowerSource[psList.size()]);
	}
}
