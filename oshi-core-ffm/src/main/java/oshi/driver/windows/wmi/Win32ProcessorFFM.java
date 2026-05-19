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
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32ProcessorFFM extends Win32Processor {
    private Win32ProcessorFFM() {
    }

    public static WmiResult<VoltProperty> queryVoltage() {
        return Win32Processor.queryVoltage(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }

    public static WmiResult<ProcessorIdProperty> queryProcessorId() {
        return Win32Processor.queryProcessorId(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }

    public static WmiResult<BitnessProperty> queryBitness() {
        return Win32Processor.queryBitness(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
