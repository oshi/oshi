# OSHI Performance Considerations


## FFM vs. JNA

OSHI provides two native access implementations:

- **JNA** (`oshi-core`): Supports JDK 8+ and all platforms OSHI targets. JNA uses reflection-based marshalling for native calls, which adds overhead per invocation.
- **FFM** (`oshi-core-ffm`): Requires JDK 25+ and currently supports Linux, macOS, Windows, FreeBSD, OpenBSD, and Solaris (illumos). FFM (Foreign Function & Memory API) uses compiler-optimized stubs for native calls, reducing per-call overhead.

The tables below show approximate average times from JMH benchmarks (1 warmup iteration, 3 measurement iterations) captured by the manually-triggered `Benchmarks (manual)` workflow (`.github/workflows/benchmarks.yaml`) on GitHub Actions runners. The workflow is `workflow_dispatch`-only so it doesn't burn CI time on every push/PR; re-run it when adding a platform or making a perf-relevant change.

### macOS

| Benchmark  | FFM     | JNA      |
|------------|---------|----------|
| CpuTicks   | ~5 µs   | ~9 µs    |
| FileStore  | ~45 µs  | ~104 µs  |
| Memory     | ~4 µs   | ~8 µs    |
| NetworkIF  | ~20 µs  | ~128 µs  |
| Processes  | ~1.6 ms | ~11 ms   |

macOS shows the largest FFM advantage because most system information requires native syscalls (`sysctl`, `proc_pidinfo`, etc.) where FFM's lower per-call overhead compounds across many invocations.

### Windows

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~0.22 ms | ~0.40 ms |
| FileStore  | ~26 µs   | ~101 µs  |
| Memory     | ~59 µs   | ~89 µs   |
| NetworkIF  | ~479 µs  | ~862 µs  |
| Processes  | ~6.0 ms  | ~7.6 ms  |

Windows benefits from FFM across all benchmarks.

### Linux

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~25 µs   | ~24 µs   |
| FileStore  | ~4 µs    | ~31 µs   |
| Memory     | ~85 µs   | ~86 µs   |
| NetworkIF  | ~833 µs  | ~842 µs  |
| Processes  | ~11 ms   | ~11 ms   |

Linux shows minimal difference between FFM and JNA for most benchmarks. This is because Linux exposes most system information through the `/proc` and `/sys` pseudo-filesystems, which OSHI reads as plain files without native calls. The FileStore benchmark is the exception, where FFM's direct `statvfs` call avoids JNA marshalling overhead.

### FreeBSD

| Benchmark  | FFM     | JNA     |
|------------|---------|---------|
| CpuTicks   | ~3 µs   | ~4 µs   |
| FileStore  | ~68 ms  | ~68 ms  |
| Memory     | ~1.7 ms | ~1.7 ms |
| NetworkIF  | ~7 ms   | ~8 ms   |
| Processes  | ~3.1 ms | ~3.0 ms |

FreeBSD, OpenBSD and Solaris all run inside a nested QEMU VM on an Ubuntu runner, so absolute numbers are noisier than the native-runner platforms above; only the relative JNA-vs-FFM comparison within a single run is meaningful. FreeBSD shows essentially no FFM/JNA gap because most data comes from `sysctl` reads that don't bottleneck on per-call overhead.

### OpenBSD

| Benchmark  | FFM     | JNA     |
|------------|---------|---------|
| CpuTicks   | ~12 µs  | ~22 µs  |
| FileStore  | ~182 ms | ~179 ms |
| Memory     | ~28 ms  | ~30 ms  |
| NetworkIF  | ~13 ms  | ~14 ms  |
| Processes  | ~366 ms | ~350 ms |

OpenBSD's `FileStore` and `Processes` numbers carry very wide error bars in the nested VM; treat them as order-of-magnitude only. Same nested-VM caveat as FreeBSD.

### Solaris (illumos)

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~16 µs   | ~1.4 ms  |
| FileStore  | ~85 ms   | ~83 ms   |
| Memory     | ~10 ms   | ~10 ms   |
| NetworkIF  | ~308 µs  | ~415 µs  |
| Processes  | ~384 ms  | ~381 ms  |

Solaris (illumos) shows the most dramatic FFM advantage on `CpuTicks` (~85x faster) because every CPU-tick read crosses a `libkstat`/`kstat2` boundary, and JNA's per-call marshalling cost dominates the workload. Same nested-VM caveat as FreeBSD.

### Running benchmarks locally

The numbers above were collected on GitHub Actions CI runners, which may differ significantly from your hardware and workload (e.g., number of running processes, mounted file stores, or network interfaces). The `oshi-benchmark` module contains JMH benchmarks that can be built and run with JDK 25+:

```sh
./mvnw package -pl oshi-benchmark -am -DskipTests -Dshade.phase=package -q
java -jar oshi-benchmark/target/benchmarks.jar -wi 1 -i 3 -f 1
```

Run `java -jar oshi-benchmark/target/benchmarks.jar -h` for the full list of JMH options, including listing available benchmarks (`-l`) and running specific ones by regex filter.

## CPU/memory trade-offs

OSHI avoids caching large amount of information, leaving caching to the user.  Limited use of caching is employed in many classes using Memoized suppliers in instance fields, thus avoiding repeated operating system calls.

Users with memory constraints can ensure the existing cached information is disposed of by using a new instance of `SystemInfo` and the subordinate classes when collecting data.

## Updating statistics on objects in a list

Many of the individual objects returned by lists, such as `OSProcess`, `NetworkIF`, `OSFileStore`, and others, have an `updateAttributes()` method that operates only on that object. These are intended for use primarily if that individual process is the only one being monitored/updated.  In many cases, the entire list must be queried to provide the information, so users updating multiple objects in a list should simply re-query the entire list once and then correlate the new set of results to the old ones in their own application.

## Windows performance counters

OSHI attempts to read process and thread information from the registry base key HKEY_PERFORMANCE_DATA in preference to performance counters for performance reasons. This approach may cause problems with localization and can be disabled in the configuration file, or by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_HKEYPERFDATA, false);` shortly after startup (at least before querying process lists).

Some users may choose to disable performance counters in their registry for performance reasons (e.g., gaming). Log warnings when detecting these disabled counters can be confusing. If your application does not use any information from these classes of performance counters, you can suppress these log warnings by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PERFOS_DISABLED, true);`, `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PERFPROC_DISABLED, true);`, and/or `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PERFDISK_DISABLED, true);`.

## Windows process command lines

On Windows, process command lines are only available in WMI and require a significant WMI overhead unless OSHI is running with elevated
permissions or the process is owned by the same user.

By default, command lines are not pre-fetched in `OSProcess` objects, and are populated by a single query for each process, assuming command line queries will be done ad-hoc by a user.

If your application requires updating more than a few command lines owned by other users, CPU performance can be significantly improved by fetching and caching the entire list of command line results. This must be enabled in the configuration file, or by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_COMMANDLINE_BATCH, true);` shortly after startup (at least before the first command line query).

## Windows suspended processes

On Windows, determining if a process is suspended requires querying the state of its threads.

By default, all Windows processes are presented as "Running". If your application requires knowing which processes are suspended, collecting the thread details works best for ad hoc requests. To query all processes, it is more efficient (but still slow) to query thread performance counters. This must be enabled in the configuration file, or by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, true);` shortly after startup (at least before the first instantiation of the Operating System class).

## Windows WMI query COM initialization

Because OSHI does not know whether COM has been initialized externally, each WMI query requires initializing and uninitializing COM.  Each WMI query's response time can be improved by about 3 to 5 milliseconds by having your application do the necessary COM initialization and extending OSHI's `WmiQueryHandler` with a version that omits that overhead.  The `UserComInit` and `WmiNoComInitQueryHandler` classes in the `oshi-demo` artifact demonstrate this capability, and can be further customized.

## Physical device latency

Updates of statistics on disks, filestores, USB devices, and some network information may incur physical device or network latency and respond more slowly than other native calls.  Periodic polling for updates should generally be less frequent than OS-kernel based statistics such as CPU and memory. Also see the discussion above regarding disk performance counters; if only unchanging disk statistics are required then disabling the querying of PerfDisk counters will slightly improve performance.
