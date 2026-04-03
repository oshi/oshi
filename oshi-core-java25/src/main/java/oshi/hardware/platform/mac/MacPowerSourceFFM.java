/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.mac.IOKitFunctions.IOPSCopyPowerSourcesInfo;
import static oshi.ffm.mac.IOKitFunctions.IOPSCopyPowerSourcesList;
import static oshi.ffm.mac.IOKitFunctions.IOPSGetPowerSourceDescription;
import static oshi.ffm.mac.IOKitFunctions.IOPSGetTimeRemainingEstimate;

import java.lang.foreign.MemorySegment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.mac.CoreFoundation.CFBooleanRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.IOKit.IOService;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.platform.mac.CFUtilFFM;
import oshi.util.platform.mac.IOKitUtilFFM;

/**
 * A Power Source
 */
@ThreadSafe
public final class MacPowerSourceFFM extends AbstractPowerSource {

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

    public static List<PowerSource> getPowerSources() {
        String psDeviceName = Constants.UNKNOWN;
        double psTimeRemainingInstant = 0d;
        double psPowerUsageRate = 0d;
        double psVoltage = -1d;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        CapacityUnits psCapacityUnits = CapacityUnits.RELATIVE;
        int psCurrentCapacity = 0;
        int psMaxCapacity = 1;
        int psDesignCapacity = 1;
        int psCycleCount = -1;
        String psChemistry = Constants.UNKNOWN;
        LocalDate psManufactureDate = null;
        String psManufacturer = Constants.UNKNOWN;
        String psSerialNumber = Constants.UNKNOWN;
        double psTemperature = 0d;

        IOService smartBattery = IOKitUtilFFM.getMatchingService("AppleSmartBattery");
        if (smartBattery != null) {
            try {
                String s = smartBattery.getStringProperty("DeviceName");
                if (s != null) {
                    psDeviceName = s;
                }
                s = smartBattery.getStringProperty("Manufacturer");
                if (s != null) {
                    psManufacturer = s;
                }
                s = smartBattery.getStringProperty("BatterySerialNumber");
                if (s != null) {
                    psSerialNumber = s;
                }

                Integer temp = smartBattery.getIntegerProperty("ManufactureDate");
                if (temp != null) {
                    int day = temp & 0x1f;
                    int month = (temp >> 5) & 0xf;
                    int year80 = (temp >> 9) & 0x7f;
                    try {
                        psManufactureDate = LocalDate.of(1980 + year80, month, day);
                    } catch (java.time.DateTimeException e) {
                        // Corrupt bitfield — leave psManufactureDate as null
                    }
                }

                temp = smartBattery.getIntegerProperty("DesignCapacity");
                if (temp != null) {
                    psDesignCapacity = temp;
                }
                temp = smartBattery.getIntegerProperty("MaxCapacity");
                if (temp != null) {
                    psMaxCapacity = temp;
                }
                temp = smartBattery.getIntegerProperty("CurrentCapacity");
                if (temp != null) {
                    psCurrentCapacity = temp;
                }
                psCapacityUnits = CapacityUnits.MAH;

                temp = smartBattery.getIntegerProperty("TimeRemaining");
                if (temp != null) {
                    psTimeRemainingInstant = temp * 60d;
                }
                temp = smartBattery.getIntegerProperty("CycleCount");
                if (temp != null) {
                    psCycleCount = temp;
                }
                temp = smartBattery.getIntegerProperty("Temperature");
                if (temp != null) {
                    psTemperature = temp / 100d;
                }
                temp = smartBattery.getIntegerProperty("Voltage");
                if (temp != null) {
                    psVoltage = temp / 1000d;
                }
                temp = smartBattery.getIntegerProperty("Amperage");
                if (temp != null) {
                    psAmperage = temp;
                }
                psPowerUsageRate = psVoltage * psAmperage;

                Boolean bool = smartBattery.getBooleanProperty("ExternalConnected");
                if (bool != null) {
                    psPowerOnLine = bool;
                }
                bool = smartBattery.getBooleanProperty("IsCharging");
                if (bool != null) {
                    psCharging = bool;
                }
                psDischarging = !psCharging && !psPowerOnLine;
            } finally {
                smartBattery.release();
            }
        }

        // Capture final copies for use in lambda/inner scope
        final String fDeviceName = psDeviceName;
        final double fTimeRemainingInstant = psTimeRemainingInstant;
        final double fPowerUsageRate = psPowerUsageRate;
        final double fVoltage = psVoltage;
        final double fAmperage = psAmperage;
        final boolean fPowerOnLine = psPowerOnLine;
        final boolean fCharging = psCharging;
        final boolean fDischarging = psDischarging;
        final CapacityUnits fCapacityUnits = psCapacityUnits;
        final int fCurrentCapacity = psCurrentCapacity;
        final int fMaxCapacity = psMaxCapacity;
        final int fDesignCapacity = psDesignCapacity;
        final int fCycleCount = psCycleCount;
        final String fChemistry = psChemistry;
        final LocalDate fManufactureDate = psManufactureDate;
        final String fManufacturer = psManufacturer;
        final String fSerialNumber = psSerialNumber;
        final double fTemperature = psTemperature;

        try {
            MemorySegment powerSourcesInfo = IOPSCopyPowerSourcesInfo();
            MemorySegment powerSourcesList = IOPSCopyPowerSourcesList(powerSourcesInfo);
            double psTimeRemainingEstimated = IOPSGetTimeRemainingEstimate();

            CFArrayRef cfList = new CFArrayRef(powerSourcesList);
            int powerSourcesCount = cfList.getCount();

            CFStringRef nameKey = CFStringRef.createCFString("Name");
            CFStringRef isPresentKey = CFStringRef.createCFString("Is Present");
            CFStringRef currentCapacityKey = CFStringRef.createCFString("Current Capacity");
            CFStringRef maxCapacityKey = CFStringRef.createCFString("Max Capacity");

            List<PowerSource> psList = new ArrayList<>(powerSourcesCount);
            try {
                for (int ps = 0; ps < powerSourcesCount; ps++) {
                    MemorySegment pwrSrcPtr = cfList.getValueAtIndex(ps);
                    MemorySegment dictionary = IOPSGetPowerSourceDescription(powerSourcesInfo, pwrSrcPtr);
                    CFDictionaryRef dict = new CFDictionaryRef(dictionary);

                    MemorySegment result = dict.getValue(isPresentKey);
                    if (!result.equals(MemorySegment.NULL)) {
                        CFBooleanRef isPresentRef = new CFBooleanRef(result);
                        if (isPresentRef.booleanValue()) {
                            result = dict.getValue(nameKey);
                            String psName = CFUtilFFM.cfPointerToString(result);

                            double currentCapacity = 0d;
                            if (dict.getValueIfPresent(currentCapacityKey, MemorySegment.NULL)) {
                                result = dict.getValue(currentCapacityKey);
                                currentCapacity = new CFNumberRef(result).intValue();
                            }
                            double maxCapacity = 1d;
                            if (dict.getValueIfPresent(maxCapacityKey, MemorySegment.NULL)) {
                                result = dict.getValue(maxCapacityKey);
                                maxCapacity = new CFNumberRef(result).intValue();
                            }
                            double psRemainingCapacityPercent = maxCapacity <= 0 ? 0d
                                    : Math.min(1d, currentCapacity / maxCapacity);

                            psList.add(new MacPowerSourceFFM(psName, fDeviceName, psRemainingCapacityPercent,
                                    psTimeRemainingEstimated, fTimeRemainingInstant, fPowerUsageRate, fVoltage,
                                    fAmperage, fPowerOnLine, fCharging, fDischarging, fCapacityUnits, fCurrentCapacity,
                                    fMaxCapacity, fDesignCapacity, fCycleCount, fChemistry, fManufactureDate,
                                    fManufacturer, fSerialNumber, fTemperature));
                        }
                    }
                }
            } finally {
                isPresentKey.release();
                nameKey.release();
                currentCapacityKey.release();
                maxCapacityKey.release();
                cfList.release();
                // powerSourcesInfo owns powerSourcesList; release info last
                oshi.ffm.mac.CoreFoundationFunctions.CFRelease(powerSourcesInfo);
            }
            return psList;
        } catch (Throwable e) {
            return new ArrayList<>();
        }
    }
}
