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

import java.time.LocalDate;
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
    private String deviceName;
    private double remainingCapacityPercent;
    private double timeRemainingEstimated;
    private double timeRemainingInstant;
    private double powerUsageRate;
    private double voltage;
    private double amperage;
    private boolean powerOnLine;
    private boolean charging;
    private boolean discharging;
    private CapacityUnits capacityUnits;
    private int currentCapacity;
    private int maxCapacity;
    private int designCapacity;
    private int cycleCount;
    private String chemistry;
    private LocalDate manufactureDate;
    private String manufacturer;
    private String serialNumber;
    private double temperature;

    public MacPowerSource(String name, String deviceName, double remainingCapacityPercent,
            double timeRemainingEstimated, double timeRemainingInstant, double powerUsageRate, double voltage,
            double amperage, boolean powerOnLine, boolean charging, boolean discharging, CapacityUnits capacityUnits,
            int currentCapacity, int maxCapacity, int designCapacity, int cycleCount, String chemistry,
            LocalDate manufactureDate, String manufacturer, String serialNumber, double temperature) {
        super();
        this.name = name;
        this.deviceName = deviceName;
        this.remainingCapacityPercent = remainingCapacityPercent;
        this.timeRemainingEstimated = timeRemainingEstimated;
        this.timeRemainingInstant = timeRemainingInstant;
        this.powerUsageRate = powerUsageRate;
        this.voltage = voltage;
        this.amperage = amperage;
        this.powerOnLine = powerOnLine;
        this.charging = charging;
        this.discharging = discharging;
        this.capacityUnits = capacityUnits;
        this.currentCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
        this.designCapacity = designCapacity;
        this.cycleCount = cycleCount;
        this.chemistry = chemistry;
        this.manufactureDate = manufactureDate;
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.temperature = temperature;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDeviceName() {
        return this.deviceName;
    }

    @Override
    public double getRemainingCapacityPercent() {
        return this.remainingCapacityPercent;
    }

    @Override
    public double getTimeRemainingEstimated() {
        return this.timeRemainingEstimated;
    }

    @Override
    public double getTimeRemainingInstant() {
        return this.timeRemainingInstant;
    }

    @Override
    public double getPowerUsageRate() {
        return this.powerUsageRate;
    }

    @Override
    public double getVoltage() {
        return this.voltage;
    }

    @Override
    public double getAmperage() {
        return this.amperage;
    }

    @Override
    public boolean isPowerOnLine() {
        return this.powerOnLine;
    }

    @Override
    public boolean isCharging() {
        return this.charging;
    }

    @Override
    public boolean isDischarging() {
        return this.discharging;
    }

    @Override
    public CapacityUnits getCapacityUnits() {
        return this.capacityUnits;
    }

    @Override
    public int getCurrentCapacity() {
        return this.currentCapacity;
    }

    @Override
    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    @Override
    public int getDesignCapacity() {
        return this.designCapacity;
    }

    @Override
    public int getCycleCount() {
        return this.cycleCount;
    }

    @Override
    public String getChemistry() {
        return this.chemistry;
    }

    @Override
    public LocalDate getManufactureDate() {
        return this.manufactureDate;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public double getTemperature() {
        return this.temperature;
    }

    @Override
    public boolean updateAttributes() {
        PowerSource[] psArr = getPowerSources();
        for (PowerSource ps : psArr) {
            if (ps.getName().equals(this.name)) {
                this.name = ps.getName();
                this.deviceName = ps.getDeviceName();
                this.remainingCapacityPercent = ps.getRemainingCapacityPercent();
                this.timeRemainingEstimated = ps.getTimeRemainingEstimated();
                this.timeRemainingInstant = ps.getTimeRemainingInstant();
                this.powerUsageRate = ps.getPowerUsageRate();
                this.voltage = ps.getVoltage();
                this.amperage = ps.getAmperage();
                this.powerOnLine = ps.isPowerOnLine();
                this.charging = ps.isCharging();
                this.discharging = ps.isDischarging();
                this.capacityUnits = ps.getCapacityUnits();
                this.currentCapacity = ps.getCurrentCapacity();
                this.maxCapacity = ps.getMaxCapacity();
                this.designCapacity = ps.getDesignCapacity();
                this.cycleCount = ps.getCycleCount();
                this.chemistry = ps.getChemistry();
                this.manufactureDate = ps.getManufactureDate();
                this.manufacturer = ps.getManufacturer();
                this.serialNumber = ps.getSerialNumber();
                this.temperature = ps.getTemperature();
                return true;
            }
        }
        // Didn't find this battery
        return false;
    }

    /**
     * Gets Battery Information.
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        String psName = Constants.UNKNOWN;
        String psDeviceName = Constants.UNKNOWN;
        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
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

        // Mac PowerSource information comes from two sources: the IOKit's IOPS
        // functions (which, in theory, return an array of objects but in most cases
        // should return one), and the IORegistry's entry for AppleSmartBattery, which
        // always returns one object.
        //
        // We start by fetching the registry information, which will be replicated
        // across all IOPS entries if there are more than one.

        IORegistryEntry smartBattery = IOKitUtil.getMatchingService("AppleSmartBattery");
        if (smartBattery != null) {
            psDeviceName = smartBattery.getStringProperty("DeviceName");
            psManufacturer = smartBattery.getStringProperty("Manufacturer");
            psSerialNumber = smartBattery.getStringProperty("BatterySerialNumber");

            int manufactureDate = smartBattery.getIntegerProperty("ManufactureDate");
            // Bits 0...4 => day (value 1-31; 5 bits)
            // Bits 5...8 => month (value 1-12; 4 bits)
            // Bits 9...15 => years since 1980 (value 0-127; 7 bits)
            int day = manufactureDate & 0x1f;
            int month = (manufactureDate >> 5) & 0xf;
            int year80 = (manufactureDate >> 9) & 0x7f;
            psManufactureDate = LocalDate.of(1980 + year80, month, day);

            psDesignCapacity = smartBattery.getIntegerProperty("DesignCapacity");
            psMaxCapacity = smartBattery.getIntegerProperty("MaxCapacity");
            psCurrentCapacity = smartBattery.getIntegerProperty("CurrentCapacity");
            psCapacityUnits = CapacityUnits.MAH;

            psTimeRemainingInstant = smartBattery.getIntegerProperty("TimeRemaining") * 60d;
            psCycleCount = smartBattery.getIntegerProperty("CycleCount");
            psTemperature = smartBattery.getIntegerProperty("Temperature") / 100d;

            psVoltage = smartBattery.getIntegerProperty("Voltage") / 1000d;
            psAmperage = smartBattery.getIntegerProperty("Amperage");
            psPowerUsageRate = psVoltage * psAmperage;

            psPowerOnLine = smartBattery.getBooleanProperty("ExternalConnected");
            psCharging = smartBattery.getBooleanProperty("IsCharging");
            psDischarging = !psCharging;

            smartBattery.release();
        }

        // Get the blob containing current power source state
        CFTypeRef powerSourcesInfo = IO.IOPSCopyPowerSourcesInfo();
        CFArrayRef powerSourcesList = IO.IOPSCopyPowerSourcesList(powerSourcesInfo);
        int powerSourcesCount = powerSourcesList.getCount();

        // Get time remaining
        // -1 = unknown, -2 = unlimited
        psTimeRemainingEstimated = IO.IOPSGetTimeRemainingEstimate();

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
                    psName = cfName.stringValue();
                    if (psName == null) {
                        psName = Constants.UNKNOWN;
                    }
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
                    psRemainingCapacityPercent = Math.min(1d, currentCapacity / maxCapacity);
                    // Add to list
                    psList.add(new MacPowerSource(psName, psDeviceName, psRemainingCapacityPercent,
                            psTimeRemainingEstimated, psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage,
                            psPowerOnLine, psCharging, psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity,
                            psDesignCapacity, psCycleCount, psChemistry, psManufactureDate, psManufacturer,
                            psSerialNumber, psTemperature));
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

    public static void main(String[] args) {
        System.out.println(getPowerSources()[0].toString());
    }

}
