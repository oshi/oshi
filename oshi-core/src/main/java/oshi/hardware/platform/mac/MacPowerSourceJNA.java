/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.mac.MacPowerSource;
import oshi.util.platform.mac.CFUtil;

/**
 * A Power Source
 */
@ThreadSafe
public final class MacPowerSourceJNA extends MacPowerSource {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;
    private static final IOKit IO = IOKit.INSTANCE;

    public MacPowerSourceJNA(String psName, String psDeviceName, double psRemainingCapacityPercent,
            double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage,
            double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
            CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
            int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
            String psSerialNumber, double psTemperature) {
        super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
    }

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
    }

    /**
     * Gets Battery Information.
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        // Get the blob containing current power source state
        CFTypeRef powerSourcesInfo = IO.IOPSCopyPowerSourcesInfo();
        CFArrayRef powerSourcesList = IO.IOPSCopyPowerSourcesList(powerSourcesInfo);
        if (powerSourcesList == null) {
            if (powerSourcesInfo != null) {
                powerSourcesInfo.release();
            }
            return Collections.emptyList();
        }
        // Keys start null so the finally block releases only what was actually created, even if an exception
        // is thrown partway through, so the CF references above are never leaked.
        CFStringRef nameKey = null;
        CFStringRef isPresentKey = null;
        CFStringRef currentCapacityKey = null;
        CFStringRef maxCapacityKey = null;
        try {
            int powerSourcesCount = powerSourcesList.getCount();

            // Get time remaining
            // -1 = unknown, -2 = unlimited
            double psTimeRemainingEstimated = IO.IOPSGetTimeRemainingEstimate();

            nameKey = CFStringRef.createCFString("Name");
            isPresentKey = CFStringRef.createCFString("Is Present");
            currentCapacityKey = CFStringRef.createCFString("Current Capacity");
            maxCapacityKey = CFStringRef.createCFString("Max Capacity");
            // For each power source, capture the present ones for the shared computation
            List<PowerSourceData> sources = new ArrayList<>(powerSourcesCount);
            for (int ps = 0; ps < powerSourcesCount; ps++) {
                // Get the dictionary for that Power Source
                Pointer pwrSrcPtr = powerSourcesList.getValueAtIndex(ps);
                CFTypeRef powerSource = new CFTypeRef();
                powerSource.setPointer(pwrSrcPtr);
                CFDictionaryRef dictionary = IO.IOPSGetPowerSourceDescription(powerSourcesInfo, powerSource);

                // Get values from dictionary (See IOPSKeys.h)
                // Skip if not present
                Pointer result = dictionary.getValue(isPresentKey);
                if (result != null) {
                    CFBooleanRef isPresentRef = new CFBooleanRef(result);
                    if (0 != CF.CFBooleanGetValue(isPresentRef)) {
                        // Get name
                        result = dictionary.getValue(nameKey);
                        String psName = CFUtil.cfPointerToString(result);
                        // Remaining Capacity = current / max
                        double currentCapacity = 0d;
                        if (dictionary.getValueIfPresent(currentCapacityKey, null)) {
                            result = dictionary.getValue(currentCapacityKey);
                            CFNumberRef cap = new CFNumberRef(result);
                            currentCapacity = cap.intValue();
                        }
                        double maxCapacity = 1d;
                        if (dictionary.getValueIfPresent(maxCapacityKey, null)) {
                            result = dictionary.getValue(maxCapacityKey);
                            CFNumberRef cap = new CFNumberRef(result);
                            maxCapacity = cap.intValue();
                        }
                        sources.add(new PowerSourceData(psName, currentCapacity, maxCapacity));
                    }
                }
            }
            return buildPowerSources(IOKitProviderJNA.INSTANCE, psTimeRemainingEstimated, sources,
                    MacPowerSourceJNA::new);
        } finally {
            if (isPresentKey != null) {
                isPresentKey.release();
            }
            if (nameKey != null) {
                nameKey.release();
            }
            if (currentCapacityKey != null) {
                currentCapacityKey.release();
            }
            if (maxCapacityKey != null) {
                maxCapacityKey.release();
            }
            // Release the blob
            powerSourcesList.release();
            if (powerSourcesInfo != null) {
                powerSourcesInfo.release();
            }
        }
    }
}
