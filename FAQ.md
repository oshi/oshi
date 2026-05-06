# Frequently Asked Questions

  * [What is the intended use of the API?](#what-is-the-intended-use-of-the-api)
  * [Is the API backwards compatible between versions?](#is-the-api-backwards-compatible-between-versions)
  * [Does OSHI support Open Service Gateway initiative (OSGi) modules?](#does-oshi-support-open-service-gateway-initiative-osgi-modules)
  * [Does OSHI support Java Module System (JPMS) modules?](#does-oshi-support-java-module-system-jpms-modules)
  * [Is OSHI Thread Safe?](#is-oshi-thread-safe)
  * [What minimum Java version is required?](#what-minimum-java-version-is-required)
  * [Which operating systems are supported?](#which-operating-systems-are-supported)
  * [How can I get reliable sensor information on Windows?](#how-can-i-get-reliable-sensor-information-on-windows)
  * [How do I resolve `Pdh call failed with error code 0xC0000BB8` issues?](#how-do-i-resolve-pdh-call-failed-with-error-code-0xc0000bb8-issues)
  * [How do I resolve JNA `NoClassDefFoundError` or `NoSuchMethodError` issues?](#how-do-i-resolve-jna-noclassdeffounderror-or-nosuchmethoderror-issues)
  * [Does OSHI work in containers (Docker, Kubernetes)?](#does-oshi-work-in-containers-docker-kubernetes)
  * [How do I configure OSHI?](#how-do-i-configure-oshi)
  * [How does OSHI support the Principle of Least Privilege?](#how-does-oshi-support-the-principle-of-least-privilege)
  * [How do I get CPU usage?](#how-do-i-get-cpu-usage)
  * [Why does OSHI's System and Processor CPU usage differ from the Windows Task Manager?](#why-does-oshi-s-system-and-processor-cpu-usage-differ-from-the-windows-task-manager)
  * [Why does OSHI's Process CPU usage differ from the Windows Task Manager?](#why-does-oshi-s-process-cpu-usage-differ-from-the-windows-task-manager)
  * [Why does OSHI freeze for 20 seconds (or larger multiples of 20 seconds) on Windows when it first starts up?](#why-does-oshi-freeze-for-20-seconds-or-larger-multiples-of-20-seconds-on-windows-when-it-first-starts-up)
  * [How is OSHI different from SIGAR?](#how-is-oshi-different-from-sigar)
  * [Does OSHI work on ...](#does-oshi-work-on)
    + [ARM hardware?](#arm-hardware)
    + [Apple Silicon hardware?](#apple-silicon-hardware)
    + [Raspberry Pi hardware?](#raspberry-pi-hardware)
  * [Will you implement ... ?](#will-you-implement)

---

## What is the intended use of the API?

Users should create a new instance of SystemInfo ([JNA](https://oshi.github.io/oshi/oshi-core/apidocs/com.github.oshi/oshi/SystemInfo.html) \| [FFM](https://oshi.github.io/oshi/oshi-core-ffm/apidocs/com.github.oshi.ffm/oshi/ffm/SystemInfo.html)) and use the getters from this class to access the platform-specific hardware and software interfaces using the respective `get*()` methods. The interfaces in `oshi.hardware` and `oshi.software.os` provide cross-platform functionality. See the `main()` method of [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) for sample code.

Methods return a "snapshot" of current levels. To display values which change over time, it is intended that users poll for information no more frequently than approximately every second. Disk and file system calls may incur some latency and should be polled less frequently.
CPU usage calculation precision depends on the relation of the polling interval to both system clock tick granularity and the number of logical processors.

## Is the API backwards compatible between versions?

OSHI follows [Semantic Versioning](https://semver.org/). The interfaces and classes in `oshi.hardware` and `oshi.software.os` are considered the OSHI API and are guaranteed to be compatible within the same major version. Classes and interfaces annotated with `@PublicApi` are part of this contract. Differences between major versions can be found in the [Upgrading.md](UPGRADING.md) document.

Most, if not all, of the platform-specific implementations of these APIs in lower level packages will remain the same, although it is not intended that users access platform-specific code, and some changes may occur between minor versions, most often in the number of arguments passed to constructors or platform-specific methods. Supporting code in the `oshi.driver` and `oshi.util` packages may, rarely, change between minor versions, usually associated with organizing package structure or changing parsing methods for efficiency/consistency/ease of use.

Code in the platform-specific `oshi.jna.*` packages is intended to be temporary and will be removed when that respective code is included in the JNA project.

## Does OSHI support Open Service Gateway initiative (OSGi) modules?

OSHI adds OSGi manifest entries using `maven-source-plugin` and `mvn-bnd-plugin`. Submit an issue if the configuration of these plugins needs to be adjusted to support your project.

## Does OSHI support Java Module System (JPMS) modules?

Yes. OSHI provides two named JPMS modules:

- `com.github.oshi` — the JNA-based implementation (`oshi-core`). Works on JDK 8+ on the classpath; JDK 9+ on the module path.
- `com.github.oshi.ffm` — the FFM-based implementation (`oshi-core-ffm`). Requires JDK 25+.

Both modules export the public API packages and declare the appropriate `requires` directives. Add the one that matches your native access preference to your `module-info.java`:

```java
requires com.github.oshi;     // JNA
requires com.github.oshi.ffm; // FFM
```

Note: In OSHI 6.x, `oshi-core` only had an `Automatic-Module-Name` and a separate `oshi-core-java11` artifact provided the full module descriptor. Starting with OSHI 7.0, `oshi-core` includes the module descriptor directly.

## Is OSHI Thread Safe?

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

## What minimum Java version is required?

OSHI 4.x and later require minimum Java 8 compatibility for the JNA implementation (`oshi-core`). This minimum level will be retained through at least OpenJDK 8 EOL.

Starting with OSHI 7.x, the FFM implementation (`oshi-core-ffm`) requires JDK 25+. The Foreign Function & Memory API became final in JDK 22 (JEP 454), but OSHI targets JDK 25 as the first LTS release with FFM support.

OSHI 3.x is compatible with Java 7 up to 3.13.x versions. OSHI 3.14.0 restored Java 6 compatibility for the `oshi-core` artifact only. These versions are no longer actively maintained.

## Which operating systems are supported?

OSHI has been implemented and tested on the following systems.  Some features may work on earlier versions.
* Windows 7 and higher.  (Nearly all features work on Vista and most work on Windows XP.)
* macOS version 10.6 (Snow Leopard) and higher.
* Linux (Most major distributions) Kernel 2.6 and higher
* FreeBSD 10
* OpenBSD 6.8
* Solaris 11 (SunOS 5.11)
* AIX 7.1 (POWER4)
* Android 7.0 and higher

The FFM implementation (`oshi-core-ffm`) supports Windows, macOS, and Linux only, and assumes a 64-bit operating system.

## How can I get reliable sensor information on Windows?

Windows sensor information is unreliable via the supported Windows API.  OSHI includes an optional dependency on [jLibreHardwareMonitor](https://github.com/pandalxb/jLibreHardwareMonitor) which gives much more reliable sensor data, but is not included transitively due to its single-OS relevance and MPL 2.0-licensed binary DLLs. To include it, define the dependency in your own project.  You can do so using Maven:

```
<dependency>
    <groupId>io.github.pandalxb</groupId>
    <artifactId>jLibreHardwareMonitor</artifactId>
</dependency>
```

Or Gradle:

```
implementation("io.github.pandalxb:jLibreHardwareMonitor")
```

## How do I resolve `Pdh call failed with error code 0xC0000BB8` issues?

OSHI (and many other programs) rely on the English Performance Counter indices in the registry. These are located at `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Perflib\009\Counter`. Sometimes when configuring localized Windows installations, these values become corrupt or are missing.

If you receive this PDH error code, investigate whether your English (page 009) performance counters are corrupt. [Rebuild them](https://docs.microsoft.com/en-us/troubleshoot/windows-server/performance/rebuild-performance-counter-library-values) if necessary.

## How do I resolve JNA `NoClassDefFoundError` or `NoSuchMethodError` issues?

OSHI uses the latest version of JNA, which may conflict with other dependencies your project (or its parent) includes.
If you experience a `NoClassDefFoundError` or `NoSuchMethodError` issues with JNA artifacts, likely causes include file system
permissions or an older version of either `jna` or `jna-platform` in your classpath from a transitive dependency on another
project. Consider one or more of the following steps to resolve the conflict:
 - JNA needs to write its [native DLL](https://javadoc.io/static/net.java.dev.jna/jna/5.13.0/com/sun/jna/Native.html), usually to a temporary file unless you've configured otherwise. File system permissions or capacity may prevent this from happening. Pre-extracting the DLL and placing it in a known location resolves this.
 - Use a dependency analyzer to verify you're importing the correct version.
 - If using Maven, import OSHI's dependency management per [Maven Documentation](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#importing-dependencies)
 - If using Maven, list OSHI earlier (or first) in your dependency list to influence dependency resolution.
 - Specify the most recent version of JNA (both `jna` and `jna-platform` artifacts) in your `pom.xml` (For Gradle, `build.gradle` includes additional options to force the version). For Android, see the next paragraph.
 - If you are using the Spring Boot Starter Parent version 2.2 and earlier that includes JNA as a dependency:
   - Upgrade to version 2.3 which does not have a JNA dependency (preferred)
   - If you must use version 2.2 or earlier, override the `jna.version` property to the latest JNA version.

For Android, see the [JNA FAQ](https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#jna-on-android) for additional requirements relating to Android and ProGuard, specifically:
 - Use the AAR artifact dependency rather than the JAR (for `jna` dependency only):
   - In Gradle (`build.gradle`), you'll need to add `@aar` after the version
   - In Maven (`pom.xml`), you'll need to specify `<type>aar</type>`
   - In both cases you should add an exclusion to your `oshi-core` dependency for the (default) `jna` JAR artifact.
 - In ProGuard, use `-keep` directives to prevent obfuscating JNA classes

## Does OSHI work in containers (Docker, Kubernetes)?

OSHI reads from the same OS-level sources (procfs, sysfs, WMI, etc.) regardless of whether it runs inside a container. This means it generally reports **host-level** information, not container-scoped values. Common issues reported by users include:

 - **CPU ticks reflect the host.** `getSystemCpuLoadTicks()` reads `/proc/stat`, which shows all host CPUs even when the container has a CPU limit. On Windows Server 2019 containers, performance counters may be entirely absent, causing all tick values to be zero ([#1976](https://github.com/oshi/oshi/issues/1976), [#2217](https://github.com/oshi/oshi/issues/2217)).
 - **Memory reports host totals.** `getTotal()` returns the host's physical memory, not the cgroup memory limit ([#893](https://github.com/oshi/oshi/issues/893)).
 - **Missing native libraries.** Minimal base images (Alpine, distroless) may lack `libudev`, causing `UnsatisfiedLinkError` when accessing the processor or disk stores. Ensure your image includes `libudev` or use OSHI 6.2+ which added a fallback ([#2032](https://github.com/oshi/oshi/issues/2032), [#2092](https://github.com/oshi/oshi/issues/2092)).
 - **Hardware identifiers are unavailable.** Serial numbers, baseboard info, and disk details are typically not exposed to containers and will return "unknown" ([#2620](https://github.com/oshi/oshi/issues/2620)).
 - **File store duplication.** Docker's overlay mounts can cause the same device to appear multiple times in `getFileStores()` ([#438](https://github.com/oshi/oshi/issues/438)).

If you need container-scoped resource limits rather than host values, read them directly from the cgroup filesystem. OSHI's `FileUtil` class (in the `oshi.util` package) can read these files. On **cgroup v2** (default on modern kernels and Kubernetes):

```java
// CPU limit: "200000 100000" means 200ms quota per 100ms period = 2 CPUs
String cpuMax = FileUtil.getStringFromFile("/sys/fs/cgroup/cpu.max");

// Memory limit (bytes), or "max" if unlimited
String memMax = FileUtil.getStringFromFile("/sys/fs/cgroup/memory.max");

// Current memory usage (bytes)
long memCurrent = FileUtil.getLongFromFile("/sys/fs/cgroup/memory.current");
```

On **cgroup v1** (older Docker / Kubernetes):

```java
// CPU quota (microseconds per period, -1 if unlimited) and period
long cpuQuota = FileUtil.getLongFromFile("/sys/fs/cgroup/cpu/cpu.cfs_quota_us");
long cpuPeriod = FileUtil.getLongFromFile("/sys/fs/cgroup/cpu/cpu.cfs_period_us");

// Memory limit (bytes)
long memLimit = FileUtil.getLongFromFile("/sys/fs/cgroup/memory/memory.limit_in_bytes");
```

## How do I configure OSHI?

OSHI supports multiple configuration mechanisms with a clear precedence order. Higher-priority sources override lower ones:

| Priority | Source | Example |
|----------|--------|---------|
| 1 (highest) | `GlobalConfig.set()` in Java code | `GlobalConfig.set("oshi.os.linux.privileged.prefix", "sudo -n");` |
| 2 | Java system properties | `java -Doshi.os.linux.privileged.prefix="sudo -n" -jar app.jar` |
| 3 | Environment variables (`OSHI_*`) | `export OSHI_OS_LINUX_PRIVILEGED_PREFIX="sudo -n"` |
| 4 | External properties file | `java -Doshi.properties.file=/etc/oshi.conf -jar app.jar` |
| 5 (lowest) | `oshi.properties` on classpath | Place in `src/main/resources/oshi.properties` |

### Environment variables

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

### External properties file

Point OSHI at a properties file on disk using the `oshi.properties.file` system property:

```sh
java -Doshi.properties.file=/etc/oshi.conf -jar app.jar
```

This is useful for server deployments where config lives in `/etc/` or Docker containers mounting config files as volumes.

### Java system properties

Pass properties on the command line with `-D`:

```sh
java -Doshi.os.linux.privileged.prefix="sudo -n" -Doshi.util.memoizer.expiration=500 -jar app.jar
```

### Programmatic configuration

Set values in Java code at startup, before creating any OSHI objects:

```java
GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 500);
```

### Properties file

Place an `oshi.properties` file in `src/main/resources/` to override defaults. See the [default oshi.properties](https://github.com/oshi/oshi/blob/master/oshi-common/src/main/resources/oshi.properties) for all available keys and their defaults.

### Spring Boot integration

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

## How does OSHI support the Principle of Least Privilege?

OSHI is designed to work without elevated permissions. The vast majority of system information — CPU, memory, disks, network, processes, and more — is available to any unprivileged user on all supported platforms. You should **not** need to run your application as root or Administrator just to use OSHI.

However, some specific features require elevated permissions to access. Rather than running your entire application with elevated privileges (which violates the [Principle of Least Privilege](https://en.wikipedia.org/wiki/Principle_of_least_privilege) and introduces unnecessary risk), OSHI provides mechanisms to grant fine-grained access to only the specific resources that need it.

### What requires elevated permissions?

**Linux:**
- Hardware details via `dmidecode` (serial numbers, BIOS info, physical memory details)
- Some `/proc/<pid>` files (e.g., `/proc/<pid>/io` for per-process I/O stats)
- Logical volume group information via `pvs`/`lvs`

**Windows:**
- Process command lines and environment variables for processes owned by other users (requires `SeDebugPrivilege` or Administrator)
- Sensor data (temperature, fan speeds) via [jLibreHardwareMonitor](https://github.com/oshi/jLibreHardwareMonitor)

**macOS:**
- TCP/UDP connection details (without elevation, connection data is limited)

### Linux: Configurable privilege escalation via sudo

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
   Or set equivalent values in `oshi.properties` or as Java system properties.

**How it works:**
- The prefix is **not** applied when already running as root (uid=0).
- Only commands and files explicitly listed in the allowlists will use privilege escalation.
- For file reads, OSHI first attempts a normal read; only if the file exists but is not readable does it fall back to the privileged `cat` command.
- The prefix is flexible — it works with `sudo`, `doas`, or a custom wrapper script for stricter environments.

**Security notes:**
- Restrict your `sudoers` entries to the minimum set of commands required.
- Take care with the file allowlist — avoid paths like `/proc/*/environ` which could expose credentials.
- For higher-security environments, consider a wrapper script that validates arguments before executing the privileged command. See [#3100](https://github.com/oshi/oshi/issues/3100) for examples.

## How do I get CPU usage?

CPU usage is computed by comparing tick counters at two points in time. A single snapshot is meaningless on its own — you must poll at least twice and calculate the difference.

**System-level CPU usage** uses `CentralProcessor.getSystemCpuLoadTicks()`, which returns 8 tick values (User, Nice, System, Idle, IOWait, IRQ, SoftIRQ, Steal). The ratio of active ticks to total ticks (active + idle) gives the CPU load:

```java
CentralProcessor cpu = new SystemInfo().getHardware().getProcessor();

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
SystemInfo si = new SystemInfo();
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

## Why does OSHI's System and Processor CPU usage differ from the Windows Task Manager?

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

## Why does OSHI's Process CPU usage differ from the Windows Task Manager?

CPU usage is generally calculated as (active time / active+idle time). On a multi-processor system, the "idle" time can be accrued on each/any of the logical processors.

For per-Process CPU ticks, there is no "idle" counter available, so the calculation ends up being (active time / up time). It is possible
for a multi-threaded process to accrue more active clock time than elapsed clock time, and result in CPU usage over 100%
(e.g., on a 4-processor system it could in theory reach 400%). This interpretation matches the value displayed in `ps` or `top` on
Unix-based operating systems. However, Windows scales process CPU usage to the system, so that the sum of all Process CPU percentages
can never exceed 100% (ignoring roundoff errors). On a 4-processor system, a single-threaded process maximizing usage of one logical
processor will show (on Windows) as 25% usage. OSHI's calculation for Process CPU load will report the Unix-based calculation in this
class, which would be closer to 100%.

If you want per-Process CPU load to match the Windows Task Manager display, you should divide OSHI's calculation by the number of logical processors.  This is an entirely cosmetic preference.

## Why does OSHI freeze for 20 seconds (or larger multiples of 20 seconds) on Windows when it first starts up?

The initial call to some Windows Management Instrumentation (WMI) queries sometimes trigger RPC-related negotiation delays and timeouts described [here](https://docs.microsoft.com/en-us/windows/win32/services/services-and-rpc-tcp). OSHI attempts to use performance counters in preference to WMI whenever possible, but includes the WMI queries as a backup. There are several potential causes of these delays, which seem to occur more often on corporate-managed machines. If you are experiencing these delays, you can configure RPC and shorten the timeout by altering registry values under `HKLM\SYSTEM\CurrentControlSet\Control`. The `SCMApiConnectionParam` value (defaults to 21000 ms) can be reduced to shorten the delay.

## How is OSHI different from SIGAR?

Both OSHI and Hyperic's [SIGAR](https://github.com/hyperic/sigar) (System Information Gatherer and Reporter)
provide cross-platform operating system and hardware information, and are both used to support distributed
system monitoring and reporting, among other use cases. The OSHI project was started, and development
continues, to overcome specific shortcomings in SIGAR for some use cases.  OSHI does have feature parity
with nearly all SIGAR functions. Key differences include:
 - **Additional DLL** SIGAR's implementation is primarily in native C, compiled separately for its supported
operating systems. It therefore requires users to download an additional DLL specific to their operating
system. This does have a few advantages for specific, targeted use cases, including faster native code routines,
and availability of some native compiler intrinsics. In contrast, OSHI accesses native APIs using JNA, which
does not require user installation of any additional platform-specific DLLs.
 - **Corporate Development / Abandonment** SIGAR was developed commercially at Hyperic to support monitoring of
their HQ product. Hyperic's products were later acquired by VMWare, which has transitioned away from Hyperic
products and have completely abandoned SIGAR. The [last release](https://github.com/hyperic/sigar/releases/tag/sigar-1.6.4)
was in 2010 and the [last source commit](https://github.com/hyperic/sigar/commit/7a6aefc7fb315fc92445edcb902a787a6f0ddbd9)
was in 2015. [Multiple independent forks](https://github.com/hyperic/sigar/issues/95) by existing users attempt
to fix specific bugs/incompatibilities but none has emerged as a maintained/released fork.  In contrast, OSHI's
development has been entirely done by open source volunteers, and it is under active development as of 2026.
 - **Support** SIGAR is completely unsupported by its authors, and there is no organized community support.
OSHI is supported actively to fix bugs, respond to questions, and implement new features.

## Does OSHI work on ...

### ARM hardware?

Yes, CI is actively conducted on Linux ARM hardware and other platforms will be added when hardware is
available for such testing. Note that many features (e.g., CPUID, and processor identification such as
family, model, stepping, and vendor frequency) are based on Intel chips and may have different corresponding
meanings.

### Apple Silicon hardware?

OSHI works with native `AArch64` support when JNA is version 5.7.0 or later.

OSHI works using virtual x86 hardware under Rosetta if you are executing an x86-based JVM.

### Raspberry Pi hardware?

Yes, most of the Linux code works here and other Pi-specific code has been implemented but has seen
limited testing.  As the developers do not have a Pi to test on, users reporting issues should be
prepared to help test solutions.

## Will you implement ... ?

Maybe!  If you can contribute all the code to implement the feature, it will almost certainly be added.  Even if you can't code but can provide pointers to where the information can be found cross-platform, your feature has a good chance. Otherwise, you can always submit an issue to ask, but are at the mercy of the developers' time, enthusiasm level, and the availability of documentation for the feature.
