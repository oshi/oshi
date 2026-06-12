/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.common.AbstractSensors;

/**
 * Abstract base shared by the Windows Sensors implementations (JNA and FFM). Holds the Open Hardware Monitor / Libre
 * Hardware Monitor / WMI fallback orchestration, the reflection-based Libre Hardware Monitor queries (which are
 * backend-agnostic), and the WMI/OHM result processing. The native driver queries are provided by the subclasses.
 */
@ThreadSafe
public abstract class WindowsSensors extends AbstractSensors {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    protected static final String COM_EXCEPTION_MSG = "COM exception: {}";

    private static final String REFLECT_EXCEPTION_MSG = "Reflect exception: {}";

    private static final String JLIBREHARDWAREMONITOR_PACKAGE = "io.github.pandalxb.jlibrehardwaremonitor";

    @Override
    public double queryCpuTemperature() {
        // Attempt to fetch value from Open Hardware Monitor if it is running, as it will give the most accurate results
        // and the time to query (or attempt) is trivial
        double tempC = getTempFromOHM();
        if (tempC > 0d) {
            return tempC;
        }
        // Fetch value from LibreHardwareMonitorLib.dll / OpenHardwareMonitorLib.dll without applications running
        tempC = getTempFromLHM();
        if (tempC > 0d) {
            return tempC;
        }
        // If we get this far, OHM is not running. Try from WMI. Other fallbacks to WMI are unreliable so we omit them.
        return getTempFromWMI();
    }

    private double getTempFromOHM() {
        WmiResult<ValueProperty> ohmSensors = queryOhmCpuSensor("Hardware", "CPU", "Temperature", false);
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            double sum = 0;
            for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                sum += WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, i);
            }
            return sum / ohmSensors.getResultCount();
        }
        return 0;
    }

    private static double getTempFromLHM() {
        return getAverageValueFromLHM("CPU", "Temperature",
                (name, value) -> !name.contains("Max") && !name.contains("Average") && value > 0);
    }

    private double getTempFromWMI() {
        double tempC = 0d;
        long tempK = 0L;
        WmiResult<TemperatureProperty> result = queryWmiTemperature();
        if (result.getResultCount() > 0) {
            LOG.debug("Found Temperature data in WMI");
            tempK = WmiUtil.getUint32asLong(result, TemperatureProperty.CURRENTTEMPERATURE, 0);
        }
        if (tempK > 2732L) {
            tempC = tempK / 10d - 273.15;
        } else if (tempK > 274L) {
            tempC = tempK - 273d;
        }
        return Math.max(tempC, +0.0);
    }

    @Override
    public int[] queryFanSpeeds() {
        // Attempt to fetch value from Open Hardware Monitor if it is running
        int[] fanSpeeds = getFansFromOHM();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        // Fetch value from LibreHardwareMonitorLib.dll / OpenHardwareMonitorLib.dll without applications running
        fanSpeeds = getFansFromLHM();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        // If we get this far, OHM is not running. Try to get from conventional WMI
        fanSpeeds = getFansFromWMI();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        // Default
        return new int[0];
    }

    private int[] getFansFromOHM() {
        WmiResult<ValueProperty> ohmSensors = queryOhmCpuSensor("Hardware", "CPU", "Fan", false);
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            int[] fanSpeeds = new int[ohmSensors.getResultCount()];
            for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                fanSpeeds[i] = (int) WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, i);
            }
            return fanSpeeds;
        }
        return new int[0];
    }

    private static int[] getFansFromLHM() {
        List<?> sensors = getLhmSensors("SuperIO", "Fan");
        if (sensors == null || sensors.isEmpty()) {
            return new int[0];
        }

        try {
            // The sensor object is confirmed to contain the getValue method.
            Class<?> sensorClass = Class.forName(JLIBREHARDWAREMONITOR_PACKAGE + ".model.Sensor");
            Method getValueMethod = sensorClass.getMethod("getValue");

            return sensors.stream().filter(sensor -> {
                try {
                    double value = (double) getValueMethod.invoke(sensor);
                    return value > 0;
                } catch (Exception e) {
                    LOG.warn(REFLECT_EXCEPTION_MSG, e.getMessage());
                    return false;
                }
            }).mapToInt(sensor -> {
                try {
                    return (int) (double) getValueMethod.invoke(sensor);
                } catch (Exception e) {
                    LOG.warn(REFLECT_EXCEPTION_MSG, e.getMessage());
                    return 0;
                }
            }).toArray();
        } catch (Exception e) {
            LOG.warn(REFLECT_EXCEPTION_MSG, e.getMessage());
        }
        return new int[0];
    }

    private int[] getFansFromWMI() {
        WmiResult<SpeedProperty> fan = queryWmiFanSpeed();
        if (fan.getResultCount() > 1) {
            LOG.debug("Found Fan data in WMI");
            int[] fanSpeeds = new int[fan.getResultCount()];
            for (int i = 0; i < fan.getResultCount(); i++) {
                fanSpeeds[i] = (int) WmiUtil.getUint64(fan, SpeedProperty.DESIREDSPEED, i);
            }
            return fanSpeeds;
        }
        return new int[0];
    }

    @Override
    public double queryCpuVoltage() {
        // Attempt to fetch value from Open Hardware Monitor if it is running
        double volts = getVoltsFromOHM();
        if (volts > 0d) {
            return volts;
        }
        // Fetch value from LibreHardwareMonitorLib.dll / OpenHardwareMonitorLib.dll without applications running
        volts = getVoltsFromLHM();
        if (volts > 0d) {
            return volts;
        }
        // If we get this far, OHM is not running. Try to get from conventional WMI
        return getVoltsFromWMI();
    }

    private double getVoltsFromOHM() {
        WmiResult<ValueProperty> ohmSensors = queryOhmCpuSensor("Sensor", "Voltage", "Voltage", true);
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            return WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, 0);
        }
        return 0d;
    }

    private static double getVoltsFromLHM() {
        return getAverageValueFromLHM("SuperIO", "Voltage",
                (name, value) -> name.toLowerCase(Locale.ROOT).contains("vcore") && value > 0);
    }

    private double getVoltsFromWMI() {
        WmiResult<VoltProperty> voltage = queryWmiVoltage();
        if (voltage.getResultCount() > 1) {
            LOG.debug("Found Voltage data in WMI");
            int decivolts = WmiUtil.getUint16(voltage, VoltProperty.CURRENTVOLTAGE, 0);
            // If the eighth bit is set, bits 0-6 contain the voltage multiplied by 10. If the eighth bit is not set,
            // then the bit setting in VoltageCaps represents the voltage value.
            if (decivolts > 0) {
                if ((decivolts & 0x80) == 0) {
                    decivolts = WmiUtil.getUint32(voltage, VoltProperty.VOLTAGECAPS, 0);
                    // This value is really a bit setting, not decivolts
                    if ((decivolts & 0x1) > 0) {
                        return 5.0;
                    } else if ((decivolts & 0x2) > 0) {
                        return 3.3;
                    } else if ((decivolts & 0x4) > 0) {
                        return 2.9;
                    }
                } else {
                    // Value from bits 0-6, divided by 10
                    return (decivolts & 0x7F) / 10d;
                }
            }
        }
        return 0d;
    }

    /**
     * Selects the CPU hardware identifier from an Open Hardware Monitor identifier result. When {@code searchCpu} is
     * set, the first identifier containing {@code cpu} is used; otherwise (or if none match) the first identifier is
     * returned.
     *
     * @param ohmHardware the OHM hardware identifier result
     * @param searchCpu   whether to search for an identifier containing {@code cpu}
     * @return the selected identifier (possibly empty)
     */
    protected static String selectOhmCpuIdentifier(WmiResult<IdentifierProperty> ohmHardware, boolean searchCpu) {
        if (searchCpu) {
            for (int i = 0; i < ohmHardware.getResultCount(); i++) {
                String id = WmiUtil.getString(ohmHardware, IdentifierProperty.IDENTIFIER, i);
                if (id.toLowerCase(Locale.ROOT).contains("cpu")) {
                    return id;
                }
            }
        }
        return WmiUtil.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
    }

    private static double getAverageValueFromLHM(String hardwareType, String sensorType,
            BiFunction<String, Double, Boolean> sensorValidFunction) {
        List<?> sensors = getLhmSensors(hardwareType, sensorType);
        if (sensors == null || sensors.isEmpty()) {
            return 0;
        }

        try {
            // The sensor object is confirmed to contain the getName and getValue methods.
            Class<?> sensorClass = Class.forName(JLIBREHARDWAREMONITOR_PACKAGE + ".model.Sensor");
            Method getNameMethod = sensorClass.getMethod("getName");
            Method getValueMethod = sensorClass.getMethod("getValue");

            double sum = 0;
            int validCount = 0;
            for (Object sensor : sensors) {
                String name = (String) getNameMethod.invoke(sensor);
                double value = (double) getValueMethod.invoke(sensor);
                if (sensorValidFunction.apply(name, value)) {
                    sum += value;
                    validCount++;
                }
            }
            return validCount > 0 ? sum / validCount : 0;
        } catch (Exception e) {
            LOG.warn(REFLECT_EXCEPTION_MSG, e.getMessage());
        }
        return 0;
    }

    private static List<?> getLhmSensors(String hardwareType, String sensorType) {
        try {
            Class<?> computerConfigClass = Class.forName(JLIBREHARDWAREMONITOR_PACKAGE + ".config.ComputerConfig");
            Class<?> libreHardwareManagerClass = Class
                    .forName(JLIBREHARDWAREMONITOR_PACKAGE + ".manager.LibreHardwareManager");

            Method computerConfigGetInstanceMethod = computerConfigClass.getMethod("getInstance");
            Object computerConfigInstance = computerConfigGetInstanceMethod.invoke(null);

            Method setEnabledMethod = computerConfigClass.getMethod("setCpuEnabled", boolean.class);
            setEnabledMethod.invoke(computerConfigInstance, true);
            setEnabledMethod = computerConfigClass.getMethod("setMotherboardEnabled", boolean.class);
            setEnabledMethod.invoke(computerConfigInstance, true);

            Method libreHardwareManagerGetInstanceMethod = libreHardwareManagerClass.getMethod("getInstance",
                    computerConfigClass);

            Object instance = libreHardwareManagerGetInstanceMethod.invoke(null, computerConfigInstance);

            Method querySensorsMethod = libreHardwareManagerClass.getMethod("querySensors", String.class, String.class);
            return (List<?>) querySensorsMethod.invoke(instance, hardwareType, sensorType);
        } catch (ClassNotFoundException e) {
            LOG.trace("jLibreHardwareMonitor not available: {}", e.getMessage());
        } catch (Exception e) {
            LOG.warn(REFLECT_EXCEPTION_MSG, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Queries an Open Hardware Monitor CPU sensor via the backend-specific WMI handler.
     *
     * @param typeToQuery the OHM hardware type to query
     * @param typeName    the OHM hardware type name
     * @param sensorType  the sensor type
     * @param searchCpu   whether to search for a {@code cpu} identifier (vs. using the first)
     * @return the sensor values, or {@code null} if unavailable
     */
    protected abstract WmiResult<ValueProperty> queryOhmCpuSensor(String typeToQuery, String typeName,
            String sensorType, boolean searchCpu);

    /**
     * Queries the current temperature from WMI {@code MSAcpi_ThermalZoneTemperature}.
     *
     * @return the WMI temperature result
     */
    protected abstract WmiResult<TemperatureProperty> queryWmiTemperature();

    /**
     * Queries fan speeds from WMI {@code Win32_Fan}.
     *
     * @return the WMI fan speed result
     */
    protected abstract WmiResult<SpeedProperty> queryWmiFanSpeed();

    /**
     * Queries CPU voltage from WMI {@code Win32_Processor}.
     *
     * @return the WMI voltage result
     */
    protected abstract WmiResult<VoltProperty> queryWmiVoltage();
}
