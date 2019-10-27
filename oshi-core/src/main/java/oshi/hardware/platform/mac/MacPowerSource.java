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

import com.sun.jna.Pointer; // NOSONAR squid:S1191

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFArrayRef;
import oshi.jna.platform.mac.CoreFoundation.CFBooleanRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFNumberRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.IORegistryEntry;
import oshi.jna.platform.mac.IOKitUtil;
import oshi.util.Constants;

/**
 * A Power Source
 */
public class MacPowerSource extends AbstractPowerSource {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;
    private static final IOKit IO = IOKit.INSTANCE;

    private String name;
    private double remainingCapacity;
    private double timeRemaining;

    public MacPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        this.name = newName;
        this.remainingCapacity = newRemainingCapacity;
        this.timeRemaining = newTimeRemaining;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getTimeRemaining() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateAttributes() {
        PowerSource[] psArr = getPowerSources();
        for (PowerSource ps : psArr) {
            if (ps.getName().equals(this.name)) {
                this.remainingCapacity = ps.getRemainingCapacity();
                this.timeRemaining = ps.getTimeRemaining();
                return;
            }
        }
        // Didn't find this battery
        this.remainingCapacity = 0d;
        this.timeRemaining = -1d;
    }

    /**
     * Gets Battery Information.
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        // Get the blob containing current power source state
        CFTypeRef powerSourcesInfo = IO.IOPSCopyPowerSourcesInfo();
        CFArrayRef powerSourcesList = IO.IOPSCopyPowerSourcesList(powerSourcesInfo);
        int powerSourcesCount = powerSourcesList.getCount();

        // Get time remaining
        // -1 = unknown, -2 = unlimited
        double timeRemaining = IO.IOPSGetTimeRemainingEstimate();

        CFStringRef nameKey = CFStringRef.createCFString("Name");
        CFStringRef isPresentKey = CFStringRef.createCFString("Is Present");
        CFStringRef currentCapacityKey = CFStringRef.createCFString("Current Capacity");
        CFStringRef maxCapacityKey = CFStringRef.createCFString("Max Capacity");
        // For each power source, output various info
        List<MacPowerSource> psList = new ArrayList<>(powerSourcesCount);
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
                    CFStringRef cfName = new CFStringRef(result);
                    String name = cfName.stringValue();
                    if (name == null) {
                        name = Constants.UNKNOWN;
                    }
                    // Remaining Capacity = current / max
                    int currentCapacity = 0;
                    if (dictionary.getValueIfPresent(currentCapacityKey, null)) {
                        result = dictionary.getValue(currentCapacityKey);
                        CFNumberRef cap = new CFNumberRef(result);
                        currentCapacity = cap.intValue();
                    }
                    int maxCapacity = 100;
                    if (dictionary.getValueIfPresent(maxCapacityKey, null)) {
                        result = dictionary.getValue(maxCapacityKey);
                        CFNumberRef cap = new CFNumberRef(result);
                        maxCapacity = cap.intValue();
                    }
                    // Add to list
                    psList.add(new MacPowerSource(name, (double) currentCapacity / maxCapacity, timeRemaining));
                }
            }
        }
        isPresentKey.release();
        nameKey.release();
        currentCapacityKey.release();
        maxCapacityKey.release();
        // Release the blob
        powerSourcesList.release();
        powerSourcesInfo.release();

        return psList.toArray(new MacPowerSource[0]);
    }

    private static int GetBatteryState() {
        String manufacturer;
        String serialNumber;
        String deviceName;
        IORegistryEntry smartBattery = IOKitUtil.getMatchingService("AppleSmartBattery");
        if (smartBattery != null) {
            deviceName = smartBattery.getStringProperty("DeviceName");
            manufacturer = smartBattery.getStringProperty("Manufacturer");
            serialNumber = smartBattery.getStringProperty("BatterySerialNumber");
            System.out.println(manufacturer + "/" + serialNumber);
            int manufactureDate = smartBattery.getIntegerProperty("ManufactureDate");
            // Bits 0...4 => day (value 1-31; 5 bits)
            // Bits 5...8 => month (value 1-12; 4 bits)
            // Bits 9...15 => years since 1980 (value 0-127; 7 bits)
            int day = manufactureDate & 0x1f;
            int month = (manufactureDate >> 5) & 0xf;
            int year80 = (manufactureDate >> 9) & 0x7f;
            System.out.format("%4d-%02d-%02d%n", 1980 + year80, month, day);

            int designCapacity = smartBattery.getIntegerProperty("DesignCapacity");
            int maxCapacity = smartBattery.getIntegerProperty("MaxCapacity");
            int curCapacity = smartBattery.getIntegerProperty("CurrentCapacity");
            System.out.println(curCapacity + "/" + maxCapacity + " of " + designCapacity + "mAh");
            int timeRemaining = smartBattery.getIntegerProperty("TimeRemaining");
            System.out.println("Remaining " + timeRemaining / 3600d + " hrs");

            int cycleCount = smartBattery.getIntegerProperty("CycleCount");
            int temp = smartBattery.getIntegerProperty("Temperature");
            System.out.println("temp " + temp / 100d + "C, cycles " + cycleCount);

            int voltage = smartBattery.getIntegerProperty("Voltage");
            int amperage = smartBattery.getIntegerProperty("Amperage");
            System.out.println(
                    voltage / 1000d + "V, " + amperage / 1000d + "A, power " + voltage / 1000000d * amperage + "W");

            // TODO
            // ExternalConnected - true if drawing power
            // BatteryInstalled - true if present
            // IsCharging - true if charging

            smartBattery.release();
        }
        return 0;
    }

    public static void main(String[] args) {
        System.out.println("RESULT: " + MacPowerSource.GetBatteryState());
    }
}
