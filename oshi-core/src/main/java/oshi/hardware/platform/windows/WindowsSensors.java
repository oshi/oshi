/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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

import java.util.List;
import java.util.Map;

import oshi.hardware.Sensors;
import oshi.util.platform.windows.WmiUtil;

public class WindowsSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    // Successful (?) WMI namespace, path and property
    private String wmiTempNamespace = null;

    private String wmiTempClass = null;

    private String wmiTempProperty = null;

    // Successful (?) WMI path and property
    private String wmiVoltNamespace = null;

    private String wmiVoltClass = null;

    private String wmiVoltProperty = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        // Initialize
        double tempC = 0d;

        // Attempt to fetch value from Open Hardware Monitor if it is running
        String cpuIdentifier = WmiUtil.selectStringFrom("root\\OpenHardwareMonitor", "Hardware", "Identifier",
                "WHERE HardwareType=\"CPU\"");
        if (cpuIdentifier.length() > 0) {
            Map<String, List<Float>> vals = WmiUtil.selectFloatsFrom("root\\OpenHardwareMonitor", "Sensor", "Value",
                    "WHERE Parent=\"" + cpuIdentifier + "\" AND SensorType=\"Temperature\"");
            if (!vals.get("Value").isEmpty()) {
                double sum = 0;
                for (double val : vals.get("Value")) {
                    sum += val;
                }
                tempC = sum / vals.get("Value").size();
            }
            return tempC;
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        long tempK;
        if (this.wmiTempClass == null) {
            this.wmiTempNamespace = "root\\cimv2";
            this.wmiTempClass = "Win32_Temperature";
            this.wmiTempProperty = "CurrentReading";
            tempK = WmiUtil.selectUint32From(this.wmiTempNamespace, this.wmiTempClass, this.wmiTempProperty, null);
            if (tempK == 0) {
                this.wmiTempClass = "Win32_TemperatureProbe";
                tempK = WmiUtil.selectUint32From(this.wmiTempNamespace, this.wmiTempClass, this.wmiTempProperty, null);
            }
            if (tempK == 0) {
                this.wmiTempClass = "Win32_PerfFormattedData_Counters_ThermalZoneInformation";
                this.wmiTempProperty = "Temperature";
                tempK = WmiUtil.selectUint32From(this.wmiTempNamespace, this.wmiTempClass, this.wmiTempProperty, null);
            }
            if (tempK == 0) {
                this.wmiTempNamespace = "root\\wmi";
                this.wmiTempClass = "MSAcpi_ThermalZoneTemperature";
                this.wmiTempProperty = "CurrentTemperature";
                tempK = WmiUtil.selectUint32From(this.wmiTempNamespace, this.wmiTempClass, this.wmiTempProperty, null);
            }
        } else {
            // We've successfully read a previous time, or failed both here and
            // with OHM, so keep using same values
            tempK = WmiUtil.selectUint32From(this.wmiTempNamespace, this.wmiTempClass, this.wmiTempProperty, null);
        }
        // Convert K to C and return result
        if (tempK > 2732) {
            tempC = tempK / 10d - 273.15;
        } else if (tempK > 274) {
            tempC = tempK - 273d;
        }
        if (tempC <= 0d) {
            // Unable to get temperature via WMI.
            tempC = 0d;
        }
        return tempC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        // Initialize
        int[] fanSpeeds = new int[1];

        // Attempt to fetch value from Open Hardware Monitor if it is running
        String cpuIdentifier = WmiUtil.selectStringFrom("root\\OpenHardwareMonitor", "Hardware", "Identifier",
                "WHERE HardwareType=\"CPU\"");
        if (cpuIdentifier.length() > 0) {
            Map<String, List<Float>> vals = WmiUtil.selectFloatsFrom("root\\OpenHardwareMonitor", "Sensor", "Value",
                    "WHERE Parent=\"" + cpuIdentifier + "\" AND SensorType=\"Fan\"");
            if (!vals.get("Value").isEmpty()) {
                fanSpeeds = new int[vals.get("Value").size()];
                for (int i = 0; i < vals.get("Value").size(); i++) {
                    fanSpeeds[i] = vals.get("Value").get(i).intValue();
                }
            }
            return fanSpeeds;
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        int rpm = WmiUtil.selectUint32From(null, "Win32_Fan", "DesiredSpeed", null).intValue();
        // Set in array and return
        if (rpm > 0) {
            fanSpeeds[0] = rpm;
        }
        return fanSpeeds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        // Initialize
        double volts = 0d;

        // Attempt to fetch value from Open Hardware Monitor if it is running
        Map<String, List<String>> voltIdentifiers = WmiUtil.selectStringsFrom("root\\OpenHardwareMonitor", "Hardware",
                "Identifier", "WHERE SensorType=\"Voltage\"");
        // Look for identifier containing "cpu"
        String voltIdentifierStr = null;
        for (String id : voltIdentifiers.get("Identifier")) {
            if (id.toLowerCase().contains("cpu")) {
                voltIdentifierStr = id;
                break;
            }
        }
        // If none contain cpu just grab the first one
        if (voltIdentifierStr == null && !voltIdentifiers.get("Identifier").isEmpty()) {
            voltIdentifierStr = voltIdentifiers.get("Identifier").get(0);
        }
        if (voltIdentifierStr != null) {
            return WmiUtil.selectFloatFrom("root\\OpenHardwareMonitor", "Sensor", "Value",
                    "WHERE Parent=\"" + voltIdentifierStr + "\" AND SensorType=\"Voltage\"");
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        int decivolts;
        if (this.wmiVoltClass == null) {
            this.wmiVoltNamespace = "root\\cimv2";
            this.wmiVoltClass = "Win32_Processor";
            this.wmiVoltProperty = "CurrentVoltage";
            decivolts = WmiUtil.selectUint32From(this.wmiVoltNamespace, this.wmiVoltClass, this.wmiVoltProperty, null)
                    .intValue();
            // If the eighth bit is set, bits 0-6 contain the voltage
            // multiplied by 10. If the eighth bit is not set, then the bit
            // setting in VoltageCaps represents the voltage value.
            if ((decivolts & 0x80) == 0 && decivolts > 0) {
                this.wmiVoltProperty = "VoltageCaps";
                // really a bit setting, not decivolts, test later
                decivolts = WmiUtil
                        .selectUint32From(this.wmiVoltNamespace, this.wmiVoltClass, this.wmiVoltProperty, null)
                        .intValue();
            }
        } else {
            // We've successfully read a previous time, or failed both here and
            // with OHM
            decivolts = WmiUtil.selectUint32From(this.wmiVoltNamespace, this.wmiVoltClass, this.wmiVoltProperty, null)
                    .intValue();
        }
        // Convert dV to V and return result
        if (decivolts > 0) {
            if ("VoltageCaps".equals(this.wmiVoltProperty)) {
                // decivolts are bits
                if ((decivolts & 0x1) > 0) {
                    volts = 5.0;
                } else if ((decivolts & 0x2) > 0) {
                    volts = 3.3;
                } else if ((decivolts & 0x4) > 0) {
                    volts = 2.9;
                }
            } else {
                // Value from bits 0-6
                volts = (decivolts & 0x7F) / 10d;
            }
        }
        return volts;
    }
}
