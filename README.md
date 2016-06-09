![OSHI](https://dl.dropboxusercontent.com/u/41603526/oshilogo.png)
[<img align="right" alt="Dependency Status" src="https://www.versioneye.com/user/projects/55fed58c601dd9001500005e/badge.svg?style=flat" />](https://www.versioneye.com/user/projects/55fed58c601dd9001500005e)
[<img align="right" alt="Build Status" src="https://travis-ci.org/dblock/oshi.svg" />](https://travis-ci.org/dblock/oshi)
[<img align="right" alt="Eclipse Public License" src="http://img.shields.io/badge/license-Eclipse-blue.svg" />](https://www.eclipse.org/legal/epl-v10.html)
[<img align="right" alt="Maven central" src="https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core/badge.svg" />](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core)

OSHI is a free JNA-based (native) operating system and hardware information library for Java. It doesn't require any additional native DLLs and aims to provide a cross-platform implementation to retrieve system information, such as version, memory, CPU, disk, battery, displays, etc.

Essentials
----------
* [Find OSHI on Maven Central](http://search.maven.org/#search|ga|1|oshi-core)
* [Download OSHI 2.5.1](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.github.dblock&a=oshi-core&v=2.5.1&e=jar) (Read [UPGRADING.md](UPGRADING.md) if upgrading from version 1.x.)
* [Download OSHI 2.6-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.dblock&a=oshi-core&v=2.6-SNAPSHOT&e=jar)
* [View the API](http://dblock.github.io/oshi/apidocs/) - [View the FAQ](https://github.com/dblock/oshi/blob/master/FAQ.md) - [View the Site](http://dblock.github.io/oshi/)
* Dependencies:
	* [Java Native Access (JNA)](https://github.com/java-native-access/jna)
	* [Simple Logging Facade for Java (SLF4J)](http://www.slf4j.org/)
	* [Java API for JSON Processing (javax.json)](https://jsonp.java.net/download.html)
* Related projects:
	* [oren](https://github.com/zcaudate/oren), a Clojure wrapper for OSHI
	* [jHardware](https://github.com/profesorfalken/jHardware), a pure Java (no JNA) project providing similar information for Windows and Unix
	* [Systeminfo Binding](https://github.com/openhab/openhab2-addons/tree/master/addons/binding/org.openhab.binding.systeminfo) for [OpenHAB](http://www.openhab.org/)

Currently supported platforms 
--------------------------- 
* Windows
* Linux
* Mac OS X<img align="right" src="https://dl.dropboxusercontent.com/u/41603526/samplejson.png" />

Currently supported features 
--------------------------
* Operating System and Version/Build
* Physical (core) and Logical (hyperthreaded) CPUs 
* System and per-processor load % and tick counters
* CPU uptime, processes, and threads
* Process uptime, cpu, memory usage
* Physical and virtual memory used/available
* Network interfaces (IPs, bandwidth in/out)
* Battery state (% capacity, time remaining)
* Disk drives (model, serial, size)
* File stores (usable and total space)
* Connected displays (with EDID info)
* USB Devices
* Sensors (temperature, fan speeds, voltage)

Output
-------------
OSHI provides output directly via java methods or in JSON format for each of its interfaces.
By periodically polling dynamic information (e.g., every second), users can calculate and track changes.

The `main()` method of [SystemInfoTest](https://github.com/dblock/oshi/blob/master/src/test/java/oshi/SystemInfoTest.java) provides sample code demonstrating
the use of the interfaces to retrieve information and calculate additional metrics such as the below examples.

General information about the operating system and processor.
```
Microsoft Windows 7 (Home)
Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
 4 physical CPU(s)
 8 logical CPU(s)
Identifier: Intel64 Family 6 Model 42 Stepping 7
Serial Num: 09203-891-5001202-52183
```
Process information is available
```
Processes: 401, Threads: 1159
   PID  %CPU %MEM       VSZ       RSS Name
 55977  27.9  0.2   6.8 GiB  34.3 MiB java
 51820  18.7  5.6   6.3 GiB 919.2 MiB eclipse
 39272  11.2 17.8   7.1 GiB   2.8 GiB prl_vm_app
 85316   6.5  2.9   5.6 GiB 471.4 MiB thunderbird
 35301   5.4  0.5   1.7 GiB  89.8 MiB Microsoft Excel
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
Sensor readings are available for some hardware (see notes in the [FAQ](https://github.com/dblock/oshi/blob/master/FAQ.md)).
```
Sensors:
 CPU Temperature: 69.8°C
 Fan Speeds:[4685, 4687]
 CPU Voltage: 3.9V
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
     |-- FaceTime HD Camera (Built-in) (Apple Inc.) [s/n: DJHB1V077FDH7HL0]
     |-- IOUSBHostDevice
         |-- Apple Internal Keyboard / Trackpad (Apple Inc.)
         |-- BRCM2070 Hub (Apple Inc.)
             |-- Bluetooth USB Host Controller (Apple Inc.)
 AppleUSBEHCI
 |-- Root Hub Simulation Simulation (Apple Inc.)
     |-- IOUSBHostDevice
         |-- Apple Thunderbolt Display (Apple Inc.) [s/n: 162C0C25]
         |-- Display Audio (Apple Inc.) [s/n: 162C0C25]
         |-- FaceTime HD Camera (Display) (Apple Inc.) [s/n: CCGCAN000TDJ9DFX]
         |-- USB2.0 Hub
             |-- ANT USBStick2 (Dynastream Innovations) [s/n: 051]
             |-- Fitbit Base Station (Fitbit Inc.)
             |-- Fitbit Base Station (Fitbit Inc.)
```


Where are we?
-------------
OSHI is a young project. While we've developed a strong core of features on major Operating Systems, we'd like *you* to contribute ports, and help implement more methods, and suggest new features. Read the [project intro](http://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html).

How is this different from ...
------------------------------

* [Sigar](http://sigar.hyperic.com): 
	* Sigar uses [JNI](http://docs.oracle.com/javase/8/docs/technotes/guides/jni/index.html) which requires a native DLL to be installed. OSHI uses [JNA](https://github.com/twall/jna) and doesn't require a native DLL to be installed. 
	* Sigar is licensed under Apache 2.0 license. OSHI is distributed under the EPL license.
	* The last stable release of Sigar (1.6.4) was in 2010. OSHI is under active development as-of 2016.
* [jHardware](https://github.com/profesorfalken/jHardware):
	* jHardware does not require [JNA](https://github.com/twall/jna) but instead uses command-line parsing.  OSHI uses some command line parsing but attempts to use native commands whenever possible.
	* jHardware presently only supports Windows and Unix systems.
	* jHardware is licensed under Apache 2.0 license. OSHI is distributed under the EPL license.
* [OperatingSystemMXBean](http://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html)
	* The `com.sun.management` MXBean may not be availabile in non-Oracle JVMs.
	* The `MXBean` has very few methods that address system-wide statistics.
	* OSHI provides significantly more information than the `OperatingSystemMXBean`.

License
-------
This project is licensed under the [Eclipse Public License 1.0](LICENSE_EPL).
