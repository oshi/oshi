/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;

/**
 * Reports the memory dimension of the reuse-vs-recreate trade-off (see {@link ReuseVsRecreateBenchmark} for the CPU
 * dimension): for each metric a monitoring agent polls, the retained size of the held object graph (what a reused
 * {@link SystemInfo} keeps alive and does not release to GC) and the size of one poll's result.
 *
 * <p>
 * This is a rough heap-differencing measurement (hold N copies, force GC, divide), not a precise object-graph walk, but
 * it is stable enough to show the order-of-magnitude contrast documented in {@code PERFORMANCE.md}. It is a plain
 * {@code main} rather than a JMH benchmark because retained footprint is not a per-operation quantity.
 *
 * <pre>
 *   java -cp oshi-benchmark/target/benchmarks.jar oshi.benchmark.MonitoringFootprintReport
 * </pre>
 */
public final class MonitoringFootprintReport {

    private static final int WARMUP = 20;
    private static final int GRAPH_COPIES = 50;
    private static final int RESULT_COPIES = 20;

    private MonitoringFootprintReport() {
    }

    private static long usedBytes() {
        Runtime r = Runtime.getRuntime();
        long used = Long.MAX_VALUE;
        // Settle: GC repeatedly until the used-memory reading stops shrinking
        for (int i = 0; i < 8; i++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            used = Math.min(used, r.totalMemory() - r.freeMemory());
        }
        return used;
    }

    // Retained bytes of `copies` reused SystemInfo instances, each with the monitored subsystem realized
    private static double retainedGraphKb(Consumer<SystemInfo> realize) {
        SystemInfo[] hold = new SystemInfo[GRAPH_COPIES];
        long before = usedBytes();
        for (int i = 0; i < GRAPH_COPIES; i++) {
            hold[i] = new SystemInfo();
            realize.accept(hold[i]);
        }
        long after = usedBytes();
        if (hold[GRAPH_COPIES - 1] == null) {
            throw new IllegalStateException();
        }
        return (after - before) / 1024.0 / GRAPH_COPIES;
    }

    // Retained bytes of `copies` poll results held from a single reused SystemInfo
    private static double resultKb(Function<SystemInfo, Object> read) {
        SystemInfo si = new SystemInfo();
        Object[] hold = new Object[RESULT_COPIES];
        long before = usedBytes();
        for (int i = 0; i < RESULT_COPIES; i++) {
            hold[i] = read.apply(si);
        }
        long after = usedBytes();
        if (hold[RESULT_COPIES - 1] == null) {
            throw new IllegalStateException();
        }
        return (after - before) / 1024.0 / RESULT_COPIES;
    }

    /**
     * Prints the footprint table.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // Match the benchmark: realistic polling always misses the sub-second memoizer TTL.
        GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 0);

        record Metric(String name, Function<SystemInfo, Object> read) {
            Consumer<SystemInfo> realize() {
                return read::apply;
            }
        }
        Metric[] metrics = { new Metric("cpuTicks", si -> si.getHardware().getProcessor().getSystemCpuLoadTicks()),
                new Metric("memAvailable", si -> si.getHardware().getMemory().getAvailable()),
                new Metric("processes", si -> si.getOperatingSystem().getProcesses()) };

        // Warm up class loading, native libraries, and JIT before measuring (see PERFORMANCE.md cold-start note)
        for (int i = 0; i < WARMUP; i++) {
            for (Metric m : metrics) {
                m.read().apply(new SystemInfo());
            }
        }

        System.out.printf("%-14s %22s %18s%n", "metric", "retained held-graph(KB)", "poll result(KB)");
        System.out.printf("%-14s %22s %18s%n", "------", "----------------------", "---------------");
        for (Metric m : metrics) {
            double graph = retainedGraphKb(m.realize());
            double result = resultKb(m.read());
            System.out.printf("%-14s %22.1f %18.1f%n", m.name(), graph, result);
        }

        double all = cacheEverythingKb();
        System.out.printf("%nHolding everything a SystemInfo returns (all subsystems + one snapshot each): %.0f KB%n",
                all);
    }

    // Retained bytes when holding a SystemInfo and one snapshot of every subsystem it exposes (upper bound of "cache
    // everything"); dominated by the process-list snapshot.
    private static double cacheEverythingKb() {
        List<Object> hold = new ArrayList<>();
        long before = usedBytes();
        for (int i = 0; i < GRAPH_COPIES; i++) {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hw = si.getHardware();
            OperatingSystem os = si.getOperatingSystem();
            hold.add(si);
            hold.add(hw.getComputerSystem());
            hold.add(hw.getProcessor().getSystemCpuLoadTicks());
            hold.add(hw.getMemory());
            hold.add(hw.getSensors());
            hold.add(hw.getPowerSources());
            hold.add(hw.getDisplays());
            hold.add(hw.getDiskStores());
            hold.add(hw.getUsbDevices(true));
            hold.add(hw.getSoundCards());
            hold.add(hw.getGraphicsCards());
            hold.add(hw.getNetworkIFs());
            hold.add(os.getFileSystem().getFileStores());
            hold.add(os.getNetworkParams());
            hold.add(os.getInternetProtocolStats());
            hold.add(os.getServices());
            hold.add(os.getSessions());
            hold.add(os.getProcesses());
        }
        long after = usedBytes();
        if (hold.isEmpty()) {
            throw new IllegalStateException();
        }
        return (after - before) / 1024.0 / GRAPH_COPIES;
    }
}
