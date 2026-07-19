# OSHI Benchmark Module

JMH benchmarks comparing the **JNA** (`oshi-core`) and **FFM** (`oshi-core-ffm`) implementations of OSHI side by side.

Requires **JDK 25+** (for FFM support).

## Running All Benchmarks

```sh
./oshi-benchmark/scripts/run-benchmarks.sh
```

This will build the uber-jar if needed and run all benchmarks with default JMH settings.

## Running a Specific Benchmark

Pass a regex matching the benchmark class or method name:

```sh
# Run only the CPU ticks benchmark
./oshi-benchmark/scripts/run-benchmarks.sh CpuTicksBenchmark

# Run only the memory benchmark
./oshi-benchmark/scripts/run-benchmarks.sh MemoryBenchmark
```

## Available Benchmarks

| Class | What it measures |
|-------|-----------------|
| `CpuTicksBenchmark` | `getSystemCpuLoadTicks()` — per-mode CPU time retrieval |
| `MemoryBenchmark` | `getTotal()`, `getAvailable()` — physical memory queries |
| `FileStoreBenchmark` | `getFileStores()` — filesystem enumeration and stats |
| `NetworkIFBenchmark` | `getNetworkIFs()` — network interface enumeration |
| `ProcessesBenchmark` | `getProcesses()` — process list retrieval |
| `ReuseVsRecreateBenchmark` | Reusing a held `SystemInfo` vs. constructing a new one for every poll (CPU ticks, memory, process list). Unlike the others, this measures the reuse-vs-recreate trade-off, not JNA vs. FFM; pair it with `-prof gc` for per-poll allocation. |

The non-JMH `MonitoringFootprintReport` reports the retained memory of the held object graph (the memory dimension of `ReuseVsRecreateBenchmark`, which JMH cannot measure per-operation):

```sh
java -cp oshi-benchmark/target/benchmarks.jar oshi.benchmark.MonitoringFootprintReport
```

Both feed the "CPU/memory trade-offs" section of [`PERFORMANCE.md`](../PERFORMANCE.md).

## Custom JMH Options

Any additional arguments are passed directly to JMH:

```sh
# 3 warmup iterations, 5 measurement iterations, 2 forks
./oshi-benchmark/scripts/run-benchmarks.sh -wi 3 -i 5 -f 2

# Run a specific benchmark with custom settings
./oshi-benchmark/scripts/run-benchmarks.sh CpuTicksBenchmark -wi 2 -i 10 -f 1
```

## Building Manually

```sh
./mvnw package -pl oshi-benchmark -am -DskipTests -Dshade.phase=package
java -jar oshi-benchmark/target/benchmarks.jar
```
