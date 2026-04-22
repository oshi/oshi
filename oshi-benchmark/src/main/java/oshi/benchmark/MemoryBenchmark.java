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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import oshi.hardware.GlobalMemory;
import oshi.hardware.VirtualMemory;
import oshi.util.GlobalConfig;

/**
 * Side-by-side benchmarks of JNA vs FFM implementations of {@link GlobalMemory} and {@link VirtualMemory} queries.
 * <p>
 * Only benchmarks methods that make native calls on each invocation: {@link GlobalMemory#getAvailable()},
 * {@link VirtualMemory#getSwapUsed()}, and {@link VirtualMemory#getSwapTotal()}. Static values like
 * {@link GlobalMemory#getTotal()} and {@link GlobalMemory#getPageSize()} are permanently cached and excluded.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsPrepend = "--enable-native-access=ALL-UNNAMED")
public class MemoryBenchmark {

    private GlobalMemory jnaMem;
    private GlobalMemory ffmMem;
    private VirtualMemory jnaVm;
    private VirtualMemory ffmVm;

    @Setup
    public void setup() {
        GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 0);
        jnaMem = new oshi.SystemInfo().getHardware().getMemory();
        ffmMem = new oshi.ffm.SystemInfo().getHardware().getMemory();
        jnaVm = jnaMem.getVirtualMemory();
        ffmVm = ffmMem.getVirtualMemory();
    }

    @Benchmark
    public void jna(Blackhole bh) {
        bh.consume(jnaMem.getAvailable());
        bh.consume(jnaVm.getSwapTotal());
        bh.consume(jnaVm.getSwapUsed());
    }

    @Benchmark
    public void ffm(Blackhole bh) {
        bh.consume(ffmMem.getAvailable());
        bh.consume(ffmVm.getSwapTotal());
        bh.consume(ffmVm.getSwapUsed());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(MemoryBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
