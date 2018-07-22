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

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.Sensors;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;
import oshi.util.platform.windows.WmiUtil.WmiProperty;
import oshi.util.platform.windows.WmiUtil.WmiQuery;
import oshi.util.platform.windows.WmiUtil.WmiResult;

public class WindowsSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    private static final String BASE_SENSOR_CLASS = "Sensor";

    enum OhmHardwareProperty implements WmiProperty {
        IDENTIFIER(ValueType.STRING);

        private ValueType type;

        OhmHardwareProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<OhmHardwareProperty> OHM_HARDWARE_QUERY = WmiUtil.createQuery(WmiUtil.OHM_NAMESPACE,
            "Hardware WHERE HardwareType=\"CPU\"", OhmHardwareProperty.class);
    private static final WmiQuery<OhmHardwareProperty> OHM_VOLTAGE_QUERY = WmiUtil.createQuery(WmiUtil.OHM_NAMESPACE,
            "Hardware WHERE SensorType=\"Voltage\"", OhmHardwareProperty.class);

    enum OhmSensorProperty implements WmiProperty {
        VALUE(ValueType.FLOAT);

        private ValueType type;

        OhmSensorProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<OhmSensorProperty> OHM_SENSOR_QUERY = WmiUtil.createQuery(WmiUtil.OHM_NAMESPACE, null,
            OhmSensorProperty.class);

    enum ThermalZoneProperty implements WmiProperty {
        NAME(ValueType.STRING), //
        TEMPERATURE(ValueType.UINT32);

        private ValueType type;

        ThermalZoneProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<ThermalZoneProperty> THERMAL_ZONE_QUERY = WmiUtil.createQuery(
            WmiUtil.DEFAULT_NAMESPACE, "Win32_PerfRawData_Counters_ThermalZoneInformation", ThermalZoneProperty.class);

    enum FanProperty implements WmiProperty {
        DESIREDSPEED(ValueType.UINT32);

        private ValueType type;

        FanProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<FanProperty> FAN_QUERY = WmiUtil.createQuery("Win32_Fan", FanProperty.class);

    enum VoltProperty implements WmiProperty {
        CURRENTVOLTAGE(ValueType.UINT32), //
        VOLTAGECAPS(ValueType.UINT32);

        private ValueType type;

        VoltProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<VoltProperty> VOLT_QUERY = WmiUtil.createQuery("Win32_Processor", VoltProperty.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        // Initialize
        double tempC = 0d;

        // Attempt to fetch value from Open Hardware Monitor if it is running
        WmiResult<OhmHardwareProperty> ohmHardware = WmiUtil.queryWMI(OHM_HARDWARE_QUERY);
        if (ohmHardware.getResultCount() > 0) {
            String cpuIdentifier = (String) ohmHardware.get(OhmHardwareProperty.IDENTIFIER).get(0);
            if (cpuIdentifier.length() > 0) {
                StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
                sb.append(" WHERE Parent = \"").append(cpuIdentifier);
                sb.append("\" AND SensorType=\"Temperature\"");
                OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
                WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);

                if (ohmSensors.getResultCount() > 0) {
                    double sum = 0;
                    for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                        sum += (Float) ohmSensors.get(OhmSensorProperty.VALUE).get(i);
                    }
                    tempC = sum / ohmSensors.getResultCount();
                }
                return tempC;
            }
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI.
        // Previously OSHI checked multiple sources but now only looks at
        // PerfCounters
        // Have removed attempts for:
        // Win32_TemperatureProbe CurrentReating is "reserved for future use"
        // MSAcpi_ThermalZoneTemperature CurrentTemperature is not on the CPU

        // This query is notoriously slow so we specify a 2-second timeout
        try {
            long tempK = 0L;

            WmiResult<ThermalZoneProperty> thermalZone = WmiUtil.queryWMI(THERMAL_ZONE_QUERY, 2000);
            if (thermalZone.getResultCount() > 0) {
                // Default to the first result
                tempK = (Long) thermalZone.get(ThermalZoneProperty.TEMPERATURE).get(0);
                // If multiple results, pick the one that's a CPU
                if (thermalZone.getResultCount() > 1) {
                    for (int i = 0; i < thermalZone.getResultCount(); i++) {
                        if (((String) thermalZone.get(ThermalZoneProperty.NAME).get(i)).toLowerCase().contains("cpu")) {
                            tempK = (Long) thermalZone.get(ThermalZoneProperty.TEMPERATURE).get(i);
                            break;
                        }
                    }
                }
            }
            // Convert K to C
            if (tempK > 2732L) {
                tempC = tempK / 10d - 273.15;
            } else if (tempK > 274L) {
                tempC = tempK - 273d;
            }
        } catch (TimeoutException e) {
            LOG.warn("Temperature query timed out. {}", e.getMessage());
            // WMI timed out.
            return 0d;
        }
        if (tempC < 0d) {
            tempC = 0d;
        }
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
            String cpuIdentifier = (String) ohmHardware.get(OhmHardwareProperty.IDENTIFIER).get(0);
            if (cpuIdentifier.length() > 0) {
                StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
                sb.append(" WHERE Parent = \"").append(cpuIdentifier);
                sb.append("\" AND SensorType=\"Fan\"");
                OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
                WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);

                if (ohmSensors.getResultCount() > 0) {
                    int[] fanSpeeds = new int[ohmSensors.getResultCount()];
                    for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                        fanSpeeds[i] = ((Float) ohmSensors.get(OhmSensorProperty.VALUE).get(i)).intValue();
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
                fanSpeeds[i] = ((Long) fan.get(FanProperty.DESIREDSPEED).get(i)).intValue();
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
                String id = (String) ohmHardware.get(OhmHardwareProperty.IDENTIFIER).get(i);
                if (id.toLowerCase().contains("cpu")) {
                    voltIdentifierStr = id;
                    break;
                }
            }
            // If none found, just get the first one
            if (voltIdentifierStr == null) {
                voltIdentifierStr = (String) ohmHardware.get(OhmHardwareProperty.IDENTIFIER).get(0);
            }
            // Now fetch sensor
            StringBuilder sb = new StringBuilder(BASE_SENSOR_CLASS);
            sb.append(" WHERE Parent = \"").append(voltIdentifierStr);
            sb.append("\" AND SensorType=\"Voltage\"");
            OHM_SENSOR_QUERY.setWmiClassName(sb.toString());
            WmiResult<OhmSensorProperty> ohmSensors = WmiUtil.queryWMI(OHM_SENSOR_QUERY);
            if (ohmSensors.getResultCount() > 0) {
                return ((Float) ohmSensors.get(OhmSensorProperty.VALUE).get(0)).doubleValue();
            }
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        WmiResult<VoltProperty> voltage = WmiUtil.queryWMI(VOLT_QUERY);
        if (voltage.getResultCount() > 1) {
            int decivolts = ((Long) voltage.get(VoltProperty.CURRENTVOLTAGE).get(0)).intValue();
            // If the eighth bit is set, bits 0-6 contain the voltage
            // multiplied by 10. If the eighth bit is not set, then the bit
            // setting in VoltageCaps represents the voltage value.
            if (decivolts > 0) {
                if ((decivolts & 0x80) == 0) {
                    decivolts = ((Long) voltage.get(VoltProperty.VOLTAGECAPS).get(0)).intValue();
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
