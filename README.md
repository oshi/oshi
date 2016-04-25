OSHI
====
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core)
[![Eclipse](http://img.shields.io/badge/license-Eclipse-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)
[![Build Status](https://travis-ci.org/dblock/oshi.svg)](https://travis-ci.org/dblock/oshi)
[![Dependency Status](https://www.versioneye.com/user/projects/55fed58c601dd9001500005e/badge.svg?style=flat)](https://www.versioneye.com/user/projects/55fed58c601dd9001500005e)

Oshi is a free JNA-based (native) operating system information library for Java. It doesn't require any additional native DLLs and aims to provide a cross-platform implementation to retrieve system information, such as version, memory, CPU, disk, battery, displays, etc.

Essentials
----------
* [Find Oshi on Maven Central](http://search.maven.org/#search|ga|1|oshi-core)
* [Download Oshi 2.3](https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.github.dblock&a=oshi-core&v=2.3&e=jar) (Read [UPGRADING.md](UPGRADING.md) if upgrading from version 1.x.)
* [Download Oshi 2.4-SNAPSHOT](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=com.github.dblock&a=oshi-core&v=2.4-SNAPSHOT&e=jar)
* [View the API](http://dblock.github.io/oshi/apidocs/)
* [View the Site](http://dblock.github.io/oshi/)
* Dependencies:
	* [Java Native Access (JNA)](https://github.com/java-native-access/jna)
	* [Simple Logging Facade for Java (SLF4J)](http://www.slf4j.org/)
	* [Java API for JSON Processing (javax.json)](https://jsonp.java.net/download.html)
* Related projects:
	* [oren](https://github.com/zcaudate/oren), a Clojure wrapper for Oshi
	* [jHardware](https://github.com/profesorfalken/jHardware), a pure Java (no JNA) project providing similar information for Windows and Unix

Where are we?
-------------
Oshi is a young project. While we've developed a strong core of features on major Operating Systems, we'd like *you* to contribute ports, and help implement more methods, and suggest new features. Read the [project intro](http://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html).

Current supported platforms
---------------------------
- Windows
- Linux
- Mac OS X

Current supported features
--------------------------

### Operating Systems ###
* Manufacturer (GNU/Linux, Microsoft, Apple)
* OS (Linux Distribution, Windows, Mac OS X)
* OS Version (Version number, Codename, Build)

### Hardware ###
* How much physical/available (free+reclaimable) RAM, and total/used swap (pagefile)
* How many Physical (core) and Logical (core * thread) CPUs 
* CPU uptime, load % and tick counters
* CPU temperature, fan speeds, voltage (if available; some OS's, some CPUs)
* Battery state (% capacity, time remaining)
* Disk drives (model, serial, size)
* File stores (usable and total space)
* Connected displays (with EDID info)

Sample Output
-------------
Here's sample output from the `main` method of [SystemInfoTest](https://github.com/dblock/oshi/blob/master/src/test/java/oshi/SystemInfoTest.java):

For Windows:

```
Microsoft Windows 7
Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
 1 physical CPU(s)
 2 logical CPU(s)
Identifier: Intel64 Family 6 Model 42 Stepping 7
Serial Num: 09203-891-5001202-52183
Memory: 1.3 GiB/2.7 GiB
Swap used: 278 MiB/2.7 GiB
Uptime: 12 days, 11:00:17
CPU, IOWait, and IRQ ticks @ 0 sec:[26579029, 0, 21746695, 549739287], 672432, [520882, 156271]
CPU, IOWait, and IRQ ticks @ 1 sec:[26579060, 0, 21746695, 549740254], 672435, [520883, 156271]
User: 3.1% Nice: 0.0% System: 0.0% Idle: 96.9%
CPU load: 3.3% (counting ticks)
CPU load: 3.2% (OS MXBean)
CPU load averages: N/A N/A N/A
CPU load per processor: 3.8% 4.0%
Processes: 45, Threads: 652
Sensors:
 CPU Temperature: 73.5°C
 Fan Speeds:[0]
 CPU Voltage: 3.3V
Power: 2:42 remaining
 System Battery @ 97.0%
File System:
 Floppy Disk Drive (A:) (Floppy Disk Drive) [FAT] 1.1 MiB of 1.4 MiB free (82.4%)
 Local Disk (C:) (Local Disk) [NTFS] 27.3 GiB of 64.0 GiB free (42.7%)
 D:\ (CD Drive) [unknown] 0 bytes of 0 bytes free 
 MobileBackups on 'psf' (W:) (Network Drive) [PrlSF] 0 bytes of 697.5 GiB free (0.0%)
 MacData on 'psf' (X:) (Network Drive) [PrlSF] 3.4 GiB of 4.4 GiB free (77.4%)
 Home on 'psf' (Y:) (Network Drive) [PrlSF] 121.7 GiB of 697.5 GiB free (17.4%)
 Host on 'psf' (Z:) (Network Drive) [PrlSF] 121.7 GiB of 697.5 GiB free (17.4%)
Disks:
 \\.\PHYSICALDRIVE0: (model: Windows 7-0 ATA Device (Standard disk drives) - S/N:314143413454355748334d4445464d5938414a34) size: 640 GB, reads: ?, writes: ?
Displays:
 Display 0:
  Manuf. ID=DEL, Product ID=4014, Analog, Serial=BENG, ManufDate=3/2006, EDID v1.3
  38 x 30 cm (15.0 x 11.8 in)
  Preferred Timing: Clock 108MHz, Active Pixels 1280x1024 
  Serial Number: DC32363EBENG
  Monitor Name: DELL 1907FP
  Range Limits: Field Rate 56-76 Hz vertical, 30-81 Hz horizontal, Max clock: 140 MHz
 Display 1:
  Manuf. ID=DEL, Product ID=4026, Analog, Serial=RFN7, ManufDate=10/2007, EDID v1.3
  38 x 30 cm (15.0 x 11.8 in)
  Preferred Timing: Clock 108MHz, Active Pixels 1280x1024 
  Serial Number: FP1827AFRFN7
  Monitor Name: DELL 1908FP
  Range Limits: Field Rate 56-76 Hz vertical, 30-81 Hz horizontal, Max clock: 140 MHz
```

For Linux:

```
GNU/Linux Fedora 20 (Heisenbug)
Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 4 physical CPU(s)
 8 logical CPU(s)
Identifier: Intel64 Family 6 Model 42 Stepping 7
Serial Num: CN123456789098
Memory: 21.8 GiB/31 GiB
Swap used: 836 MiB/2 GiB[
Uptime: 12 days, 11:00:17
CPU, IOWait, and IRQ ticks @ 0 sec:[967282, 15484, 195343, 124216619], 6176, [4054, 2702]
CPU, IOWait, and IRQ ticks @ 1 sec:[967308, 15484, 195346, 124216790], 6177, [4057, 2705]
User: 13.0% Nice: 0.0% System: 1.5% Idle: 85.5%
CPU load: 14.5% (counting ticks)
CPU load: 14.3% (OS MXBean)
CPU load averages: 1.13 1.19 0.97
CPU load per processor: 21.4% 4.9% 19.5% 4.0% 27.5% 4.6% 19.9% 4.8%
Processes: 251, Threads: 468
Sensors:
 CPU Temperature: 62.0°C
 Fan Speeds:[2339, 2344]
 CPU Voltage: 3.6V
Power: 2:42 remaining
 BAT0 @ 97.0%
File System:
 / (Local Disk) [ext4] 52.8 GiB of 60.9 GiB free (86.7%)
 Home (Mount Point) [prl_fs] 134.5 GiB of 697.5 GiB free (19.3%)
 MacData (Mount Point) [prl_fs] 3.4 GiB of 4.4 GiB free (77.4%)
 MobileBackups (Mount Point) [prl_fs] 0 bytes of 697.5 GiB free (0.0%)
Disks:
 /dev/sda: (model: Linux-0 - S/N: XQ1PCFXNN6YMN4JDF120) size: 480 GB, reads: 2.3 GiB, writes: 6.0 GiB 
 /dev/sr0: (model: Virtual_DVD-ROM__1_ - S/N: -_31415B265) size: 1.0 GB, reads: ?, writes: ? 
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

For Mac OS X:

```
Apple Mac OS X 10.11.3 (El Capitan) build 15D21
Intel(R) Core(TM) i7-2820QM CPU @ 2.30GHz
 2 physical CPU(s)
 4 logical CPU(s)
Identifier: Intel64 Family 6 Model 42 Stepping 7
Serial Num: C02FG3HIJK45
Memory: 4.8 GiB/16 GiB
Swap used: 836 MiB/2 GiB
Uptime: 12 days, 11:00:17
CPU, IOWait, and IRQ ticks @ 0 sec:[15973594, 0, 21796209, 286595204], 0, [0, 0]
CPU, IOWait, and IRQ ticks @ 1 sec:[15973619, 0, 21796271, 286595920], 0, [0, 0]
User: 3.1% Nice: 0.0% System: 7.7% Idle: 89.2%
CPU load: 11.3% (counting ticks)
CPU load: 11.4% (OS MXBean)
CPU load average: 1.64 1.70 1.30
CPU load per processor: 25.2% 1.9% 17.3% 1.9%
Processes: 266, Threads: 1733
Sensors:
 CPU Temperature: 67.0°C
 Fan Speeds:[3402, 3399]
 CPU Voltage: 4.3V
Power: 2:42 remaining
 InternalBattery-0 @ 96.0%
File System:
 Data (Network Drive) [smbfs] 15.7 GiB of 1.8 TiB free (0.8%)
 MacData (Volume) [hfs] 3.4 GiB of 4.4 GiB free (77.4%)
 Macintosh HD (/) (Local Disk) [hfs] 134.4 GiB of 697.5 GiB free (19.3%)
 MobileBackups (Network Drive) [mtmfs] 0 bytes of 697.5 GiB free (0.0%)
 Time Machine Backups (Local Disk) [hfs] 134.4 GiB of 697.5 GiB free (19.3%)
Disks:
 disk0: (model: ST2000LM003 HN-M201RAD - S/N: S34RJ9FGB21964) size: 2.0 TB, reads: ?, writes: ? 
 disk1: (model: MATSHITADVD-R   UJ-8A8 - S/N: D0211230551DDPPAF) size: ?, reads: ?, writes: ? 
Displays:
 Display 0:
  Manuf. ID=A, Product ID=9cb6, Analog, Serial=00000000, ManufDate=6/2009, EDID v1.3
  33 x 21 cm (13.0 x 8.3 in)
  Preferred Timing: Clock 119MHz, Active Pixels 3840x1440 
  Manufacturer Data: 000000010006103000000000000000000A20
  Unspecified Text: LTN158MT07
  Monitor Name: Color LCD
 Display 1:
  Manuf. ID=A, Product ID=9227, Analog, Serial=162C0C25, ManufDate=11/2012, EDID v1.4
  60 x 34 cm (23.6 x 13.4 in)
  Preferred Timing: Clock 241MHz, Active Pixels 2560x3840 
  Preferred Timing: Clock 74MHz, Active Pixels 1280x3840 
  Serial Number: C02JM2PFF1GC
  Monitor Name: Thunderbolt
```

How is this different from ...
------------------------------

* [Sigar](http://sigar.hyperic.com): 
	* Sigar uses [JNI](http://docs.oracle.com/javase/8/docs/technotes/guides/jni/index.html) which requires a native DLL to be installed. Oshi uses [JNA](https://github.com/twall/jna) and doesn't require a native DLL to be installed. 
	* Sigar is licensed under Apache 2.0 license. Oshi is distributed under the EPL license.
	* The last stable release of Sigar (1.6.4) was in 2010. Oshi is under active development as-of 2016.
* [jHardware](https://github.com/profesorfalken/jHardware):
	* jHardware does not require [JNA](https://github.com/twall/jna) but instead uses command-line parsing.  Oshi uses some command line parsing but attempts to use native commands whenever possible.
	* jHardware presently only supports Windows and Unix systems.
	* jHardware is licensed under Apache 2.0 license. Oshi is distributed under the EPL license.
* [OperatingSystemMXBean](http://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html)
	* The `com.sun.management` MXBean may not be availabile in non-Oracle JVMs.
	* The `MXBean` has very few methods that address system-wide statistics.
	* Oshi provides significantly more information than the `OperatingSystemMXBean`.

License
-------
This project is licensed under the [Eclipse Public License 1.0](LICENSE_EPL).
