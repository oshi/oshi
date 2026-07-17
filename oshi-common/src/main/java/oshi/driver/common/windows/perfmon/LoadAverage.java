/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to calculate a load average equivalent metric on Windows. Starts a daemon thread to collect the necessary
 * counters and averages in 5-second intervals. Subclasses provide the platform-specific perfmon queries.
 */
@ThreadSafe
public abstract class LoadAverage {

    /**
     * Default constructor.
     */
    protected LoadAverage() {
    }

    // Daemon thread for Load Average
    private Thread loadAvgThread = null;

    private final double[] loadAverages = new double[] { -1d, -1d, -1d };
    private static final double[] EXP_WEIGHT = new double[] {
            // 1-, 5-, and 15-minute exponential smoothing weight
            Math.exp(-5d / 60d), Math.exp(-5d / 300d), Math.exp(-5d / 900d) };

    /**
     * Queries the raw idle-process performance counters. Implemented per-backend (JNA/FFM); the aggregation into
     * non-idle ticks is shared in {@link #queryNonIdleTicks()}.
     *
     * @return a pair of (instance names, counter values keyed by property)
     */
    protected abstract Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters();

    /**
     * Queries the raw processor-queue-length performance counter. Implemented per-backend (JNA/FFM).
     *
     * @return the counter values keyed by property
     */
    protected abstract Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength();

    /**
     * Computes the non-idle ticks from the idle-process performance counters by summing the {@code _Total} instance and
     * subtracting the {@code Idle} instance.
     *
     * @return A pair of (nonIdleTicks, nonIdleBase) values
     */
    protected Pair<Long, Long> queryNonIdleTicks() {
        Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> idleValues = queryIdleProcessCounters();
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

    /**
     * Returns the processor queue length from the raw performance counter.
     *
     * @return The processor queue length
     */
    protected long queryQueueLength() {
        return queryProcessorQueueLength().getOrDefault(ProcessorQueueLengthProperty.PROCESSORQUEUELENGTH, 0L);
    }

    /**
     * Queries the load average values.
     *
     * @param nelem number of elements to return (1, 2, or 3)
     * @return array of load averages
     */
    public double[] queryLoadAverage(int nelem) {
        synchronized (loadAverages) {
            return Arrays.copyOf(loadAverages, nelem);
        }
    }

    /**
     * Stops the load average daemon thread.
     */
    public synchronized void stopDaemon() {
        if (loadAvgThread != null) {
            loadAvgThread.interrupt();
            loadAvgThread = null;
        }
    }

    /**
     * Starts the load average daemon thread.
     */
    public synchronized void startDaemon() {
        if (loadAvgThread != null) {
            return;
        }
        loadAvgThread = new Thread("OSHI Load Average daemon") {
            @Override
            public void run() {
                // Initialize tick counters
                Pair<Long, Long> nonIdlePair = queryNonIdleTicks();
                long nonIdleTicks0 = nonIdlePair.getA();
                long nonIdleBase0 = nonIdlePair.getB();
                long nonIdleTicks;
                long nonIdleBase;

                // Use nanoTime to synchronize queries at 5 seconds
                long initNanos = System.nanoTime();
                long delay;

                // The two components of load average
                double runningProcesses;
                long queueLength;

                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                while (!Thread.currentThread().isInterrupted()) {
                    // get non-idle ticks, proxy for average processes running
                    nonIdlePair = queryNonIdleTicks();
                    nonIdleTicks = nonIdlePair.getA() - nonIdleTicks0;
                    nonIdleBase = nonIdlePair.getB() - nonIdleBase0;
                    if (nonIdleBase > 0 && nonIdleTicks > 0) {
                        runningProcesses = (double) nonIdleTicks / nonIdleBase;
                    } else {
                        runningProcesses = 0d;
                    }
                    nonIdleTicks0 = nonIdlePair.getA();
                    nonIdleBase0 = nonIdlePair.getB();
                    // get processes waiting
                    queueLength = queryQueueLength();

                    synchronized (loadAverages) {
                        // Init to running procs the first time
                        if (loadAverages[0] < 0d) {
                            Arrays.fill(loadAverages, runningProcesses);
                        }
                        // Use exponential smoothing to update values
                        for (int i = 0; i < loadAverages.length; i++) {
                            loadAverages[i] *= EXP_WEIGHT[i];
                            loadAverages[i] += (runningProcesses + queueLength) * (1d - EXP_WEIGHT[i]);
                        }
                    }

                    delay = 5000L - (System.nanoTime() - initNanos) % 5_000_000_000L / 1_000_000;
                    if (delay < 500L) {
                        delay += 5000L;
                    }
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        loadAvgThread.setDaemon(true);
        loadAvgThread.start();
    }
}
