/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.hardware.common.platform.mac.IOKitProvider.RegistryEntry;
import oshi.util.Constants;

/**
 * Abstract base for the macOS PowerSource. The {@code AppleSmartBattery} registry read and the per-power-source field
 * computation are shared; the JNA and FFM subclasses supply an {@link IOKitProvider} and read the IOKit Power Sources
 * (IOPS) list into {@link PowerSourceData} carriers via {@link #buildPowerSources}.
 */
@ThreadSafe
public abstract class MacPowerSource extends AbstractPowerSource {

    /**
     * Creates a MacPowerSource.
     *
     * @param psName                     power source name
     * @param psDeviceName               device name
     * @param psRemainingCapacityPercent remaining capacity percentage
     * @param psTimeRemainingEstimated   estimated time remaining
     * @param psTimeRemainingInstant     instant time remaining
     * @param psPowerUsageRate           power usage rate
     * @param psVoltage                  voltage
     * @param psAmperage                 amperage
     * @param psPowerOnLine              whether on AC power
     * @param psCharging                 whether charging
     * @param psDischarging              whether discharging
     * @param psCapacityUnits            capacity units
     * @param psCurrentCapacity          current capacity
     * @param psMaxCapacity              max capacity
     * @param psDesignCapacity           design capacity
     * @param psCycleCount               cycle count
     * @param psChemistry                chemistry
     * @param psManufactureDate          manufacture date
     * @param psManufacturer             manufacturer
     * @param psSerialNumber             serial number
     * @param psTemperature              temperature
     */
    public MacPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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

    /** Battery-wide fields read from the {@code AppleSmartBattery} registry entry, shared by every power source. */
    private static final class BatteryReadings {
        private String deviceName = Constants.UNKNOWN;
        private String manufacturer = Constants.UNKNOWN;
        private String serialNumber = Constants.UNKNOWN;
        private LocalDate manufactureDate;
        private CapacityUnits capacityUnits = CapacityUnits.RELATIVE;
        private int designCapacity = -1;
        private int maxCapacity = 1;
        private int currentCapacity;
        private int cycleCount = -1;
        private double timeRemainingInstant;
        private double temperature;
        private double voltage = -1d;
        private double amperage;
        private double powerUsageRate;
        private boolean powerOnLine;
        private boolean charging;
        private boolean discharging;
    }

    /** Raw values read from a single IOKit Power Source (IOPS) dictionary by the JNA/FFM subclasses. */
    public static final class PowerSourceData {
        private final String name;
        private final double currentCapacity;
        private final double maxCapacity;

        /**
         * Creates a single power-source reading.
         *
         * @param name            the power source name
         * @param currentCapacity the current capacity
         * @param maxCapacity     the maximum capacity
         */
        public PowerSourceData(String name, double currentCapacity, double maxCapacity) {
            this.name = name;
            this.currentCapacity = currentCapacity;
            this.maxCapacity = maxCapacity;
        }
    }

    /** Creates a concrete (JNA or FFM) Mac power source from the computed battery fields. */
    @FunctionalInterface
    protected interface PowerSourceFactory {
        MacPowerSource create(String psName, String psDeviceName, double psRemainingCapacityPercent,
                double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate,
                double psVoltage, double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
                CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
                int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
                String psSerialNumber, double psTemperature);
    }

    /**
     * Reads the {@code AppleSmartBattery} registry entry (via {@code ioKit}) and merges it with the per-source IOPS
     * readings into a list of power sources.
     * <p>
     * Mac PowerSource information comes from two sources: the IORegistry's {@code AppleSmartBattery} entry
     * (battery-wide fields, read here through the shared {@link IOKitProvider}) and the IOKit's IOPS functions (one
     * entry per power source, read natively by the caller into {@code sources}).
     *
     * @param ioKit                  the IOKit provider backing the registry read
     * @param timeRemainingEstimated the estimated time remaining (-1 unknown, -2 unlimited) from IOPS
     * @param sources                the present power sources read from the IOPS list
     * @param factory                creates the platform-specific instance
     * @return a list of power sources, one per element of {@code sources}
     */
    protected static List<PowerSource> buildPowerSources(IOKitProvider ioKit, double timeRemainingEstimated,
            List<PowerSourceData> sources, PowerSourceFactory factory) {
        BatteryReadings b = ioKit.withMatchingService("AppleSmartBattery", MacPowerSource::readBattery);
        if (b == null) {
            b = new BatteryReadings();
        }
        List<PowerSource> psList = new ArrayList<>(sources.size());
        for (PowerSourceData src : sources) {
            double psRemainingCapacityPercent = src.maxCapacity <= 0 ? 0d
                    : Math.min(1d, src.currentCapacity / src.maxCapacity);
            psList.add(factory.create(src.name, b.deviceName, psRemainingCapacityPercent, timeRemainingEstimated,
                    b.timeRemainingInstant, b.powerUsageRate, b.voltage, b.amperage, b.powerOnLine, b.charging,
                    b.discharging, b.capacityUnits, b.currentCapacity, b.maxCapacity, b.designCapacity, b.cycleCount,
                    Constants.UNKNOWN, b.manufactureDate, b.manufacturer, b.serialNumber, b.temperature));
        }
        return psList;
    }

    private static BatteryReadings readBattery(RegistryEntry smartBattery) {
        BatteryReadings b = new BatteryReadings();
        String s = smartBattery.getStringProperty("DeviceName");
        if (s != null) {
            b.deviceName = s;
        }
        s = smartBattery.getStringProperty("Manufacturer");
        if (s != null) {
            b.manufacturer = s;
        }
        s = smartBattery.getStringProperty("BatterySerialNumber");
        if (s != null) {
            b.serialNumber = s;
        }

        Integer temp = smartBattery.getIntegerProperty("ManufactureDate");
        if (temp != null) {
            // Bits 0...4 => day (value 1-31; 5 bits)
            // Bits 5...8 => month (value 1-12; 4 bits)
            // Bits 9...15 => years since 1980 (value 0-127; 7 bits)
            int day = temp & 0x1f;
            int month = (temp >> 5) & 0xf;
            int year80 = (temp >> 9) & 0x7f;
            try {
                b.manufactureDate = LocalDate.of(1980 + year80, month, day);
            } catch (DateTimeException e) {
                // Corrupt bitfield — leave manufactureDate as null
            }
        }

        temp = smartBattery.getIntegerProperty("DesignCapacity");
        if (temp != null) {
            b.designCapacity = temp;
        }
        temp = smartBattery.getIntegerProperty("MaxCapacity");
        if (temp != null) {
            b.maxCapacity = temp;
        }
        temp = smartBattery.getIntegerProperty("CurrentCapacity");
        if (temp != null) {
            b.currentCapacity = temp;
        }
        b.capacityUnits = CapacityUnits.MAH;

        temp = smartBattery.getIntegerProperty("TimeRemaining");
        if (temp != null) {
            b.timeRemainingInstant = temp * 60d;
        }
        temp = smartBattery.getIntegerProperty("CycleCount");
        if (temp != null) {
            b.cycleCount = temp;
        }
        temp = smartBattery.getIntegerProperty("Temperature");
        if (temp != null) {
            b.temperature = temp / 100d;
        }
        temp = smartBattery.getIntegerProperty("Voltage");
        if (temp != null) {
            b.voltage = temp / 1000d;
        }
        temp = smartBattery.getIntegerProperty("Amperage");
        if (temp != null) {
            b.amperage = temp;
        }
        b.powerUsageRate = b.voltage * b.amperage;

        Boolean bool = smartBattery.getBooleanProperty("ExternalConnected");
        if (bool != null) {
            b.powerOnLine = bool;
        }
        bool = smartBattery.getBooleanProperty("IsCharging");
        if (bool != null) {
            b.charging = bool;
        }
        b.discharging = !b.charging && !b.powerOnLine;

        return b;
    }
}
