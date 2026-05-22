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

<!-- Pick an OSHI implementation -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-core</artifactId>        <!-- JNA, JDK 8+ -->
    <version>${oshi.version}</version>
</dependency>
<!-- AND/OR -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-core-ffm</artifactId>    <!-- FFM, JDK 25+, Linux/MacOS/Windows only -->
    <version>${oshi.version}</version>
</dependency>
<!-- OR -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-common</artifactId>    <!-- No native access, Linux only, JDK 8+ -->
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
OshiMetrics.bindTo(registry, SystemInfoFactory.create());
```

`SystemInfoFactory.create()` auto-selects the best available implementation (JNA, FFM, or native-free on Linux).
See the [project README](../README.md) for details on implementation choices.

### Selective registration (Builder API)

```java
OshiMetrics.builder(SystemInfoFactory.create())
    .enableGeneral(true)
    .enableCpu(true)
    .enableMemory(true)
    .enablePaging(false)
    .enableDisk(true)
    .enableFileSystem(true)
    .enableNetwork(false)
    .enableProcess(true)
    .enableContainer(true)
    .build()
    .bindTo(registry);
```

All categories are enabled by default. Categories: `general`, `cpu`, `memory`, `paging`, `disk`, `fileSystem`, `network`, `process`, `container`.

### Spring Boot or OpenTelemetry Spring Boot Starter

`OshiMetrics` implements `MeterBinder`, so Spring Boot auto-discovers it if you expose it as a bean. If you use the
[OpenTelemetry Spring Boot Starter](https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/), the Micrometer
bridge is auto-configured and metrics flow to your OTel backend automatically.

```java
@Bean
public OshiMetrics oshiMetrics() {
    return new OshiMetrics(SystemInfoFactory.create());
}
```

### OpenTelemetry

OSHI metrics can be exported to any [OpenTelemetry](https://opentelemetry.io/) backend using the
[OpenTelemetry Micrometer bridge](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/micrometer/micrometer-1.5/library).
Since `oshi-metrics` already follows OpenTelemetry semantic conventions for metric names and attributes, the resulting
telemetry is fully specification-compliant.

#### With the OpenTelemetry Java Agent

If your application runs with the
[OpenTelemetry Java Agent](https://opentelemetry.io/docs/zero-code/java/agent/), enable the Micrometer bridge and
register OSHI metrics with Micrometer's global registry:

```
-Dotel.instrumentation.micrometer.enabled=true
```

```java
import io.micrometer.core.instrument.Metrics;

OshiMetrics.bindTo(Metrics.globalRegistry, SystemInfoFactory.create());
```

The agent automatically bridges `Metrics.globalRegistry` to the OpenTelemetry SDK — no additional wiring needed.

#### Programmatic (no agent)

Add the [OpenTelemetry Micrometer bridge](https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-micrometer-1.5)
dependency:

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-micrometer-1.5</artifactId>
    <version>${opentelemetry-instrumentation.version}</version>
</dependency>
```

Then create a bridged registry and bind OSHI metrics to it:

```java
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;

OpenTelemetry otel = // your configured OpenTelemetry SDK instance
MeterRegistry otelRegistry = OpenTelemetryMeterRegistry.create(otel);

OshiMetrics.bindTo(otelRegistry, SystemInfoFactory.create());
```

See [Register all metrics](#register-all-metrics) for `SystemInfoFactory` options (JNA, FFM, or native-free on Linux).

#### Note on the upstream OSHI instrumentation

The [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) project
includes a built-in OSHI instrumentation that produces a small subset of system metrics (memory, network, and disk).
If you enable both that instrumentation and `oshi-metrics` via the Micrometer bridge, you may see duplicate metrics.
To avoid this, disable the upstream instrumentation:

```
-Dotel.instrumentation.oshi.experimental-metrics.enabled=false
```

The `oshi-metrics` module provides significantly broader coverage and stays in sync with OSHI API changes.

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
| `process.context_switches` | FunctionCounter | `{context_switch}` | `process.context_switch.type` (voluntary, involuntary or total) | Context switches; split via getrusage on POSIX for current process, total-only on Windows |
| `process.uptime` | Gauge | `s` | — | Process uptime |

#### Not implemented

- `process.network.io` — OSHI does not expose per-process network I/O

### [Container metrics](https://opentelemetry.io/docs/specs/semconv/system/container-metrics/) (when containerized)

| Metric | Instrument Type | Unit | Description |
|--------|----------------|------|-------------|
| `container.uptime` | Gauge | `s` | Time the container has been running (from PID 1 start time) |
| `container.cpu.time` | FunctionCounter | `s` | Total CPU time consumed by the container |
| `container.memory.usage` | Gauge | `By` | Container memory usage |
| `container.memory.available` | Gauge | `By` | Container memory available (limit - usage, only when limit is set) |

#### Not implemented

- `container.cpu.usage` — requires polling interval
- `container.disk.io`, `container.network.io` — OSHI does not expose per-cgroup I/O
- `container.memory.rss`, `container.memory.working_set`, `container.memory.paging.faults` — not available via CgroupInfo
- `container.filesystem.*` — not available via CgroupInfo
