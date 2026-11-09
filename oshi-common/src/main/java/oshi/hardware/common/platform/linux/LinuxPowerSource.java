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

import org.jspecify.annotations.Nullable;

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
        /** Time to empty in seconds, averaged by the battery. */
        POWER_SUPPLY_TIME_TO_EMPTY_AVG,
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
            int psCycleCount, String psChemistry, @Nullable LocalDate psManufactureDate, String psManufacturer,
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
        boolean externalOnLine = queryExternalPowerOnLine(powerSupplyPath);
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
                    psList.add(buildPowerSource(name, props, externalOnLine));
                }
            }
        }
        return Collections.unmodifiableList(psList);
    }

    /**
     * Reports whether an external supply (a mains adapter, a USB port or a wireless charger) is delivering power.
     *
     * @return whether any external supply is online
     */
    protected static boolean queryExternalPowerOnLine() {
        return queryExternalPowerOnLine(SysPath.POWER_SUPPLY);
    }

    /**
     * Reports whether an external supply under the given power supply directory is delivering power. The kernel reports
     * one entry per supply, with a {@code type} of {@code Battery}, {@code UPS}, {@code Mains}, {@code USB} or
     * {@code Wireless}, and the external ones an {@code online} flag.
     *
     * @param powerSupplyPath the power supply directory
     * @return whether any external supply is online
     */
    static boolean queryExternalPowerOnLine(String powerSupplyPath) {
        File[] supplies = new File(powerSupplyPath).listFiles();
        if (supplies != null) {
            for (File supply : supplies) {
                String path = powerSupplyPath + "/" + supply.getName() + "/";
                String type = FileUtil.getStringFromFile(path + "type");
                if (("Mains".equals(type) || "USB".equals(type) || "Wireless".equals(type))
                        && FileUtil.getIntFromFile(path + "online") > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Builds a LinuxPowerSource from parsed uevent properties.
     *
     * @param name           the power supply name
     * @param props          the parsed properties map
     * @param externalOnLine whether an external supply is online, from {@link #queryExternalPowerOnLine()}
     * @return a new LinuxPowerSource instance
     */
    protected static LinuxPowerSource buildPowerSource(String name, Map<Prop, String> props, boolean externalOnLine) {
        String psName = props.getOrDefault(Prop.POWER_SUPPLY_NAME, name);
        String status = props.get(Prop.POWER_SUPPLY_STATUS);
        boolean psCharging = "Charging".equals(status);
        boolean psDischarging = "Discharging".equals(status);
        // A battery that is charging, or held from charging at a charge limit, is plugged in. This covers boards
        // whose adapter has no power_supply entry of its own.
        boolean psPowerOnLine = externalOnLine || psCharging || "Not charging".equals(status);
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

        // TIME_TO_EMPTY_NOW when discharging, TIME_TO_FULL_NOW when charging; both in seconds. Few drivers provide
        // them (the ACPI battery driver does not), so otherwise divide the energy or charge left to go by the rate.
        Prop timeKey = psCharging ? Prop.POWER_SUPPLY_TIME_TO_FULL_NOW : Prop.POWER_SUPPLY_TIME_TO_EMPTY_NOW;
        double psTimeRemainingInstant = ParseUtil.parseIntOrDefault(props.get(timeKey), -1);
        if (psTimeRemainingInstant < 0 && (psCharging || psDischarging)) {
            psTimeRemainingInstant = timeFromRate(psCharging, psCapacityUnits, psCurrentCapacity, psMaxCapacity,
                    psPowerUsageRate, psAmperage);
        }
        // The estimate follows the other platforms: unlimited on external power, and while discharging the battery's
        // own average where it keeps one
        double psTimeRemainingEstimated = -1d;
        if (psDischarging) {
            psTimeRemainingEstimated = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_TIME_TO_EMPTY_AVG), -1);
            if (psTimeRemainingEstimated < 0) {
                psTimeRemainingEstimated = psTimeRemainingInstant;
            }
        } else if (psPowerOnLine) {
            psTimeRemainingEstimated = -2d;
        }

        int year = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_MANUFACTURE_YEAR), -1);
        int month = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_MANUFACTURE_MONTH), -1);
        int day = ParseUtil.parseIntOrDefault(props.get(Prop.POWER_SUPPLY_MANUFACTURE_DAY), -1);
        @Nullable
        LocalDate psManufactureDate = (year > 0 && month > 0 && day > 0) ? LocalDate.of(year, month, day) : null;

        return new LinuxPowerSource(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }

    /**
     * Computes the seconds to empty, or to full while charging, from the capacity and the rate it is changing at.
     *
     * @param charging        whether the battery is charging
     * @param units           the units of the capacities, mWh or mAh
     * @param currentCapacity the current capacity, or -1 if unknown
     * @param maxCapacity     the full capacity, or -1 if unknown
     * @param powerUsageRate  the power in mW, signed
     * @param amperage        the current in mA, signed
     * @return the seconds remaining, or -1 if unknown
     */
    private static double timeFromRate(boolean charging, CapacityUnits units, int currentCapacity, int maxCapacity,
            double powerUsageRate, double amperage) {
        // Energy pairs with power and charge with current, so the hours come out of either ratio
        double rate = Math.abs(units == CapacityUnits.MWH ? powerUsageRate : amperage);
        int toGo = charging ? maxCapacity - currentCapacity : currentCapacity;
        if (rate <= 0 || currentCapacity < 0 || (charging && maxCapacity < 0) || toGo < 0) {
            return -1d;
        }
        return 3600d * toGo / rate;
    }
}
