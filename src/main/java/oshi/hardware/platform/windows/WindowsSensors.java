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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.Sensors;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.ExecutingCommand;

public class WindowsSensors implements Sensors {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    @Override
    public double getCpuTemperature() {
        ArrayList<String> hwInfo = ExecutingCommand.runNative("wmic Temperature get CurrentReading");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("currentreading")) {
                continue;
            } else {
                // If successful this line is in tenths of degrees Kelvin
                try {
                    int tempK = Integer.parseInt(checkLine.trim());
                    if (tempK > 0) {
                        return (tempK - 2715) / 10d;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        // Above query failed, try something else
        hwInfo = ExecutingCommand
                .runNative("wmic /namespace:\\\\root\\wmi PATH MSAcpi_ThermalZoneTemperature get CurrentTemperature");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("currenttemperature")) {
                continue;
            } else {
                // If successful this line is in tenths of degrees Kelvin
                try {
                    int tempK = Integer.parseInt(checkLine.trim());
                    if (tempK > 0) {
                        return (tempK - 2715) / 10d;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        // Above query failed, try something else
        hwInfo = ExecutingCommand
                .runNative("wmic /namespace:\\\\root\\cimv2 PATH Win32_TemperatureProbe get CurrentReading");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("currentreading")) {
                continue;
            } else {
                // If successful this line is in tenths of degrees Kelvin
                try {
                    int tempK = Integer.parseInt(checkLine.trim());
                    if (tempK > 0) {
                        return (tempK - 2715) / 10d;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        return 0d;
    }

    @Override
    public int[] getFanSpeeds() {
        int[] fanSpeeds = new int[1];
        ArrayList<String> hwInfo = ExecutingCommand
                .runNative("wmic /namespace:\\\\root\\cimv2 PATH Win32_Fan get DesiredSpeed");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("desiredspeed")) {
                continue;
            } else {
                // If successful
                try {
                    int rpm = Integer.parseInt(checkLine.trim());
                    // Check if 8th bit (of 16 bit number) is set
                    if (rpm > 0) {
                        fanSpeeds[0] = rpm;
                        return fanSpeeds;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        return fanSpeeds;
    }

    @Override
    public double getCpuVoltage() {
        ArrayList<String> hwInfo = ExecutingCommand.runNative("wmic cpu get CurrentVoltage");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("currentvoltage")) {
                continue;
            } else {
                // If successful this line is in tenths of volts
                try {
                    int decivolts = Integer.parseInt(checkLine.trim());
                    if (decivolts > 0) {
                        return decivolts / 10d;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        // Above query failed, try something else
        hwInfo = ExecutingCommand.runNative("wmic /namespace:\\\\root\\cimv2 PATH Win32_Processor get CurrentVoltage");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("currentreading")) {
                continue;
            } else {
                // If successful:
                // If the eighth bit is set, bits 0-6 contain the voltage
                // multiplied by 10. If the eighth bit is not set, then the bit
                // setting in VoltageCaps represents the voltage value.
                try {
                    int decivolts = Integer.parseInt(checkLine.trim());
                    // Check if 8th bit (of 16 bit number) is set
                    if ((decivolts & 0x80) > 0 && decivolts > 0) {
                        return decivolts / 10d;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        // Above query failed, try something else
        hwInfo = ExecutingCommand.runNative("wmic /namespace:\\\\root\\cimv2 PATH Win32_Processor get VoltageCaps");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("currentreading")) {
                continue;
            } else {
                // If successful:
                // Bits 0-3 of the field represent specific voltages that the
                // processor socket can accept.
                try {
                    int voltagebits = Integer.parseInt(checkLine.trim());
                    // Return appropriate voltage
                    if ((voltagebits & 0x1) > 0) {
                        return 5.0;
                    } else if ((voltagebits & 0x2) > 0) {
                        return 3.3;
                    } else if ((voltagebits & 0x4) > 0) {
                        return 2.9;
                    }
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                }
                break;
            }
        }
        return 0d;
    }

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder fanSpeedsArrayBuilder = jsonFactory.createArrayBuilder();
        for (int speed : getFanSpeeds()) {
            fanSpeedsArrayBuilder.add(speed);
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("cpuTemperature", getCpuTemperature())
                .add("fanSpeeds", fanSpeedsArrayBuilder.build()).add("cpuVoltage", getCpuVoltage()).build();
    }
}
