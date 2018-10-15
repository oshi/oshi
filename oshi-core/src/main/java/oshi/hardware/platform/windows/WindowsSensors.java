/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.PdhUtil; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.PdhUtil.PdhException;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.Sensors;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiUtil;

public class WindowsSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    private static final String BASE_SENSOR_CLASS = "Sensor";

    enum OhmHardwareProperty {
        IDENTIFIER;
    }

    private static final WmiQuery<OhmHardwareProperty> OHM_HARDWARE_QUERY = new WmiQuery<>(WmiUtil.OHM_NAMESPACE,
            "Hardware WHERE HardwareType=\"CPU\"", OhmHardwareProperty.class);
    private static final WmiQuery<OhmHardwareProperty> OHM_VOLTAGE_QUERY = new WmiQuery<>(WmiUtil.OHM_NAMESPACE,
            "Hardware WHERE SensorType=\"Voltage\"", OhmHardwareProperty.class);

    enum OhmSensorProperty {
        VALUE;
    }

    private static final WmiQuery<OhmSensorProperty> OHM_SENSOR_QUERY = new WmiQuery<>(WmiUtil.OHM_NAMESPACE, null,
            OhmSensorProperty.class);

    enum FanProperty {
        DESIREDSPEED;
    }

    private static final WmiQuery<FanProperty> FAN_QUERY = new WmiQuery<>("Win32_Fan", FanProperty.class);

    enum VoltProperty {
        CURRENTVOLTAGE, VOLTAGECAPS;
    }

    private static final WmiQuery<VoltProperty> VOLT_QUERY = new WmiQuery<>("Win32_Processor", VoltProperty.class);

    /*
     * For temperature query
     */
    enum ThermalZoneProperty {
        NAME, TEMPERATURE;
    }

    // Only one of these will be used
    private transient PerfCounter thermalZoneCounter = null;
    private transient WmiQuery<ThermalZoneProperty> thermalZoneQuery = null;

    public WindowsSensors() {
        initPdhCounters();
    }

    private void initPdhCounters() {
        String thermalZoneInfo = PdhUtil.PdhLookupPerfNameByIndex(null,
                PdhUtil.PdhLookupPerfIndexByEnglishName("Thermal Zone Information"));
        boolean enumeration = false;
        if (!thermalZoneInfo.isEmpty()) {
            try {
                PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, thermalZoneInfo, 100);
                // Default to first value
                // Prefer a value with "CPU" in it
                String cpuInstance = "";
                for (String instance : objectItems.getInstances()) {
                    if (cpuInstance.isEmpty() || instance.toLowerCase().contains("cpu")) {
                        cpuInstance = instance;
                        enumeration = true;
                    }
                }
                this.thermalZoneCounter = PerfDataUtil.createCounter("Thermal Zone Information", cpuInstance,
                        "Temperature");
            } catch (PdhException e) {
                LOG.warn("Unable to enumerate performance counter instances for {}.", thermalZoneInfo);
            }
        }
        if (!enumeration || !PerfDataUtil.addCounterToQuery(this.thermalZoneCounter)) {
            this.thermalZoneCounter = null;
            this.thermalZoneQuery = new WmiQuery<>("Win32_PerfRawData_Counters_ThermalZoneInformation",
                    ThermalZoneProperty.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        // Initialize default
        double tempC = 0d;

        // Attempt to fetch value from Open Hardware Monitor if it is running,
        // as it will give the most accurate results and the time to query (or
        // attempt) is trivial
        WmiResult<OhmHardwareProperty> ohmHardware = WmiUtil.queryWMI(OHM_HARDWARE_QUERY);
        if (ohmHardware.getResultCount() > 0) {
            String cpuIdentifier = WmiUtil.getString(ohmHardware, OhmHardwareProperty.IDENTIFIER, 0);
            if (cpuIdentifier.length() > 0) {
                StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
                sb.append(" WHERE Parent = \"").append(cpuIdentifier);
                sb.append("\" AND SensorType=\"Temperature\"");
                OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
                WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);

                if (ohmSensors.getResultCount() > 0) {
                    double sum = 0;
                    for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                        sum += WmiUtil.getFloat(ohmSensors, OhmSensorProperty.VALUE, i);
                    }
                    tempC = sum / ohmSensors.getResultCount();
                }
                if (tempC > 0) {
                    return tempC;
                }
            }
        }

        // If we get this far, OHM is not running. Try from PDH
        long tempK = 0L;
        if (this.thermalZoneQuery == null) {
            PerfDataUtil.updateQuery(this.thermalZoneCounter);
            tempK = PerfDataUtil.queryCounter(this.thermalZoneCounter);
        } else {
            // No counter, use WMI
            WmiResult<ThermalZoneProperty> result = WmiUtil.queryWMI(this.thermalZoneQuery);
            // Default to first value
            // Prefer a value with "CPU" in name
            for (int i = 0; i < result.getResultCount(); i++) {
                if (tempK == 0L
                        || WmiUtil.getString(result, ThermalZoneProperty.NAME, i).toLowerCase().contains("cpu")) {
                    tempK = WmiUtil.getUint32(result, ThermalZoneProperty.TEMPERATURE, i);
                }
            }
        }
        if (tempK > 2732L) {
            tempC = tempK / 10d - 273.15;
        } else if (tempK > 274L) {
            tempC = tempK - 273d;
        }
        if (tempC < 0d) {
            tempC = 0d;
        }

        // Other fallbacks to WMI are unreliable so we omit them
        // Win32_TemperatureProbe is the official location but is not currently
        // populated and is "reserved for future use"
        // MSAcpu_ThermalZoneTemperature only updates during a high temperature
        // event and is otherwise unchanged/misleading.
        return tempC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        // Attempt to fetch value from Open Hardware Monitor if it is running
        WmiResult<OhmHardwareProperty> ohmHardware = WmiUtil.queryWMI(OHM_HARDWARE_QUERY);
        if (ohmHardware.getResultCount() > 0) {
            String cpuIdentifier = WmiUtil.getString(ohmHardware, OhmHardwareProperty.IDENTIFIER, 0);
            if (cpuIdentifier.length() > 0) {
                StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
                sb.append(" WHERE Parent = \"").append(cpuIdentifier);
                sb.append("\" AND SensorType=\"Fan\"");
                OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
                WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);

                if (ohmSensors.getResultCount() > 0) {
                    int[] fanSpeeds = new int[ohmSensors.getResultCount()];
                    for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                        fanSpeeds[i] = (int) WmiUtil.getFloat(ohmSensors, OhmSensorProperty.VALUE, i);
                    }
                    return fanSpeeds;
                }
            }
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        WmiResult<FanProperty> fan = WmiUtil.queryWMI(FAN_QUERY);
        if (fan.getResultCount() > 1) {
            int[] fanSpeeds = new int[fan.getResultCount()];
            for (int i = 0; i < fan.getResultCount(); i++) {
                fanSpeeds[i] = WmiUtil.getUint32(fan, FanProperty.DESIREDSPEED, i);
            }
            return fanSpeeds;
        }
        // Default
        return new int[1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        // Attempt to fetch value from Open Hardware Monitor if it is running
        WmiResult<OhmHardwareProperty> ohmHardware = WmiUtil.queryWMI(OHM_VOLTAGE_QUERY);
        if (ohmHardware.getResultCount() > 0) {
            // Look for identifier containing "cpu"
            String voltIdentifierStr = null;
            for (int i = 0; i < ohmHardware.getResultCount(); i++) {
                String id = WmiUtil.getString(ohmHardware, OhmHardwareProperty.IDENTIFIER, i);
                if (id.toLowerCase().contains("cpu")) {
                    voltIdentifierStr = id;
                    break;
                }
            }
            // If none found, just get the first one
            if (voltIdentifierStr == null) {
                voltIdentifierStr = WmiUtil.getString(ohmHardware, OhmHardwareProperty.IDENTIFIER, 0);
            }
            // Now fetch sensor
            StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
            sb.append(" WHERE Parent = \"").append(voltIdentifierStr);
            sb.append("\" AND SensorType=\"Voltage\"");
            OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
            WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);
            if (ohmSensors.getResultCount() > 0) {
                return WmiUtil.getFloat(ohmSensors, OhmSensorProperty.VALUE, 0);
            }
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        WmiResult<VoltProperty> voltage = WmiUtil.queryWMI(VOLT_QUERY);
        if (voltage.getResultCount() > 1) {
            int decivolts = WmiUtil.getUint32(voltage, VoltProperty.CURRENTVOLTAGE, 0);
            // If the eighth bit is set, bits 0-6 contain the voltage
            // multiplied by 10. If the eighth bit is not set, then the bit
            // setting in VoltageCaps represents the voltage value.
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
        // Default
        return 0d;
    }
}
