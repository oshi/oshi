/*
 * Copyright 2022-2026 The OSHI Project Contributors
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
 * JNA implementation of {@link LoadAverage} using JNA-based perfmon drivers.
 */
@ThreadSafe
public final class LoadAverageJNA extends LoadAverage {

    private static final LoadAverageJNA INSTANCE = new LoadAverageJNA();

    private LoadAverageJNA() {
    }

    public static LoadAverageJNA getInstance() {
        return INSTANCE;
    }

    @Override
    protected Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        return ProcessInformationJNA.queryIdleProcessCounters();
    }

    @Override
    protected Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        return SystemInformationJNA.queryProcessorQueueLength();
    }
}
