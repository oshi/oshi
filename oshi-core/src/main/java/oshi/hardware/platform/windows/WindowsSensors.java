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

import oshi.hardware.Sensors;
import oshi.jna.platform.windows.PdhUtil;
import oshi.jna.platform.windows.PdhUtil.PdhEnumObjectItems;
import oshi.jna.platform.windows.PdhUtil.PdhException;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.WmiQuery;
import oshi.util.platform.windows.WmiUtil.WmiResult;

public class WindowsSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    private static final String BASE_SENSOR_CLASS = "Sensor";
    private static final String THERMAL_ZONE_INFO = "Thermal Zone Information";

    enum OhmHardwareProperty {
        IDENTIFIER;
    }

    private static final WmiQuery<OhmHardwareProperty> OHM_HARDWARE_QUERY = WmiUtil.createQuery(WmiUtil.OHM_NAMESPACE,
            "Hardware WHERE HardwareType=\"CPU\"", OhmHardwareProperty.class);
    private static final WmiQuery<OhmHardwareProperty> OHM_VOLTAGE_QUERY = WmiUtil.createQuery(WmiUtil.OHM_NAMESPACE,
            "Hardware WHERE SensorType=\"Voltage\"", OhmHardwareProperty.class);

    enum OhmSensorProperty {
        VALUE;
    }

    private static final WmiQuery<OhmSensorProperty> OHM_SENSOR_QUERY = WmiUtil.createQuery(WmiUtil.OHM_NAMESPACE, null,
            OhmSensorProperty.class);

    enum FanProperty {
        DESIREDSPEED;
    }

    private static final WmiQuery<FanProperty> FAN_QUERY = WmiUtil.createQuery("Win32_Fan", FanProperty.class);

    enum VoltProperty {
        CURRENTVOLTAGE, VOLTAGECAPS;
    }

    private static final WmiQuery<VoltProperty> VOLT_QUERY = WmiUtil.createQuery("Win32_Processor", VoltProperty.class);

    private String thermalZoneQueryString = "";

    public WindowsSensors() {
        try {
            PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, THERMAL_ZONE_INFO, 100);
            if (!objectItems.getInstances().isEmpty()) {
                // Default to first value
                thermalZoneQueryString = objectItems.getInstances().get(0);
                // Prefer a value with "CPU" in it
                for (String instance : objectItems.getInstances()) {
                    if (instance.toLowerCase().contains("cpu")) {
                        thermalZoneQueryString = instance;
                    }
                }
            }
            if (!thermalZoneQueryString.isEmpty()) {
                thermalZoneQueryString = String.format("\\%s(%s)\\Temperature", THERMAL_ZONE_INFO,
                        thermalZoneQueryString);
                PerfDataUtil.addCounter(thermalZoneQueryString);
            }
        } catch (PdhException e) {
            LOG.warn("Unable to enumerate performance counter instances for " + THERMAL_ZONE_INFO);
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
            String cpuIdentifier = ohmHardware.getString(OhmHardwareProperty.IDENTIFIER, 0);
            if (cpuIdentifier.length() > 0) {
                StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
                sb.append(" WHERE Parent = \"").append(cpuIdentifier);
                sb.append("\" AND SensorType=\"Temperature\"");
                OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
                WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);

                if (ohmSensors.getResultCount() > 0) {
                    double sum = 0;
                    for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                        sum += ohmSensors.getFloat(OhmSensorProperty.VALUE, i);
                    }
                    tempC = sum / ohmSensors.getResultCount();
                }
                return tempC;
            }
        }

        // If we get this far, OHM is not running. Try from PDH
        if (PerfDataUtil.isCounter(thermalZoneQueryString)) {
            long tempK = PerfDataUtil.queryCounter(thermalZoneQueryString);
            if (tempK > 2732L) {
                tempC = tempK / 10d - 273.15;
            } else if (tempK > 274L) {
                tempC = tempK - 273d;
            }
            if (tempC < 0d) {
                tempC = 0d;
            }
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
            String cpuIdentifier = ohmHardware.getString(OhmHardwareProperty.IDENTIFIER, 0);
            if (cpuIdentifier.length() > 0) {
                StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
                sb.append(" WHERE Parent = \"").append(cpuIdentifier);
                sb.append("\" AND SensorType=\"Fan\"");
                OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
                WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);

                if (ohmSensors.getResultCount() > 0) {
                    int[] fanSpeeds = new int[ohmSensors.getResultCount()];
                    for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                        fanSpeeds[i] = (ohmSensors.getFloat(OhmSensorProperty.VALUE, i)).intValue();
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
                fanSpeeds[i] = fan.getInteger(FanProperty.DESIREDSPEED, i);
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
                String id = ohmHardware.getString(OhmHardwareProperty.IDENTIFIER, i);
                if (id.toLowerCase().contains("cpu")) {
                    voltIdentifierStr = id;
                    break;
                }
            }
            // If none found, just get the first one
            if (voltIdentifierStr == null) {
                voltIdentifierStr = ohmHardware.getString(OhmHardwareProperty.IDENTIFIER, 0);
            }
            // Now fetch sensor
            StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
            sb.append(" WHERE Parent = \"").append(voltIdentifierStr);
            sb.append("\" AND SensorType=\"Voltage\"");
            OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
            WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);
            if (ohmSensors.getResultCount() > 0) {
                return ohmSensors.getFloat(OhmSensorProperty.VALUE, 0);
            }
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        WmiResult<VoltProperty> voltage = WmiUtil.queryWMI(VOLT_QUERY);
        if (voltage.getResultCount() > 1) {
            int decivolts = voltage.getInteger(VoltProperty.CURRENTVOLTAGE, 0);
            // If the eighth bit is set, bits 0-6 contain the voltage
            // multiplied by 10. If the eighth bit is not set, then the bit
            // setting in VoltageCaps represents the voltage value.
            if (decivolts > 0) {
                if ((decivolts & 0x80) == 0) {
                    decivolts = voltage.getInteger(VoltProperty.VOLTAGECAPS, 0);
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
