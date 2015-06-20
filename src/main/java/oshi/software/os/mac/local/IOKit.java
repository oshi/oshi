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
