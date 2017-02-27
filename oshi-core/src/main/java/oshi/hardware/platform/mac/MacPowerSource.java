/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFArrayRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
import oshi.jna.platform.mac.IOKit;
import oshi.util.platform.mac.CfUtil;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public class MacPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacPowerSource.class);

    public MacPowerSource() {
        super();
        LOG.debug("Initialized MacPowerSource");
    }
    
    @Deprecated
    public MacPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized MacPowerSource");
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
            MacPowerSource pSource = new MacPowerSource();
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

            // Get name
            pSource.setName(CfUtil.cfPointerToString(
                    CoreFoundation.INSTANCE.CFDictionaryGetValue(dictionary, IOKit.IOPS_NAME_KEY)));

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
            IntByReference designCapacity = new IntByReference();
            if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(dictionary, IOKit.IOPS_DESIGN_CAPACITY_KEY,
                    designCapacity)) {
                designCapacity = new IntByReference(1);
            }
            IntByReference voltage = new IntByReference();
            if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(dictionary, IOKit.IOPS_VOLTAGE_KEY, voltage)) {
                voltage = new IntByReference(0);
            }
            IntByReference current = new IntByReference();
            if (!CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(dictionary, IOKit.IOPS_CURRENT_KEY, current)) {
                current = new IntByReference(0);
            }
            
            long power = voltage.getValue() * current.getValue() / 1000;
            long remainingCapacity = currentCapacity.getValue() / maxCapacity.getValue();
            double remainingCharge;
            if(timeRemaining < 0)
                remainingCharge = (power * timeRemaining) / 60;
            else
                remainingCharge = 0;
            
            pSource.setRemainingCapacity(remainingCapacity);
            pSource.setTimeRemaining(timeRemaining);
            pSource.setHealth((double) maxCapacity.getValue() / designCapacity.getValue()); 
            pSource.setMaximumCharge((long) (remainingCharge / remainingCapacity));
            pSource.setRemainingCharge((long) remainingCharge); 
            pSource.setPower((long) power);
            psList.add(pSource);
        }
        // Release the blob
        CoreFoundation.INSTANCE.CFRelease(powerSourcesInfo);

        return psList.toArray(new MacPowerSource[psList.size()]);
    }
}
