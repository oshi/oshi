/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import oshi.software.os.mac.local.CoreFoundation.CFArrayRef;
import oshi.software.os.mac.local.CoreFoundation.CFDictionaryRef;
import oshi.software.os.mac.local.CoreFoundation.CFStringRef;
import oshi.software.os.mac.local.CoreFoundation.CFTypeRef;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Power Supply stats
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface IOKit extends Library {
	IOKit INSTANCE = (IOKit) Native.loadLibrary("IOKit", IOKit.class);

	public static final CFStringRef IOPS_NAME_KEY = CFStringRef
			.toCFString("Name");
	public static final CFStringRef IOPS_IS_PRESENT_KEY = CFStringRef
			.toCFString("Is Present");
	public static final CFStringRef IOPS_CURRENT_CAPACITY_KEY = CFStringRef
			.toCFString("Current Capacity");
	public static final CFStringRef IOPS_MAX_CAPACITY_KEY = CFStringRef
			.toCFString("Max Capacity");

	CFTypeRef IOPSCopyPowerSourcesInfo();

	CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

	CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

	double IOPSGetTimeRemainingEstimate();
}
