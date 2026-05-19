/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Processor;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32ProcessorJNA extends Win32Processor {
    private Win32ProcessorJNA() {
    }

    public static WmiResult<VoltProperty> queryVoltage() {
        return Win32Processor.queryVoltage(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }

    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        return Win32Processor.queryProcessorId(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }

    public static WmiResult<BitnessProperty> queryBitness() {
        return Win32Processor.queryBitness(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
