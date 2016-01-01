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
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;

import oshi.jna.platform.mac.CoreFoundation.CFArrayRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * Power Supply stats. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface IOKit extends Library {
    IOKit INSTANCE = (IOKit) Native.loadLibrary("IOKit", IOKit.class);

    static final CFStringRef IOPS_NAME_KEY = CFStringRef.toCFString("Name");

    static final CFStringRef IOPS_IS_PRESENT_KEY = CFStringRef.toCFString("Is Present");

    static final CFStringRef IOPS_CURRENT_CAPACITY_KEY = CFStringRef.toCFString("Current Capacity");

    static final CFStringRef IOPS_MAX_CAPACITY_KEY = CFStringRef.toCFString("Max Capacity");

    CFTypeRef IOPSCopyPowerSourcesInfo();

    CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

    CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

    double IOPSGetTimeRemainingEstimate();
}
