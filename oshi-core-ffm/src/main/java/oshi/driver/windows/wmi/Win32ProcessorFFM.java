/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Processor;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_Processor} using FFM.
 */
@ThreadSafe
public final class Win32ProcessorFFM extends Win32Processor {

    private static final String WMI_HANDLER_NULL = "WmiQueryHandlerFFM.createInstance() returned null";

    private Win32ProcessorFFM() {
    }

    public static WmiResult<VoltProperty> queryVoltage() {
        WmiQuery<VoltProperty> voltQuery = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(), WMI_HANDLER_NULL).queryWMI(voltQuery);
    }

    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        WmiQuery<ProcessorIdProperty> idQuery = new WmiQuery<>(WIN32_PROCESSOR, ProcessorIdProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(), WMI_HANDLER_NULL).queryWMI(idQuery);
    }

    public static WmiResult<BitnessProperty> queryBitness() {
        WmiQuery<BitnessProperty> bitnessQuery = new WmiQuery<>(WIN32_PROCESSOR, BitnessProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(), WMI_HANDLER_NULL).queryWMI(bitnessQuery);
    }
}
