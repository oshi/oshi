/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.LhmMetrics;
import oshi.driver.common.windows.LhmMetrics.Reading;
import oshi.driver.common.windows.wmi.HardwareMonitorDisabled;
import oshi.driver.common.windows.wmi.LhmSensor;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmHardware;
import oshi.driver.common.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.common.AbstractSensors;
import oshi.util.GlobalConfig;

/**
 * Abstract base shared by the Windows Sensors implementations (JNA and FFM). Holds the orchestration of the sensor
 * sources, which are tried in order: the Open Hardware Monitor and Libre Hardware Monitor WMI namespaces, published by
 * either application while it runs (Libre Hardware Monitor only up to version 0.9.4); the web server of Libre Hardware
 * Monitor 0.9.5 and later, if {@link GlobalConfig#OSHI_OS_WINDOWS_LHM_URL} names one; and finally plain WMI. The source
 * order and result processing are backend-agnostic; the native driver queries are provided by the subclasses.
 */
@ThreadSafe
public abstract class WindowsSensors extends AbstractSensors {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    protected static final String COM_EXCEPTION_MSG = "COM exception";

    /** Open Hardware Monitor's {@code HardwareType} value for a processor. */
    private static final String OHM_CPU = "CPU";

    /** Libre Hardware Monitor's {@code HardwareType} value for a processor, which OHM spells {@code CPU}. */
    private static final String LHM_CPU = "Cpu";

    /**
     * Both monitors' {@code HardwareType} value for the motherboard's Super I/O chip, which carries the fan sensors; a
     * processor carries none.
     */
    private static final String SUPER_IO = "SuperIO";

    /** {@code SensorType} values. Both monitors publish the same names in WMI. */
    private static final String TEMPERATURE = "Temperature";
    private static final String FAN = "Fan";
    private static final String VOLTAGE = "Voltage";

    /** One request to the Libre Hardware Monitor web server serves all three sensor reads of a refresh. */
    private final Supplier<List<Reading>> lhmMetrics = memoize(this::queryLhmMetrics, defaultExpiration());

    /**
     * Queries the Libre Hardware Monitor web server named by {@link GlobalConfig#OSHI_OS_WINDOWS_LHM_URL}.
     *
     * @return the readings, empty if no server is configured or it could not be reached
     */
    protected List<Reading> queryLhmMetrics() {
        String url = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_LHM_URL, "");
        return url.trim().isEmpty() ? Collections.emptyList() : LhmMetrics.query(url);
    }

    /**
     * Averages the temperature readings of the first processor, using the same rule as the WMI namespaces.
     *
     * @param readings the web server's readings
     * @return the average temperature in degrees Celsius, or 0 if there is none
     */
    static double getCpuTemperature(List<Reading> readings) {
        String cpu = null;
        double sum = 0;
        int count = 0;
        for (Reading r : readings) {
            if (LhmMetrics.CPU_TEMPERATURE.equals(r.getMetric())) {
                if (cpu == null) {
                    cpu = r.getHardwareId();
                }
                if (cpu.equals(r.getHardwareId()) && isCpuTemperatureReading(r.getSensorName(), r.getValue())) {
                    sum += r.getValue();
                    count++;
                }
            }
        }
        return count > 0 ? sum / count : 0;
    }

    /**
     * Collects the motherboard's fan readings, from every Super I/O chip, as the WMI namespaces do.
     *
     * @param readings the web server's readings
     * @return the fan speeds in RPM, empty if there are none
     */
    static int[] getFanSpeeds(List<Reading> readings) {
        return readings.stream().filter(r -> LhmMetrics.MOTHERBOARD_FAN.equals(r.getMetric()))
                .mapToInt(r -> (int) r.getValue()).toArray();
    }

    /**
     * Finds the processor's voltage: its own first voltage sensor if it has one, as the WMI namespaces read, otherwise
     * the Super I/O chip's {@code Vcore}.
     *
     * @param readings the web server's readings
     * @return the voltage in volts, or 0 if there is none
     */
    static double getCpuVoltage(List<Reading> readings) {
        for (Reading r : readings) {
            if (LhmMetrics.CPU_VOLTAGE.equals(r.getMetric()) && r.getValue() > 0) {
                return r.getValue();
            }
        }
        for (Reading r : readings) {
            if (LhmMetrics.MOTHERBOARD_VOLTAGE.equals(r.getMetric()) && r.getValue() > 0
                    && r.getSensorName().toLowerCase(Locale.ROOT).contains("vcore")) {
                return r.getValue();
            }
        }
        return 0d;
    }

    @Override
    public double queryCpuTemperature() {
        // Attempt to fetch value from Open Hardware Monitor if it is running, as it will give the most accurate results
        // and the time to query (or attempt) is trivial
        double tempC = getTempFromMonitorWmi(OhmHardware.OHM_NAMESPACE, OHM_CPU);
        if (tempC > 0d) {
            return tempC;
        }
        // Then Libre Hardware Monitor, the maintained successor, which publishes the same schema
        tempC = getTempFromMonitorWmi(LhmSensor.LHM_NAMESPACE, LHM_CPU);
        if (tempC > 0d) {
            return tempC;
        }
        // Then Libre Hardware Monitor 0.9.5 and later, which replaced WMI with a web server
        tempC = getCpuTemperature(lhmMetrics.get());
        if (tempC > 0d) {
            return tempC;
        }
        // If we get this far, OHM is not running. Try from WMI. Other fallbacks to WMI are unreliable so we omit them.
        return getTempFromWMI();
    }

    private double getTempFromMonitorWmi(String namespace, String cpuTypeName) {
        if (HardwareMonitorDisabled.isWmiDisabled(namespace)) {
            return 0;
        }
        WmiResult<ValueProperty> ohmSensors = queryHardwareMonitorSensor(namespace, OhmHardware.HARDWARE, cpuTypeName,
                TEMPERATURE);
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            double sum = 0;
            int count = 0;
            for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                double value = WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, i);
                if (isCpuTemperatureReading(WmiUtil.getString(ohmSensors, ValueProperty.NAME, i), value)) {
                    sum += value;
                    count++;
                }
            }
            return count > 0 ? sum / count : 0;
        }
        return 0;
    }

    /**
     * Whether a processor temperature sensor is a reading to average. Both monitors also publish summaries of the
     * per-core readings ({@code Core Max}, {@code Core Average}) and, on Intel processors, each core's
     * {@code Distance to TjMax}, which is a margin below the throttling point rather than a temperature.
     *
     * @param name  the sensor name
     * @param value the sensor value, in degrees Celsius
     * @return true for a positive temperature that is not a summary or a margin
     */
    static boolean isCpuTemperatureReading(String name, double value) {
        return value > 0 && !name.contains("Max") && !name.contains("Average") && !name.contains("Distance");
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
        int[] fanSpeeds = getFansFromMonitorWmi(OhmHardware.OHM_NAMESPACE);
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        // Then Libre Hardware Monitor, the maintained successor, which publishes the same schema
        fanSpeeds = getFansFromMonitorWmi(LhmSensor.LHM_NAMESPACE);
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }
        // Then Libre Hardware Monitor 0.9.5 and later, which replaced WMI with a web server
        fanSpeeds = getFanSpeeds(lhmMetrics.get());
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

    private int[] getFansFromMonitorWmi(String namespace) {
        if (HardwareMonitorDisabled.isWmiDisabled(namespace)) {
            return new int[0];
        }
        WmiResult<ValueProperty> ohmSensors = queryHardwareMonitorSensor(namespace, OhmHardware.HARDWARE, SUPER_IO,
                FAN);
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            int[] fanSpeeds = new int[ohmSensors.getResultCount()];
            for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                fanSpeeds[i] = (int) WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, i);
            }
            return fanSpeeds;
        }
        return new int[0];
    }

    private int[] getFansFromWMI() {
        WmiResult<SpeedProperty> fan = queryWmiFanSpeed();
        if (fan.getResultCount() > 0) {
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
        double volts = getVoltsFromMonitorWmi(OhmHardware.OHM_NAMESPACE, OHM_CPU);
        if (volts > 0d) {
            return volts;
        }
        // Then Libre Hardware Monitor, the maintained successor, which publishes the same schema
        volts = getVoltsFromMonitorWmi(LhmSensor.LHM_NAMESPACE, LHM_CPU);
        if (volts > 0d) {
            return volts;
        }
        // Then Libre Hardware Monitor 0.9.5 and later, which replaced WMI with a web server
        volts = getCpuVoltage(lhmMetrics.get());
        if (volts > 0d) {
            return volts;
        }
        // If we get this far, OHM is not running. Try to get from conventional WMI
        return getVoltsFromWMI();
    }

    private double getVoltsFromMonitorWmi(String namespace, String cpuTypeName) {
        if (HardwareMonitorDisabled.isWmiDisabled(namespace)) {
            return 0d;
        }
        // The processor's voltage sensors, reached the same way as its temperature and fan sensors. This asked the
        // Hardware class for SensorType="Voltage" until 2026-09; that class has no SensorType property, so the query
        // could never match and this tier always fell through to plain WMI.
        WmiResult<ValueProperty> ohmSensors = queryHardwareMonitorSensor(namespace, OhmHardware.HARDWARE, cpuTypeName,
                VOLTAGE);
        if (ohmSensors != null && ohmSensors.getResultCount() > 0) {
            return WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, 0);
        }
        return 0d;
    }

    private double getVoltsFromWMI() {
        WmiResult<VoltProperty> voltage = queryWmiVoltage();
        if (voltage.getResultCount() > 0) {
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
     * Selects the hardware identifiers to read sensors from. The query which produced the result already filtered on
     * {@code HardwareType}. A processor's sensors are read from the first identifier; fans are read from every Super
     * I/O chip, since a board may carry more than one and the first need not be the one wired to the fan headers.
     *
     * @param hardware the hardware identifier result
     * @param typeName the {@code HardwareType} the result was filtered on
     * @return the identifiers to query, empty if there are none
     */
    protected static List<String> selectHardwareIdentifiers(WmiResult<IdentifierProperty> hardware, String typeName) {
        List<String> identifiers = new ArrayList<>();
        int count = SUPER_IO.equals(typeName) ? hardware.getResultCount() : Math.min(1, hardware.getResultCount());
        for (int i = 0; i < count; i++) {
            String identifier = WmiUtil.getString(hardware, IdentifierProperty.IDENTIFIER, i);
            if (!identifier.isEmpty()) {
                identifiers.add(identifier);
            }
        }
        return identifiers;
    }

    /**
     * Queries a hardware-monitor CPU sensor via the backend-specific WMI handler. Open Hardware Monitor and Libre
     * Hardware Monitor publish the same schema under their own namespaces, so the namespace selects which is queried.
     *
     * @param namespace   the WMI namespace, either {@link OhmHardware#OHM_NAMESPACE} or {@link LhmSensor#LHM_NAMESPACE}
     * @param typeToQuery the class whose {@code Type} column is filtered, {@link OhmHardware#HARDWARE}
     * @param typeName    the hardware type name, which differs between the two monitors
     * @param sensorType  the sensor type
     * @return the sensor values, or {@code null} if unavailable
     */
    protected abstract @Nullable WmiResult<ValueProperty> queryHardwareMonitorSensor(String namespace,
            String typeToQuery, String typeName, String sensorType);

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
