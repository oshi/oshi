/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperatureFFM;
import oshi.driver.windows.wmi.OhmHardwareFFM;
import oshi.driver.windows.wmi.OhmSensorFFM;
import oshi.driver.windows.wmi.Win32FanFFM;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.ffm.windows.com.FfmComException;
import oshi.hardware.common.AbstractSensors;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;
import oshi.util.platform.windows.WmiUtilFFM;

/**
 * Sensors from WMI or Open Hardware Monitor or Libre Hardware Monitor using FFM.
 */
@ThreadSafe
final class WindowsSensorsFFM extends AbstractSensors {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensorsFFM.class);

    private static final String COM_EXCEPTION_MSG = "COM exception: {}";

    private static final String REFLECT_EXCEPTION_MSG = "Reflect exception: {}";

    private static final String JLIBREHARDWAREMONITOR_PACKAGE = "io.github.pandalxb.jlibrehardwaremonitor";

    @Override
    public double queryCpuTemperature() {
        double tempC = getTempFromOHM();
        if (tempC > 0d) {
            return tempC;
        }
        tempC = getTempFromLHM();
        if (tempC > 0d) {
            return tempC;
        }
        tempC = getTempFromWMI();
        return tempC;
    }

    private static double getTempFromOHM() {
        WmiResult<ValueProperty> ohmSensors = getOhmSensors("Hardware", "CPU", "Temperature", (h, ohmHardware) -> {
            String cpuIdentifier = WmiUtilFFM.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
            if (!cpuIdentifier.isEmpty()) {
                return OhmSensorFFM.querySensorValue(h, cpuIdentifier, "Temperature");
            }
            return null;
        });
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            double sum = 0;
            for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                sum += WmiUtilFFM.getFloat(ohmSensors, ValueProperty.VALUE, i);
            }
            return sum / ohmSensors.getResultCount();
        }
        return 0;
    }

    private static double getTempFromLHM() {
        return getAverageValueFromLHM("CPU", "Temperature",
                (name, value) -> !name.contains("Max") && !name.contains("Average") && value > 0);
    }

    private static double getTempFromWMI() {
        double tempC = 0d;
        long tempK = 0L;
        WmiResult<TemperatureProperty> result = MSAcpiThermalZoneTemperatureFFM.queryCurrentTemperature();
        if (result.getResultCount() > 0) {
            LOG.debug("Found Temperature data in WMI");
            tempK = WmiUtilFFM.getUint32asLong(result, TemperatureProperty.CURRENTTEMPERATURE, 0);
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
        int[] fanSpeeds = getFansFromOHM();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        fanSpeeds = getFansFromLHM();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        fanSpeeds = getFansFromWMI();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        return new int[0];
    }

    private static int[] getFansFromOHM() {
        WmiResult<ValueProperty> ohmSensors = getOhmSensors("Hardware", "CPU", "Fan", (h, ohmHardware) -> {
            String cpuIdentifier = WmiUtilFFM.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
            if (!cpuIdentifier.isEmpty()) {
                return OhmSensorFFM.querySensorValue(h, cpuIdentifier, "Fan");
            }
            return null;
        });
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            int[] fanSpeeds = new int[ohmSensors.getResultCount()];
            for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                fanSpeeds[i] = (int) WmiUtilFFM.getFloat(ohmSensors, ValueProperty.VALUE, i);
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

    private static int[] getFansFromWMI() {
        WmiResult<SpeedProperty> fan = Win32FanFFM.querySpeed();
        if (fan.getResultCount() > 1) {
            LOG.debug("Found Fan data in WMI");
            int[] fanSpeeds = new int[fan.getResultCount()];
            for (int i = 0; i < fan.getResultCount(); i++) {
                fanSpeeds[i] = (int) WmiUtilFFM.getUint64(fan, SpeedProperty.DESIREDSPEED, i);
            }
            return fanSpeeds;
        }
        return new int[0];
    }

    @Override
    public double queryCpuVoltage() {
        double volts = getVoltsFromOHM();
        if (volts > 0d) {
            return volts;
        }
        volts = getVoltsFromLHM();
        if (volts > 0d) {
            return volts;
        }
        volts = getVoltsFromWMI();
        return volts;
    }

    private static double getVoltsFromOHM() {
        WmiResult<ValueProperty> ohmSensors = getOhmSensors("Sensor", "Voltage", "Voltage", (h, ohmHardware) -> {
            String cpuIdentifier = null;
            for (int i = 0; i < ohmHardware.getResultCount(); i++) {
                String id = WmiUtilFFM.getString(ohmHardware, IdentifierProperty.IDENTIFIER, i);
                if (id.toLowerCase(Locale.ROOT).contains("cpu")) {
                    cpuIdentifier = id;
                    break;
                }
            }
            if (cpuIdentifier == null) {
                cpuIdentifier = WmiUtilFFM.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
            }
            return OhmSensorFFM.querySensorValue(h, cpuIdentifier, "Voltage");
        });
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            return WmiUtilFFM.getFloat(ohmSensors, ValueProperty.VALUE, 0);
        }
        return 0d;
    }

    private static double getVoltsFromLHM() {
        return getAverageValueFromLHM("SuperIO", "Voltage",
                (name, value) -> name.toLowerCase(Locale.ROOT).contains("vcore") && value > 0);
    }

    private static double getVoltsFromWMI() {
        WmiResult<VoltProperty> voltage = Win32ProcessorFFM.queryVoltage();
        if (voltage.getResultCount() > 1) {
            LOG.debug("Found Voltage data in WMI");
            int decivolts = WmiUtilFFM.getUint16(voltage, VoltProperty.CURRENTVOLTAGE, 0);
            if (decivolts > 0) {
                if ((decivolts & 0x80) == 0) {
                    decivolts = WmiUtilFFM.getUint32(voltage, VoltProperty.VOLTAGECAPS, 0);
                    if ((decivolts & 0x1) > 0) {
                        return 5.0;
                    } else if ((decivolts & 0x2) > 0) {
                        return 3.3;
                    } else if ((decivolts & 0x4) > 0) {
                        return 2.9;
                    }
                } else {
                    return (decivolts & 0x7F) / 10d;
                }
            }
        }
        return 0d;
    }

    private static WmiResult<ValueProperty> getOhmSensors(String typeToQuery, String typeName, String sensorType,
            BiFunction<WmiQueryHandlerFFM, WmiResult<IdentifierProperty>, WmiResult<ValueProperty>> querySensorFunction) {
        WmiQueryHandlerFFM h = Objects.requireNonNull(WmiQueryHandlerFFM.createInstance());
        boolean comInit = false;
        WmiResult<ValueProperty> ohmSensors = null;
        try {
            comInit = h.initCOM();
            WmiResult<IdentifierProperty> ohmHardware = OhmHardwareFFM.queryHwIdentifier(h, typeToQuery, typeName);
            if (ohmHardware.getResultCount() > 0) {
                LOG.debug("Found {} data in Open Hardware Monitor", sensorType);
                ohmSensors = querySensorFunction.apply(h, ohmHardware);
            }
        } catch (FfmComException e) {
            LOG.warn(COM_EXCEPTION_MSG, e.getMessage());
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
        return ohmSensors;
    }

    private static double getAverageValueFromLHM(String hardwareType, String sensorType,
            BiFunction<String, Double, Boolean> sensorValidFunction) {
        List<?> sensors = getLhmSensors(hardwareType, sensorType);
        if (sensors == null || sensors.isEmpty()) {
            return 0;
        }
        try {
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
}
