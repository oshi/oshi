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
    protected Pair<Long, Long> queryNonIdleTicks() {
        Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> idleValues = ProcessInformationJNA
                .queryIdleProcessCounters();
        List<String> instances = idleValues.getA();
        Map<IdleProcessorTimeProperty, List<Long>> valueMap = idleValues.getB();
        List<Long> proctimeTicks = valueMap.get(IdleProcessorTimeProperty.PERCENTPROCESSORTIME);
        List<Long> proctimeBase = valueMap.get(IdleProcessorTimeProperty.ELAPSEDTIME);
        if (proctimeTicks == null || proctimeBase == null) {
            return new Pair<>(0L, 0L);
        }
        long nonIdleTicks = 0L;
        long nonIdleBase = 0L;
        for (int i = 0; i < instances.size(); i++) {
            if (i >= proctimeTicks.size() || i >= proctimeBase.size()) {
                break;
            }
            if ("_Total".equals(instances.get(i))) {
                nonIdleTicks += proctimeTicks.get(i);
                nonIdleBase += proctimeBase.get(i);
            } else if ("Idle".equals(instances.get(i))) {
                nonIdleTicks -= proctimeTicks.get(i);
            }
        }
        return new Pair<>(nonIdleTicks, nonIdleBase);
    }

    @Override
    protected long queryQueueLength() {
        return SystemInformationJNA.queryProcessorQueueLength()
                .getOrDefault(ProcessorQueueLengthProperty.PROCESSORQUEUELENGTH, 0L);
    }
}
