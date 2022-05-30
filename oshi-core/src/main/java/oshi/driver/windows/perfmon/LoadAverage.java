/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.windows.perfmon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to calculate a load average equivalent metric on Windows. Starts a
 * daemon thread to collect the necessary counters and averages in 5-second
 * intervals.
 */
@ThreadSafe
public final class LoadAverage {

    // C start a daemon thread for Load Average
    private static Thread loadAvgThread = null;
    private static double[] loadAverages = new double[] { -1d, -1d, -1d };
    private static final double[] LOADAVERAGE_WEIGHT = new double[] { 11d / 12d, 59d / 60d, 179d / 180d };

    private LoadAverage() {
    }

    public static double[] queryLoadAverage(int nelem) {
        synchronized (loadAverages) {
            return Arrays.copyOf(loadAverages, nelem);
        }
    }

    public static synchronized void stopDaemon() {
        if (loadAvgThread != null) {
            loadAvgThread.interrupt();
            loadAvgThread = null;
        }
    }

    public static synchronized void startDaemon() {
        if (loadAvgThread != null) {
            return;
        }
        loadAvgThread = new Thread(null, null, "OSHI Load Average daemon") {
            @Override
            public void run() {
                // Initialize tick counters
                Pair<Long, Long> nonIdlePair = LoadAverage.queryNonIdleTicks();
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
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                while (!Thread.currentThread().isInterrupted()) {
                    // get non-idle ticks, proxy for average processes running
                    nonIdlePair = LoadAverage.queryNonIdleTicks();
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
                    queueLength = SystemInformation.queryProcessorQueueLength()
                            .getOrDefault(ProcessorQueueLengthProperty.PROCESSORQUEUELENGTH, 0L);

                    synchronized (loadAverages) {
                        // Init to running procs the first time
                        if (loadAverages[0] < 0d) {
                            Arrays.fill(loadAverages, runningProcesses);
                        }
                        // Use exponential smoothing to update values
                        for (int i = 0; i < loadAverages.length; i++) {
                            loadAverages[i] *= LOADAVERAGE_WEIGHT[i];
                            loadAverages[i] += (runningProcesses + queueLength) * (1d - LOADAVERAGE_WEIGHT[i]);
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

    private static Pair<Long, Long> queryNonIdleTicks() {
        Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> idleValues = ProcessInformation
                .queryIdleProcessCounters();
        List<String> instances = idleValues.getA();
        Map<IdleProcessorTimeProperty, List<Long>> valueMap = idleValues.getB();
        List<Long> proctimeTicks = valueMap.get(IdleProcessorTimeProperty.PERCENTPROCESSORTIME);
        List<Long> proctimeBase = valueMap.get(IdleProcessorTimeProperty.ELAPSEDTIME);
        long nonIdleTicks = 0L;
        long nonIdleBase = 0L;
        for (int i = 0; i < instances.size(); i++) {
            if ("_Total".equals(instances.get(i))) {
                nonIdleTicks += proctimeTicks.get(i);
                nonIdleBase += proctimeBase.get(i);
            } else if ("Idle".equals(instances.get(i))) {
                nonIdleTicks -= proctimeTicks.get(i);
            }
        }
        return new Pair<>(nonIdleTicks, nonIdleBase);
    }
}
