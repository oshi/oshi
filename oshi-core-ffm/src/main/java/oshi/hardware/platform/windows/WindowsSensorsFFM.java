/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

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
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperatureFFM;
import oshi.driver.windows.wmi.OhmHardwareFFM;
import oshi.driver.windows.wmi.OhmSensorFFM;
import oshi.driver.windows.wmi.Win32FanFFM;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.ffm.platform.windows.com.FfmComException;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;
import oshi.hardware.common.platform.windows.WindowsSensors;

/**
 * FFM-backed Windows Sensors from WMI or Open Hardware Monitor or Libre Hardware Monitor.
 */
@ThreadSafe
final class WindowsSensorsFFM extends WindowsSensors {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsSensorsFFM.class);

    @Override
    protected WmiResult<ValueProperty> queryOhmCpuSensor(String typeToQuery, String typeName, String sensorType,
            boolean searchCpu) {
        return getOhmSensors(typeToQuery, typeName, sensorType, (h, ohmHardware) -> {
            String cpuIdentifier = selectOhmCpuIdentifier(ohmHardware, searchCpu);
            if (!cpuIdentifier.isEmpty()) {
                return OhmSensorFFM.querySensorValue(h, cpuIdentifier, sensorType);
            }
            return null;
        });
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

    @Override
    protected WmiResult<TemperatureProperty> queryWmiTemperature() {
        return MSAcpiThermalZoneTemperatureFFM.queryCurrentTemperature();
    }

    @Override
    protected WmiResult<SpeedProperty> queryWmiFanSpeed() {
        return Win32FanFFM.querySpeed();
    }

    @Override
    protected WmiResult<VoltProperty> queryWmiVoltage() {
        return Win32ProcessorFFM.queryVoltage();
    }
}
