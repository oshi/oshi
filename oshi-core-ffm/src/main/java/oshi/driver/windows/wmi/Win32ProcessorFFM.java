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
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_Processor} using FFM.
 */
@ThreadSafe
public final class Win32ProcessorFFM extends Win32Processor {

    private Win32ProcessorFFM() {
    }

    /**
     * Returns processor voltage.
     *
     * @return Current voltage of the processor.
     */
    public static WmiResult<VoltProperty> queryVoltage() {
        WmiQuery<VoltProperty> voltQuery = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(voltQuery);
    }

    /**
     * Returns processor ID.
     *
     * @return Processor information that describes the processor features.
     */
    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        WmiQuery<ProcessorIdProperty> idQuery = new WmiQuery<>(WIN32_PROCESSOR, ProcessorIdProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(idQuery);
    }

    /**
     * Returns address width.
     *
     * @return On a 32-bit operating system, the value is 32 and on a 64-bit operating system it is 64.
     */
    public static WmiResult<BitnessProperty> queryBitness() {
        WmiQuery<BitnessProperty> bitnessQuery = new WmiQuery<>(WIN32_PROCESSOR, BitnessProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(bitnessQuery);
    }
}
