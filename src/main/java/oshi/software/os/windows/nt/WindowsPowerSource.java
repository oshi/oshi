/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.windows.nt;

import oshi.hardware.PowerSource;
import oshi.software.os.windows.nt.PowrProf.SystemBatteryState;
import oshi.util.FormatUtil;

import com.sun.jna.NativeLong;

/**
 * A Power Source
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WindowsPowerSource implements PowerSource {
	private String name;
	private double remainingCapacity;
	private double timeRemaining;

	public WindowsPowerSource(String name, double remainingCapacity,
			double timeRemaining) {
		this.name = name;
		this.remainingCapacity = remainingCapacity;
		this.timeRemaining = timeRemaining;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public double getRemainingCapacity() {
		return remainingCapacity;
	}

	@Override
	public double getTimeRemaining() {
		return timeRemaining;
	}

	/**
	 * Battery Information
	 */
	public static PowerSource[] getPowerSources() {
		// Windows provides a single unnamed battery
		String name = "System Battery";
		WindowsPowerSource[] psArray = new WindowsPowerSource[1];
		// Get structure
		SystemBatteryState batteryState = new SystemBatteryState();
		if (0 != PowrProf.INSTANCE.CallNtPowerInformation(
				PowrProf.SYSTEM_BATTERY_STATE, null, new NativeLong(0),
				batteryState, new NativeLong(batteryState.size()))
				|| batteryState.batteryPresent == 0) {
			psArray[0] = new WindowsPowerSource("Unknown", 0d, -1d);
		} else {
			int estimatedTime = -2; // -1 = unknown, -2 = unlimited
			if (batteryState.acOnLine == 0 && batteryState.charging == 0
					&& batteryState.discharging > 0)
				estimatedTime = batteryState.estimatedTime;
			long maxCapacity = FormatUtil
					.getUnsignedInt(batteryState.maxCapacity);
			long remainingCapacity = FormatUtil
					.getUnsignedInt(batteryState.remainingCapacity);

			psArray[0] = new WindowsPowerSource(name,
					(double) remainingCapacity / maxCapacity,
					(double) estimatedTime);
		}

		return psArray;
	}
}
