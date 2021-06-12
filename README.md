![OSHI](https://dl.dropboxusercontent.com/s/c82qboyvvudpvdp/oshilogo.png)

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.oshi/oshi-core/badge.svg?)](https://search.maven.org/search?q=com.github.oshi)
[![Java CI](https://github.com/oshi/oshi/workflows/Java%20CI/badge.svg)](https://github.com/oshi/oshi/actions?query=workflow%3A%22Java+CI%22)
[![Unix CI](https://github.com/oshi/oshi/workflows/Unix%20CI/badge.svg)](https://github.com/oshi/oshi/actions?query=workflow%3A%22Unix+CI%22)
[![Travis Build Status](https://travis-ci.org/oshi/oshi.svg)](https://travis-ci.org/oshi/oshi)
[![Appveyor Build status](https://ci.appveyor.com/api/projects/status/v489i8xoyfspxx7s?svg=true)](https://ci.appveyor.com/project/dbwiddis/oshi)
[![Cirrus Build Status](https://api.cirrus-ci.com/github/oshi/oshi.svg)](https://cirrus-ci.com/github/oshi/oshi)
[![Tidelift](https://tidelift.com/badges/package/maven/com.github.oshi:oshi-core)](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&utm_medium=referral&utm_campaign=readme)
[![SonarQube Bugs](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=bugs)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Maintainability](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Reliability](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![SonarQube Security](https://sonarcloud.io/api/project_badges/measure?project=oshi_oshi&metric=security_rating)](https://sonarcloud.io/dashboard?id=oshi_oshi)
[![Coverity Scan Build Status](https://img.shields.io/coverity/scan/9332.svg)](https://scan.coverity.com/projects/dblock-oshi)
[![Codacy Grade](https://app.codacy.com/project/badge/Grade/4002c92342814fe1989a7841d9f427f1)](https://www.codacy.com/gh/oshi/oshi/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=oshi/oshi&amp;utm_campaign=Badge_Grade)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/oshi/oshi.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/oshi/oshi/context:java)
[![Coverage Status](https://coveralls.io/repos/github/oshi/oshi/badge.svg?branch=master)](https://coveralls.io/github/oshi/oshi?branch=master)
[![Scrutinizer Code Quality](https://scrutinizer-ci.com/g/oshi/oshi/badges/quality-score.png?b=master)](https://scrutinizer-ci.com/g/oshi/oshi/?branch=master)
[![Openhub Stats](https://www.openhub.net/p/oshi/widgets/project_thin_badge.gif)](https://www.openhub.net/p/oshi?ref=github)
[![first-timers-only](https://img.shields.io/badge/first--timers--only-friendly-blue.svg?style=flat-square)](https://www.firsttimersonly.com/)
[![github-sponsor](https://img.shields.io/badge/github-donate-yellow.svg)](https://github.com/sponsors/dbwiddis)
[![buymeacoffee](https://img.shields.io/badge/buy%20me%20a%20coffee-donate-yellow.svg)](https://buymeacoffee.com/dbwiddis)

OSHI is a free JNA-based (native) Operating System and Hardware Information library for Java.
It does not require the installation of any additional native libraries and aims to provide a 
cross-platform implementation to retrieve system information, such as OS version, processes, 
memory and CPU usage, disks and partitions, devices, sensors, etc.

Supported platforms 
--------------------------- 
Windows • Linux • macOS • Unix (AIX, FreeBSD, OpenBSD, Solaris) 

Essentials
----------
* [API](https://oshi.github.io/oshi/oshi-core/apidocs/) (javadocs) - [Operating System](https://oshi.github.io/oshi/oshi-core/apidocs/oshi/software/os/package-summary.html) / [Hardware](https://oshi.github.io/oshi/oshi-core/apidocs/oshi/hardware/package-summary.html)
* [FAQ](https://github.com/oshi/oshi/blob/master/FAQ.md)
* [Find OSHI on Maven Central](https://search.maven.org/search?q=com.github.oshi)
* [Upgrading from an earlier version?](https://github.com/oshi/oshi/blob/master/UPGRADING.md) 

Supported features 
--------------------------
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

Downloads
---------
| Stable Release Version | Current Development Version | Dependencies |
| ------------- | ------------- | ------------- |
| [oshi-core-5.7.5](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.github.oshi&a=oshi-core&v=5.7.5&e=jar)  | [oshi-core-5.7.6-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.oshi&a=oshi-core&v=5.7.6-SNAPSHOT&e=jar) | [JNA](https://github.com/java-native-access/jna) • [SLF4J](http://www.slf4j.org/) |

| Java 11 (JPMS) Version | Current Development Version | 
| ------------- | ------------- | 
| [oshi-core-java11-5.7.5](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.github.oshi&a=oshi-core-java11&v=5.7.5&e=jar)  | [oshi-core-java11-5.7.6-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.oshi&a=oshi-core-java11&v=5.7.6-SNAPSHOT&e=jar) |

Usage
-----
1. Include OSHI and its dependencies on your classpath.  We strongly recommend you add OSHI as a dependency to your project dependency manager such as Maven or Gradle. You can [find the appropriate syntax to include OSHI here](https://search.maven.org/artifact/com.github.oshi/oshi-core/5.7.5/jar). 

2. Create a new instance of `SystemInfo` 

3. Use the getters from `SystemInfo` to access hardware or operating system components, such as:

```
SystemInfo si = new SystemInfo();
HardwareAbstractionLayer hal = si.getHardware();
CentralProcessor cpu = hal.getProcessor();
```

See the [PERFORMANCE](PERFORMANCE.md) document for general CPU/Memory tradeoffs and specific Windows (WMI) recommendations depending upon your application.

See the [FAQ](FAQ.md) document for common implementation and calculation questions.

Some settings are configurable in the [`oshi.properties`](https://github.com/oshi/oshi/blob/master/oshi-core/src/main/resources/oshi.properties) file, which may also be manipulated using the [`GlobalConfig`](https://oshi.github.io/oshi/apidocs/oshi/util/GlobalConfig.html) class. This should be done at startup, as configuration is not thread-safe and OSHI does not guarantee re-reading the configuration during operation.

The `oshi-demo` artifact includes [several proof-of-concept examples](https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/) of using OSHI to obtain information, including a basic Swing GUI.

Note: OSHI uses the latest version of [JNA](https://github.com/java-native-access/jna).
If you experience a `NoClassDefFoundError` or `NoSuchMethodError` issues with JNA artifacts, you likely have
an older version of either `jna` or `jna-platform` in your classpath from a transitive dependency on another project.
Consider one or more of the following steps to resolve the conflict:
 - Listing OSHI earlier (or first) in your dependency list 
 - Specifying the most recent version of JNA (both `jna` and `jna-platform` artifacts) in your `pom.xml` as dependencies.
 - If you are using the Spring Boot Starter Parent version 2.2 and earlier that includes JNA as a dependency:
   - Upgrade to version 2.3 which does not have a JNA dependency (preferred)
   - If you must use version 2.2 or earlier, override the `jna.version` property to the latest JNA version.

Support
-------------------
* For bug reports, feature requests, or general questions about OSHI's longer term plans, please [create an issue](https://github.com/oshi/oshi/issues).  
* For help integrating OSHI into your own project or maintainer code review of your PRs, tag `@dbwiddis` in issues or pull requests on your project site.
* For "how to" questions regarding use of the API, consult examples in the `oshi-demo` project, create an issue, or [search on Stack Overflow](https://stackoverflow.com/search?q=%5Boshi%5D+is%3Aquestion) using the `oshi` tag, asking a new question if it hasn't been answered before.
* To say thanks to OSHI's primary maintainer, you can [sponsor him](https://github.com/sponsors/dbwiddis) or [buy him a coffee](https://www.buymeacoffee.com/dbwiddis).

OSHI for enterprise
-------------------
Available as part of the Tidelift Subscription

The maintainers of OSHI and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&utm_medium=referral&utm_campaign=readme)

Security contact information
----------------------------
To report a security vulnerability, please use the [Tidelift security contact](https://tidelift.com/security).
Tidelift will coordinate the fix and disclosure.

Output
-------------
OSHI provides output directly via Java methods for each of its interfaces.  
By periodically polling dynamic information (e.g., every second), users can calculate and track changes.

You can see more examples and run the [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java)
and see the full output for your system by cloning the project and building it with [Maven](https://maven.apache.org/index.html):

```
git clone https://github.com/oshi/oshi.git && cd oshi

./mvnw test-compile -pl oshi-core exec:java \
  -Dexec.mainClass="oshi.SystemInfoTest" \
  -Dexec.classpathScope="test"
```

In addition, the `oshi-demo` project includes an [OshiGui](https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/OshiGui.java) class implementing a basic Swing GUI offering suggestions for potential visualizations using OSHI in a UI, monitoring, or alerting application, as shown below:

General information about the operating system and computer system hardware:
![Operating System and Hardware](https://github.com/dbwiddis/oshi/blob/master/src/site/markdown/OSHW.PNG)

By measuring ticks (user, nice, system, idle, iowait, and irq) between time intervals, percent usage can be calculated.
Per-processor information is also provided.
![CPU Usage](https://github.com/dbwiddis/oshi/blob/master/src/site/markdown/CPU.PNG)

Process information including CPU and memory per process is available.
![Process Statistics](https://github.com/dbwiddis/oshi/blob/master/src/site/markdown/Procs.PNG)

Memory and swapfile information is available.
![Memory Statistics](https://github.com/dbwiddis/oshi/blob/master/src/site/markdown/Memory.PNG)

Statistics for the system battery are provided:

```
Power Sources: 
 Name: InternalBattery-0, Device Name: bq20z451,
 RemainingCapacityPercent: 100.0%, Time Remaining: 5:42, Time Remaining Instant: 5:42,
 Power Usage Rate: -16045.216mW, Voltage: 12.694V, Amperage: -1264.0mA,
 Power OnLine: false, Charging: false, Discharging: true,
 Capacity Units: MAH, Current Capacity: 7213, Max Capacity: 7315, Design Capacity: 7336,
 Cycle Count: 6, Chemistry: LIon, Manufacture Date: 2019-06-11, Manufacturer: SMP,
 SerialNumber: D869243A2U3J65JAB, Temperature: 30.46°C
```

The EDID for each Display is provided. This can be parsed with various utilities for detailed information. OSHI provides a summary of selected data.

```
Displays:
 Display 0:
  Manuf. ID=SAM, Product ID=2ad, Analog, Serial=HA19, ManufDate=3/2008, EDID v1.3
  41 x 27 cm (16.1 x 10.6 in)
  Preferred Timing: Clock 106MHz, Active Pixels 3840x2880 
  Range Limits: Field Rate 56-75 Hz vertical, 30-81 Hz horizontal, Max clock: 140 MHz
  Monitor Name: SyncMaster
  Serial Number: H9FQ345476
 Display 1:
  Manuf. ID=SAM, Product ID=226, Analog, Serial=HA19, ManufDate=4/2007, EDID v1.3
  41 x 26 cm (16.1 x 10.2 in)
  Preferred Timing: Clock 106MHz, Active Pixels 3840x2880 
  Range Limits: Field Rate 56-75 Hz vertical, 30-81 Hz horizontal, Max clock: 140 MHz
  Monitor Name: SyncMaster
  Serial Number: HMCP431880
```

Disks and usage (reads, writes, transfer times) are shown, and partitions can be mapped to filesystems.

```
Disks:
 disk0: (model: SanDisk Ultra II 960GB - S/N: 161008800550) size: 960.2 GB, reads: 1053132 (23.0 GiB), writes: 243792 (11.1 GiB), xfer: 73424854 ms
 |-- disk0s1: EFI (EFI System Partition) Maj:Min=1:1, size: 209.7 MB
 |-- disk0s2: Macintosh HD (Macintosh SSD) Maj:Min=1:2, size: 959.3 GB @ /
 disk1: (model: Disk Image - S/N: ) size: 960.0 GB, reads: 3678 (60.0 MiB), writes: 281 (8.6 MiB), xfer: 213627 ms
 |-- disk1s1: EFI (EFI System Partition) Maj:Min=1:4, size: 209.7 MB
 |-- disk1s2: Dropbox (disk image) Maj:Min=1:5, size: 959.7 GB @ /Volumes/Dropbox

```

Sensor readings are available for some hardware (see notes in the [API](https://oshi.github.io/oshi/apidocs/oshi/hardware/Sensors.html)).

```
Sensors:
 CPU Temperature: 69.8°C
 Fan Speeds:[4685, 4687]
 CPU Voltage: 3.9V
```

Attached USB devices can be listed:

```
USB Devices:
 AppleUSBEHCI
 |-- Root Hub Simulation Simulation (Apple Inc.)
     |-- IOUSBHostDevice
         |-- IR Receiver (Apple Computer, Inc.)
         |-- USB Receiver (Logitech)
 AppleUSBEHCI
 |-- Root Hub Simulation Simulation (Apple Inc.)
     |-- FaceTime HD Camera (Built-in) (Apple Inc.) [s/n: DJHB1V077FDH5HL0]
     |-- IOUSBHostDevice
         |-- Apple Internal Keyboard / Trackpad (Apple Inc.)
         |-- BRCM2070 Hub (Apple Inc.)
             |-- Bluetooth USB Host Controller (Apple Inc.)
 AppleUSBEHCI
 |-- Root Hub Simulation Simulation (Apple Inc.)
     |-- IOUSBHostDevice
         |-- Apple Thunderbolt Display (Apple Inc.) [s/n: 162C0C25]
         |-- Display Audio (Apple Inc.) [s/n: 162C0C25]
         |-- FaceTime HD Camera (Display) (Apple Inc.) [s/n: CCGCAN000TDJ7DFX]
         |-- USB2.0 Hub
             |-- ANT USBStick2 (Dynastream Innovations) [s/n: 051]
             |-- Fitbit Base Station (Fitbit Inc.)
```

Where are we? How can I help?
-----------------------------
[OSHI originated](https://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html) 
as a platform-independent library that did not require additional software and had a license compatible with 
both open source and commercial products. We have developed a strong core of features on major Operating Systems, 
but we would love for *you* to help by:
* Testing!  Our CI testing is limited to a few platforms.  Download and test the program on various operating systems/versions and hardware and help identify gaps that our limited development and testing may have missed. Particular testing needs include:
  * Windows systems with over 64 processors
  * Raspberry Pi
  * Less common Linux distributions
* Contributing code.  See something that's not working right or could work better?  Help us fix it!  New contributors are welcome.
* Documenting implementation.  Our Wiki is sparse and the `oshi-demo` artifact is a place to host proof-of-concept ideas.  Want to help new users follow in your footsteps?
* Suggesting new features.  Do you need OSHI to do something it doesn't currently do?  Let us know.

Acknowledgements
-------------------
Many thanks to the following companies for providing free support of Open Source projects including OSHI:
* [SonarCloud](https://sonarcloud.io/about) for a range of code quality tools
* [Travis CI](https://travis-ci.org/) for continuous integration testing
* The [jProfile Java Profiler](https://www.ej-technologies.com/products/jprofiler/overview.html) used to eliminate cpu bottlenecks

Applications and Projects using OSHI
-------------------
<table style="padding:6px"><tr>
    <td align=center width=134px><a href="https://www.atlassian.com/software/confluence"><img src="./src/site/markdown/confluence.png" alt="Atlassian Confluence" width=122px><sub><br/>Atlassian Confluence</sub></a></td>
    <td align=center width=134px><a href="https://apereo.github.io/cas"><img src="./src/site/markdown/cas.png" alt="CAS Server" width=122px><sub><br/><br/>CAS Server</sub></a></td>
    <td align=center width=134px><a href="https://docs.geoserver.org/stable/en/user/community/status-monitoring/index.html"><img src="./src/site/markdown/geoserver.png" alt="GeoServer" width=122px><sub><br/>GeoServer</sub></a></td>
    <td align=center width=134px><a href="https://flink.apache.org/"><img src="./src/site/markdown/flink.jpg" alt="Apache Flink" width=122px><sub><br/>Apache Flink</sub></a></td>
    <td align=center width=134px><a href="https://deeplearning4j.org/"><img src="./src/site/markdown/dl4j.png" alt="DeepLearning4J" width=122px><sub><br/>DeepLearning4J</sub></a></td>
    <td align=center width=134px><a href="https://kamon.io/"><img src="./src/site/markdown/kamon.png" alt="Kamon System Metrics" width=122px><sub><br/>Kamon System Metrics</sub></a></td>
  </tr><tr></tr><tr>
    <td align=center width=134px><a href="https://github.com/nosqlbench/nosqlbench"><img src="./src/site/markdown/nosqlbench.png" alt="NoSQLbench" width=122px><sub><br/>NoSQLbench</sub></a></td>
    <td align=center width=134px><a href="https://octopus.com/"><img src="./src/site/markdown/octopus.svg" alt="Octopus Deploy" width=122px><sub><br/>Octopus Deploy</sub></a></td>
    <td align=center width=134px><a href="https://karaf.apache.org/"><img src="./src/site/markdown/karaf.png" alt="Apache Karaf" width=122px><sub><br/>Apache Karaf</sub></a></td>
    <td align=center width=134px><a href="https://github.com/UniversalMediaServer/UniversalMediaServer"><img src="./src/site/markdown/ums.png" alt="Universal Media Server" width=122px><sub><br/>Universal Media Server</sub></a></td>
    <td align=center width=134px><a href="https://github.com/psi-probe/psi-probe"><img src="./src/site/markdown/psi.jpg" alt="PSI Probe" width=122px><sub><br/>PSI Probe</sub></a></td>
    <td align=center width=134px><a href="https://jppf.org/"><br/><img src="./src/site/markdown/jppf.gif" alt="JPPF" width=122px><sub><br/><br/><br/>JPPF</sub></a></td>
  </tr><tr></tr><tr>
    <td align=center width=134px><a href="https://www.appdynamics.com/"><img src="./src/site/markdown/appdynamics.png" alt="AppDynamics" width=122px><sub><br/>AppDynamics</sub></a></td>
    <td align=center width=134px><a href="https://www.alluxio.io/"><img src="./src/site/markdown/alluxio.png" alt="Alluxio" width=122px><sub><br/>Alluxio</sub></a></td>
    <td align=center width=134px><a href="https://lightstep.com/"><img src="./src/site/markdown/lightstep.png" alt="Lightstep" width=122px><sub><br/>Lightstep</sub></a></td>
    <td align=center width=134px><a href="https://www.hivemq.com/"><img src="./src/site/markdown/hivemq.png" alt="HiveMQ" width=122px><sub><br/>HiveMQ</sub></a></td>
    <td align=center width=134px><a href="https://mosip.io/"><img src="./src/site/markdown/mosip.png" alt="MOSIP" width=122px><sub><br/>MOSIP</sub></a></td>
    <td align=center width=134px><a href="https://www.handle.net/"><br/><img src="./src/site/markdown/hdl.gif" alt="Handle.net" width=122px><sub><br/><br/><br/>Handle.net</sub></a></td>
  </tr><tr></tr><tr>
    <td align=center width=134px><a href="https://xap.github.io/"><img src="./src/site/markdown/xap.png" alt="GigaSpaces XAP" width=122px><sub><br/><br/>GigaSpaces XAP</sub></a></td>
    <td align=center width=134px><a href="https://github.com/openhab/openhab-addons/tree/main/bundles/org.openhab.binding.systeminfo"><img src="./src/site/markdown/openhab.png" alt="OpenHAB Systeminfo Binding" width=122px><sub><br/>OpenHAB Systeminfo Binding</sub></a></td>
    <td align=center width=134px><a href="https://wiki.jenkins.io/display/JENKINS/Swarm+Plugin"><img src="./src/site/markdown/jenkins.png" alt="Jenkins Swarm Plugin" width=122px><sub><br/>Jenkins Swarm Plugin</sub></a></td>
    <td align=center width=134px><a href="https://ibm-watson-iot.github.io/iot-java/"><img src="./src/site/markdown/watson.png" alt="IBM Watson IoT Platform" width=122px><sub><br/>IBM Watson IoT Platform</sub></a></td>
    <td align=center width=134px><a href="https://github.com/fulcrumgenomics/dagr"><img src="./src/site/markdown/dagr.png" alt="DAGR" width=122px><sub><br/>DAGR</sub></a></td>
    <td align=center width=134px><a href="https://dolphinscheduler.apache.org/"><img src="./src/site/markdown/dolphinscheduler.png" alt="Apache Dolphin Scheduler" width=122px><sub><br/>Apache Dolphin Scheduler</sub></a></td> 
  </tr><tr></tr><tr>
    <td align=center width=134px><a href="https://github.com/hawkular/hawkular-agent"><img src="./src/site/markdown/hawkular.png" alt="Hawkular Agent" width=122px><sub><br/>Hawkular Agent</sub></a></td>
    <td align=center width=134px><a href="https://dynamiasoluciones.com/"><img src="./src/site/markdown/dynamia.png" alt="DynamiaModules SaaS Remote" width=122px><sub><br/>DynamiaModules SaaS Remote</sub></a></td>
    <td align=center width=134px><a href="https://doris.incubator.apache.org/"><img src="./src/site/markdown/doris.png" alt="Apache Doris" width=122px><sub><br/>Apache Doris</sub></a></td>
    <td align=center width=134px><a href="https://www.semux.org/"><img src="./src/site/markdown/semux.svg" alt="Semux" width=122px><sub><br/>Semux</sub></a></td>
    <td align=center width=134px><a href="https://github.com/stylefeng/Guns"><img src="./src/site/markdown/guns.png" alt="Guns" width=122px><sub><br/>Guns</sub></a></td>
    <td align=center width=134px><a href="https://github.com/Tencent/bk-ci"><img src="./src/site/markdown/bkci.png" alt="BlueKing CI" width=122px><sub><br/>BlueKing CI</sub></a></td>
  </tr><tr></tr><tr>
    <td align=center width=134px><a href="https://nsquared.co.nz/files/n2jslee/technical_guide/"><img src="./src/site/markdown/nsquared.png" alt="N-Squared Java SLEE" width=122px><sub><br/>N-Squared Java SLEE</sub></a></td>
    <td align=center width=134px><a href="https://developer.envisioniot.com/"><img src="./src/site/markdown/enosiot.png" alt="EnOS IoT Hub" width=122px><sub><br/>EnOS IoT Hub</sub></a></td>
    <td align=center width=134px><a href="https://www.fastchar.com/fc_lib/13"><img src="./src/site/markdown/fastchar.png" alt="FastChar-ExtJs" width=122px><sub><br/>FastChar-ExtJs</sub></a></td>
    <td align=center width=134px><a href="https://github.com/criteo/garmadon"><br/><img src="./src/site/markdown/garmadon.png" alt="Garmadon" width=122px><sub><br/><br/><br/>Garmadon</sub></a></td>
    <td align=center width=134px><a href="https://www.hutool.cn/"><br/><img src="./src/site/markdown/hutool.jpg" alt="Hutool" width=122px><sub><br/><br/><br/>Hutool</sub></a></td>  
    <td align=center width=134px><a href="https://github.com/fizzed/crux"><img src="./src/site/markdown/crux.png" alt="Crux Utilities" width=122px><sub><br/><br/>Crux Utilities</sub></a></td>
  </tr><tr></tr><tr>
    <td align=center width=134px><a href="https://github.com/Krillsson/sys-API"><img src="./src/site/markdown/sysapi.png" alt="sys-API" width=122px><sub><br/>sys-API</sub></a></td>
    <td align=center width=134px><a href="https://www.nexess-solutions.com/fr/produits/application-nexcap-mat/"><img src="./src/site/markdown/nexcap.png" alt="NexCap MAT" width=122px><sub><br/>NexCap MAT</sub></a></td>
    <td align=center width=134px><a href="https://360suite.io/"><br/><img src="./src/site/markdown/360suite.png" alt="SAP 360Suite" width=122px><sub><br/><br/>SAP 360Suite</sub></a></td>
    <td align=center width=134px><a href="https://github.com/gomint/gomint"><img src="./src/site/markdown/gomint.png" alt="GoMint" width=122px><sub><br/><br/>GoMint</sub></a></td>
    <td align=center width=134px><a href="https://www.ruoyi.vip/"><img src="./src/site/markdown/ruoyi.png" alt="RuoYi Management System" width=122px><sub><br/>RuoYi Management System</sub></a></td>
    <td align=center width=134px><a href="https://github.com/WeiYe-Jing/datax-web"><img src="./src/site/markdown/dataxweb.jpg" alt="DataX Web" width=122px><sub><br/>DataX Web</sub></a></td>
</tr></table>

License
-------
This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
