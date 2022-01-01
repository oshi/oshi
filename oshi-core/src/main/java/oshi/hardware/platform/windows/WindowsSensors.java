/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.COMException; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperature;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.windows.wmi.OhmHardware;
import oshi.driver.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.windows.wmi.OhmSensor;
import oshi.driver.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.windows.wmi.Win32Fan;
import oshi.driver.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.windows.wmi.Win32Processor;
import oshi.driver.windows.wmi.Win32Processor.VoltProperty;
import oshi.hardware.common.AbstractSensors;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Sensors from WMI or Open Hardware Monitor
 */
@ThreadSafe
final class WindowsSensors extends AbstractSensors {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensors.class);

    private static final String COM_EXCEPTION_MSG = "COM exception: {}";

    @Override
    public double queryCpuTemperature() {
        // Attempt to fetch value from Open Hardware Monitor if it is running,
        // as it will give the most accurate results and the time to query (or
        // attempt) is trivial
        double tempC = getTempFromOHM();
        if (tempC > 0d) {
            return tempC;
        }

        // If we get this far, OHM is not running. Try from WMI
        tempC = getTempFromWMI();

        // Other fallbacks to WMI are unreliable so we omit them
        // Win32_TemperatureProbe is the official location but is not currently
        // populated and is "reserved for future use"
        return tempC;
    }

    private static double getTempFromOHM() {
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            WmiResult<IdentifierProperty> ohmHardware = OhmHardware.queryHwIdentifier(h, "Hardware", "CPU");
            if (ohmHardware.getResultCount() > 0) {
                LOG.debug("Found Temperature data in Open Hardware Monitor");
                String cpuIdentifier = WmiUtil.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
                if (cpuIdentifier.length() > 0) {
                    WmiResult<ValueProperty> ohmSensors = OhmSensor.querySensorValue(h, cpuIdentifier, "Temperature");
                    if (ohmSensors.getResultCount() > 0) {
                        double sum = 0;
                        for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                            sum += WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, i);
                        }
                        return sum / ohmSensors.getResultCount();
                    }
                }
            }
        } catch (COMException e) {
            LOG.warn(COM_EXCEPTION_MSG, e.getMessage());
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
        return 0;
    }

    private static double getTempFromWMI() {
        double tempC = 0d;
        long tempK = 0L;
        WmiResult<TemperatureProperty> result = MSAcpiThermalZoneTemperature.queryCurrentTemperature();
        if (result.getResultCount() > 0) {
            LOG.debug("Found Temperature data in WMI");
            tempK = WmiUtil.getUint32asLong(result, TemperatureProperty.CURRENTTEMPERATURE, 0);
        }
        if (tempK > 2732L) {
            tempC = tempK / 10d - 273.15;
        } else if (tempK > 274L) {
            tempC = tempK - 273d;
        }
        return tempC < 0d ? 0d : tempC;
    }

    @Override
    public int[] queryFanSpeeds() {
        // Attempt to fetch value from Open Hardware Monitor if it is running
        int[] fanSpeeds = getFansFromOHM();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        fanSpeeds = getFansFromWMI();
        if (fanSpeeds.length > 0) {
            return fanSpeeds;
        }

        // Default
        return new int[0];
    }

    private static int[] getFansFromOHM() {
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            WmiResult<IdentifierProperty> ohmHardware = OhmHardware.queryHwIdentifier(h, "Hardware", "CPU");
            if (ohmHardware.getResultCount() > 0) {
                LOG.debug("Found Fan data in Open Hardware Monitor");
                String cpuIdentifier = WmiUtil.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
                if (cpuIdentifier.length() > 0) {
                    WmiResult<ValueProperty> ohmSensors = OhmSensor.querySensorValue(h, cpuIdentifier, "Fan");
                    if (ohmSensors.getResultCount() > 0) {
                        int[] fanSpeeds = new int[ohmSensors.getResultCount()];
                        for (int i = 0; i < ohmSensors.getResultCount(); i++) {
                            fanSpeeds[i] = (int) WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, i);
                        }
                        return fanSpeeds;
                    }
                }
            }
        } catch (COMException e) {
            LOG.warn(COM_EXCEPTION_MSG, e.getMessage());
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
        return new int[0];
    }

    private static int[] getFansFromWMI() {
        WmiResult<SpeedProperty> fan = Win32Fan.querySpeed();
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

        // If we get this far, OHM is not running.
        // Try to get from conventional WMI
        volts = getVoltsFromWMI();

        return volts;
    }

    private static double getVoltsFromOHM() {
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            WmiResult<IdentifierProperty> ohmHardware = OhmHardware.queryHwIdentifier(h, "Sensor", "Voltage");
            if (ohmHardware.getResultCount() > 0) {
                LOG.debug("Found Voltage data in Open Hardware Monitor");
                // Look for identifier containing "cpu"
                String cpuIdentifier = null;
                for (int i = 0; i < ohmHardware.getResultCount(); i++) {
                    String id = WmiUtil.getString(ohmHardware, IdentifierProperty.IDENTIFIER, i);
                    if (id.toLowerCase().contains("cpu")) {
                        cpuIdentifier = id;
                        break;
                    }
                }
                // If none found, just get the first one
                if (cpuIdentifier == null) {
                    cpuIdentifier = WmiUtil.getString(ohmHardware, IdentifierProperty.IDENTIFIER, 0);
                }
                // Now fetch sensor
                WmiResult<ValueProperty> ohmSensors = OhmSensor.querySensorValue(h, cpuIdentifier, "Voltage");
                if (ohmSensors.getResultCount() > 0) {
                    return WmiUtil.getFloat(ohmSensors, ValueProperty.VALUE, 0);
                }
            }
        } catch (COMException e) {
            LOG.warn(COM_EXCEPTION_MSG, e.getMessage());
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
        return 0d;
    }

    private static double getVoltsFromWMI() {
        WmiResult<VoltProperty> voltage = Win32Processor.queryVoltage();
        if (voltage.getResultCount() > 1) {
            LOG.debug("Found Voltage data in WMI");
            int decivolts = WmiUtil.getUint16(voltage, VoltProperty.CURRENTVOLTAGE, 0);
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
        return 0d;
    }
}
