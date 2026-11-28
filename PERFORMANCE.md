# OSHI Performance Considerations


## FFM vs. JNA

OSHI provides two native access implementations:

- **JNA** (`oshi-core`): Supports JDK 8+ and all platforms OSHI targets. JNA uses reflection-based marshalling for native calls, which adds overhead per invocation.
- **FFM** (`oshi-core-ffm`): Requires JDK 25+ and supports the same platforms as the JNA implementation. FFM (Foreign Function & Memory API) uses compiler-optimized stubs for native calls, reducing per-call overhead.

The tables below show approximate average times from JMH benchmarks captured by the manually-triggered `Benchmarks (manual)` workflow (`.github/workflows/benchmarks.yaml`), which runs the published `oshi-benchmark` jar on GitHub Actions runners (5 warmup iterations, 10 measurement iterations, 3 forks), in nested VMs, and on a shared AIX host (1 warmup iteration, 3 measurement iterations, 1 fork). The workflow is `workflow_dispatch`-only so it doesn't burn CI time on every push/PR; re-run it when adding a platform or making a perf-relevant change. See [Running benchmarks yourself](#running-benchmarks-yourself) to measure your own hardware.

### macOS

| Benchmark  | FFM     | JNA      |
|------------|---------|----------|
| CpuTicks   | ~7 µs   | ~10 µs   |
| FileStore  | ~54 µs  | ~110 µs  |
| Memory     | ~3.6 µs | ~8.6 µs  |
| NetworkIF  | ~38 µs  | ~203 µs  |
| Processes  | ~3.9 ms | ~16 ms   |

macOS shows the largest FFM advantage because most system information requires native syscalls (`sysctl`, `proc_pidinfo`, etc.) where FFM's lower per-call overhead compounds across many invocations.

### Windows

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~0.13 ms | ~0.26 ms |
| FileStore  | ~17 µs   | ~96 µs   |
| Memory     | ~44 µs   | ~61 µs   |
| NetworkIF  | ~447 µs  | ~635 µs  |
| Processes  | ~4.1 ms  | ~5.3 ms  |

Windows benefits from FFM across every benchmark, by 1.3x to 6x. Earlier captures occasionally showed FFM slower on `NetworkIF`, from WMI query latency spikes on the CI runner.

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
| CpuTicks   | ~3 µs   | ~8 µs   |
| FileStore  | ~11 ms  | ~11 ms  |
| Memory     | ~1.5 ms | ~1.5 ms |
| NetworkIF  | ~4.4 ms | ~4.4 ms |
| Processes  | ~2.8 ms | ~2.9 ms |

FreeBSD, NetBSD, OpenBSD and Solaris all run inside a nested QEMU VM on an Ubuntu runner, so absolute numbers are noisier than the native-runner platforms above; only the relative JNA-vs-FFM comparison within a single run is meaningful. FreeBSD shows essentially no FFM/JNA gap outside `CpuTicks` because most data comes from `sysctl` reads and command output that don't bottleneck on per-call overhead; `NetworkIF` runs `ifconfig` for each interface's speed and status. The JNA `CpuTicks` figure has an error bar larger than itself.

### NetBSD

| Benchmark  | FFM     | JNA     |
|------------|---------|---------|
| CpuTicks   | ~4 µs   | ~11 µs  |
| FileStore  | ~9.2 ms | ~9.2 ms |
| Memory     | ~2.8 ms | ~2.7 ms |
| NetworkIF  | ~9 ms   | ~9 ms   |
| Processes  | ~30 ms  | ~28 ms  |

NetBSD's JNA column requires pkgsrc's `java-jna` for the native `libjnidispatch`; without it, OSHI's JNA implementation falls back to the command-line path the native-free provider uses. Only `CpuTicks`, one `kern.cp_time` sysctl per processor, is dominated by the native calls, and FFM's lower per-call overhead shows there. The other benchmarks spend their time outside the binding, so the two implementations are indistinguishable. One JNA `NetworkIF` iteration hit a ~63 ms outlier in the capture; the steady-state figure is shown. Same nested-VM caveat as FreeBSD.

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
| CpuTicks   | ~13 µs   | ~1.7 ms  |
| FileStore  | ~82 ms   | ~87 ms   |
| Memory     | ~9.5 ms  | ~9.8 ms  |
| NetworkIF  | ~316 µs  | ~399 µs  |
| Processes  | ~1.1 ms  | ~1.1 ms  |

Solaris (illumos) shows the most dramatic FFM advantage on `CpuTicks` (~100x faster) because every CPU-tick read crosses a `libkstat`/`kstat2` boundary, and JNA's per-call marshalling cost dominates the workload. `Processes` fell from ~200–300 ms to ~1 ms in 7.8.0, when `getProcesses()` stopped launching a `getent` process for every process's user and group name already in `/etc/passwd` and `/etc/group` ([#3890](https://github.com/oshi/oshi/pull/3890)); Linux and AIX shared the fix. Same nested-VM caveat as FreeBSD.

### AIX

| Benchmark  | FFM      | JNA      |
|------------|----------|----------|
| CpuTicks   | ~77 µs   | ~8.7 ms  |
| FileStore  | ~325 ms  | ~319 ms  |
| Memory     | ~128 µs  | ~280 µs  |
| NetworkIF  | ~113 µs  | ~505 µs  |
| Processes  | ~15 ms   | ~19 ms   |

AIX shows a Solaris-like FFM advantage of roughly two orders of magnitude on `CpuTicks`: every read traverses a `libperfstat` call, and JNA's per-call marshalling dominates against AIX's tiny native cost. `FileStore` is bottlenecked on filesystem stat() calls and is insensitive to native-binding overhead. `NetworkIF` reads its counters from `libperfstat` and each interface's status from its flags; 7.8.1 ran `ifconfig` for the status instead, which took ~30 ms. Error bars on the AIX `CpuTicks`, `Processes` and JNA `NetworkIF` figures exceed the values themselves. AIX runs on cfarm119 (shared real hardware via SSH, OpenJ9 JDK 25), so numbers are more stable than the nested-VM platforms above but noisier than the dedicated GitHub Actions runners.

### Running benchmarks yourself

The numbers above were collected on CI runners, which may differ significantly from your hardware and workload (e.g., number of running processes, mounted file stores, or network interfaces). Each release from 7.8.1 on publishes the benchmarks as a runnable jar, on Maven Central as `com.github.oshi:oshi-benchmark` and attached to the [GitHub release](https://github.com/oshi/oshi/releases), so measuring your own system needs only JDK 25+ and no build:

```sh
VERSION=7.8.1
curl -LO https://repo1.maven.org/maven2/com/github/oshi/oshi-benchmark/$VERSION/oshi-benchmark-$VERSION.jar
java -jar oshi-benchmark-$VERSION.jar -wi 5 -i 10 -f 3
```

The [oshi-benchmark README](oshi-benchmark/README.md) covers running individual benchmarks, JMH options, the memory footprint report, NetBSD's JNA setup, the CI workflow's inputs, and building the jar from source.

## CPU/memory trade-offs

OSHI avoids caching large amount of information, leaving caching to the user.  Limited use of caching is employed in many classes using Memoized suppliers in instance fields, thus avoiding repeated operating system calls.

Users with memory constraints can ensure the existing cached information is disposed of by using a new instance of `SystemInfo` and the subordinate classes when collecting data.

### Reusing vs. recreating `SystemInfo` when polling

A monitoring agent that samples the same metrics repeatedly should **hold a single `SystemInfo` and its subordinate objects** rather than constructing a new `SystemInfo` for each poll. Realistic poll intervals are far longer than the sub-second memoizer TTL, so the memoizer does not help *across* polls; the trade-off is the CPU to reconstruct the object graph each poll versus the memory retained by holding it.

The dominant reconstruction cost is the `CentralProcessor` (CPU topology and identifier queries). Reading CPU load ticks once per poll, measured by `ReuseVsRecreateBenchmark` with memoization disabled:

| Platform | Recreate per poll | Reuse (held) per poll | Retained if held |
|----------|------------------:|----------------------:|-----------------:|
| Windows  | 6.5 ms  | 255 µs  | 4.1 KB |
| macOS    | 7.4 ms  | 2.8 µs  | 8.6 KB |
| Linux    | 2.4 ms  | 16.9 µs | 5.4 KB |

Reconstructing costs milliseconds per poll (6–7 ms on Windows, where CPU identity comes from WMI/registry, and on macOS) for a few KB of retained memory saved — a strongly favorable trade for CPU-derived metrics. Two caveats:

- **Available memory does not benefit:** recreate ≈ reuse on all platforms (`GlobalMemory` is cheap to construct), so the win is specific to the `CentralProcessor`.
- **The process list does not benefit:** `getProcesses()` is a live full query (5–17 ms and 1–16 MB allocated per poll on these platforms, reused or not). To track specific processes, hold the individual `OSProcess` objects and call `updateAttributes()` rather than re-querying the whole list (see below).

Holding a `SystemInfo` and one snapshot of *every* subsystem it returns (measured by `MonitoringFootprintReport`) retains roughly **150–400 KB** on the CI runners (149 KB on Linux, 247 KB on Windows, 387 KB on macOS), much of it the process-list snapshot, which grows with the number of processes; the persistent hardware/OS caches alone are under ~160 KB. Applications with tight memory constraints can release even that by discarding the `SystemInfo` between collections, paying the reconstruction cost above.

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
