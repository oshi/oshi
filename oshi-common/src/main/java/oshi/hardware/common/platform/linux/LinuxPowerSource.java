/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static oshi.util.Constants.UNKNOWN;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.SysPath;

/**
 * A Power Source
 */
@ThreadSafe
public class LinuxPowerSource extends AbstractPowerSource {

    /**
     * Power supply uevent properties from sysfs.
     */
    protected enum Prop {
        /** Power supply name. */
        POWER_SUPPLY_NAME,
        /** Power supply status (Charging, Discharging, etc.). */
        POWER_SUPPLY_STATUS,
        /** Remaining capacity percentage. */
        POWER_SUPPLY_CAPACITY,
        /** Whether the power supply is present. */
        POWER_SUPPLY_PRESENT,
        /** Whether the power supply is online. */
        POWER_SUPPLY_ONLINE,
        /** Current energy in microwatt-hours. */
        POWER_SUPPLY_ENERGY_NOW,
        /** Current charge in microamp-hours. */
        POWER_SUPPLY_CHARGE_NOW,
        /** Full energy capacity in microwatt-hours. */
        POWER_SUPPLY_ENERGY_FULL,
        /** Full charge capacity in microamp-hours. */
        POWER_SUPPLY_CHARGE_FULL,
        /** Design energy capacity in microwatt-hours. */
        POWER_SUPPLY_ENERGY_FULL_DESIGN,
        /** Design charge capacity in microamp-hours. */
        POWER_SUPPLY_CHARGE_FULL_DESIGN,
        /** Current voltage in microvolts. */
        POWER_SUPPLY_VOLTAGE_NOW,
        /** Current power draw in microwatts. */
        POWER_SUPPLY_POWER_NOW,
        /** Current draw in microamps. */
        POWER_SUPPLY_CURRENT_NOW,
        /** Battery cycle count. */
        POWER_SUPPLY_CYCLE_COUNT,
        /** Battery technology/chemistry. */
        POWER_SUPPLY_TECHNOLOGY,
        /** Model name. */
        POWER_SUPPLY_MODEL_NAME,
        /** Manufacturer name. */
        POWER_SUPPLY_MANUFACTURER,
        /** Serial number. */
        POWER_SUPPLY_SERIAL_NUMBER,
        /** Temperature in tenths of degrees Celsius. */
        POWER_SUPPLY_TEMP,
        /** Estimated time to empty in seconds. */
        POWER_SUPPLY_TIME_TO_EMPTY_NOW,
        /** Estimated time to full in seconds. */
        POWER_SUPPLY_TIME_TO_FULL_NOW,
        /** Manufacture year. */
        POWER_SUPPLY_MANUFACTURE_YEAR,
        /** Manufacture month. */
        POWER_SUPPLY_MANUFACTURE_MONTH,
        /** Manufacture day. */
        POWER_SUPPLY_MANUFACTURE_DAY
    }

    /**
     * Initialization-on-demand holder: the map is only constructed if the non-udev path is reached, avoiding
     * unnecessary work on systems where udev is available.
     */
    private static final class PropByName {
        static final Map<String, Prop> MAP = new HashMap<>();
        static {
            for (Prop p : Prop.values()) {
                MAP.put(p.name(), p);
            }
        }
    }

    /**
     * Creates a LinuxPowerSource with the given parameters.
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
    public LinuxPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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

    /**
     * Copy constructor.
     *
     * @param src the source to copy from
     */
    protected LinuxPowerSource(LinuxPowerSource src) {
        this(src.getName(), src.getDeviceName(), src.getRemainingCapacityPercent(), src.getTimeRemainingEstimated(),
                src.getTimeRemainingInstant(), src.getPowerUsageRate(), src.getVoltage(), src.getAmperage(),
                src.isPowerOnLine(), src.isCharging(), src.isDischarging(), src.getCapacityUnits(),
                src.getCurrentCapacity(), src.getMaxCapacity(), src.getDesignCapacity(), src.getCycleCount(),
                src.getChemistry(), src.getManufactureDate(), src.getManufacturer(), src.getSerialNumber(),
                src.getTemperature());
    }

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
    }

    /**
     * Gets Battery Information
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        return getPowerSources(SysPath.POWER_SUPPLY);
    }

    /**
     * Gets Battery Information from the specified power supply path.
     *
     * @param powerSupplyPath The path to the power supply directory.
     * @return An array of PowerSource objects representing batteries, etc.
     */
    static List<PowerSource> getPowerSources(String powerSupplyPath) {
        List<PowerSource> psList = new ArrayList<>();
        File psDir = new File(powerSupplyPath);
        File[] psArr = psDir.listFiles();
        if (psArr == null) {
            return Collections.emptyList();
        }
        for (File ps : psArr) {
            String name = ps.getName();
            if (!name.startsWith("ADP") && !name.startsWith("AC") && !name.contains("USBC")) {
                List<String> psInfo = FileUtil.readFile(powerSupplyPath + "/" + name + "/uevent", false);
                Map<Prop, String> props = new EnumMap<>(Prop.class);
                for (String line : psInfo) {
                    String[] split = line.split("=", 2);
                    if (split.length > 1 && !split[1].isEmpty()) {
                        Prop p = PropByName.MAP.get(split[0]);
                        if (p != null) {
                            props.put(p, split[1]);
                        }
                    }
                }
                if (ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_PRESENT), 1) > 0) {
                    psList.add(buildPowerSource(name, props));
                }
            }
        }
        return psList;
    }

    /**
     * Builds a LinuxPowerSource from parsed uevent properties.
     *
     * @param name  the power supply name
     * @param props the parsed properties map
     * @return a new LinuxPowerSource instance
     */
    protected static LinuxPowerSource buildPowerSource(String name, Map<Prop, String> props) {
        String psName = props.getOrDefault(Prop.POWER_SUPPLY_NAME, name);
        String status = props.get(Prop.POWER_SUPPLY_STATUS);
        boolean psCharging = "Charging".equals(status);
        boolean psDischarging = "Discharging".equals(status);
        double psRemainingCapacityPercent = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_CAPACITY), -100)
                / 100d;

        // Debian/Ubuntu provides Energy (µWh). Fedora/RHEL provides Charge (µAh).
        // Parse raw values in micro-units first, fall back across Energy/Charge, then
        // divide by 1000 at the end to avoid integer division truncating the -1 sentinel.
        int rawNow = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_ENERGY_NOW), -1);
        CapacityUnits psCapacityUnits;
        if (rawNow >= 0) {
            psCapacityUnits = CapacityUnits.MWH;
        } else {
            rawNow = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_CHARGE_NOW), -1);
            psCapacityUnits = CapacityUnits.MAH;
        }
        int psCurrentCapacity = rawNow >= 0 ? rawNow / 1000 : -1;

        int rawFull = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_ENERGY_FULL), -1);
        if (rawFull < 0) {
            rawFull = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_CHARGE_FULL), -1);
        }
        int psMaxCapacity = rawFull >= 0 ? rawFull / 1000 : -1;

        int rawDesign = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_ENERGY_FULL_DESIGN), -1);
        if (rawDesign < 0) {
            rawDesign = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_CHARGE_FULL_DESIGN), -1);
        }
        int psDesignCapacity = rawDesign >= 0 ? rawDesign / 1000 : -1;

        // Debian/Ubuntu provides Voltage and Power. Fedora/RHEL provides Voltage and Current.
        double psVoltage = ParseUtil.parseDoubleOrDefault(props.get(Prop.POWER_SUPPLY_VOLTAGE_NOW), -1d) / 1_000_000d;
        double psPowerUsageRate = 0d;
        double psAmperage = 0d;
        // From Physics we know P = IV so I = P/V
        // Linux always reports POWER_NOW and CURRENT_NOW as non-negative; sign is
        // conveyed by STATUS. Negate when discharging to match the PowerSource contract
        // (positive = charging, negative = discharging).
        double sign = psDischarging ? -1d : 1d;
        if (psVoltage > 0) {
            double rawPower = ParseUtil.parseDoubleOrDefault(props.get(Prop.POWER_SUPPLY_POWER_NOW), -1d);
            double rawCurrent = ParseUtil.parseDoubleOrDefault(props.get(Prop.POWER_SUPPLY_CURRENT_NOW), -1d);
            if (rawPower >= 0) {
                psPowerUsageRate = sign * rawPower / 1000d;
            }
            if (rawCurrent >= 0) {
                psAmperage = sign * rawCurrent / 1000d;
            }
            if (rawPower < 0 && rawCurrent >= 0) {
                psPowerUsageRate = psAmperage * psVoltage;
            } else if (rawCurrent < 0 && rawPower >= 0) {
                psAmperage = psPowerUsageRate / psVoltage;
            }
        }

        int psCycleCount = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_CYCLE_COUNT), -1);
        String psChemistry = props.getOrDefault(Prop.POWER_SUPPLY_TECHNOLOGY, UNKNOWN);
        String psDeviceName = props.getOrDefault(Prop.POWER_SUPPLY_MODEL_NAME, UNKNOWN);
        String psManufacturer = props.getOrDefault(Prop.POWER_SUPPLY_MANUFACTURER, UNKNOWN);
        String psSerialNumber = props.getOrDefault(Prop.POWER_SUPPLY_SERIAL_NUMBER, UNKNOWN);

        // TEMP is in tenths of degrees Celsius
        int rawTemp = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_TEMP), -1);
        double psTemperature = rawTemp >= 0 ? rawTemp / 10d : 0d;

        // TIME_TO_EMPTY_NOW when discharging, TIME_TO_FULL_NOW when charging; both in seconds
        Prop timeKey = psCharging ? Prop.POWER_SUPPLY_TIME_TO_FULL_NOW : Prop.POWER_SUPPLY_TIME_TO_EMPTY_NOW;
        double psTimeRemainingInstant = ParseUtil.parseIntOrDefault(props.get(timeKey), -1);

        int year = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_MANUFACTURE_YEAR), -1);
        int month = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_MANUFACTURE_MONTH), -1);
        int day = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_MANUFACTURE_DAY), -1);
        LocalDate psManufactureDate = (year > 0 && month > 0 && day > 0) ? LocalDate.of(year, month, day) : null;

        return new LinuxPowerSource(psName, psDeviceName, psRemainingCapacityPercent, -1d, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, false, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
    }
}
