/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.PowerSource;
import oshi.software.os.mac.local.CoreFoundation.CFArrayRef;
import oshi.software.os.mac.local.CoreFoundation.CFDictionaryRef;
import oshi.software.os.mac.local.CoreFoundation.CFTypeRef;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * A Power Source
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacPowerSource implements PowerSource {

	private String name;

	private double remainingCapacity;

	private double timeRemaining;

	public MacPowerSource(String name, double remainingCapacity,
			double timeRemaining) {
		this.name = name;
		this.remainingCapacity = remainingCapacity;
		this.timeRemaining = timeRemaining;
	}

	public String getName() {
		return name;
	}

	public double getRemainingCapacity() {
		return remainingCapacity;
	}

	public double getTimeRemaining() {
		return timeRemaining;
	}

	/**
	 * Battery Information
	 */
	public static PowerSource[] getPowerSources() {
		// Get the blob containing current power source state
		CFTypeRef powerSourcesInfo = IOKit.INSTANCE.IOPSCopyPowerSourcesInfo();
		CFArrayRef powerSourcesList = IOKit.INSTANCE
				.IOPSCopyPowerSourcesList(powerSourcesInfo);
		int powerSourcesCount = CoreFoundation.INSTANCE
				.CFArrayGetCount(powerSourcesList);

		// Get time remaining
		// -1 = unknown, -2 = unlimited
		double timeRemaining = IOKit.INSTANCE.IOPSGetTimeRemainingEstimate();

		// For each power source, output various info
		List<MacPowerSource> psList = new ArrayList<MacPowerSource>(
				powerSourcesCount);
		for (int ps = 0; ps < powerSourcesCount; ps++) {
			// Get the dictionary for that Power Source
			CFTypeRef powerSource = CoreFoundation.INSTANCE
					.CFArrayGetValueAtIndex(powerSourcesList, ps);
			CFDictionaryRef dictionary = IOKit.INSTANCE
					.IOPSGetPowerSourceDescription(powerSourcesInfo,
							powerSource);

			// Get values from dictionary (See IOPSKeys.h)
			// Skip if not present
			boolean isPresent = false;
			Pointer isPresentRef = CoreFoundation.INSTANCE
					.CFDictionaryGetValue(dictionary, IOKit.IOPS_IS_PRESENT_KEY);
			if (isPresentRef != null)
				isPresent = CoreFoundation.INSTANCE
						.CFBooleanGetValue(isPresentRef);
			if (!isPresent)
				continue;

			// Name
			Pointer name = CoreFoundation.INSTANCE.CFDictionaryGetValue(
					dictionary, IOKit.IOPS_NAME_KEY);
			long length = CoreFoundation.INSTANCE.CFStringGetLength(name);
			long maxSize = CoreFoundation.INSTANCE
					.CFStringGetMaximumSizeForEncoding(length,
							CoreFoundation.UTF_8);
			Pointer nameBuf = new Memory(maxSize);
			CoreFoundation.INSTANCE.CFStringGetCString(name, nameBuf, maxSize,
					CoreFoundation.UTF_8);

			// Remaining Capacity = current / max
			IntByReference currentCapacity = new IntByReference();
			if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(
					dictionary, IOKit.IOPS_CURRENT_CAPACITY_KEY,
					currentCapacity))
				currentCapacity = new IntByReference(0);
			IntByReference maxCapacity = new IntByReference();
			if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(
					dictionary, IOKit.IOPS_MAX_CAPACITY_KEY, maxCapacity))
				maxCapacity = new IntByReference(1);

			// Add to list
			psList.add(new MacPowerSource(nameBuf != null ? nameBuf
					.getString(0) : "Unknown", (double) currentCapacity
					.getValue() / maxCapacity.getValue(), timeRemaining));
		}
		// Release the blob
		CoreFoundation.INSTANCE.CFRelease(powerSourcesInfo);

		return psList.toArray(new MacPowerSource[psList.size()]);
	}
}
