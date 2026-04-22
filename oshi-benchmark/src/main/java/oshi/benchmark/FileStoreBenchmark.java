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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;

/**
 * Side-by-side benchmarks of JNA vs FFM implementations of {@link OSFileStore#updateAttributes()}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsPrepend = "--enable-native-access=ALL-UNNAMED")
public class FileStoreBenchmark {

    private List<OSFileStore> jnaStores;
    private List<OSFileStore> ffmStores;

    @Setup
    public void setup() {
        GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 0);
        OperatingSystem jnaOs = new oshi.SystemInfo().getOperatingSystem();
        OperatingSystem ffmOs = new oshi.ffm.SystemInfo().getOperatingSystem();
        jnaStores = jnaOs.getFileSystem().getFileStores();
        ffmStores = ffmOs.getFileSystem().getFileStores();
    }

    @Benchmark
    public void jna(Blackhole bh) {
        for (OSFileStore store : jnaStores) {
            bh.consume(store.updateAttributes());
        }
    }

    @Benchmark
    public void ffm(Blackhole bh) {
        for (OSFileStore store : ffmStores) {
            bh.consume(store.updateAttributes());
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(FileStoreBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
