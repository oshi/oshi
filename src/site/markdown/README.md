![OSHI](https://dl.dropboxusercontent.com/s/c82qboyvvudpvdp/oshilogo.png)

[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.oshi/oshi-core/badge.svg?)](https://search.maven.org/search?q=com.github.oshi)
[![Tidelift](https://tidelift.com/badges/package/maven/com.github.oshi:oshi-core)](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&amp;utm_medium=referral&amp;utm_campaign=readme)
[![Travis Build Status](https://travis-ci.org/oshi/oshi.svg)](https://travis-ci.org/oshi/oshi)
[![Appveyor Build status](https://ci.appveyor.com/api/projects/status/v489i8xoyfspxx7s?svg=true)](https://ci.appveyor.com/project/dbwiddis/oshi)
[![Coverage Status](https://coveralls.io/repos/github/oshi/oshi/badge.svg?branch=master)](https://coveralls.io/github/oshi/oshi?branch=master)
[![codecov.io](https://codecov.io/github/oshi/oshi/coverage.svg?branch=master)](https://codecov.io/github/oshi/oshi?branch=master)
[![Coverity Scan Build Status](https://img.shields.io/coverity/scan/9332.svg)](https://scan.coverity.com/projects/dblock-oshi)
[![Codacy Grade](https://api.codacy.com/project/badge/Grade/5370178ae91d4f56b43de2f26f7c5e7a)](https://www.codacy.com/app/widdis/oshi?utm_source=github.com&amp;amp;utm_medium=referral&amp;amp;utm_content=oshi/oshi&amp;amp;utm_campaign=Badge_Grade)
[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![SonarQube Bugs](https://sonarcloud.io/api/project_badges/measure?project=com.github.oshi%3Aoshi-parent&amp;metric=bugs)](https://sonarcloud.io/dashboard?id=com.github.oshi%3Aoshi-parent)
[![SonarQube Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=com.github.oshi%3Aoshi-parent&amp;metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=com.github.oshi%3Aoshi-parent)
[![SonarQube Maintainability](https://sonarcloud.io/api/project_badges/measure?project=com.github.oshi%3Aoshi-parent&amp;metric=sqale_rating)](https://sonarcloud.io/dashboard?id=com.github.oshi%3Aoshi-parent)
[![SonarQube Reliability](https://sonarcloud.io/api/project_badges/measure?project=com.github.oshi%3Aoshi-parent&amp;metric=reliability_rating)](https://sonarcloud.io/dashboard?id=com.github.oshi%3Aoshi-parent)
[![SonarQube Security](https://sonarcloud.io/api/project_badges/measure?project=com.github.oshi%3Aoshi-parent&amp;metric=security_rating)](https://sonarcloud.io/dashboard?id=com.github.oshi%3Aoshi-parent)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/oshi/oshi.svg?logo=lgtm&amp;logoWidth=18)](https://lgtm.com/projects/g/oshi/oshi/context:java)
[![Scrutinizer Code Quality](https://scrutinizer-ci.com/g/oshi/oshi/badges/quality-score.png?b=master)](https://scrutinizer-ci.com/g/oshi/oshi/?branch=master)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&amp;repo=oshi/oshi)](https://dependabot.com)
[![Openhub Stats](https://www.openhub.net/p/oshi/widgets/project_thin_badge.gif)](https://www.openhub.net/p/oshi?ref=github)
[![first-timers-only](https://img.shields.io/badge/first--timers--only-friendly-blue.svg?style=flat-square)](https://www.firsttimersonly.com/)
[![Join the chat at https://gitter.im/oshi/oshi](https://badges.gitter.im/oshi/oshi.svg)](https://gitter.im/oshi/oshi?utm_source=badge&amp;utm_medium=badge&amp;utm_campaign=pr-badge&amp;utm_content=badge)

OSHI is a free [JNA](https://github.com/java-native-access/jna)-based (native) 
Operating System and Hardware Information library for Java.
It does not require the installation of any additional native libraries and aims to provide a 
cross-platform implementation to retrieve system information, such as OS version, processes, 
memory &amp; CPU usage, disks &amp; partitions, devices, sensors, etc.

Supported platforms 
--------------------------- 
Windows • Linux • Mac OS X • Unix (Solaris, FreeBSD) 

Essentials
----------
* [API](http://oshi.github.io/oshi/apidocs/) (javadocs) - [Operating System](http://oshi.github.io/oshi/apidocs/oshi/software/os/package-summary.html) / [Hardware](http://oshi.github.io/oshi/apidocs/oshi/hardware/package-summary.html)
* [FAQ](https://github.com/oshi/oshi/blob/master/FAQ.md)
* [Find OSHI on Maven Central](https://search.maven.org/search?q=com.github.oshi)
* [Upgrading from an earlier version?](https://github.com/oshi/oshi/blob/master/UPGRADING.md) 

Supported features 
--------------------------
* Computer System and firmware, baseboard 
* Operating System and Version/Build
* Physical (core) and Logical (hyperthreaded) CPUs, processor groups, NUMA nodes
* System and per-processor load % and tick counters
* CPU uptime, processes, and threads
* Process uptime, CPU, memory usage, user/group, command line
* Physical and virtual memory used/available
* Mounted filesystems (type, usable and total space)
* Disk drives (model, serial, size) and partitions
* Network interfaces (IPs, bandwidth in/out)
* Battery state (% capacity, time remaining, power usage stats)
* Connected displays (with EDID info)
* USB Devices
* Sensors (temperature, fan speeds, voltage)

Downloads
---------
| Stable Release Version | Current Development Version | Dependencies |
| ------------- | ------------- | ------------- |
| [oshi-core-5.1.2](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&amp;g=com.github.oshi&amp;a=oshi-core&amp;v=5.1.2&amp;e=jar)  | [oshi-core-5.2.0-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&amp;g=com.github.oshi&amp;a=oshi-core&amp;v=5.2.0-SNAPSHOT&amp;e=jar) | [JNA](https://github.com/java-native-access/jna) • [SLF4J](http://www.slf4j.org/) |

Usage
-----
Include OSHI and its dependencies on your classpath.  We strongly recommend you add OSHI as a dependency to your project dependency manager such as Maven or Gradle. You can [find the appropriate syntax to include OSHI here](https://search.maven.org/artifact/com.github.oshi/oshi-core/5.1.2/jar). 

Create a new instance of `SystemInfo` and use the getters to access additional information, such as:
```
SystemInfo si = new SystemInfo();
HardwareAbstractionLayer hal = si.getHardware();
CentralProcessor cpu = hal.getProcessor();
```

You can see more examples and run the [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java)
and see the full output for your system by cloning the project and building it with [Maven](http://maven.apache.org/index.html):

```
git clone https://github.com/oshi/oshi.git && cd oshi

./mvnw test-compile -pl oshi-core exec:java \
  -Dexec.mainClass="oshi.SystemInfoTest" \
  -Dexec.classpathScope="test"
```

Note: OSHI uses the latest version of JNA, which may conflict with other dependencies your project (or its parent) includes. If you experience issues with `NoClassDefFound` errors for JNA artifacts, consider one or more of the following steps to resolve the conflict:
 - Listing OSHI earlier (or first) in your dependency list 
 - Specifying the most recent version of JNA (both `jna` and `jna-platform` artifacts) as a dependency
 - If you are using a parent (e.g., Spring Boot) that includes JNA as a dependency, override the `jna.version` property or equivalent

OSHI for enterprise
-------------------
Available as part of the Tidelift Subscription

The maintainers of OSHI and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-github-oshi-oshi-core?utm_source=maven-com-github-oshi-oshi-core&amp;utm_medium=referral&amp;utm_campaign=readme)

Security contact information
----------------------------
To report a security vulnerability, please use the [Tidelift security contact](https://tidelift.com/security).
Tidelift will coordinate the fix and disclosure.

Output
-------------
OSHI provides output directly via Java methods for each of its interfaces.  
By periodically polling dynamic information (e.g., every second), users can calculate and track changes.

The `main()` method of [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) 
provides sample code demonstrating the use of `oshi-core` interfaces to retrieve information and calculate additional metrics shown in the examples below.

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

Sensor readings are available for some hardware (see notes in the [API](http://oshi.github.io/oshi/apidocs/oshi/hardware/Sensors.html)).
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
[OSHI originated](http://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html) 
as a platform-independent library that did not require additional software and had a license compatible with 
both open source and commercial products. We have developed a strong core of features on major Operating Systems, 
but we would love for *you* to help by:
* Testing!  Our CI testing is limited.  Download and test the program on various operating systems/versions and hardware and help identify gaps that our limited development and testing may have missed.
* Offering access to an unsupported OS.  An AIX port will be written if access for development/testing can be provided!
* Contributing code.  See something that's not working right or could work better?  Help us fix it!  New contributors welcome.
* Contributing ports.  Have an OS that's not covered? It's likely one of the existing ports can be slightly modified. 
* Documenting implementation.  Our Wiki is sparse.  Want to help new users follow in your footsteps?
* Suggesting new features.  Do you need OSHI to do something it doesn't currently do?  Let us know.

Acknowledgements
-------------------
Many thanks to the following companies for providing free support of Open Source projects including OSHI:
* [SonarCloud](https://sonarcloud.io/about) for a range of code quality tools
* [Travis CI](https://travis-ci.org/) for continuous integration testing
* The [jProfile Java Profiler](https://www.ej-technologies.com/products/jprofiler/overview.html) used to eliminate cpu bottlenecks

Projects using OSHI
-------------------
* [Apache Flink](https://flink.apache.org/)
* [Atlassian Confluence](https://www.atlassian.com/software/confluence)
* [CAS Server](https://apereo.github.io/cas)
* [Kamon System Metrics](https://kamon.io/)
* [DeepLearning4J](https://deeplearning4j.org/)
* [Hutool](https://www.hutool.cn/)
* [Dolphin Scheduler](https://dolphinscheduler.apache.org/)
* [Guns](https://github.com/stylefeng/Guns)
* [GeoServer](https://docs.geoserver.org/stable/en/user/community/status-monitoring/index.html)
* [ND4J](http://nd4j.org/)
* [Apache Doris](http://doris.incubator.apache.org/)
* [UniversalMediaServer](https://github.com/UniversalMediaServer/UniversalMediaServer)
* [PSI Probe](https://github.com/psi-probe/psi-probe)
* [JPPF](https://jppf.org/)
* [Octopus Deploy](https://octopus.com/)
* [GigaSpaces XAP](https://xap.github.io/)
* [openHAB Systeminfo Binding](https://github.com/openhab/openhab2-addons/tree/master/bundles/org.openhab.binding.systeminfo)
* [Jenkins swarm plugin](https://wiki.jenkins.io/display/JENKINS/Swarm+Plugin)
* [Java for IBM Watson IoT Platform](https://ibm-watson-iot.github.io/iot-java/)
* [Semux](https://www.semux.org/)
* [Hawkular Agent](https://github.com/hawkular/hawkular-agent)
* [Dagr](https://github.com/fulcrumgenomics/dagr)
* [sys-API](https://github.com/Krillsson/sys-API)
* [NexCapMAT](http://www.nexess-solutions.com/fr/produits/application-nexcap-mat/)
* [360Suite](https://360suite.io/)
* [GoMint](https://gomint.io/)
* [Stefan's OS](https://BotCompany.de/)
* [Eclipse Passage](https://projects.eclipse.org/projects/technology.passage)
* [Eclipse Orbit](https://projects.eclipse.org/projects/tools.orbit)
* [Ruoyi](http://www.ruoyi.vip/)
* [DataX](https://github.com/WeiYe-Jing/datax-web)

License
-------
This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
