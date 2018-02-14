![OSHI](https://dl.dropboxusercontent.com/s/c82qboyvvudpvdp/oshilogo.png)

[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.oshi/oshi-core/badge.svg)](http://search.maven.org/#search|ga|1|com.github.oshi)
[![Build Status](https://travis-ci.org/oshi/oshi.svg)](https://travis-ci.org/oshi/oshi)
[![SonarQube Quality Gate](https://sonarqube.com/api/badges/gate?key=com.github.oshi:oshi-parent)](https://sonarqube.com/dashboard?id=com.github.oshi%3Aoshi-parent)
[![Coverage Status](https://coveralls.io/repos/github/oshi/oshi/badge.svg?branch=master)](https://coveralls.io/github/oshi/oshi?branch=master)
[![codecov.io](https://codecov.io/github/oshi/oshi/coverage.svg?branch=master)](https://codecov.io/github/oshi/oshi?branch=master)
[![Coverity Scan Build Status](https://img.shields.io/coverity/scan/9332.svg)](https://scan.coverity.com/projects/dblock-oshi)
[![Codacy Grade](https://api.codacy.com/project/badge/Grade/5370178ae91d4f56b43de2f26f7c5e7a)](https://www.codacy.com/app/widdis/oshi?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=oshi/oshi&amp;utm_campaign=Badge_Grade)
[![Dependency Status](https://www.versioneye.com/user/projects/58baedff01b5b7003d62096a/badge.svg)](https://www.versioneye.com/user/projects/58baedff01b5b7003d62096a)
[![References](https://www.versioneye.com/java/com.github.dblock:oshi-core/reference_badge.svg?style=flat-square)](https://www.versioneye.com/java/com.github.dblock:oshi-core/references)
[![Eclipse Public License](http://img.shields.io/badge/license-Eclipse-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)
[![Join the chat at https://gitter.im/oshi/oshi](https://badges.gitter.im/oshi/oshi.svg)](https://gitter.im/oshi/oshi?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Project Stats](https://www.openhub.net/p/oshi/widgets/project_thin_badge.gif)](https://www.openhub.net/p/oshi?ref=github)

OSHI is a free JNA-based (native) Operating System and Hardware Information library for Java.
It doesn't require the installation of any additional native libraries and aims to provide a 
cross-platform implementation to retrieve system information, such as OS version, processes, 
memory & CPU usage, disks & partitions, devices, sensors, etc.

OSHI provides lightweight Java objects to enable the core functionality in the `oshi-core` module,
and extends that with flexible, configurable JSON-formatted data in the `oshi-json` module.

Supported platforms 
--------------------------- 
Windows • Linux • Mac OS X • Unix (Solaris, FreeBSD) 

Supported features 
--------------------------
* Computer System and firmware, baseboard 
* Operating System and Version/Build
* Physical (core) and Logical (hyperthreaded) CPUs 
* System and per-processor load % and tick counters
* CPU uptime, processes, and threads
* Process uptime, cpu, memory usage
* Physical and virtual memory used/available
* Mounted filesystems (type, usable and total space)
* Disk drives (model, serial, size) and partitions
* Network interfaces (IPs, bandwidth in/out)
* Battery state (% capacity, time remaining)
* Connected displays (with EDID info)
* USB Devices
* Sensors (temperature, fan speeds, voltage)

Essentials
----------
* [Find OSHI on Maven Central](http://search.maven.org/#search|ga|1|com.github.oshi)
* [FAQ](https://github.com/oshi/oshi/blob/master/FAQ.md) • [API](http://oshi.github.io/oshi/apidocs/) • [Site](http://oshi.github.io/oshi/) 
* [Upgrading from an earlier version?](https://github.com/oshi/oshi/blob/master/UPGRADING.md) 

Downloads
---------
| Stable Release Version | Current Development Version | Dependencies |
| ------------- | ------------- | ------------- |
| [oshi-core-3.4.4](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.github.oshi&a=oshi-core&v=3.4.4&e=jar)  | [oshi-core-3.5.0-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.oshi&a=oshi-core&v=3.5.0-SNAPSHOT&e=jar) | [JNA](https://github.com/java-native-access/jna) • [SLF4J](http://www.slf4j.org/) • [threetenbp](http://www.threeten.org/threetenbp/) |
| [oshi-json-3.4.4](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.github.oshi&a=oshi-json&v=3.4.4&e=jar)   | [oshi-json-3.5.0-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.oshi&a=oshi-json&v=3.5.0-SNAPSHOT&e=jar)  | [javax.json](https://jsonp.java.net/download.html) |

Projects using OSHI
-------------------
* [Systeminfo Binding](https://github.com/openhab/openhab2-addons/tree/master/addons/binding/org.openhab.binding.systeminfo) for [OpenHAB](http://www.openhab.org/)
* [Hawkular WildFly Agent](https://github.com/hawkular/hawkular-agent) for [Hawkular](http://www.hawkular.org/)
* [UniversalMediaServer](https://github.com/UniversalMediaServer/UniversalMediaServer)
* [Dagr](https://github.com/fulcrumgenomics/dagr)
* [sys-API](https://github.com/Krillsson/sys-API)

Output
-------------
OSHI provides output directly via java methods or, for the `oshi-json` project, in JSON format for each of its interfaces.
By periodically polling dynamic information (e.g., every second), users can calculate and track changes.

The `main()` method of [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) 
provides sample code demonstrating the use of `oshi-core` interfaces to retrieve information and calculate additional metrics such as the below examples.

For `oshi-json`, [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-json/src/test/java/oshi/SystemInfoTest.java) 
enables the same capabilities via decorator classes. 
See [oshi.json.properties](https://github.com/oshi/oshi/blob/master/oshi-json/src/test/resources/oshi.json.properties) 
for a sample properties configuration file to customize JSON results.

General information about the operating system and computer system.
```
Apple macOS 10.12.3 (Sierra) build 16D32
manufacturer: Apple Inc.
model: MacBook Pro (MacBookPro8,2)
serialnumber: C02FG6XYDF71
```
Processor identification.
```
Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
 4 physical CPU(s)
 8 logical CPU(s)
Identifier: Intel64 Family 6 Model 42 Stepping 7
ProcessorID: BFEBFBFF000206A7
```
By measuring ticks (user, nice, system, idle, iowait, and irq) between time intervals, percent usage can be calculated.
Java MXBean and per-processor information is also provided.
```
CPU, IOWait, and IRQ ticks @ 0 sec:[967282, 15484, 195343, 124216619], 6176, [4054, 2702]
CPU, IOWait, and IRQ ticks @ 1 sec:[967308, 15484, 195346, 124216790], 6177, [4057, 2705]
User: 13.0% Nice: 0.0% System: 1.5% Idle: 85.5%
CPU load: 8.8% (counting ticks)
CPU load: 9.0% (OS MXBean)
CPU load averages: 2.69 2.47 2.38
CPU load per processor: 23.6% 1.3% 18.2% 0.7% 12.9% 0.7% 12.1% 1.3%
```
Process information including CPU and memory per process is available.
```
Processes: 401, Threads: 1159
   PID  %CPU %MEM       VSZ       RSS Name
 55977  27.9  0.2   6.8 GiB  34.3 MiB java
 51820  18.7  5.6   6.3 GiB 919.2 MiB eclipse
 39272  11.2 17.8   7.1 GiB   2.8 GiB prl_vm_app
 85316   6.5  2.9   5.6 GiB 471.4 MiB thunderbird
 35301   5.4  0.5   1.7 GiB  89.8 MiB Microsoft Excel
 ```
Memory and swapfile information is available.
```
Memory: 2.9 GiB/16 GiB
Swap used: 90.8 MiB/1 GiB
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
Sensor readings are available for some hardware (see notes in the [FAQ](https://github.com/oshi/oshi/blob/master/FAQ.md)).
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

You can run the [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java)
and see the full output for your system by cloning the project and building it with [Maven](http://maven.apache.org/index.html).

```
git clone https://github.com/oshi/oshi.git && cd oshi

mvn test-compile -pl oshi-core -q exec:java \
  -Dexec.mainClass="oshi.SystemInfoTest" \
  -Dexec.classpathScope="test"
```


Where are we?
-------------
[OSHI originated](http://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html) 
as a platform-independent library that did not require additional software and had a license compatible with 
both open source and commercial products. We've developed a strong core of features on major Operating Systems, 
but we would love *you* to help by:
* Testing!  Download and test the program on different software and hardware and help identify gaps that our limited development and testing may have missed.
* Contributing ports.  Have an OS that's not covered? It's likely one of the existing ports can be slightly modified.
* Contributing code.  See something that's not working right or could work better?  Help us fix it!  New contributors welcome.
* Documenting implementation.  Our Wiki is blank.  Want to help new users follow in your footsteps?
* Suggesting new features.  Do you need OSHI to do something it doesn't currently do?  Let us know.


How is this different from ...
------------------------------

* [Sigar](http://sigar.hyperic.com): 
	* Sigar uses [JNI](http://docs.oracle.com/javase/8/docs/technotes/guides/jni/index.html) which requires a native DLL to be installed. OSHI uses [JNA](https://github.com/twall/jna) and doesn't require a native DLL to be installed. 
	* Sigar is licensed under Apache 2.0 license. OSHI is distributed under the EPL license.
	* The last stable release of Sigar (1.6.4) was in 2010. OSHI is under active development as-of 2017. 
* [jHardware](https://github.com/profesorfalken/jHardware):
	* jHardware does not require [JNA](https://github.com/twall/jna) but instead uses command-line parsing and has a limited set of features.  OSHI integrates more native code through JNA and supports more platforms and more features.
	* jHardware presently only supports Windows and *Nix systems.
* [OperatingSystemMXBean](http://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html)
	* The `com.sun.management` MXBean may not be available in non-Oracle JVMs.
	* The `MXBean` has very few methods that address system-wide statistics.
	* OSHI provides significantly more information than the `OperatingSystemMXBean`.

License
-------
This project is licensed under the [Eclipse Public License 1.0](https://www.eclipse.org/legal/epl-v10.html).
