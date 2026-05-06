![OSHI](https://dl.dropboxusercontent.com/s/c82qboyvvudpvdp/oshilogo.png)

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.oshi/oshi-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?namespace=com.github.oshi&amp;sort=name)
[![first-timers-only](https://img.shields.io/badge/first--timers--only-friendly-blue.svg?style=flat-square)](https://www.firsttimersonly.com/)
[![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/oshi/oshi?utm_source=oss&amp;utm_medium=github&amp;utm_campaign=oshi%2Foshi&amp;labelColor=171717&amp;color=FF570A&amp;link=https%3A%2F%2Fcoderabbit.ai&amp;label=CodeRabbit+Reviews)](https://app.coderabbit.ai/dashboard/summary)
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
- UNIX (AIX, FreeBSD, OpenBSD, Solaris) — JNA only

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
* USB Devices
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
  * JNA: [oshi-core-7.0.1](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/7.0.1)
  * FFM: [oshi-core-ffm-7.0.1](https://central.sonatype.com/artifact/com.github.oshi/oshi-core-ffm/7.0.1)

Current Development (SNAPSHOT) Versions
  * JNA: [oshi-core-7.1.0-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core/7.1.0-SNAPSHOT)
  * FFM: [oshi-core-ffm-7.1.0-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core-ffm/7.1.0-SNAPSHOT/)

Legacy Versions
  * JDK7: [oshi-core-3.13.6](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/3.13.6)
  * JDK6: [oshi-core-3.14.0](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/3.14.0)

Usage
-----
1. Include OSHI and its dependencies on your classpath.
   - We strongly recommend you add `oshi-core` as a dependency to your project dependency manager such as Maven or Gradle.
   - For Windows, consider the optional `jLibreHardwareMonitor` dependency if you need sensor information. Note the binary DLLs in this dependency are licensed under MPL 2.0.
   - For Android, you'll need to add the [AAR artifact for JNA](https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#jna-on-android) and exclude OSHI's transitive (JAR) dependency.
   - See the [FAQ](https://github.com/oshi/oshi/blob/master/FAQ.md#how-do-i-resolve-jna-noclassdeffounderror-or-nosuchmethoderror-issues) if you encounter `NoClassDefFoundError` or `NoSuchMethodError` problems.
2. Create a new instance of `SystemInfo`
3. Use the getters from `SystemInfo` to access hardware or operating system components, such as:

```java
SystemInfo si = new SystemInfo(); // oshi.SystemInfo or oshi.ffm.SystemInfo
HardwareAbstractionLayer hal = si.getHardware();
CentralProcessor cpu = hal.getProcessor();
```

To include both implementations and select at runtime:

```java
// Runtime selection — both return the same HAL/OS interfaces
HardwareAbstractionLayer hal;
OperatingSystem os;
if (useFFM) { // Requires JDK 25+; supports Windows, macOS, Linux
    oshi.ffm.SystemInfo si = new oshi.ffm.SystemInfo();
    hal = si.getHardware();
    os = si.getOperatingSystem();
} else { // JDK 8+; supports all platforms including AIX, FreeBSD, OpenBSD, Solaris
    oshi.SystemInfo si = new oshi.SystemInfo();
    hal = si.getHardware();
    os = si.getOperatingSystem();
}
```

Some settings are configurable in the [`oshi.properties`](https://github.com/oshi/oshi/blob/master/oshi-common/src/main/resources/oshi.properties) file, which may also be manipulated using the [`GlobalConfig`](https://www.oshi.ooo/oshi-core/apidocs/com.github.oshi.common/oshi/util/GlobalConfig.html) class or using Java System Properties. This should be done at startup, as configuration is not thread-safe and OSHI does not guarantee re-reading the configuration during operation.

Documentation
-------------
* Javadocs — [JNA](https://oshi.github.io/oshi/oshi-core/apidocs/) \| [FFM](https://oshi.github.io/oshi/oshi-core-ffm/apidocs/)
* [FAQ](https://github.com/oshi/oshi/blob/master/FAQ.md)
* [Change Log](https://github.com/oshi/oshi/blob/master/CHANGELOG.md)
* [Performance Considerations](https://github.com/oshi/oshi/blob/master/PERFORMANCE.md)
* [Major Version Breaking Changes](https://github.com/oshi/oshi/blob/master/UPGRADING.md)
* [Sample Output](SampleOutput.html)
* [Applications and Projects using OSHI](Projects.html)

Additional Modules
------------------

### [`oshi-demo`](https://github.com/oshi/oshi/blob/master/oshi-demo/) — Examples and Demos

Proof-of-concept examples including a Swing GUI, JSON output, JMX integration, and more. Try instantly with [jbang](https://www.jbang.dev/):

```sh
jbang json@oshi/oshi    # JSON dump of system info
jbang gui@oshi/oshi     # Swing GUI dashboard
```

See the [oshi-demo README](https://github.com/oshi/oshi/blob/master/oshi-demo/) for all available demos and usage instructions.

### [`oshi-metrics`](https://github.com/oshi/oshi/blob/master/oshi-metrics/) — Micrometer Metrics

First-party [Micrometer](https://micrometer.io/) integration providing system, process, and container metrics following [OpenTelemetry semantic conventions](https://opentelemetry.io/docs/specs/semconv/system/system-metrics/). Works with Prometheus, Grafana, Datadog, and any Micrometer-compatible backend.

```java
OshiMetrics.bindTo(registry, si.getHardware(), si.getOperatingSystem());
```

See the [oshi-metrics README](https://github.com/oshi/oshi/blob/master/oshi-metrics/) for full setup, selective registration, and the complete list of metrics.

### [`oshi-benchmark`](https://github.com/oshi/oshi/blob/master/oshi-benchmark/) — JMH Benchmarks

JMH benchmarks comparing JNA and FFM implementations side by side. Requires JDK 25+.

```sh
./oshi-benchmark/scripts/run-benchmarks.sh
```

See the [oshi-benchmark README](https://github.com/oshi/oshi/blob/master/oshi-benchmark/) for running specific benchmarks and custom JMH options.

Support
-------
* For bug reports, feature requests, or general questions about OSHI's longer term plans, please [create an issue](https://github.com/oshi/oshi/issues).
* For help integrating OSHI into your own project or maintainer code review of your PRs, tag `@dbwiddis` in issues or pull requests on your project site.
* For "how to" questions regarding the use of the API, consult examples in the `oshi-demo` project, create an issue, or [search on Stack Overflow](https://stackoverflow.com/search?q=%5Boshi%5D+is%3Aquestion) using the `oshi` tag, asking a new question if it hasn't been answered before.
* To say thanks to OSHI's primary maintainer, you can [sponsor him](https://github.com/sponsors/dbwiddis) or [buy him a coffee](https://www.buymeacoffee.com/dbwiddis).

OSHI for Enterprise
-------------------
Available as part of the Tidelift Subscription [![Tidelift](https://tidelift.com/badges/package/maven/com.github.oshi:oshi-core)](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&amp;utm_medium=referral&amp;utm_campaign=readme)

The maintainers of OSHI and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&amp;utm_medium=referral&amp;utm_campaign=readme)

Security Contact Information
----------------------------
To report a security vulnerability, please use the [Tidelift security contact](https://tidelift.com/security).
Tidelift will coordinate the fix and disclosure.

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
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/4002c92342814fe1989a7841d9f427f1)](https://app.codacy.com/gh/oshi/oshi/dashboard?utm_source=gh&amp;utm_medium=referral&amp;utm_content=&amp;utm_campaign=Badge_grade)
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
* [How to Contribute](https://github.com/oshi/oshi/blob/master/CONTRIBUTING.md)
* [How to Release](https://github.com/oshi/oshi/blob/master/RELEASING.md)

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
