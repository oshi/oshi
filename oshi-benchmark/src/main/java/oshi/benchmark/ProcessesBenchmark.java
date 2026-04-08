/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.benchmark;

import java.util.List;
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

import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Side-by-side benchmarks of JNA vs FFM implementations of {@link OperatingSystem#getProcesses()}.
 *
 * <p>
 * Run via the fat jar:
 *
 * <pre>
 *   java -jar oshi-benchmark/target/benchmarks.jar
 * </pre>
 *
 * Or just this benchmark:
 *
 * <pre>
 *   java -jar oshi-benchmark/target/benchmarks.jar ProcessesBenchmark
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsPrepend = "--enable-native-access=ALL-UNNAMED")
public class ProcessesBenchmark {

    private OperatingSystem jnaOs;
    private OperatingSystem ffmOs;

    @Setup
    public void setup() {
        jnaOs = new oshi.SystemInfo().getOperatingSystem();
        ffmOs = new oshi.ffm.SystemInfo().getOperatingSystem();
    }

    @Benchmark
    public List<OSProcess> jna() {
        return jnaOs.getProcesses();
    }

    @Benchmark
    public List<OSProcess> ffm() {
        return ffmOs.getProcesses();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(ProcessesBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
