/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Processor;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Processor} using JNA.
 */
@ThreadSafe
public final class Win32ProcessorJNA extends Win32Processor {

    private Win32ProcessorJNA() {
    }

    /**
     * Returns processor voltage.
     *
     * @return Current voltage of the processor. If the eighth bit is set, bits 0-6 contain the voltage multiplied by
     *         10. If the eighth bit is not set, then the bit setting in VoltageCaps represents the voltage value.
     */
    public static WmiResult<VoltProperty> queryVoltage() {
        WmiQuery<VoltProperty> voltQuery = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(voltQuery);
    }

    /**
     * Returns processor ID.
     *
     * @return Processor information that describes the processor features.
     */
    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        WmiQuery<ProcessorIdProperty> idQuery = new WmiQuery<>(WIN32_PROCESSOR, ProcessorIdProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(idQuery);
    }

    /**
     * Returns address width.
     *
     * @return On a 32-bit operating system, the value is 32 and on a 64-bit operating system it is 64.
     */
    public static WmiResult<BitnessProperty> queryBitness() {
        WmiQuery<BitnessProperty> bitnessQuery = new WmiQuery<>(WIN32_PROCESSOR, BitnessProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(bitnessQuery);
    }
}
