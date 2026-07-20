/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import oshi.SystemInfo;
import oshi.util.GlobalConfig;

/**
 * Measures the CPU cost of reusing a {@link SystemInfo} (holding the constructed object graph) versus creating a new
 * one for every poll, for the metrics a monitoring agent samples repeatedly (CPU ticks, available memory, process
 * list).
 *
 * <p>
 * The memoizer is <strong>disabled</strong> ({@code oshi.util.memoizer.expiration = 0}) in {@link #setup()} because
 * real monitoring polls (e.g. once per second) always fall outside the memoizer's sub-second TTL — so every poll is a
 * cache miss, and the honest question is the cost of <em>reconstructing the objects</em>, not of a cache hit.
 *
 * <p>
 * Pair this with {@code -prof gc} (allocation per poll) and {@link MonitoringFootprintReport} (retained memory of the
 * held object graph) for the full CPU-vs-memory picture documented in {@code PERFORMANCE.md}.
 *
 * <pre>
 *   java -jar oshi-benchmark/target/benchmarks.jar ReuseVsRecreateBenchmark -prof gc
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsPrepend = "--enable-native-access=ALL-UNNAMED")
public class ReuseVsRecreateBenchmark {

    /** The monitored metric to sample. */
    @Param({ "cpuTicks", "memAvailable", "processes" })
    public String metric;

    private SystemInfo reused;

    /** Creates a new benchmark instance. Required by JMH for {@code @State} classes. */
    public ReuseVsRecreateBenchmark() {
    }

    /**
     * Disables memoization (so each poll is a real query, as it would be at realistic poll intervals) and builds the
     * reusable {@link SystemInfo}, realizing the subsystem under test.
     */
    @Setup
    public void setup() {
        GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 0);
        reused = new SystemInfo();
        read(reused);
    }

    private Object read(SystemInfo si) {
        switch (metric) {
            case "cpuTicks":
                return si.getHardware().getProcessor().getSystemCpuLoadTicks();
            case "memAvailable":
                return si.getHardware().getMemory().getAvailable();
            case "processes":
                return si.getOperatingSystem().getProcesses();
            default:
                throw new IllegalArgumentException("Unknown metric: " + metric);
        }
    }

    /**
     * Polls the metric from a reused {@link SystemInfo}, so the constructed object graph is not rebuilt.
     *
     * @return the metric result
     */
    @Benchmark
    public Object reuse() {
        return read(reused);
    }

    /**
     * Polls the metric from a freshly constructed {@link SystemInfo}, rebuilding the object graph on every poll.
     *
     * @return the metric result
     */
    @Benchmark
    public Object recreate() {
        return read(new SystemInfo());
    }

    /**
     * Standalone entry point.
     *
     * @param args unused
     * @throws RunnerException if the benchmark fails to run
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(ReuseVsRecreateBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
