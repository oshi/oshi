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
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.PowerSource;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.CoreFoundation.CFArrayRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;

/**
 * A Power Source
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacPowerSource implements PowerSource {
    private static final Logger LOG = LoggerFactory.getLogger(MacPowerSource.class);

    private String name;

    private double remainingCapacity;

    private double timeRemaining;

    public MacPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        this.name = newName;
        this.remainingCapacity = newRemainingCapacity;
        this.timeRemaining = newTimeRemaining;
        LOG.debug("Initialized MacPowerSource");
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
     * Gets Battery Information
     * 
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        // Get the blob containing current power source state
        CFTypeRef powerSourcesInfo = IOKit.INSTANCE.IOPSCopyPowerSourcesInfo();
        CFArrayRef powerSourcesList = IOKit.INSTANCE.IOPSCopyPowerSourcesList(powerSourcesInfo);
        int powerSourcesCount = CoreFoundation.INSTANCE.CFArrayGetCount(powerSourcesList);

        // Get time remaining
        // -1 = unknown, -2 = unlimited
        double timeRemaining = IOKit.INSTANCE.IOPSGetTimeRemainingEstimate();

        // For each power source, output various info
        List<MacPowerSource> psList = new ArrayList<>(powerSourcesCount);
        for (int ps = 0; ps < powerSourcesCount; ps++) {
            // Get the dictionary for that Power Source
            CFTypeRef powerSource = CoreFoundation.INSTANCE.CFArrayGetValueAtIndex(powerSourcesList, ps);
            CFDictionaryRef dictionary = IOKit.INSTANCE.IOPSGetPowerSourceDescription(powerSourcesInfo, powerSource);

            // Get values from dictionary (See IOPSKeys.h)
            // Skip if not present
            boolean isPresent = false;
            Pointer isPresentRef = CoreFoundation.INSTANCE.CFDictionaryGetValue(dictionary, IOKit.IOPS_IS_PRESENT_KEY);
            if (isPresentRef != null) {
                isPresent = CoreFoundation.INSTANCE.CFBooleanGetValue(isPresentRef);
            }
            if (!isPresent) {
                continue;
            }

            // Name
            Pointer name = CoreFoundation.INSTANCE.CFDictionaryGetValue(dictionary, IOKit.IOPS_NAME_KEY);
            long length = CoreFoundation.INSTANCE.CFStringGetLength(name);
            long maxSize = CoreFoundation.INSTANCE.CFStringGetMaximumSizeForEncoding(length, CoreFoundation.UTF_8);
            Pointer nameBuf = new Memory(maxSize);
            CoreFoundation.INSTANCE.CFStringGetCString(name, nameBuf, maxSize, CoreFoundation.UTF_8);

            // Remaining Capacity = current / max
            IntByReference currentCapacity = new IntByReference();
            if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(dictionary, IOKit.IOPS_CURRENT_CAPACITY_KEY,
                    currentCapacity)) {
                currentCapacity = new IntByReference(0);
            }
            IntByReference maxCapacity = new IntByReference();
            if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(dictionary, IOKit.IOPS_MAX_CAPACITY_KEY,
                    maxCapacity)) {
                maxCapacity = new IntByReference(1);
            }

            // Add to list
            psList.add(new MacPowerSource(nameBuf.getString(0),
                    (double) currentCapacity.getValue() / maxCapacity.getValue(), timeRemaining));
        }
        // Release the blob
        CoreFoundation.INSTANCE.CFRelease(powerSourcesInfo);

        return psList.toArray(new MacPowerSource[psList.size()]);
    }
}
