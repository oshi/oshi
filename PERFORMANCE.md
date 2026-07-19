# OSHI Performance Considerations


## FFM vs. JNA

OSHI provides two native access implementations:

- **JNA** (`oshi-core`): Supports JDK 8+ and all platforms OSHI targets. JNA uses reflection-based marshalling for native calls, which adds overhead per invocation.
- **FFM** (`oshi-core-ffm`): Requires JDK 25+ and currently supports Linux, macOS, Windows, FreeBSD, OpenBSD, Solaris (illumos), and AIX. FFM (Foreign Function & Memory API) uses compiler-optimized stubs for native calls, reducing per-call overhead.

The tables below show approximate average times from JMH benchmarks (5 warmup iterations, 7 measurement iterations, 5 forks) captured by the manually-triggered `Benchmarks (manual)` workflow (`.github/workflows/benchmarks.yaml`) on GitHub Actions runners. The workflow is `workflow_dispatch`-only so it doesn't burn CI time on every push/PR; re-run it when adding a platform or making a perf-relevant change.

### macOS

| Benchmark  | FFM     | JNA      |
|------------|---------|----------|
| CpuTicks   | ~5 µs   | ~6 µs    |
| FileStore  | ~43 µs  | ~97 µs   |
| Memory     | ~3 µs   | ~7 µs    |
| NetworkIF  | ~18 µs  | ~115 µs  |
| Processes  | ~1.5 ms | ~10.7 ms |

macOS shows the largest FFM advantage because most system information requires native syscalls (`sysctl`, `proc_pidinfo`, etc.) where FFM's lower per-call overhead compounds across many invocations.

### Windows

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~0.24 ms | ~0.46 ms |
| FileStore  | ~32 µs   | ~117 µs  |
| Memory     | ~72 µs   | ~111 µs  |
| NetworkIF  | ~4.0 ms  | ~1.1 ms  |
| Processes  | ~8.2 ms  | ~8.8 ms  |

Windows benefits from FFM across most benchmarks. The NetworkIF anomaly (FFM slower) is due to occasional WMI query latency spikes on the CI runner.

### Linux

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~23 µs   | ~24 µs   |
| FileStore  | ~4 µs    | ~30 µs   |
| Memory     | ~83 µs   | ~82 µs   |
| NetworkIF  | ~949 µs  | ~951 µs  |
| Processes  | ~10.6 ms | ~10.9 ms |

Linux shows minimal difference between FFM and JNA for most benchmarks. This is because Linux exposes most system information through the `/proc` and `/sys` pseudo-filesystems, which OSHI reads as plain files without native calls. The FileStore benchmark is the exception, where FFM's direct `statvfs` call avoids JNA marshalling overhead.

### FreeBSD

| Benchmark  | FFM     | JNA     |
|------------|---------|---------|
| CpuTicks   | ~4 µs   | ~4 µs   |
| FileStore  | ~66 ms  | ~66 ms  |
| Memory     | ~1.6 ms | ~1.6 ms |
| NetworkIF  | ~11 ms  | ~11 ms  |
| Processes  | ~2.9 ms | ~3.0 ms |

FreeBSD, OpenBSD and Solaris all run inside a nested QEMU VM on an Ubuntu runner, so absolute numbers are noisier than the native-runner platforms above; only the relative JNA-vs-FFM comparison within a single run is meaningful. FreeBSD shows essentially no FFM/JNA gap because most data comes from `sysctl` reads that don't bottleneck on per-call overhead.

### OpenBSD

| Benchmark  | FFM     | JNA     |
|------------|---------|---------|
| CpuTicks   | ~11 µs  | ~22 µs  |
| FileStore  | ~140 ms | ~139 ms |
| Memory     | ~32 ms  | ~34 ms  |
| NetworkIF  | ~14 ms  | ~14 ms  |
| Processes  | ~364 ms | ~363 ms |

OpenBSD's `FileStore` and `Processes` numbers carry very wide error bars in the nested VM; treat them as order-of-magnitude only. Same nested-VM caveat as FreeBSD.

### Solaris (illumos)

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~16 µs   | ~1.3 ms  |
| FileStore  | ~80 ms   | ~82 ms   |
| Memory     | ~9.8 ms  | ~9.9 ms  |
| NetworkIF  | ~306 µs  | ~415 µs  |
| Processes  | ~367 ms  | ~368 ms  |

Solaris (illumos) shows the most dramatic FFM advantage on `CpuTicks` (~80x faster) because every CPU-tick read crosses a `libkstat`/`kstat2` boundary, and JNA's per-call marshalling cost dominates the workload. Same nested-VM caveat as FreeBSD.

### AIX

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~67 µs   | ~5.5 ms  |
| FileStore  | ~267 ms  | ~267 ms  |
| Memory     | ~114 µs  | ~203 µs  |
| NetworkIF  | ~49 µs   | ~341 µs  |
| Processes  | ~8.5 ms  | ~9.2 ms  |

AIX shows a Solaris-like ~80x FFM advantage on `CpuTicks`: every read traverses a `libperfstat` call, and JNA's per-call marshalling dominates against AIX's tiny native cost. `FileStore` is bottlenecked on filesystem stat() calls and is insensitive to native-binding overhead. AIX runs on cfarm119 (shared real hardware via SSH, OpenJ9 JDK 25), so numbers are more stable than the nested-VM platforms above but noisier than the dedicated GitHub Actions runners.

### Running benchmarks locally

The numbers above were collected on GitHub Actions CI runners, which may differ significantly from your hardware and workload (e.g., number of running processes, mounted file stores, or network interfaces). The `oshi-benchmark` module contains JMH benchmarks that can be built and run with JDK 25+:

```sh
./mvnw package -pl oshi-benchmark -am -DskipTests -Dshade.phase=package -q
java -jar oshi-benchmark/target/benchmarks.jar -wi 5 -i 7 -f 5
```

Run `java -jar oshi-benchmark/target/benchmarks.jar -h` for the full list of JMH options, including listing available benchmarks (`-l`) and running specific ones by regex filter.

## CPU/memory trade-offs

OSHI avoids caching large amount of information, leaving caching to the user.  Limited use of caching is employed in many classes using Memoized suppliers in instance fields, thus avoiding repeated operating system calls.

Users with memory constraints can ensure the existing cached information is disposed of by using a new instance of `SystemInfo` and the subordinate classes when collecting data.

### Reusing vs. recreating `SystemInfo` when polling

A monitoring agent that samples the same metrics repeatedly should **hold a single `SystemInfo` and its subordinate objects** rather than constructing a new `SystemInfo` for each poll. Realistic poll intervals are far longer than the sub-second memoizer TTL, so the memoizer does not help *across* polls; the trade-off is the CPU to reconstruct the object graph each poll versus the memory retained by holding it.

The dominant reconstruction cost is the `CentralProcessor` (CPU topology and identifier queries). Reading CPU load ticks once per poll, measured by `ReuseVsRecreateBenchmark` with memoization disabled (`-f 3`):

| Platform | Recreate per poll | Reuse (held) per poll | Retained if held |
|----------|------------------:|----------------------:|-----------------:|
| Windows  | 10.7 ms | 632 µs  | 3.9 KB |
| macOS    | 3.8 ms  | 2.4 µs  | 8.5 KB |
| Linux    | 2.4 ms  | 19.5 µs | 5.0 KB |

Reconstructing costs milliseconds per poll (most on Windows, where CPU identity comes from WMI/registry) for a few KB of retained memory saved — a strongly favorable trade for CPU-derived metrics. Two caveats:

- **Available memory does not benefit:** recreate ≈ reuse on all platforms (`GlobalMemory` is cheap to construct), so the win is specific to the `CentralProcessor`.
- **The process list does not benefit:** `getProcesses()` is a live full query (~10 ms and 1–15 MB allocated per poll on every platform, reused or not). To track specific processes, hold the individual `OSProcess` objects and call `updateAttributes()` rather than re-querying the whole list (see below).

Holding a `SystemInfo` and one snapshot of *every* subsystem it returns (measured by `MonitoringFootprintReport`) retains roughly **0.65 MB**, dominated by the process-list snapshot; the persistent hardware/OS caches alone are under ~150 KB. Applications with tight memory constraints can release even that by discarding the `SystemInfo` between collections, paying the reconstruction cost above.

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

On Linux, `getFileStores()` calls `statvfs()` for each mount, which can block indefinitely on a `hard`-mounted NFS filesystem whose server is unreachable (the default mount option has no timeout). To avoid this, OSHI probes each NFS server's reachability (TCP port 2049, 2-second timeout) in parallel before calling `statvfs()`, and returns a zero-valued file store for unreachable mounts rather than hanging. This adds a single, short TCP connect for reachable NFS mounts and does not affect local filesystems or `localOnly` queries. The probe can be disabled with `GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_FILESYSTEM_CHECKNFS, false);` (or the equivalent property) if you handle NFS timeouts externally.
