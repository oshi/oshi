# OSHI Demo Module

Proof-of-concept examples demonstrating OSHI capabilities, including a Swing GUI, JSON output, JMX integration, and utility tools.

## Quick Start with jbang

The fastest way to try OSHI — no clone or build required:

```sh
# List all available demos
jbang alias list oshi/oshi

# JSON dump of all system info
jbang json@oshi/oshi

# Swing GUI dashboard
jbang gui@oshi/oshi

# Detect if running in a VM
jbang detectvm@oshi/oshi

# Generate a unique machine identifier
jbang id@oshi/oshi

# Show disk store for a given path
jbang diskstore@oshi/oshi
```

## Running from Source

```sh
# Build the project
./mvnw install -DskipTests

# Run any demo class
./mvnw exec:java -pl oshi-demo -Dexec.mainClass="oshi.demo.Json"
./mvnw exec:java -pl oshi-demo -Dexec.mainClass="oshi.demo.OshiGui"
```

## Available Demos

| Class | Description |
|-------|-------------|
| `Json` | Dumps all system information as JSON |
| `OshiGui` | Swing GUI with CPU, memory, disk, network panels |
| `DetectVM` | Detects virtualization and container platforms |
| `ComputerID` | Generates a unique hardware-based machine identifier |
| `DiskStoreForPath` | Shows which disk store backs a given file path |
| `OshiHTTPServer` | Simple HTTP server exposing system info |
| `PollGpuStats` | Polls and displays GPU statistics |
| `SmcDump` | Dumps macOS SMC sensor keys |

### JMX Integration

The `oshi.demo.jmx` package demonstrates exposing OSHI data via JMX MBeans:

```sh
./mvnw exec:java -pl oshi-demo -Dexec.mainClass="oshi.demo.jmx.demo.OshiJMXServer"
```

Then connect with `jconsole` or any JMX client to browse system metrics as MBeans.
