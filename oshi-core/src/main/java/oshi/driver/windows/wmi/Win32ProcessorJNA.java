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

    private static final String WMI_HANDLER_NULL = "WmiQueryHandler.createInstance() returned null";

    private Win32ProcessorJNA() {
    }

    public static WmiResult<VoltProperty> queryVoltage() {
        WmiQuery<VoltProperty> voltQuery = new WmiQuery<>(WIN32_PROCESSOR, VoltProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance(), WMI_HANDLER_NULL).queryWMI(voltQuery);
    }

    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        WmiQuery<ProcessorIdProperty> idQuery = new WmiQuery<>(WIN32_PROCESSOR, ProcessorIdProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance(), WMI_HANDLER_NULL).queryWMI(idQuery);
    }

    public static WmiResult<BitnessProperty> queryBitness() {
        WmiQuery<BitnessProperty> bitnessQuery = new WmiQuery<>(WIN32_PROCESSOR, BitnessProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance(), WMI_HANDLER_NULL).queryWMI(bitnessQuery);
    }
}
