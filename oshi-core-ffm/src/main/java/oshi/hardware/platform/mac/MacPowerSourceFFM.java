/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.platform.mac.IOKitFunctions.IOPSCopyPowerSourcesInfo;
import static oshi.ffm.platform.mac.IOKitFunctions.IOPSCopyPowerSourcesList;
import static oshi.ffm.platform.mac.IOKitFunctions.IOPSGetPowerSourceDescription;
import static oshi.ffm.platform.mac.IOKitFunctions.IOPSGetTimeRemainingEstimate;

import java.lang.foreign.MemorySegment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.platform.mac.CoreFoundation.CFBooleanRef;
import oshi.ffm.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.ffm.platform.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.util.platform.mac.CFUtilFFM;
import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.mac.MacPowerSource;

/**
 * A Power Source
 */
@ThreadSafe
public final class MacPowerSourceFFM extends MacPowerSource {

    public MacPowerSourceFFM(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
        CFStringRef nameKey = CFStringRef.createCFString("Name");
        CFStringRef isPresentKey = CFStringRef.createCFString("Is Present");
        CFStringRef currentCapacityKey = CFStringRef.createCFString("Current Capacity");
        CFStringRef maxCapacityKey = CFStringRef.createCFString("Max Capacity");
        try (nameKey; isPresentKey; currentCapacityKey; maxCapacityKey) {
            MemorySegment powerSourcesInfo = IOPSCopyPowerSourcesInfo();
            // wrapped only to release the native CF object on close
            try (var _ = new CFTypeRef(powerSourcesInfo)) {
                CFArrayRef cfList = new CFArrayRef(IOPSCopyPowerSourcesList(powerSourcesInfo));
                try (cfList) {
                    double psTimeRemainingEstimated = IOPSGetTimeRemainingEstimate();
                    int powerSourcesCount = cfList.getCount();

                    List<PowerSourceData> sources = new ArrayList<>(powerSourcesCount);
                    for (int ps = 0; ps < powerSourcesCount; ps++) {
                        MemorySegment pwrSrcPtr = cfList.getValueAtIndex(ps);
                        MemorySegment dictionary = IOPSGetPowerSourceDescription(powerSourcesInfo, pwrSrcPtr);
                        @SuppressWarnings("resource") // borrowed ref from IOPSGetPowerSourceDescription
                        CFDictionaryRef dict = new CFDictionaryRef(dictionary);

                        MemorySegment result = dict.getValue(isPresentKey);
                        if (!result.equals(MemorySegment.NULL) && CFBooleanRef.booleanValue(result)) {
                            result = dict.getValue(nameKey);
                            String psName = CFUtilFFM.cfPointerToString(result);

                            double currentCapacity = 0d;
                            if (dict.getValueIfPresent(currentCapacityKey, MemorySegment.NULL)) {
                                result = dict.getValue(currentCapacityKey);
                                currentCapacity = CFNumberRef.intValue(result);
                            }
                            double maxCapacity = 1d;
                            if (dict.getValueIfPresent(maxCapacityKey, MemorySegment.NULL)) {
                                result = dict.getValue(maxCapacityKey);
                                maxCapacity = CFNumberRef.intValue(result);
                            }
                            sources.add(new PowerSourceData(psName, currentCapacity, maxCapacity));
                        }
                    }
                    return buildPowerSources(IOKitProviderFFM.INSTANCE, psTimeRemainingEstimated, sources,
                            MacPowerSourceFFM::new);
                }
            }
        } catch (Throwable _) {
            return new ArrayList<>();
        }
    }
}
