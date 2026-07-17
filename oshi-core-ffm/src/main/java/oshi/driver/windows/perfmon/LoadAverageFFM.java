/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.LoadAverage;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.util.tuples.Pair;

/**
 * FFM implementation of {@link LoadAverage} using FFM-based perfmon drivers.
 */
@ThreadSafe
public final class LoadAverageFFM extends LoadAverage {

    private static final LoadAverageFFM INSTANCE = new LoadAverageFFM();

    private LoadAverageFFM() {
    }

    public static LoadAverageFFM getInstance() {
        return INSTANCE;
    }

    @Override
    protected Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        return ProcessInformationFFM.queryIdleProcessCounters();
    }

    @Override
    protected Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        return SystemInformationFFM.queryProcessorQueueLength();
    }
}
