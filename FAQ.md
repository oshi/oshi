# Frequently Asked Questions

* [About OSHI](#about-oshi)
  * [What is the intended use of the API?](#what-is-the-intended-use-of-the-api)
  * [Will you implement my feature request?](#will-you-implement-my-feature-request)
* [Requirements and Compatibility](#requirements-and-compatibility)
  * [What minimum Java version is required?](#what-minimum-java-version-is-required)
  * [Which operating systems are supported?](#which-operating-systems-are-supported)
  * [Does OSHI work on ...](#does-oshi-work-on-)
    + [ARM hardware?](#arm-hardware)
    + [Apple Silicon hardware?](#apple-silicon-hardware)
    + [Raspberry Pi hardware?](#raspberry-pi-hardware)
  * [Is the API backwards compatible between versions?](#is-the-api-backwards-compatible-between-versions)
  * [Does OSHI support Java Module System (JPMS) modules?](#does-oshi-support-java-module-system-jpms-modules)
  * [Does OSHI support Open Service Gateway initiative (OSGi) modules?](#does-oshi-support-open-service-gateway-initiative-osgi-modules)
* [Using the API](#using-the-api)
  * [Is OSHI Thread Safe?](#is-oshi-thread-safe)
  * [What do OSHI's annotations mean?](#what-do-oshis-annotations-mean)
    + [API stability: `@PublicApi`](#api-stability-publicapi)
    + [Thread safety: `@ThreadSafe`, `@Immutable`, `@NotThreadSafe`, `@GuardedBy`](#thread-safety-threadsafe-immutable-notthreadsafe-guardedby)
    + [Nullability: `@NullMarked`, `@Nullable`](#nullability-nullmarked-nullable)
  * [How do I get CPU usage?](#how-do-i-get-cpu-usage)
* [Configuration and Deployment](#configuration-and-deployment)
  * [How does OSHI support the Principle of Least Privilege?](#how-does-oshi-support-the-principle-of-least-privilege)
    + [What requires elevated permissions?](#what-requires-elevated-permissions)
    + [Linux: Configurable privilege escalation via sudo](#linux-configurable-privilege-escalation-via-sudo)
  * [How do I configure OSHI?](#how-do-i-configure-oshi)
    + [Environment variables](#environment-variables)
    + [External properties file](#external-properties-file)
    + [Java system properties](#java-system-properties)
    + [Programmatic configuration](#programmatic-configuration)
    + [Properties file](#properties-file)
    + [Spring Boot integration](#spring-boot-integration)
  * [What optional software gives OSHI more to report?](#what-optional-software-gives-oshi-more-to-report)
    + [Linux](#linux)
    + [Windows](#windows)
    + [macOS](#macos)
    + [FreeBSD](#freebsd)
    + [Solaris / illumos](#solaris--illumos)
    + [NetBSD](#netbsd)
    + [AIX](#aix)
  * [Does OSHI work in containers (Docker, Kubernetes)?](#does-oshi-work-in-containers-docker-kubernetes)
  * [How can I get reliable sensor information on Windows?](#how-can-i-get-reliable-sensor-information-on-windows)
    + [The optional jLibreHardwareMonitor dependency](#the-optional-jlibrehardwaremonitor-dependency)
    + [Turning off the sources you do not use](#turning-off-the-sources-you-do-not-use)
* [Troubleshooting and Unexpected Values](#troubleshooting-and-unexpected-values)
  * [How do I resolve JNA `NoClassDefFoundError` or `NoSuchMethodError` issues?](#how-do-i-resolve-jna-noclassdeffounderror-or-nosuchmethoderror-issues)
  * [How do I resolve `Pdh call failed with error code 0xC0000BB8` issues?](#how-do-i-resolve-pdh-call-failed-with-error-code-0xc0000bb8-issues)
  * [Why does OSHI freeze for 20 seconds (or larger multiples of 20 seconds) on Windows when it first starts up?](#why-does-oshi-freeze-for-20-seconds-or-larger-multiples-of-20-seconds-on-windows-when-it-first-starts-up)
  * [Why does OSHI's System and Processor CPU usage differ from the Windows Task Manager?](#why-does-oshis-system-and-processor-cpu-usage-differ-from-the-windows-task-manager)
  * [Why does OSHI's Process CPU usage differ from the Windows Task Manager?](#why-does-oshis-process-cpu-usage-differ-from-the-windows-task-manager)
  * [Why is a BSD process's CPU time sometimes flat when I expect it to have increased?](#why-is-a-bsd-processs-cpu-time-sometimes-flat-when-i-expect-it-to-have-increased)
  * [Why is `getCurrentFreq()` the same number every time on Apple Silicon?](#why-is-getcurrentfreq-the-same-number-every-time-on-apple-silicon)

---

## About OSHI

### What is the intended use of the API?

Users should create a new instance of `SystemInfo` using `SystemInfoFactory.create()` (see the [Usage](README.md#usage) section) and use the getters to access the platform-specific hardware and software interfaces using the respective `get*()` methods. The interfaces in `oshi.hardware` and `oshi.software.os` provide cross-platform functionality. See the `main()` method of [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) for sample code.

Methods return a "snapshot" of current levels. To display values which change over time, it is intended that users poll for information no more frequently than approximately every second. Disk and file system calls may incur some latency and should be polled less frequently.
CPU usage calculation precision depends on the relation of the polling interval to both system clock tick granularity and the number of logical processors.

### Will you implement my feature request?

Maybe!  If you can contribute all the code to implement the feature, it will almost certainly be added.  Even if you can't code but can provide pointers to where the information can be found cross-platform, your feature has a good chance. Otherwise, you can always submit an issue to ask, but are at the mercy of the developers' time, enthusiasm level, and the availability of documentation for the feature.

## Requirements and Compatibility

### What minimum Java version is required?

OSHI 4.x and later require minimum Java 8 compatibility for the JNA implementation (`oshi-core`). This minimum level will be retained through at least OpenJDK 8 EOL.

Starting with OSHI 7.x, the FFM implementation (`oshi-core-ffm`) requires JDK 25+. The Foreign Function & Memory API became final in JDK 22 (JEP 454), but OSHI targets JDK 25 as the first LTS release with FFM support.

OSHI 3.x is compatible with Java 7 up to 3.13.x versions. OSHI 3.14.0 restored Java 6 compatibility for the `oshi-core` artifact only. These versions are no longer actively maintained.

### Which operating systems are supported?

OSHI has been implemented and tested on the following systems.  Some features may work on earlier versions.
* Windows 7 and higher.  (Nearly all features work on Vista and most work on Windows XP.)
* macOS version 10.6 (Snow Leopard) and higher.
* Linux (Most major distributions) Kernel 2.6 and higher
* DragonFly BSD 6.4
* FreeBSD 10
* NetBSD 10.1
* OpenBSD 6.8
* Solaris 11 / illumos (SunOS 5.11)
* AIX 7.1 (POWER4)
* Android 7.0 and higher

The FFM implementation (`oshi-core-ffm`) supports the same platforms as the JNA implementation and assumes a 64-bit operating system.

### Does OSHI work on ...

#### ARM hardware?

Yes, CI is actively conducted on Linux ARM hardware and other platforms will be added when hardware is
available for such testing. Note that many features (e.g., CPUID, and processor identification such as
family, model, stepping, and vendor frequency) are based on Intel chips and may have different corresponding
meanings.

#### Apple Silicon hardware?

OSHI works with native `AArch64` support when JNA is version 5.7.0 or later.

OSHI works using virtual x86 hardware under Rosetta if you are executing an x86-based JVM.

#### Raspberry Pi hardware?

Yes, most of the Linux code works here and other Pi-specific code has been implemented but has seen
limited testing.  As the developers do not have a Pi to test on, users reporting issues should be
prepared to help test solutions.

### Is the API backwards compatible between versions?

OSHI follows [Semantic Versioning](https://semver.org/). The interfaces and classes in `oshi.hardware` and `oshi.software.os` are considered the OSHI API and are guaranteed to be compatible within the same major version. Classes and interfaces annotated with `@PublicApi` are part of this contract. Differences between major versions can be found in the [Upgrading.md](UPGRADING.md) document.

Most, if not all, of the platform-specific implementations of these APIs in lower level packages will remain the same, although it is not intended that users access platform-specific code, and some changes may occur between minor versions, most often in the number of arguments passed to constructors or platform-specific methods. Supporting code in the `oshi.driver` and `oshi.util` packages may, rarely, change between minor versions, usually associated with organizing package structure or changing parsing methods for efficiency/consistency/ease of use.

Code in the `oshi.jna.*` and `oshi.ffm.*` packages, other than the `oshi.ffm.SystemInfo` entry point, is intended to be temporary and is not intended for dependent projects to rely on. This covers the native mappings and the utilities supporting them. The JNA code will be removed as it is included in the JNA project, and the FFM code may likewise move to a project of its own.

### Does OSHI support Java Module System (JPMS) modules?

Yes. OSHI provides three named JPMS modules:

- `com.github.oshi.common` — the public API interfaces and the native-free implementation (`oshi-common`). Works on JDK 8+ on the classpath; JDK 9+ on the module path. Currently supports Linux only.
- `com.github.oshi` — the JNA-based implementation (`oshi-core`). Works on JDK 8+ on the classpath; JDK 9+ on the module path.
- `com.github.oshi.ffm` — the FFM-based implementation (`oshi-core-ffm`). Requires JDK 25+.

All modules export the public API packages and declare the appropriate `requires` directives. Add the one that matches your native access preference to your `module-info.java`:

```java
requires com.github.oshi.common; // API + native-free implementation (Linux only)
requires com.github.oshi;        // JNA
requires com.github.oshi.ffm;    // FFM
```

Note: In OSHI 6.x, `oshi-core` only had an `Automatic-Module-Name` and a separate `oshi-core-java11` artifact provided the full module descriptor. Starting with OSHI 7.0, `oshi-core` includes the module descriptor directly.

### Does OSHI support Open Service Gateway initiative (OSGi) modules?

OSHI adds OSGi manifest entries using `maven-source-plugin` and `mvn-bnd-plugin`. Submit an issue if the configuration of these plugins needs to be adjusted to support your project.

## Using the API

### Is OSHI Thread Safe?

OSHI 5.X and above is thread safe with the exceptions noted below. `@Immutable`, `@ThreadSafe`, and `@NotThreadSafe` document
each class. The following classes are not thread-safe:
 - `GlobalConfig` does not protect against multiple threads manipulating the configuration programmatically.
 However, these methods are intended to be used by a single thread at startup in lieu of reading a configuration file.
 OSHI gives no guarantees on re-reading changed configurations.
 - On non-Windows platforms, the `getSessions()` method on the `OperatingSystem` interface uses native code which is not thread safe. While OSHI's methods employ synchronization to coordinate access from its own threads, users are cautioned that other operating system code may access the same underlying data structures and produce unexpected results, particularly on servers with frequent new logins.
The `oshi.os.unix.whocommand` property may be set to parse the Posix-standard `who` command in preference to the native implementation,
which may use reentrant code on some platforms.
 - The `PerfCounterQueryHandler` class is not thread-safe but is only internally used in single-thread contexts,
and is not intended for user use.

Earlier versions do not guarantee thread safety, and it should not be assumed.

### What do OSHI's annotations mean?

OSHI uses annotations to document three separate contracts: what is part of the API, what is safe to call from multiple threads, and what can be `null`. All three are documentation for callers rather than anything enforced at runtime, and none of them are retained in a way that affects your application's behavior.

#### API stability: `@PublicApi`

`oshi.annotation.PublicApi` marks the types covered by the Semantic Versioning guarantee described [above](#is-the-api-backwards-compatible-between-versions). Everything in `oshi.hardware` and `oshi.software.os` is part of the API whether or not it carries the annotation; the annotation additionally marks API types that live elsewhere, such as `oshi.util.PlatformEnum` and the `oshi.ffm.SystemInfo` entry point. Unannotated types in the lower-level packages may change between minor versions.

#### Thread safety: `@ThreadSafe`, `@Immutable`, `@NotThreadSafe`, `@GuardedBy`

The annotations in `oshi.annotation.concurrent` are modeled on those in *Java Concurrency in Practice*. `@Immutable` means the instance's state cannot change after construction, `@ThreadSafe` means it may be used from multiple threads without external synchronization, and `@NotThreadSafe` means it may not. `@GuardedBy` names the lock protecting a field. See [Is OSHI Thread Safe?](#is-oshi-thread-safe) for the classes that are exceptions to the general guarantee.

#### Nullability: `@NullMarked`, `@Nullable`

OSHI uses [JSpecify](https://jspecify.dev/) annotations to document which values can be `null`. A package annotated `@NullMarked` (in its `package-info.java`) declares that **every type usage in a signature is non-null unless it is explicitly annotated `@Nullable`** — parameters, return types, and fields alike. Every package in `oshi-common`, `oshi-core` and `oshi-core-ffm` is marked. The API packages — `oshi`, `oshi.hardware`, `oshi.software.os`, `oshi.util` and its subpackages, `oshi.spi`, `oshi.annotation` and `oshi.ffm` — carry `@NullMarked` in their `package-info.java`, which every consumer sees, including on a Java 8 or classpath build. The implementation packages behind them are marked on their module descriptors instead, which a consumer sees only on the module path; they are internal either way. The native mapping packages under `oshi.jna` and `oshi.ffm.platform` are explicitly `@NullUnmarked`: they mirror C structs and libraries, where nullability is the operating system's to state and not OSHI's. The Micrometer bindings in `oshi-metrics` are not marked; they implement Micrometer interfaces that carry no nullability annotations of their own, so a marking there would mostly be OSHI asserting a contract belonging to another project.

Very little in the API is nullable, because OSHI reports a value it could not read as a sentinel rather than as `null`. Generally:

- strings become `oshi.util.Constants.UNKNOWN` or the empty string,
- collections and arrays become empty rather than `null`,
- numbers become `0`, `-1`, or `Double.NaN` depending on which is out of the value's legitimate range.

The Javadoc for the [`oshi.hardware`](https://www.oshi.ooo/oshi-core/apidocs/com.github.oshi.common/oshi/hardware/package-summary.html) and [`oshi.software.os`](https://www.oshi.ooo/oshi-core/apidocs/com.github.oshi.common/oshi/software/os/package-summary.html) packages lists these sentinels, and a getter that uses a more specific one names it in its own Javadoc. Prefer testing for those over a null check; an existing null check on a member that is not `@Nullable` is dead code. The handful of genuinely nullable API members — such as `OperatingSystem.getProcess(int)` for a process that is not running, and `PowerSource.getManufactureDate()` for a battery that does not report one — are annotated `@Nullable` and say so in their Javadoc. Nullable *parameters* are more common, and mark an argument as optional: passing `null` for the `filter` and `sort` arguments of `OperatingSystem.getProcesses(Predicate, Comparator, int)` requests no filtering and no sorting.

OSHI's own build enforces all of this: [NullAway](https://github.com/uber/NullAway) runs over the marked packages in `@NullMarked`-only, JSpecify mode, at `ERROR` severity, so a violation fails the build. The annotations are a checked guarantee rather than an aspiration.

The `org.jspecify:jspecify` dependency is compile-time metadata only. It is declared `optional` in Maven and `requires static` in the module descriptor, so it does not reach your runtime classpath, and you do not need it on your own compile classpath unless you want to run a null-checking analyzer against OSHI's signatures yourself.

### How do I get CPU usage?

CPU usage is computed by comparing tick counters at two points in time. A single snapshot is meaningless on its own — you must poll at least twice and calculate the difference.

There are two ways to get there:

- **With Micrometer**, the [`oshi-metrics`](oshi-metrics/README.md) module does it for you. It registers `system.cpu.time` and `process.cpu.time` as cumulative counters of CPU seconds for each `cpu.mode`, and your monitoring backend turns them into usage as a rate between samples (for example, Prometheus `rate()`). This is the simpler choice if you already export metrics.
- **With the API directly**, you keep the previous sample yourself and let OSHI compute the difference, as shown below. Use this when you need the number in your own code.

**System-level CPU usage** uses `CentralProcessor.getSystemCpuLoadTicks()`, which returns 8 tick values (User, Nice, System, Idle, IOWait, IRQ, SoftIRQ, Steal). The ratio of active ticks to total ticks (active + idle) gives the CPU load:

```java
CentralProcessor cpu = SystemInfoFactory.create().getHardware().getProcessor();

// First snapshot
long[] prevTicks = cpu.getSystemCpuLoadTicks();
Thread.sleep(1000);

// Second snapshot — OSHI computes the delta internally
double load = cpu.getSystemCpuLoadBetweenTicks(prevTicks);
System.out.printf("CPU Load: %.1f%%%n", load * 100);

// Save current ticks for the next interval
prevTicks = cpu.getSystemCpuLoadTicks();
```

**Process-level CPU usage** uses `OSProcess.getProcessCpuLoadBetweenTicks(previousSnapshot)`. Unlike system ticks, processes have **no idle counter** — the calculation is (kernel + user time) / elapsed up time. This means a multi-threaded process on a 4-core system can report up to 400% CPU (matching `top` on Linux/Unix). On Windows, the Task Manager divides by logical processor count to cap at 100%; to match that display, divide OSHI's value by `getLogicalProcessorCount()`.

```java
SystemInfoProvider si = SystemInfoFactory.create();
OperatingSystem os = si.getOperatingSystem();
int cpuCount = si.getHardware().getProcessor().getLogicalProcessorCount();

// First snapshot: build a map of PID -> OSProcess
Map<Integer, OSProcess> priorSnapshot = new HashMap<>();
for (OSProcess p : os.getProcesses(null, null, 0)) {
    priorSnapshot.put(p.getProcessID(), p);
}
Thread.sleep(2000);

// Second snapshot: compute per-process CPU
for (OSProcess p : os.getProcesses(null, null, 0)) {
    double cpu = p.getProcessCpuLoadBetweenTicks(priorSnapshot.get(p.getProcessID()));
    // Unix-style (can exceed 100%):
    System.out.printf("PID %d: %.1f%% (Unix-style)%n", p.getProcessID(), cpu * 100);
    // Windows Task Manager-style (capped per system):
    System.out.printf("PID %d: %.1f%% (Windows-style)%n", p.getProcessID(), cpu * 100 / cpuCount);
    priorSnapshot.put(p.getProcessID(), p);
}
```

**Key differences between system and process CPU:**

| | System CPU | Process CPU |
|---|---|---|
| Idle ticks | Yes (8 tick types including Idle) | No idle counter |
| Calculation | active / (active + idle) | (kernel + user) / elapsed time |
| Range | 0–100% (unless Windows Utility mode) | 0 – (100% × logical CPUs) |
| Windows note | Set `OSHI_OS_WINDOWS_CPU_UTILITY` to match Task Manager | Divide by logical CPU count to match Task Manager |

**Tips:**
 - Poll at least every 1–2 seconds for meaningful results. The first call returns cumulative data, so discard it or use it as the baseline.
 - For non-blocking periodic monitoring, store the previous ticks yourself (as shown above) rather than using the convenience `getSystemCpuLoad(delay)` method, which blocks the calling thread.
 - See the [ProcessorPanel](https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/gui/ProcessorPanel.java) and [ProcessPanel](https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/gui/ProcessPanel.java) in the `oshi-demo` module for working GUI examples of both system and process CPU polling.

## Configuration and Deployment

### How does OSHI support the Principle of Least Privilege?

OSHI is designed to work without elevated permissions. The vast majority of system information — CPU, memory, disks, network, processes, and more — is available to any unprivileged user on all supported platforms. You should **not** need to run your application as root or Administrator just to use OSHI.

However, some specific features require elevated permissions to access. Rather than running your entire application with elevated privileges (which violates the [Principle of Least Privilege](https://en.wikipedia.org/wiki/Principle_of_least_privilege) and introduces unnecessary risk), OSHI provides mechanisms to grant fine-grained access to only the specific resources that need it.

#### What requires elevated permissions?

**Linux:**
- Physical memory module details via `dmidecode`, and the system serial number and UUID, from root-only DMI files or `dmidecode` (OSHI also tries `lshal` and `lshw` for those two)
- Some `/proc/<pid>` files of other users' processes, such as `/proc/<pid>/io` for per-process I/O stats, which otherwise read as `0`
- The physical volumes of logical volume groups, via `pvs`. Groups and their logical volumes are listed without it. OSHI runs `pvs` without the privileged prefix described below, so it needs the application itself to have the permission.

**Windows:**
- Environment variables of processes owned by other users, which are otherwise empty (requires `SeDebugPrivilege` or Administrator). Their command lines are read directly with the same privilege; without it, OSHI asks WMI instead.
- Sensor data (temperature, fan speeds) via a hardware monitoring application or the optional [jLibreHardwareMonitor](https://github.com/pandalxb/jLibreHardwareMonitor) dependency

**macOS:**
- Sockets of processes owned by other users, which `getConnections()` otherwise leaves out
- Exact TCPv4 segment and error counts, which are otherwise estimated from the IP and UDP totals

#### Linux: Configurable privilege escalation via sudo

On Linux, OSHI supports configurable privilege escalation using three properties in [`oshi.properties`](https://github.com/oshi/oshi/blob/master/oshi-common/src/main/resources/oshi.properties) (or via [`GlobalConfig`](https://www.oshi.ooo/oshi-core/apidocs/com.github.oshi.common/oshi/util/GlobalConfig.html)):

| Property | Description |
|---|---|
| `oshi.os.linux.privileged.prefix` | Command prefix, e.g., `sudo -n` |
| `oshi.os.linux.privileged.allowlist` | Comma-separated commands eligible for the prefix, e.g., `dmidecode,lshw` |
| `oshi.os.linux.privileged.file.allowlist` | Comma-separated file paths or glob patterns eligible for privileged read via the prefix + `cat`, e.g., `/proc/*/io` |

**Example setup:**

1. Configure passwordless sudo for only the specific commands your application needs (`sudo visudo`):
   ```
   oshiuser ALL=(ALL) NOPASSWD: /usr/sbin/dmidecode, /usr/bin/lshw, /usr/bin/cat
   ```

2. Set the OSHI configuration at startup:
   ```java
   GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
   GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, "dmidecode,lshw");
   GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST, "/proc/*/io");
   ```
   Or set equivalent values in `oshi.properties`, as Java system properties, or by any other method in [How do I configure OSHI?](#how-do-i-configure-oshi).

**How it works:**
- The prefix is **not** applied when already running as root (uid=0).
- Only commands and files explicitly listed in the allowlists will use privilege escalation.
- For file reads, OSHI first attempts a normal read; only if the file exists but is not readable does it fall back to the privileged `cat` command.
- The prefix is flexible — it works with `sudo`, `doas`, or a custom wrapper script for stricter environments.

**Security notes:**
- Restrict your `sudoers` entries to the minimum set of commands required.
- Take care with the file allowlist — avoid paths like `/proc/*/environ` which could expose credentials.
- For higher-security environments, consider a wrapper script that validates arguments before executing the privileged command. See [#3100](https://github.com/oshi/oshi/issues/3100) for examples.

### How do I configure OSHI?

OSHI supports multiple configuration mechanisms with a clear precedence order. Higher-priority sources override lower ones:

| Priority | Source | Example |
|----------|--------|---------|
| 1 (highest) | `GlobalConfig.set()` in Java code | `GlobalConfig.set("oshi.os.linux.privileged.prefix", "sudo -n");` |
| 2 | Java system properties | `java -Doshi.os.linux.privileged.prefix="sudo -n" -jar app.jar` |
| 3 | Environment variables (`OSHI_*`) | `export OSHI_OS_LINUX_PRIVILEGED_PREFIX="sudo -n"` |
| 4 | External properties file | `java -Doshi.properties.file=/etc/oshi.conf -jar app.jar` |
| 5 (lowest) | `oshi.properties` on classpath | Place in `src/main/resources/oshi.properties` |

#### Environment variables

Environment variables use the convention: uppercase the property name and replace dots with underscores.

| Property key | Environment variable |
|---|---|
| `oshi.os.linux.privileged.prefix` | `OSHI_OS_LINUX_PRIVILEGED_PREFIX` |
| `oshi.util.memoizer.expiration` | `OSHI_UTIL_MEMOIZER_EXPIRATION` |
| `oshi.util.proc.path` | `OSHI_UTIL_PROC_PATH` |

This is the recommended approach for containers, Kubernetes, and 12-factor apps:

```sh
# Docker
docker run -e OSHI_OS_LINUX_PRIVILEGED_PREFIX="sudo -n" myapp

# Kubernetes (in pod spec)
env:
  - name: OSHI_OS_LINUX_PRIVILEGED_PREFIX
    value: "sudo -n"
```

#### External properties file

Point OSHI at a properties file on disk using the `oshi.properties.file` system property:

```sh
java -Doshi.properties.file=/etc/oshi.conf -jar app.jar
```

This is useful for server deployments where config lives in `/etc/` or Docker containers mounting config files as volumes.

#### Java system properties

Pass properties on the command line with `-D`:

```sh
java -Doshi.os.linux.privileged.prefix="sudo -n" -Doshi.util.memoizer.expiration=500 -jar app.jar
```

#### Programmatic configuration

Set values in Java code at startup, before creating any OSHI objects:

```java
GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 500);
```

#### Properties file

Place an `oshi.properties` file in `src/main/resources/` to override defaults. See the [default oshi.properties](https://github.com/oshi/oshi/blob/master/oshi-common/src/main/resources/oshi.properties) for all available keys and their defaults.

#### Spring Boot integration

Spring Boot does not automatically propagate its `application.yml` properties to Java system properties. Use one of these approaches:

**Option A** — Set environment variables (works with the new `OSHI_*` support):
```yaml
# In your deployment config or docker-compose.yml
environment:
  OSHI_OS_LINUX_PRIVILEGED_PREFIX: "sudo -n"
```

**Option B** — Bridge all `oshi.*` properties from `application.yml` (recommended for Spring Boot):

```yaml
# application.yml
oshi:
  os:
    linux:
      privileged:
        prefix: "sudo -n"
  util:
    memoizer:
      expiration: 500
```

```java
@Configuration
public class OshiConfig {
    @Autowired
    private Environment env;

    @PostConstruct
    void init() {
        // Bridge any oshi.* properties from Spring Environment to OSHI GlobalConfig
        for (String key : new String[] {
                GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX,
                GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST,
                GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_FILE_ALLOWLIST,
                GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION
                // Add other keys as needed
        }) {
            String value = env.getProperty(key);
            if (value != null) {
                GlobalConfig.set(key, value);
            }
        }
    }
}
```

**Option C** — Use `-D` flags in your startup script:
```sh
java -Doshi.os.linux.privileged.prefix="sudo -n" -jar myapp.jar
```

### What optional software gives OSHI more to report?

OSHI bundles no native libraries and requires nothing beyond a JDK. Everything on this page is
**optional**: OSHI probes for each item at the moment it needs it. When an item is absent, OSHI
reads the value from another source where one exists, sometimes a less detailed one, and reports a
sentinel only when no source has it. Nothing here is needed for OSHI to start. Install an item only
if you want the data or detail it adds.

That holds unconditionally for the **command-line tools** below: OSHI runs one, gets nothing, and
moves on. It holds for the **shared libraries** on any current release, where OSHI tests for the
library when it first needs it and skips the calls that depend on it. Releases before 6.2 could
instead throw `UnsatisfiedLinkError` when `libudev` was missing, which the
[containers answer](#does-oshi-work-in-containers-docker-kubernetes) covers.

Two related sections: several of these commands must run as root, which
[the least-privilege section](#how-does-oshi-support-the-principle-of-least-privilege) covers, and a
few can be switched off explicitly, which [configuration](#how-do-i-configure-oshi) covers.

#### Linux

| Install | What it adds |
|---|---|
| **udev** — the `libudev.so.1` library, packaged as `libudev1`, `systemd-libs` or similar | Disk stores, the USB device tree and logical volume groups: without it, `oshi-core` and `oshi-core-ffm` return those lists empty, while the native-free implementation reads disks and USB devices from sysfs. Also network interface model names, which otherwise come from sysfs and are often just the interface name. Power sources lose nothing; OSHI reads the same data from sysfs. Frequently missing from slim container images — set `oshi.os.linux.allowudev=false` to skip the probe. |
| **systemd** (`libsystemd`) | `OperatingSystem.getSessions()` through logind, where `utmp` is unavailable or deprecated. Without it, or with `oshi.os.linux.allowsystemd=false`, OSHI reads `utmp`, then the logind session files, then runs `who`. |
| **dmidecode** | Physical memory module details, which have no other source. Also the firmware name and the revision in its version, and the system serial number and UUID when the root-only DMI files under `/sys/devices/virtual/dmi/id` cannot be read; OSHI then tries `lshal` and `lshw`. Without it, the processor ID comes from `cpuid` or is built from `/proc/cpuinfo`. Needs root. |
| **lshw** | Graphics cards when `lspci` finds none, the last fallback for the system model, serial number and UUID, and the processor's rated frequency where its name does not state one (otherwise `/proc/cpuinfo` and cpufreq). Needs root. |
| **lspci** | Graphics cards and their VRAM aperture, without root. It is the first source OSHI tries, with `lshw` as the fallback; with neither, the card list is empty. |
| **lscpu** | CPU cache sizes and NUMA node mapping when the sysfs CPU topology is missing, and the processor vendor and name where `/proc/cpuinfo` lacks them, which is common on ARM. |
| **cpuid** | The processor ID, when `dmidecode` is absent or not permitted. Without either, OSHI builds the ID from `/proc/cpuinfo`. |
| **NVIDIA driver / NVML** — the `libnvidia-ml` library, which ships with the proprietary driver | GPU utilization, memory, temperature, power and clocks on NVIDIA cards, whose proprietary driver publishes none of them through sysfs. AMD and Intel cards report these through sysfs and hwmon without it. |
| **vcgencmd** (Raspberry Pi only; present in Raspberry Pi OS, packaged separately elsewhere) | Firmware details, which a Pi has no DMI data to supply. SoC temperature and core voltage come from it first, and otherwise from the kernel's hwmon and thermal sensors. |

#### Windows

| Install | What it adds |
|---|---|
| **LibreHardwareMonitor**, or the older **OpenHardwareMonitor**, running as Administrator with WMI publishing enabled | CPU temperature, fan speeds and voltage. LibreHardwareMonitor is the maintained one and also publishes GPU temperature, power, clocks, fan, utilization and memory. Nothing extra to add to your build. Without either, OSHI still asks Windows' own WMI classes (`MSAcpi_ThermalZoneTemperature`, `Win32_Fan`, `Win32_Processor`), which many motherboards leave empty or unchanging. |
| The optional **jLibreHardwareMonitor** dependency | The same sensors with no monitoring application running — it ships the monitoring libraries itself. See the details and caveats below. |
| **Vendor GPU drivers** (NVIDIA NVML, AMD ADL) | GPU utilization, temperature, power, clocks and fan speed. Normally already present with the driver. Without them, OSHI uses LibreHardwareMonitor if it is running, and utilization and dedicated and shared memory also come from Windows performance counters. |

#### macOS

Nothing to install. Sensor, GPU and hardware data all come from system frameworks (SMC, IOKit,
CoreGraphics) that ship with the OS.

#### FreeBSD

| Install | What it adds |
|---|---|
| **dmidecode** | Firmware, baseboard and computer-system manufacturer, model, version and serial number, and the processor ID. Without it, the system UUID still comes from `kern.hostuuid` and the processor ID from `/var/run/dmesg.boot`. |

OSHI also reads `lshal` for the USB device tree and as a second source for the system serial
number, but that is a legacy path rather than something to install: HAL was abandoned upstream and
its FreeBSD port was deleted in 2021. On a current system the command is absent, so the USB device
list is empty and the serial number comes only from `dmidecode`.

#### Solaris / illumos

| Install | What it adds |
|---|---|
| **sneep** | The system serial number, when `smbios` does not report one. OSHI also tries `prtconf` after it. |
| **PICL** (`prtpicl`) | Temperature, fan speed and voltage sensors. Present on Solaris; check your illumos distribution. |

#### NetBSD

| Install | What it adds |
|---|---|
| pkgsrc **`java-jna`**, with `-Djna.boot.library.path=/usr/pkg/lib` | Native calls for `oshi-core`, which JNA cannot load from its own jar on NetBSD. Without it, `oshi-core` runs commands instead (`sysctl` for CPU ticks and load average, `netstat` for the routing table), which is slower but reports the same values, with three exceptions: the current thread ID is `0`, other processes' environment variables are empty, and the current process's open-file limits are the system-wide ones. `oshi-core-ffm` does not need it. |

#### AIX

Nothing to install; `libperfstat` is part of the base system.

### Does OSHI work in containers (Docker, Kubernetes)?

OSHI reads from the same OS-level sources (procfs, sysfs, WMI, etc.) regardless of whether it runs inside a container. This means it generally reports **host-level** information, not container-scoped values. Common issues reported by users include:

 - **CPU ticks reflect the host.** `getSystemCpuLoadTicks()` reads `/proc/stat`, which shows all host CPUs even when the container has a CPU limit. On Windows Server 2019 containers, performance counters may be entirely absent, causing all tick values to be zero ([#1976](https://github.com/oshi/oshi/issues/1976), [#2217](https://github.com/oshi/oshi/issues/2217)).
 - **Memory reports host totals.** `getTotal()` returns the host's physical memory, not the cgroup memory limit ([#893](https://github.com/oshi/oshi/issues/893)).
 - **Missing native libraries.** Minimal base images (Alpine, distroless) may lack `libudev`, causing `UnsatisfiedLinkError` when accessing the processor or disk stores. Ensure your image includes `libudev` or use OSHI 6.2+ which added a fallback ([#2032](https://github.com/oshi/oshi/issues/2032), [#2092](https://github.com/oshi/oshi/issues/2092)). See [optional software](#what-optional-software-gives-oshi-more-to-report) for what else a slim image leaves out and what each item costs you.
 - **Hardware identifiers are unavailable.** Serial numbers, baseboard info, and disk details are typically not exposed to containers and will return "unknown" ([#2620](https://github.com/oshi/oshi/issues/2620)).
 - **File store duplication.** Docker's overlay mounts can cause the same device to appear multiple times in `getFileStores()` ([#438](https://github.com/oshi/oshi/issues/438)).

If you need container-scoped resource limits rather than host values, use OSHI's `CgroupInfo` API, which abstracts the differences between cgroup v1 and v2:

```java
OperatingSystem os = SystemInfoFactory.create().getOperatingSystem();
CgroupInfo cgroup = os.getCgroupInfo();

if (cgroup.isContainerized()) {
    double effectiveCpus = cgroup.getEffectiveCpus();  // quota / period
    long memoryLimit = cgroup.getMemoryLimit();        // bytes, or UNLIMITED_MEMORY
    long memoryUsage = cgroup.getMemoryUsage();        // current usage in bytes
}
```

See the [`CgroupInfo`](https://oshi.github.io/oshi/oshi-core/apidocs/com.github.oshi.common/oshi/software/os/CgroupInfo.html) Javadoc for the full API.

### How can I get reliable sensor information on Windows?

Windows sensor information is unreliable through the supported Windows API: `Win32_Fan` and
`MSAcpi_ThermalZoneTemperature` are frequently unimplemented, and `Win32_Processor` reports voltage as a handful of
capability bits. The two better sources are listed under
[optional software](#what-optional-software-gives-oshi-more-to-report) above; OSHI tries them in order and uses
whichever answers, so nothing needs configuring to prefer one. When neither answers, OSHI still asks those Windows
classes.

The monitoring applications need no dependency at all. The second source does, and it comes with caveats worth reading
before you adopt it.

#### The optional jLibreHardwareMonitor dependency

[jLibreHardwareMonitor](https://github.com/pandalxb/jLibreHardwareMonitor) ships the monitoring libraries itself, so no
application has to be running. It is not included transitively, due to its single-OS relevance and MPL 2.0-licensed
binary DLLs. To include it, define the dependency in your own project **with an explicit version** — OSHI publishes no
BOM and manages only the JNA artifacts, so a version-less declaration will not resolve. Using Maven:

```xml
<dependency>
    <groupId>io.github.pandalxb</groupId>
    <artifactId>jLibreHardwareMonitor</artifactId>
    <version>1.0.6</version>
</dependency>
```

Or Gradle:

```groovy
implementation("io.github.pandalxb:jLibreHardwareMonitor:1.0.6")
```

Your JVM must run **as Administrator**, as the underlying library loads a kernel driver for most sensors.

Note one known limitation: this dependency reaches the monitoring libraries through a PowerShell session, and on
Windows Server 2019 / Windows 10 1809-era hosts whose console handle is not a real screen buffer — a CI build agent,
for example — PowerShell writes an error banner into the output being parsed, so no sensor data is returned and the
dependency logs at ERROR. OSHI falls back to plain WMI in that case. See
[issue #3707](https://github.com/oshi/oshi/issues/3707) for the details and current status.

#### Turning off the sources you do not use

Either application can be started or stopped at any time, so OSHI queries their namespaces on each sensor read, in the
order below, until one returns data, and moves on to the next source when neither is running. If you know you do not run one of
them, say so up front and OSHI will not attempt the query:

| Property | Skips |
|---|---|
| `oshi.os.windows.ohm.disabled` | The `ROOT\OpenHardwareMonitor` namespace |
| `oshi.os.windows.lhm.disabled` | The `ROOT\LibreHardwareMonitor` namespace, for GPU metrics as well as sensors |

Both default to `false`. The jLibreHardwareMonitor dependency needs no such switch: it is not transitive, so declaring
it is itself the decision to use it, and OSHI checks once whether it is on the class path. If the PowerShell limitation
above affects you, remove the dependency.

## Troubleshooting and Unexpected Values

### How do I resolve JNA `NoClassDefFoundError` or `NoSuchMethodError` issues?

OSHI uses the latest version of JNA, which may conflict with other dependencies your project (or its parent) includes.
If you experience a `NoClassDefFoundError` or `NoSuchMethodError` issues with JNA artifacts, likely causes include file system
permissions or an older version of either `jna` or `jna-platform` in your classpath from a transitive dependency on another
project. Consider one or more of the following steps to resolve the conflict:
 - Ensure you are using the most recent version of JNA (both `jna` and `jna-platform` artifacts) in your `pom.xml` or `build.gradle`. Use a dependency analyzer to verify the resolved version.
 - If using Maven, import OSHI's dependency management per [Maven Documentation](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#importing-dependencies) (preferred), or list OSHI earlier (or first) in your dependency list to influence dependency resolution.
 - If using Gradle, force the JNA version in your `build.gradle` using `resolutionStrategy { force 'net.java.dev.jna:jna:<version>' }` for both `jna` and `jna-platform` artifacts.
 - JNA needs to write its [native DLL](https://javadoc.io/static/net.java.dev.jna/jna/5.13.0/com/sun/jna/Native.html), usually to a temporary file unless you've configured otherwise. File system permissions or capacity may prevent this from happening. Pre-extracting the DLL and placing it in a known location resolves this.
 - If you are using the Spring Boot Starter Parent version 2.2 and earlier that includes JNA as a dependency:
   - Upgrade to version 2.3 which does not have a JNA dependency (preferred)
   - If you must use version 2.2 or earlier, override the `jna.version` property to the latest JNA version.

For Android, see the [JNA FAQ](https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#jna-on-android) for additional requirements relating to Android and ProGuard, specifically:
 - Use the AAR artifact dependency rather than the JAR (for `jna` dependency only):
   - In Gradle (`build.gradle`), you'll need to add `@aar` after the version
   - In Maven (`pom.xml`), you'll need to specify `<type>aar</type>`
   - In both cases you should add an exclusion to your `oshi-core` dependency for the (default) `jna` JAR artifact.
 - In ProGuard, use `-keep` directives to prevent obfuscating JNA classes

### How do I resolve `Pdh call failed with error code 0xC0000BB8` issues?

OSHI (and many other programs) rely on the English Performance Counter indices in the registry. These are located at `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Perflib\009\Counter`. Sometimes when configuring localized Windows installations, these values become corrupt or are missing.

If you receive this PDH error code, investigate whether your English (page 009) performance counters are corrupt. [Rebuild them](https://docs.microsoft.com/en-us/troubleshoot/windows-server/performance/rebuild-performance-counter-library-values) if necessary.

### Why does OSHI freeze for 20 seconds (or larger multiples of 20 seconds) on Windows when it first starts up?

The initial call to some Windows Management Instrumentation (WMI) queries sometimes trigger RPC-related negotiation delays and timeouts described [here](https://docs.microsoft.com/en-us/windows/win32/services/services-and-rpc-tcp). OSHI attempts to use performance counters in preference to WMI whenever possible, but includes the WMI queries as a backup. There are several potential causes of these delays, which seem to occur more often on corporate-managed machines. If you are experiencing these delays, you can configure RPC and shorten the timeout by altering registry values under `HKLM\SYSTEM\CurrentControlSet\Control`. The `SCMApiConnectionParam` value (defaults to 21000 ms) can be reduced to shorten the delay.

### Why does OSHI's System and Processor CPU usage differ from the Windows Task Manager?

CPU usage is generally calculated as (active time / active+idle time).

For System and per-Processor CPU ticks calculations, the total number of "idle" ticks is available for this calculation, which matches operating system displays on Windows 7 and earlier, and Unix-based operating systems, and CPU usage will never exceed 100%.

Starting with Windows 8, a change was made to the way that Task Manager and Performance Monitor report CPU utilization.
The values in Task Manager now correspond to the `Processor Information\% Processor Utility` and `Processor Information\% Privileged Utility` performance counters, not to the `Processor Information\% Processor Time` and `Processor Information\% Privileged Time` counters as in Windows 7.

This fundamentally changes the Task Manager's meaning of "CPU Usage". Windows documentation for `% Processor Time` states:
> % Processor Time is the percentage of elapsed time that the processor spends to execute a non-Idle thread... This counter is the primary indicator of processor activity, and displays the average percentage of busy time observed during the sample interval.

The documentation for `% Processor Utility` now used by the Task Manager displays a different metric:
> Processor Utility is the amount of work a processor is completing, as a percentage of the amount of work the processor could complete if it were running at its nominal performance and never idle. On some processors, Processor Utility may exceed 100%.

Features which change CPU frequency such as Intel Speed Step, Intel Turbo Boost, AMD Precision Boost, and others, can cause this value to exceed 100% on both individual processors and the entire system. While a "work completed" metric has some benefits as a performance measure, the Task Manager caps the value at 100%, which means the Task Manager shows "the amount of work a processor is completing compared to its nominal performance, except if it's over 100% we won't tell you how much extra work it's doing."

If you desire OSHI's output to match the Task Manager, you may optionally enable this setting in the configuration file or using a Java System Property, or by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);` shortly after startup (at least before the first instantiation of the Central Processor class). Note that OSHI will not cap its CPU Usage calculation at 100%, giving you more information than the Windows Task Manager if the "work completed" metric is important to you.

For this calculation to produce meaningful data, the ticks used to calculate usage must have come from the same instance of CentralProcessor. Also, the first polling interval must be less than 7 minutes to properly initialize values.

### Why does OSHI's Process CPU usage differ from the Windows Task Manager?

CPU usage is generally calculated as (active time / active+idle time). On a multi-processor system, the "idle" time can be accrued on each/any of the logical processors.

For per-Process CPU ticks, there is no "idle" counter available, so the calculation ends up being (active time / up time). It is possible
for a multi-threaded process to accrue more active clock time than elapsed clock time, and result in CPU usage over 100%
(e.g., on a 4-processor system it could in theory reach 400%). This interpretation matches the value displayed in `ps` or `top` on
Unix-based operating systems. However, Windows scales process CPU usage to the system, so that the sum of all Process CPU percentages
can never exceed 100% (ignoring roundoff errors). On a 4-processor system, a single-threaded process maximizing usage of one logical
processor will show (on Windows) as 25% usage. OSHI's calculation for Process CPU load will report the Unix-based calculation in this
class, which would be closer to 100%.

If you want per-Process CPU load to match the Windows Task Manager display, you should divide OSHI's calculation by the number of logical processors.  This is an entirely cosmetic preference.

### Why is a BSD process's CPU time sometimes flat when I expect it to have increased?

Because OSHI clamps it. The BSD `ps` time columns are not reliably monotonic, so `updateAttributes()` on an
`OSProcess` or `OSThread` never lets `getKernelTime()` or `getUserTime()` report a value below the one it last
reported; a read that would decrease holds at the previous value instead.

On DragonFly the `TIME` column is a sum over the process's *currently live* LWPs (threads) rather than a monotonic
accumulator. When the `KERN_PROC_FLAG_LWP` flag is absent, the kernel accumulates every live LWP into a single record
(`kl_uticks += lwp->lwp_thread->td_uticks` in `fill_kinfo_lwp()`, with the per-LWP `bzero` skipped in
`sysctl_out_proc()`), and `ps` prints `uticks + sticks + iticks` from that record. An exited LWP's usage folds into the
process-level `p_ru`, which the `TIME` column never reads — so when a thread exits, the CPU time it had accumulated
leaves the total. Sampling a busy JVM every 500ms on DragonFly 6.4 produced 7.80, 9.22, 9.78, 8.25, 8.80, 8.19, 8.19,
8.19 seconds while `NLWP` moved 30, 34, 34, 34, 34, 33, 33, 33; the 33 per-thread rows summed to 8.29s against a
process row of 8.26s.

FreeBSD's kernel does the same kind of clamping internally: `calcru1()` divides a high-resolution runtime by a
statistical user/system tick ratio, enforces monotonicity on the resulting buckets, restores the previous values for
regressions under 3µs or 1%, and prints `calcru: runtime went backwards` for larger ones. DragonFly has no `calcru1()`
and no such warning, so OSHI applies the equivalent guard in userland for the whole BSD family.

Note also that DragonFly's `ps` offers neither a `systime` nor a `cputime` keyword, so `OSProcess.getKernelTime()` is
always 0 there and `getUserTime()` carries the entire total.

### Why is `getCurrentFreq()` the same number every time on Apple Silicon?

Because by default it is not a measurement. macOS exposes no equivalent of Linux's `scaling_cur_freq`, so each core
reports its cluster's nominal maximum from the power manager's `voltage-states` table — a fixed hardware property. On
an M3 Pro that is 2.7 GHz for the six efficiency cores and 4.1 GHz for the six performance cores, whatever the machine
is doing.

The real frequency is derivable from the private IOReport framework, which publishes how many ticks each core, and each
cluster of cores, spent in each of its DVFS states. Setting `oshi.os.mac.cpu.frequency.ioreport` to `true` reports the
average of a cluster's frequencies weighted by that residency, with idle time excluded — the frequency the hardware ran
at while it had work, which is the figure `powermetrics` prints as the cluster's HW active frequency. Cores in a cluster
share one frequency domain and so share that value, and a core that did not run at all over the interval reports its
cluster's lowest frequency instead.

The per-core channels are deliberately not used for the value: a core reports the state it *asked* for, which is the
fastest one whenever it has work, so under a power or thermal limit it reads high. On an M3 Pro with six busy threads
every performance core reports 4.06 GHz — the nominal maximum, which is what this setting exists to replace — while the
cluster reports the 3.58 GHz that `powermetrics` agrees it ran at. They are the fallback: a chip whose cluster channels
cannot be matched to the kinds of core it reports is read from the cores' own residency instead, which is right whenever
nothing is capping the cluster.

It is opt-in for two reasons. IOReport is not a public API, and reading it means holding a subscription for the
lifetime of the process. And the value is a rate, so it needs an interval: the first call establishes the baseline and
returns the nominal frequencies, and each later call covers the time since the previous one. A program that reads
`getCurrentFreq()` once will therefore see no difference.
