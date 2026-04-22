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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import oshi.hardware.CentralProcessor;
import oshi.util.GlobalConfig;

/**
 * Side-by-side benchmarks of JNA vs FFM implementations of {@link CentralProcessor#getProcessorCpuLoadTicks()}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsPrepend = "--enable-native-access=ALL-UNNAMED")
public class CpuTicksBenchmark {

    private CentralProcessor jnaCpu;
    private CentralProcessor ffmCpu;

    @Setup
    public void setup() {
        GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 0);
        jnaCpu = new oshi.SystemInfo().getHardware().getProcessor();
        ffmCpu = new oshi.ffm.SystemInfo().getHardware().getProcessor();
    }

    @Benchmark
    public long[][] jna() {
        return jnaCpu.getProcessorCpuLoadTicks();
    }

    @Benchmark
    public long[][] ffm() {
        return ffmCpu.getProcessorCpuLoadTicks();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(CpuTicksBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
