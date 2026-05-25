![OSHI](https://dl.dropboxusercontent.com/s/c82qboyvvudpvdp/oshilogo.png)

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.oshi/oshi-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?namespace=com.github.oshi&sort=name)
[![javadoc](https://javadoc.io/badge2/com.github.oshi/oshi-core/javadoc.svg)](https://javadoc.io/doc/com.github.oshi/oshi-core)
[![first-timers-only](https://img.shields.io/badge/first--timers--only-friendly-blue.svg?style=flat-square)](https://www.firsttimersonly.com/)
[![GitHub contributors](https://img.shields.io/github/contributors/oshi/oshi)](https://github.com/oshi/oshi/graphs/contributors)

OSHI is a free native (JNA or FFM) Operating System and Hardware Information library for Java.
It does not require the installation of any additional native libraries and aims to provide a
cross-platform implementation to retrieve system information, such as OS version, processes,
memory and CPU usage, disks and partitions, devices, sensors, etc.

Supported Platforms
---------------------------
- Windows
- macOS
- Linux (Android)
- UNIX (AIX, FreeBSD, OpenBSD, Solaris)

Supported Features
------------------
* Computer System and firmware, baseboard
* Operating System and Version/Build
* Physical (core) and Logical (hyperthreaded) CPUs, processor groups, NUMA nodes
* System and per-processor load, usage tick counters, interrupts, uptime
* Process uptime, CPU, memory usage, user/group, command line args, thread details
* Physical and virtual memory used/available
* Mounted filesystems (type, usable and total space, options, reads and writes)
* Disk drives (model, serial, size, reads and writes) and partitions
* Network interfaces (IPs, bandwidth in/out), network parameters, TCP/UDP statistics
* Battery state (% capacity, time remaining, power usage stats)
* Peripheral devices (USB, Bluetooth)
* Connected displays (with EDID info), graphics and audio cards
* Sensors (temperature, fan speeds, voltage) on some hardware
* Container resource limits and usage (cgroup v1/v2)
* Printers (name, status, driver)

Native Access Implementations
----------------------------
OSHI provides two native access implementations:
- **JNA** (`oshi-core`): Uses [Java Native Access](https://github.com/java-native-access/jna). Supports JDK 8+. JPMS module: `com.github.oshi`.
- **FFM** (`oshi-core-ffm`): Uses the JDK [Foreign Function & Memory API](https://openjdk.org/jeps/454). Requires JDK 25+. JPMS module: `com.github.oshi.ffm`.

Both implementations share the same API interfaces from `oshi-common`. Choose one at compile time, or include both and select at runtime (see [Usage](#usage) below).

Downloads and Dependency Management
-----------------------------------
Stable Release Versions
  * JNA: [oshi-core-7.2.0](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/7.2.0)
  * FFM: [oshi-core-ffm-7.2.0](https://central.sonatype.com/artifact/com.github.oshi/oshi-core-ffm/7.2.0)

Current Development (SNAPSHOT) Versions
  * JNA: [oshi-core-7.2.1-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core/7.2.1-SNAPSHOT)
  * FFM: [oshi-core-ffm-7.2.1-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core-ffm/7.2.1-SNAPSHOT/)

Legacy Versions
  * JDK7: [oshi-core-3.13.6](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/3.13.6)
  * JDK6: [oshi-core-3.14.0](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/3.14.0)

Usage
-----
1. Include OSHI and its dependencies on your classpath.
   - We strongly recommend you add `oshi-core` (and/or `oshi-core-ffm`) as a dependency to your project dependency manager such as Maven or Gradle. Transitive dependencies (including `oshi-common` and JNA) are resolved automatically.
   - If you manage JAR files manually, download all needed JARs from the [oshi-dist](https://repo1.maven.org/maven2/com/github/oshi/oshi-dist/) zip files. See [UPGRADING.md](UPGRADING.md#project-dependencies) for details.
   - For Windows, consider the optional `jLibreHardwareMonitor` dependency if you need sensor information. Note the binary DLLs in this dependency are licensed under MPL 2.0.
   - For Android, you'll need to add the [AAR artifact for JNA](https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#jna-on-android) and exclude OSHI's transitive (JAR) dependency.
   - See the [FAQ](FAQ.md#how-do-i-resolve-jna-noclassdeffounderror-or-nosuchmethoderror-issues) if you encounter `NoClassDefFoundError` or `NoSuchMethodError` problems.
2. Create a new instance of `SystemInfo` (implementing `SystemInfoProvider`):

```java
// Automatically selects the best available implementation based on your classpath and runtime.
SystemInfoProvider si = SystemInfoFactory.create();
```

| Classpath | JDK | Platform | Selected implementation |
|-----------|-----|----------|------------------------|
| `oshi-core` only | 8+ | Any | JNA (`oshi.SystemInfo`) |
| `oshi-core-ffm` only | 25+ | Linux, macOS, Windows | FFM (`oshi.ffm.SystemInfo`) |
| Both `oshi-core` and `oshi-core-ffm` | 25+ | Linux, macOS, Windows | FFM (higher priority) |
| Both `oshi-core` and `oshi-core-ffm` | &lt;25 or unsupported platform | Any | JNA (FFM unavailable) |
| `oshi-common` only | 8+ | Linux | No `--enable-native-access` required (`oshi.nativefree.SystemInfo`) |

You can also instantiate directly:

```java
SystemInfoProvider si = new oshi.SystemInfo();            // JNA (oshi-core)
SystemInfoProvider si = new oshi.ffm.SystemInfo();        // FFM (oshi-core-ffm, JDK 25+)
SystemInfoProvider si = new oshi.nativefree.SystemInfo(); // no native access (oshi-common)
```

3. Use the getters from `SystemInfo` to access hardware or operating system components, such as:

```java
HardwareAbstractionLayer hal = si.getHardware();
CentralProcessor cpu = hal.getProcessor();
OperatingSystem os = si.getOperatingSystem();
```

Some settings are configurable in the [`oshi.properties`](https://github.com/oshi/oshi/blob/master/oshi-common/src/main/resources/oshi.properties) file, which may also be manipulated using the [`GlobalConfig`](https://www.oshi.ooo/oshi-core/apidocs/com.github.oshi.common/oshi/util/GlobalConfig.html) class or using Java System Properties. This should be done at startup, as configuration is not thread-safe and OSHI does not guarantee re-reading the configuration during operation.

Documentation
-------------
* Javadocs — [JNA](https://oshi.github.io/oshi/oshi-core/apidocs/) \| [FFM](https://oshi.github.io/oshi/oshi-core-ffm/apidocs/)
* [FAQ](FAQ.md)
* [Change Log](https://github.com/oshi/oshi/blob/master/CHANGELOG.md)
* [Performance Considerations](PERFORMANCE.md)
* [Major Version Breaking Changes](UPGRADING.md)
* [Sample Output](src/site/markdown/SampleOutput.md)
* [Applications and Projects using OSHI](src/site/resources/Projects.md)

Additional Modules
------------------

### [`oshi-demo`](oshi-demo/) — Examples and Demos

Proof-of-concept examples including a Swing GUI, JSON output, JMX integration, and more. Try instantly with [jbang](https://www.jbang.dev/):

```sh
jbang json@oshi/oshi    # JSON dump of system info
jbang gui@oshi/oshi     # Swing GUI dashboard
```

See the [oshi-demo README](oshi-demo/) for all available demos and usage instructions.

### [`oshi-metrics`](oshi-metrics/) — Micrometer Metrics

First-party [Micrometer](https://micrometer.io/) integration providing system, process, and container metrics following [OpenTelemetry semantic conventions](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/). Works with Prometheus, Grafana, Datadog, and any Micrometer-compatible backend.

```java
OshiMetrics.bindTo(registry, SystemInfoFactory.create());
```

See the [oshi-metrics README](oshi-metrics/) for full setup, selective registration, and the complete list of metrics.

### [`oshi-benchmark`](oshi-benchmark/) — JMH Benchmarks

JMH benchmarks comparing JNA and FFM implementations side by side. Requires JDK 25+.

```sh
./oshi-benchmark/scripts/run-benchmarks.sh
```

See the [oshi-benchmark README](oshi-benchmark/) for running specific benchmarks and custom JMH options.

Support
-------
* For bug reports, feature requests, or general questions about OSHI's longer term plans, please [create an issue](https://github.com/oshi/oshi/issues).
* For help integrating OSHI into your own project or maintainer code review of your PRs, tag `@dbwiddis` in issues or pull requests on your project site.
* For "how to" questions regarding the use of the API, consult examples in the `oshi-demo` project, create an issue, or [search on Stack Overflow](https://stackoverflow.com/search?q=%5Boshi%5D+is%3Aquestion) using the `oshi` tag, asking a new question if it hasn't been answered before.
* To say thanks to OSHI's primary maintainer, you can [sponsor him](https://github.com/sponsors/dbwiddis) or [buy him a coffee](https://www.buymeacoffee.com/dbwiddis).

OSHI for Enterprise
-------------------
Available as part of the Tidelift Subscription [![Tidelift](https://tidelift.com/badges/package/maven/com.github.oshi:oshi-core)](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&utm_medium=referral&utm_campaign=readme)

The maintainers of OSHI and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&utm_medium=referral&utm_campaign=readme)

Security Contact Information
----------------------------
Please see [SECURITY.md](SECURITY.md).

Continuous Integration Test Status
----------------------------------
[![AppVeyor Build Status](https://img.shields.io/appveyor/ci/dbwiddis/oshi/master.svg?logo=appveyor&logoColor=white)](https://ci.appveyor.com/project/dbwiddis/oshi)
[![Cirrus CI Build Status](https://img.shields.io/cirrus/github/oshi/oshi/master.svg?logo=cirrusci&logoColor=white)](https://cirrus-ci.com/github/oshi/oshi)
[![Windows CI](https://github.com/oshi/oshi/workflows/Windows%20CI/badge.svg)](https://github.com/oshi/oshi/actions?query=workflow%3A%22Windows+CI%22)
[![macOS CI](https://github.com/oshi/oshi/workflows/macOS%20CI/badge.svg)](https://github.com/oshi/oshi/actions?query=workflow%3A%22macOS+CI%22)
[![Linux CI](https://github.com/oshi/oshi/workflows/Linux%20CI/badge.svg)](https://github.com/oshi/oshi/actions?query=workflow%3A%22Linux+CI%22)
[![Unix CI](https://github.com/oshi/oshi/workflows/Unix%20CI/badge.svg)](https://github.com/oshi/oshi/actions?query=workflow%3A%22Unix+CI%22)
[![SonarQube Bugs](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=bugs)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Maintainability](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Reliability](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Security](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=security_rating)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![Coverity Scan Build Status](https://img.shields.io/coverity/scan/28367.svg)](https://scan.coverity.com/projects/oshi-oshi)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/4002c92342814fe1989a7841d9f427f1)](https://app.codacy.com/gh/oshi/oshi/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CodeQL](https://github.com/oshi/oshi/workflows/CodeQL/badge.svg)](https://github.com/oshi/oshi/security/code-scanning)
[![Coverage Status](https://codecov.io/github/oshi/oshi/graph/badge.svg?token=XpNPRyv8TJ)](https://codecov.io/github/oshi/oshi)

How Can I Help?
---------------
[OSHI originated](https://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html)
as a platform-independent library that did not require additional software and had a license compatible with
both open source and commercial products. We have developed a strong core of features on major Operating Systems,
but we would love for *you* to help by:
* Testing!  Our CI testing is limited to a few platforms.  Download and test the program on various operating systems/versions and hardware and help identify gaps that our limited development and testing may have missed. Specific high priority testing needs include:
  * Windows systems with over 64 logical processors
  * Raspberry Pi
  * Less common Linux distributions
* Contributing code.  See something that's not working right or could work better?  Help us fix it!  New contributors are welcome.
* Documenting implementation.  Our Wiki is sparse and the `oshi-demo` artifact is a place to host proof-of-concept ideas.  Want to help new users follow in your footsteps?
* Suggesting new features.  Do you need OSHI to do something it doesn't currently do?  Let us know.

Contributing to OSHI
--------------------
* [How to Contribute](CONTRIBUTING.md)
* [How to Release](RELEASING.md)

Acknowledgments
---------------
Many thanks to the following companies for providing free support of Open Source projects including OSHI:
* [SonarCloud](https://sonarcloud.io/about) for a range of code quality tools
* [GitHub Actions](https://github.com/features/actions), [AppVeyor](https://www.appveyor.com/), and [Cirrus CI](https://cirrus-ci.org/) for continuous integration testing
* [CodeRabbit](https://www.coderabbit.ai/) for automated AI code review
* The [jProfile Java Profiler](https://www.ej-technologies.com/products/jprofiler/overview.html) used to eliminate CPU bottlenecks

License
-------
This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
