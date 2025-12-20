![OSHI](https://dl.dropboxusercontent.com/s/c82qboyvvudpvdp/oshilogo.png)

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.oshi/oshi-core.svg?label=Maven%20Central)](https://central.sonatype.com/search?namespace=com.github.oshi&sort=name)
[![first-timers-only](https://img.shields.io/badge/first--timers--only-friendly-blue.svg?style=flat-square)](https://www.firsttimersonly.com/)
[![Openhub Stats](https://www.openhub.net/p/oshi/widgets/project_thin_badge.gif)](https://www.openhub.net/p/oshi?ref=github)

OSHI is a free JNA-based (native) Operating System and Hardware Information library for Java.
It does not require the installation of any additional native libraries and aims to provide a
cross-platform implementation to retrieve system information, such as OS version, processes,
memory and CPU usage, disks and partitions, devices, sensors, etc.

- [Supported Platforms](#supported-platforms)
- [Downloads and Dependency Management](#downloads-and-dependency-management)
- [Documentation](#documentation)
- [Usage](#usage)
- [Supported Features](#supported-features)
- [Support](#support)
- [OSHI for Enterprise](#oshi-for-enterprise)
- [Security Contact Information](#security-contact-information)
- [Continuous Integration Test Status](#continuous-integration-test-status)
- [How Can I Help?](#how-can-i-help)
- [Contributing to OSHI](#contributing-to-oshi)
- [Acknowledgments](#acknowledgments)
- [License](#license)

Supported Platforms
---------------------------
- Windows
- macOS
- Linux (Android)
- UNIX (AIX, FreeBSD, OpenBSD, Solaris)

Documentation
-------------
* [API](https://oshi.github.io/oshi/oshi-core-java11/apidocs/) (javadocs)
* [FAQ](src/site/markdown/FAQ.md)
* [Change Log](CHANGELOG.md)
* [Performance Considerations](src/site/markdown/Performance.md)
* [Major Version Breaking Changes](src/site/markdown/Upgrading.md)
* [Sample Output](src/site/markdown/SampleOutput.md)
* [Applications and Projects using OSHI](src/site/resources/Projects.md)

Downloads and Dependency Management
-----------------------------------
Stable Release Version
  * JDK8: [oshi-core-6.9.2](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/6.9.2)
  * JPMS: [oshi-core-java11-6.9.2](https://central.sonatype.com/artifact/com.github.oshi/oshi-core-java11/6.9.2)
  * FFM: [oshi-core-java25-6.9.2](https://central.sonatype.com/artifact/com.github.oshi/oshi-core-java25/6.9.2)
  * JDK6: [oshi-core-3.14.0](https://central.sonatype.com/artifact/com.github.oshi/oshi-core/3.14.0)

Current Development (SNAPSHOT) downloads
  * JDK8: [oshi-core-6.9.3-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core/6.9.3-SNAPSHOT)
  * JPMS: [oshi-core-java11-6.9.3-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core-java11/6.9.3-SNAPSHOT/)
  * FFM: [oshi-core-java25-6.9.3-SNAPSHOT](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/github/oshi/oshi-core-java25/6.9.3-SNAPSHOT/)

OSHI Java 25+ Module
----------------------------
A new module, **`oshi-core-java25`**, is now available.

- **Purpose:** This module intends to provide API-compatible implementations using the JDK Foreign Function & Memory (FFM) API, replacing JNA for native access over time with community contributions.
- **Compatibility:**
  - Compiles on **JDK 25+**.
  - Initial support is limited to operating systems with JDK 25 builds; broader OS support and migration of more native implementations will follow.
  - Contributions are welcome and encouraged!
- **Usage:**
  - Use this dependency **in place of** `oshi-core`.
  - Import oshi.SystemInfoFFM instead of oshi.SystemInfo as the entry-point.
  - All other imports (oshi.hardware.*, oshi.software.os.*) remain unchanged.
- **Status:**
  - Some methods still delegate to legacy JNA-based internals until their FFM equivalents are implemented.

Usage
-----
1. Include OSHI and its dependencies on your classpath.
   - We strongly recommend you add `oshi-core` as a dependency to your project dependency manager such as Maven or Gradle.
   - For Windows, consider the optional `jLibreHardwareMonitor` dependency if you need sensor information. Note the binary DLLs in this dependency are licensed under MPL 2.0.
   - For Android, you'll need to add the [AAR artifact for JNA](https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#jna-on-android) and exclude OSHI's transitive (JAR) dependency.
   - See the [FAQ](https://github.com/oshi/oshi/blob/master/src/site/markdown/FAQ.md#how-do-i-resolve-jna-noclassdeffounderror-or-nosuchmethoderror-issues) if you encounter `NoClassDefFoundError` or `NoSuchMethodError` problems.
2. Create a new instance of `SystemInfo`
3. Use the getters from `SystemInfo` to access hardware or operating system components, such as:

```java
SystemInfo si = new SystemInfo(); // or new SystemInfoFFM() on java25 version
HardwareAbstractionLayer hal = si.getHardware();
CentralProcessor cpu = hal.getProcessor();
```

Sample Output
-------------

See [SystemInfoTest.java](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) for examples. To see sample output for your machine:
```sh
git clone https://github.com/oshi/oshi.git && cd oshi

./mvnw test-compile -pl oshi-core exec:java \
  -Dexec.mainClass="oshi.SystemInfoTest" \
  -Dexec.classpathScope="test"
```

Some settings are configurable in the [`oshi.properties`](https://github.com/oshi/oshi/blob/master/oshi-core/src/main/resources/oshi.properties) file, which may also be manipulated using the [`GlobalConfig`](https://oshi.github.io/oshi/oshi-core/apidocs/oshi/util/GlobalConfig.html) class or using Java System Properties. This should be done at startup, as configuration is not thread-safe and OSHI does not guarantee re-reading the configuration during operation.

The `oshi-demo` artifact includes [several proof-of-concept examples](https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/) of using OSHI to obtain information, including a basic Swing GUI.

You can run some of the demos using `jbang`:

```sh
# list all the aliases
jbang alias list oshi/oshi

# run the json demo
jbang json@oshi/oshi

#run the gui
jbang gui@oshi/oshi
```

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
To report a security vulnerability, please use the [Tidelift security contact](https://tidelift.com/security).
Tidelift will coordinate the fix and disclosure.

Continuous Integration Test Status
----------------------------------
[![Appveyor Build status](https://ci.appveyor.com/api/projects/status/v489i8xoyfspxx7s?svg=true)](https://ci.appveyor.com/project/dbwiddis/oshi)
[![Cirrus Build Status](https://api.cirrus-ci.com/github/oshi/oshi.svg)](https://cirrus-ci.com/github/oshi/oshi)
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
[![Codacy Grade](https://app.codacy.com/project/badge/Grade/4002c92342814fe1989a7841d9f427f1)](https://www.codacy.com/gh/oshi/oshi/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=oshi/oshi&amp;utm_campaign=Badge_Grade)
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
* [How to Contribute](src/site/markdown/Contributing.md)
* [How to Release](src/site/markdown/Releasing.md)

Acknowledgments
---------------
Many thanks to the following companies for providing free support of Open Source projects including OSHI:
* [SonarCloud](https://sonarcloud.io/about) for a range of code quality tools
* [GitHub Actions](https://github.com/features/actions), [AppVeyor](https://www.appveyor.com/), and [Cirrus CI](https://cirrus-ci.org/) for continuous integration testing
* The [jProfile Java Profiler](https://www.ej-technologies.com/products/jprofiler/overview.html) used to eliminate CPU bottlenecks

License
-------
This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
