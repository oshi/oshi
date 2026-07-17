/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.util.tuples.Pair;

/**
 * Tests the non-idle-tick and queue-length aggregation hoisted into {@link LoadAverage}.
 */
class LoadAverageTest {

    // Builds a LoadAverage whose raw-counter hooks return the supplied fixtures, so the shared aggregation can be
    // exercised without the platform-specific perfmon drivers.
    private static LoadAverage stub(Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> idleCounters,
            Map<ProcessorQueueLengthProperty, Long> queueLength) {
        return new LoadAverage() {
            @Override
            protected Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
                return idleCounters;
            }

            @Override
            protected Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
                return queueLength;
            }
        };
    }

    @Test
    void testQueryNonIdleTicksSumsTotalMinusIdle() {
        Map<IdleProcessorTimeProperty, List<Long>> valueMap = new EnumMap<>(IdleProcessorTimeProperty.class);
        valueMap.put(IdleProcessorTimeProperty.PERCENTPROCESSORTIME, Arrays.asList(1000L, 300L));
        valueMap.put(IdleProcessorTimeProperty.ELAPSEDTIME, Arrays.asList(5000L, 4000L));
        Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> counters = new Pair<>(
                Arrays.asList("_Total", "Idle"), valueMap);

        Pair<Long, Long> ticks = stub(counters, Collections.emptyMap()).queryNonIdleTicks();
        // _Total contributes ticks 1000 and base 5000; Idle subtracts its 300 ticks and leaves base untouched
        assertThat(ticks.getA(), is(700L));
        assertThat(ticks.getB(), is(5000L));
    }

    @Test
    void testQueryNonIdleTicksMissingCountersReturnsZero() {
        Pair<Long, Long> ticks = stub(new Pair<>(Collections.singletonList("_Total"),
                Collections.<IdleProcessorTimeProperty, List<Long>>emptyMap()), Collections.emptyMap())
                        .queryNonIdleTicks();
        assertThat(ticks.getA(), is(0L));
        assertThat(ticks.getB(), is(0L));
    }

    @Test
    void testQueryQueueLength() {
        assertThat(stub(null, Collections.singletonMap(ProcessorQueueLengthProperty.PROCESSORQUEUELENGTH, 3L))
                .queryQueueLength(), is(3L));
        assertThat(stub(null, Collections.emptyMap()).queryQueueLength(), is(0L));
    }
}
