/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer; // NOSONAR
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
            String name = CfUtil
                    .cfPointerToString(CoreFoundation.INSTANCE.CFDictionaryGetValue(dictionary, IOKit.IOPS_NAME_KEY));

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
            psList.add(new MacPowerSource(name, (double) currentCapacity.getValue() / maxCapacity.getValue(),
                    timeRemaining));
        }
        // Release the blob
        CoreFoundation.INSTANCE.CFRelease(powerSourcesInfo);

        return psList.toArray(new MacPowerSource[0]);
    }
}
