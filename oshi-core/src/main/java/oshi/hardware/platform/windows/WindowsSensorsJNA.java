/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Objects;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.COMException;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperatureJNA;
import oshi.driver.windows.wmi.OhmHardwareJNA;
import oshi.driver.windows.wmi.OhmSensorJNA;
import oshi.driver.windows.wmi.Win32FanJNA;
import oshi.driver.windows.wmi.Win32ProcessorJNA;
import oshi.hardware.common.platform.windows.WindowsSensors;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * JNA-backed Windows Sensors from WMI or Open Hardware Monitor or Libre Hardware Monitor.
 */
@ThreadSafe
final class WindowsSensorsJNA extends WindowsSensors {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensorsJNA.class);

    @Override
    protected WmiResult<ValueProperty> queryOhmCpuSensor(String typeToQuery, String typeName, String sensorType,
            boolean searchCpu) {
        return getOhmSensors(typeToQuery, typeName, sensorType, (h, ohmHardware) -> {
            String cpuIdentifier = selectOhmCpuIdentifier(ohmHardware, searchCpu);
            if (!cpuIdentifier.isEmpty()) {
                return OhmSensorJNA.querySensorValue(h, cpuIdentifier, sensorType);
            }
            return null;
        });
    }

    private static WmiResult<ValueProperty> getOhmSensors(String typeToQuery, String typeName, String sensorType,
            BiFunction<WmiQueryHandler, WmiResult<IdentifierProperty>, WmiResult<ValueProperty>> querySensorFunction) {
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        WmiResult<ValueProperty> ohmSensors = null;
        try {
            comInit = h.initCOM();
            WmiResult<IdentifierProperty> ohmHardware = OhmHardwareJNA.queryHwIdentifier(h, typeToQuery, typeName);
            if (ohmHardware.getResultCount() > 0) {
                LOG.debug("Found {} data in Open Hardware Monitor", sensorType);
                ohmSensors = querySensorFunction.apply(h, ohmHardware);
            }
        } catch (COMException e) {
            LOG.warn(COM_EXCEPTION_MSG, e.getMessage());
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
        return ohmSensors;
    }

    @Override
    protected WmiResult<TemperatureProperty> queryWmiTemperature() {
        return MSAcpiThermalZoneTemperatureJNA.queryCurrentTemperature();
    }

    @Override
    protected WmiResult<SpeedProperty> queryWmiFanSpeed() {
        return Win32FanJNA.querySpeed();
    }

    @Override
    protected WmiResult<VoltProperty> queryWmiVoltage() {
        return Win32ProcessorJNA.queryVoltage();
    }
}
