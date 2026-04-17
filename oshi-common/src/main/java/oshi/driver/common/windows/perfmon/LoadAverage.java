/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import java.util.Arrays;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;

/**
 * Utility to calculate a load average equivalent metric on Windows. Starts a daemon thread to collect the necessary
 * counters and averages in 5-second intervals. Subclasses provide the platform-specific perfmon queries.
 */
@ThreadSafe
public abstract class LoadAverage {

    // Daemon thread for Load Average
    private Thread loadAvgThread = null;

    private final double[] loadAverages = new double[] { -1d, -1d, -1d };
    private static final double[] EXP_WEIGHT = new double[] {
            // 1-, 5-, and 15-minute exponential smoothing weight
            Math.exp(-5d / 60d), Math.exp(-5d / 300d), Math.exp(-5d / 900d) };

    /**
     * Query the non-idle ticks from performance counters.
     *
     * @return A pair of (nonIdleTicks, nonIdleBase) values
     */
    protected abstract Pair<Long, Long> queryNonIdleTicks();

    /**
     * Query the processor queue length from performance counters.
     *
     * @return The processor queue length
     */
    protected abstract long queryQueueLength();

    public double[] queryLoadAverage(int nelem) {
        synchronized (loadAverages) {
            return Arrays.copyOf(loadAverages, nelem);
        }
    }

    public synchronized void stopDaemon() {
        if (loadAvgThread != null) {
            loadAvgThread.interrupt();
            loadAvgThread = null;
        }
    }

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
