# OSHI Benchmark Module

JMH benchmarks comparing the **JNA** (`oshi-core`) and **FFM** (`oshi-core-ffm`) implementations of OSHI side by side.

Requires **JDK 25+** (for FFM support).

## Quick Start

Each release from 7.8.1 on publishes `oshi-benchmark` as a single runnable jar holding OSHI, JNA and JMH, so nothing needs to be built. Download it from Maven Central, or from the release's assets on [GitHub](https://github.com/oshi/oshi/releases):

```sh
VERSION=7.8.1   # 7.8.1 or later
curl -LO https://repo1.maven.org/maven2/com/github/oshi/oshi-benchmark/$VERSION/oshi-benchmark-$VERSION.jar
# or: curl -LO https://github.com/oshi/oshi/releases/download/oshi-parent-$VERSION/oshi-benchmark-$VERSION.jar
```

Then run it:

```sh
java -jar oshi-benchmark-$VERSION.jar                       # run every benchmark
java -jar oshi-benchmark-$VERSION.jar -l                    # list the benchmarks
java -jar oshi-benchmark-$VERSION.jar CpuTicksBenchmark     # run those matching a regex
java -jar oshi-benchmark-$VERSION.jar -wi 3 -i 5 -f 2       # 3 warmup iterations, 5 measured, 2 forks
java -jar oshi-benchmark-$VERSION.jar -prof gc MemoryBenchmark  # add per-operation allocation
java -jar oshi-benchmark-$VERSION.jar -h                    # every JMH option
```

Any JMH option works. The benchmarks pass `--enable-native-access=ALL-UNNAMED` to each forked JVM themselves, so the command line needs no extra flags.

The non-JMH `MonitoringFootprintReport` (see [below](#available-benchmarks)) runs from the same jar, and does need the flag:

```sh
java --enable-native-access=ALL-UNNAMED -cp oshi-benchmark-$VERSION.jar oshi.benchmark.MonitoringFootprintReport
```

JNA's native library is included for every platform OSHI supports except NetBSD, where it comes from pkgsrc's `java-jna` package. Point JNA at it with `JAVA_TOOL_OPTIONS`, which also reaches the forked JVMs:

```sh
JAVA_TOOL_OPTIONS="-Djna.boot.library.path=/usr/pkg/lib" java -jar oshi-benchmark-$VERSION.jar
```

Without it, OSHI's JNA backend falls back to command-line tools on NetBSD, so its results there do not measure native JNA calls.

The jar keeps each bundled library's license text, and `META-INF/BUNDLED-LICENSES.txt` inside it says which file holds which.

## Available Benchmarks

| Class | What it measures |
|-------|-----------------|
| `CpuTicksBenchmark` | `getSystemCpuLoadTicks()` — per-mode CPU time retrieval |
| `MemoryBenchmark` | `getTotal()`, `getAvailable()` — physical memory queries |
| `FileStoreBenchmark` | `getFileStores()` — filesystem enumeration and stats |
| `NetworkIFBenchmark` | `getNetworkIFs()` — network interface enumeration |
| `ProcessesBenchmark` | `getProcesses()` — process list retrieval |
| `ReuseVsRecreateBenchmark` | Reusing a held `SystemInfo` vs. constructing a new one for every poll (CPU ticks, memory, process list). Unlike the others, this measures the reuse-vs-recreate trade-off, not JNA vs. FFM; pair it with `-prof gc` for per-poll allocation. |

The non-JMH `MonitoringFootprintReport` reports the retained memory of the held object graph (the memory dimension of `ReuseVsRecreateBenchmark`, which JMH cannot measure per-operation).

Both feed the "CPU/memory trade-offs" section of [`PERFORMANCE.md`](../PERFORMANCE.md).

## Running in CI

The manual [`Benchmarks`](../.github/workflows/benchmarks.yaml) workflow runs the published jar on Linux, macOS, Windows, FreeBSD, OpenBSD, NetBSD, AIX and OpenIndiana. Its inputs choose the version (`latest`, a release, or a snapshot of master such as `7.8.2-SNAPSHOT`), the platforms, a benchmark regex, and the JMH options for the native runners and for the slower VM jobs:

```sh
gh workflow run benchmarks.yaml                                   # latest release, every platform
gh workflow run benchmarks.yaml -f platforms=linux,macos -f benchmarks=CpuTicksBenchmark
gh workflow run benchmarks.yaml -f version=7.8.2-SNAPSHOT -f native-args="-wi 2 -i 5 -f 1"
```

## Building From Source

To benchmark changes that are not yet published, build the same jar from the repository:

```sh
./mvnw package -pl oshi-benchmark -am -DskipTests
java -jar oshi-benchmark/target/benchmarks.jar CpuTicksBenchmark
```

Or let the script build it on first use; it passes every argument to JMH:

```sh
./oshi-benchmark/scripts/run-benchmarks.sh CpuTicksBenchmark -wi 2 -i 10 -f 1
```

The script reuses an existing `target/benchmarks.jar`, so rebuild with `./mvnw` after changing code.
