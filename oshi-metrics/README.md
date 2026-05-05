# OSHI Metrics Module

Provides [Micrometer](https://micrometer.io/) meter bindings for OSHI system metrics, following the
[OpenTelemetry Semantic Conventions for System Metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/).

## Usage

```java
SystemInfo si = new SystemInfo();
OshiMetrics.bindTo(registry, si.getHardware(), si.getOperatingSystem());
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
| `system.cpu.time` | FunctionCounter | `s` | `cpu.mode` | Seconds each logical CPU spent on each mode |
| `system.cpu.frequency` | Gauge | `Hz` | `cpu.logical_number` | Operating frequency of the logical CPU in Hertz |

#### `system.cpu.utilization`

This metric is **not implemented** because it requires computing the rate of change of `system.cpu.time` over a
measurement interval. OSHI does not own a polling loop — the application or observability agent should control the
sampling interval.

Users can implement this themselves using `CentralProcessor.getSystemCpuLoadBetweenTicks(long[] prevTicks)` with a
`ScheduledExecutorService` and register the results as a Gauge with `cpu.mode` attributes.

### [Memory metrics](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/#memory-metrics)

| Metric | Instrument Type | Unit | Attributes | Description |
|--------|----------------|------|------------|-------------|
| `system.memory.usage` | Gauge | `By` | `system.memory.state` | Reports memory in use by state (used, free) |
| `system.memory.utilization` | Gauge | `1` | `system.memory.state` | Fraction of memory in use by state (0.0–1.0) |
