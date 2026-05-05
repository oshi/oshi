# OSHI Metrics Module

Provides [Micrometer](https://micrometer.io/) meter bindings for OSHI system metrics, following the
[OpenTelemetry Semantic Conventions for System Metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/).

## Setup

Add `oshi-metrics` alongside your OSHI implementation and a Micrometer registry:

```xml
<!-- OSHI metrics (requires JDK 17+) -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-metrics</artifactId>
    <version>${oshi.version}</version>
</dependency>

<!-- Pick ONE OSHI implementation -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-core</artifactId>        <!-- JNA, JDK 8+ -->
    <version>${oshi.version}</version>
</dependency>
<!-- OR -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-core-ffm</artifactId>    <!-- FFM, JDK 25+ -->
    <version>${oshi.version}</version>
</dependency>

<!-- Your Micrometer registry (e.g., Prometheus) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>${micrometer.version}</version>
</dependency>
```

## Usage

### Register all metrics

```java
SystemInfo si = new SystemInfo();  // or new oshi.ffm.SystemInfo()
OshiMetrics.bindTo(registry, si.getHardware(), si.getOperatingSystem());
```

### Selective registration (Builder API)

```java
SystemInfo si = new SystemInfo();
OshiMetrics.builder(si.getHardware(), si.getOperatingSystem())
    .enableGeneral(true)
    .enableCpu(true)
    .enableMemory(true)
    .enablePaging(false)
    .enableDisk(true)
    .enableFileSystem(true)
    .enableNetwork(false)
    .enableProcess(true)
    .build()
    .bindTo(registry);
```

All categories are enabled by default.

### Spring Boot

`OshiMetrics` implements `MeterBinder`, so Spring Boot auto-discovers it if you expose it as a bean:

```java
@Bean
public OshiMetrics oshiMetrics() {
    SystemInfo si = new SystemInfo();
    return new OshiMetrics(si.getHardware(), si.getOperatingSystem());
}
```

## Implemented Metrics

### [General metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#general-metrics)

| Metric | Instrument Type | Unit | Description |
|--------|----------------|------|-------------|
| `system.uptime` | Gauge | `s` | The time the system has been running |

### [Processor metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#processor-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.cpu.physical.count` | Gauge | `{cpu}` | — | Reports the number of actual physical processor cores on the hardware |
| `system.cpu.logical.count` | Gauge | `{cpu}` | — | Reports the number of logical (virtual) processor cores |
| `system.cpu.time` | FunctionCounter | `s` | `cpu.mode` | Seconds spent in each CPU mode |
| `system.cpu.frequency` | Gauge | `Hz` | `cpu.logical_number` | Operating frequency of the logical CPU in Hertz |

#### `system.cpu.utilization`

This metric is **not implemented** because it requires computing the rate of change of `system.cpu.time` over a
measurement interval. Users can implement this using `CentralProcessor.getSystemCpuLoadBetweenTicks(long[] prevTicks)`
with a `ScheduledExecutorService`.

### [Memory metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#memory-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.memory.usage` | Gauge | `By` | `system.memory.state` | Reports memory in use by state (used, free) |
| `system.memory.limit` | Gauge | `By` | — | Total memory available in the system |
| `system.memory.utilization` | Gauge | `1` | `system.memory.state` | Fraction of memory in use by state (0.0–1.0) |

### [Paging/Swap metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#pagingswap-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.paging.usage` | Gauge | `By` | `system.paging.state` | Unix swap or Windows pagefile usage (used, free) |
| `system.paging.utilization` | Gauge | `1` | `system.paging.state` | Fraction of swap/pagefile in use (0.0–1.0) |
| `system.paging.operations` | FunctionCounter | `{operation}` | `system.paging.direction` | Cumulative paging operations (in, out) |

#### `system.paging.faults`

Not implemented — OSHI does not expose system-level major/minor page fault counters.

### [Disk controller metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#disk-controller-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.disk.io` | FunctionCounter | `By` | `system.device`, `disk.io.direction` | Disk bytes transferred (read, write) |
| `system.disk.operations` | FunctionCounter | `{operation}` | `system.device`, `disk.io.direction` | Disk operations count (read, write) |
| `system.disk.io_time` | FunctionCounter | `s` | `system.device` | Time disk spent activated |
| `system.disk.limit` | Gauge | `By` | `system.device` | Total storage capacity of the disk |

#### Not implemented

- `system.disk.operation_time` — OSHI does not expose per-direction operation time
- `system.disk.merged` — OSHI does not expose merged operation counts

### [Filesystem metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#filesystem-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.filesystem.usage` | Gauge | `By` | `system.device`, `system.filesystem.mountpoint`, `system.filesystem.type`, `system.filesystem.mode`, `system.filesystem.state` | Filesystem space usage (used, free) |
| `system.filesystem.utilization` | Gauge | `1` | (same as above) | Fraction of filesystem space in use (0.0–1.0) |
| `system.filesystem.limit` | Gauge | `By` | `system.device`, `system.filesystem.mountpoint`, `system.filesystem.type`, `system.filesystem.mode` | Total capacity of the filesystem |

### [Network metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#network-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.network.io` | FunctionCounter | `By` | `system.device`, `network.io.direction` | Network bytes transferred (receive, transmit) |
| `system.network.packet.count` | FunctionCounter | `{packet}` | `system.device`, `network.io.direction` | Network packets (receive, transmit) |
| `system.network.packet.dropped` | FunctionCounter | `{packet}` | `system.device`, `network.io.direction` | Dropped packets (receive only) |
| `system.network.errors` | FunctionCounter | `{error}` | `system.device`, `network.io.direction` | Network errors (receive, transmit) |
| `system.network.connection.count` | Gauge | `{connection}` | `network.transport`, `network.connection.state` | Connection count by protocol and state |

### [Aggregate system process metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#aggregate-system-process-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.process.count` | Gauge | `{process}` | — | Total number of processes on the system |

#### Not implemented

- `system.process.created` — OSHI does not expose total processes created over uptime

### [Process metrics](https://opentelemetry.io/docs/specs/semconv/system/process-metrics/) (current JVM process)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `process.cpu.time` | FunctionCounter | `s` | `cpu.mode` (user, system) | CPU seconds by mode |
| `process.memory.usage` | Gauge | `By` | — | Physical memory (RSS) |
| `process.memory.virtual` | Gauge | `By` | — | Committed virtual memory |
| `process.disk.io` | FunctionCounter | `By` | `disk.io.direction` (read, write) | Disk bytes transferred |
| `process.thread.count` | Gauge | `{thread}` | — | Thread count |
| `process.open_file_descriptor.count` | Gauge | `{file_descriptor}` | — | Open file descriptors |
| `process.paging.faults` | FunctionCounter | `{fault}` | `system.paging.fault.type` (major, minor) | Page faults |
| `process.context_switches` | FunctionCounter | `{context_switch}` | `process.context_switch.type` (total) | Context switches |
| `process.uptime` | Gauge | `s` | — | Process uptime |

#### Not implemented

- `process.network.io` — OSHI does not expose per-process network I/O
