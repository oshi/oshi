/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Sensors;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.ExecutingCommand;

public class WindowsSensors implements Sensors {
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    // If null, haven't attempted OHM.
    private String tempIdentifierStr = null;
    // Successful (?) WMI path and property
    private String wmiTempPath = null;
    private String wmiTempProperty = null;

    // If false, can't get from WMI
    private boolean fanSpeedWMI = true;

    // If null, haven't attempted OHM.
    private String voltIdentifierStr = null;
    // Successful (?) WMI path and property
    private String wmiVoltPath = null;
    private String wmiVoltProperty = null;

    /*
     * Gets integer value from WMI or -1 if failed
     */
    private int wmiGetValue(String path, String property) {
        int value = -1;
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, property));
        for (String checkLine : data) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains(property.toLowerCase())) {
                continue;
            } else {
                // If successful this line is the desired value
                try {
                    value = Integer.parseInt(checkLine.trim());
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                    value = -1;
                }
                break;
            }
        }
        return value;
    }

    /*
     * Returns string value in the second property when first property matches
     * the key
     */
    private String[] wmiGetStrValuesForKey(String path, String key, String properties) {
        List<String> values = new ArrayList<String>();
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, properties));
        for (String checkLine : data) {
            String[] keyValue = checkLine.split("\\s+");
            if (keyValue.length < 2) {
                continue;
            }
            if (keyValue[0].equals(key)) {
                values.add(keyValue[1]);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    /*
     * Returns array of values in the third property when first two properties
     * match the keys
     */
    private double[] wmiGetValuesForKeys(String path, String key0, String key1, String properties) {
        List<Double> values = new ArrayList<Double>();
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, properties));
        for (String checkLine : data) {
            double value;
            String[] keyValue = checkLine.split("\\s+");
            if (keyValue.length < 3) {
                continue;
            }
            if ((key0 == null || keyValue[0].equals(key0)) && keyValue[1].equals(key1)) {
                // If successful this line is the desired value
                try {
                    value = Double.parseDouble(keyValue[2]);
                } catch (NumberFormatException e) {
                    // If we failed to parse, do nothing
                    continue;
                }
                values.add(value);
            }
        }
        double[] valueArray = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            valueArray[i] = values.get(i);
        }
        return valueArray;
    }

    @Override
    public double getCpuTemperature() {
        // Initialize
        double tempC = 0d;
        // If Open Hardware Monitor identifier is set, we couldn't get through
        // normal WMI, and got ID from OHM at least once so go directly to OHM
        if (this.tempIdentifierStr != null) {
            double[] vals = wmiGetValuesForKeys("/namespace:\\\\root\\OpenHardwareMonitor PATH Sensor",
                    this.tempIdentifierStr, "Temperature", "Parent,SensorType,Value");
            if (vals.length > 0) {
                double sum = 0;
                for (double val : vals) {
                    sum += val;
                }
                tempC = sum / vals.length;
            }
            return tempC;
        }
        // This branch is used the first time and all subsequent times if
        // successful (tempIdenifierStr == null)
        // Try to get value using initial or updated successful values
        int tempK = 0;
        if (this.wmiTempPath == null) {
            this.wmiTempPath = "Temperature";
            this.wmiTempProperty = "CurrentReading";
            tempK = wmiGetValue(this.wmiTempPath, this.wmiTempProperty);
            if (tempK < 0) {
                this.wmiTempPath = "/namespace:\\\\root\\cimv2 PATH Win32_TemperatureProbe";
                tempK = wmiGetValue(this.wmiTempPath, this.wmiTempProperty);
            }
            if (tempK < 0) {
                this.wmiTempPath = "/namespace:\\\\root\\wmi PATH MSAcpi_ThermalZoneTemperature";
                this.wmiTempProperty = "CurrentTemperature";
                tempK = wmiGetValue(this.wmiTempPath, this.wmiTempProperty);
            }
        } else {
            // We've successfully read a previous time, or failed both here and
            // with OHM
            tempK = wmiGetValue(this.wmiTempPath, this.wmiTempProperty);
        }
        // Convert K to C and return result
        if (tempK > 0) {
            tempC = (tempK / 10d) - 273.15;
        }
        if (tempC <= 0d) {
            // Unable to get temperature via WMI. Future attempts will be
            // attempted via Open Hardware Monitor WMI if successful
            String[] cpuIdentifiers = wmiGetStrValuesForKey("/namespace:\\\\root\\OpenHardwareMonitor PATH Hardware",
                    "CPU", "HardwareType,Identifier");
            if (cpuIdentifiers.length > 0) {
                this.tempIdentifierStr = cpuIdentifiers[0];
            }
            // If not null, recurse and get value via OHM
            if (this.tempIdentifierStr != null) {
                return getCpuTemperature();
            }
        }
        return tempC;
    }

    @Override
    public int[] getFanSpeeds() {
        // Initialize
        int[] fanSpeeds = new int[1];
        // If we couldn't get through normal WMI go directly to OHM
        if (!this.fanSpeedWMI) {
            double[] vals = wmiGetValuesForKeys("/namespace:\\\\root\\OpenHardwareMonitor PATH Sensor", null, "Fan",
                    "Parent,SensorType,Value");
            if (vals.length > 0) {
                fanSpeeds = new int[vals.length];
                for (int i = 0; i < vals.length; i++) {
                    fanSpeeds[i] = (int) vals[i];
                }
            }
            return fanSpeeds;
        }
        // This branch is used the first time and all subsequent times if
        // successful (fanSpeedWMI == true)
        // Try to get value
        int rpm = wmiGetValue("/namespace:\\\\root\\cimv2 PATH Win32_Fan", "DesiredSpeed");
        // Set in array and return
        if (rpm > 0) {
            fanSpeeds[0] = rpm;
        } else {
            // Fail, switch to OHM
            this.fanSpeedWMI = false;
            return getFanSpeeds();
        }
        return fanSpeeds;
    }

    @Override
    public double getCpuVoltage() {
        // Initialize
        double volts = 0d;
        // If we couldn't get through normal WMI go directly to OHM
        if (this.voltIdentifierStr != null) {
            double[] vals = wmiGetValuesForKeys("/namespace:\\\\root\\OpenHardwareMonitor PATH Sensor",
                    this.voltIdentifierStr, "Voltage", "Identifier,SensorType,Value");
            if (vals.length > 0) {
                // Return the first voltage reading
                volts = vals[0];
            }
            return volts;
        }
        // This branch is used the first time and all subsequent times if
        // successful (voltIdenifierStr == null)
        // Try to get value
        // Try to get value using initial or updated successful values
        int decivolts = 0;
        if (this.wmiVoltPath == null) {
            this.wmiVoltPath = "CPU";
            this.wmiVoltProperty = "CurrentVoltage";
            decivolts = wmiGetValue(this.wmiVoltPath, this.wmiVoltProperty);
            if (decivolts < 0) {
                this.wmiVoltPath = "/namespace:\\\\root\\cimv2 PATH Win32_Processor";
                decivolts = wmiGetValue(this.wmiVoltPath, this.wmiVoltProperty);
            }
            // If the eighth bit is set, bits 0-6 contain the voltage
            // multiplied by 10. If the eighth bit is not set, then the bit
            // setting in VoltageCaps represents the voltage value.
            if ((decivolts & 0x80) == 0 && decivolts > 0) {
                this.wmiVoltProperty = "VoltageCaps";
                // really a bit setting, not decivolts, test later
                decivolts = wmiGetValue(this.wmiVoltPath, this.wmiVoltProperty);
            }
        } else {
            // We've successfully read a previous time, or failed both here and
            // with OHM
            decivolts = wmiGetValue(this.wmiVoltPath, this.wmiVoltProperty);
        }
        // Convert dV to V and return result
        if (decivolts > 0) {
            if (this.wmiVoltProperty.equals("VoltageCaps")) {
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
        if (volts <= 0d) {
            // Unable to get voltage via WMI. Future attempts will be
            // attempted via Open Hardware Monitor WMI if successful
            String[] voltIdentifiers = wmiGetStrValuesForKey("/namespace:\\\\root\\OpenHardwareMonitor PATH Hardware",
                    "Voltage", "SensorType,Identifier");
            // Look for identifier containing "cpu"
            for (String id : voltIdentifiers) {
                if (id.toLowerCase().contains("cpu")) {
                    this.voltIdentifierStr = id;
                    break;
                }
            }
            // If none contain cpu just grab the first one
            if (voltIdentifiers.length > 0) {
                this.voltIdentifierStr = voltIdentifiers[0];
            }
            // If not null, recurse and get value via OHM
            if (this.voltIdentifierStr != null) {
                return getCpuVoltage();
            }
        }
        return volts;
    }

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder fanSpeedsArrayBuilder = jsonFactory.createArrayBuilder();
        for (int speed : getFanSpeeds()) {
            fanSpeedsArrayBuilder.add(speed);
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("cpuTemperature", getCpuTemperature()).add("fanSpeeds", fanSpeedsArrayBuilder.build())
                .add("cpuVoltage", getCpuVoltage()).build();
    }
}
