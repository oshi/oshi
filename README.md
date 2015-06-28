OSHI
====
[![Build Status](https://travis-ci.org/dblock/oshi.svg)](https://travis-ci.org/dblock/oshi)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core)
[![Eclipse](http://img.shields.io/badge/license-Eclipse-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)

Oshi is a free JNA-based (native) operating system information library for Java. It doesn't require any additional native DLLs and aims to provide a cross-platform implementation to retrieve system information, such as version, memory, CPU, disk, battery, etc.

Essentials
----------
* [Download Oshi 1.3](http://search.maven.org/#artifactdetails|com.github.dblock|oshi-core|1.3|jar)
* [View the API](http://dblock.github.io/oshi/apidocs/)
* [View the Site](http://dblock.github.io/oshi/)

Where are we?
-------------
Oshi is a young project. We'd like *you* to contribute ports and help imiplement more methods. Read the [project intro](http://code.dblock.org/introducing-oshi-operating-system-and-hardware-information-java).

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
* How much physical/available (free+reclaimable) RAM
* How many Logical CPUs (core * thread)
* CPU load % and tick counters
* Battery state (% capacity, time remaining)
* File stores (usable and total space)

Sample Output
-------------
Here's sample tests output:

For Windows:

```
Microsoft Windows 7
2 CPU(s):
 Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
 Intel(R) Core(TM)2 Duo CPU T7300  @ 2.00GHz
Identifier: Intel64 Family 6 Model 42 Stepping 7
Memory: 532.1 MB/2.0 GB
CPU ticks @ 0 sec:[26579029, 0, 21746695, 549739287]
CPU ticks @ 1 sec:[26579060, 0, 21746695, 549740254]
User: 3.1% Nice: 0.0% System: 0.0% Idle: 96.9%
CPU load: 3.3% (counting ticks)
CPU load: 3.2% (OS MXBean)
CPU load average: N/A
CPU load per processor: 3.8% 4.0%
Power: 2:42 remaining
 System Battery @ 97.0%
File System:
 Floppy Disk Drive (A:) (Floppy Disk Drive) 1.1 MB of 1.4 MB free (82.4%)
 Local Disk (C:) (Local Disk) 27.3 GB of 64.0 GB free (42.7%)
 D:\ (CD Drive) 0 bytes of 0 bytes free 
 MobileBackups on 'psf' (W:) (Network Drive) 0 bytes of 697.5 GB free (0.0%)
 MacData on 'psf' (X:) (Network Drive) 3.4 GB of 4.4 GB free (77.4%)
 Home on 'psf' (Y:) (Network Drive) 121.7 GB of 697.5 GB free (17.4%)
 Host on 'psf' (Z:) (Network Drive) 121.7 GB of 697.5 GB free (17.4%)
```

For Linux:

```
GNU/Linux Fedora 20 (Heisenbug)
8 CPU(s):
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
 Intel(R) Core(TM) i7-3720QM CPU @ 2.60GHz
Identifier: Intel64 Family 6 Model 42 Stepping 7
Memory: 21.0 GB/31.0 GB
CPU ticks @ 0 sec:[967282, 15484, 195343, 124216619]
CPU ticks @ 1 sec:[967308, 15484, 195346, 124216790]
User: 13.0% Nice: 0.0% System: 1.5% Idle: 85.5%
CPU load: 14.5% (counting ticks)
CPU load: 14.3% (OS MXBean)
CPU load average: 1.13
CPU load per processor: 21.4% 4.9% 19.5% 4.0% 27.5% 4.6% 19.9% 4.8%
Power: 2:42 remaining
 BAT0 @ 97.0%
File System:
 / (Local Disk) 52.8 GB of 60.9 GB free (86.7%)
 Home (Mount Point) 134.5 GB of 697.5 GB free (19.3%)
 MacData (Mount Point) 3.4 GB of 4.4 GB free (77.4%)
 MobileBackups (Mount Point) 0 bytes of 697.5 GB free (0.0%)
```

For Mac OS X:

```
Apple Mac OS X 10.10.4 (Yosemite) build 14E36b
4 CPU(s):
 Intel(R) Core(TM) i7-2820QM CPU @ 2.30GHz
 Intel(R) Core(TM) i7-2820QM CPU @ 2.30GHz
 Intel(R) Core(TM) i7-2820QM CPU @ 2.30GHz
 Intel(R) Core(TM) i7-2820QM CPU @ 2.30GHz
Identifier: Intel64 Family 6 Model 42 Stepping 7
Memory: 17.3 MB/4 GB
CPU ticks @ 0 sec:[15973594, 0, 21796209, 286595204]
CPU ticks @ 1 sec:[15973619, 0, 21796271, 286595920]
User: 3.1% Nice: 0.0% System: 7.7% Idle: 89.2%
CPU load: 11.3% (counting ticks)
CPU load: 11.4% (OS MXBean)
CPU load average: 1.48
CPU load per processor: 25.2% 1.9% 17.3% 1.9% 
Power: 2:42 remaining
 InternalBattery-0 @ 96.0%
File System:
 Data (Network Drive) 15.7 GB of 1.8 TiB free (0.8%)
 MacData (Volume) 3.4 GB of 4.4 GB free (77.4%)
 Macintosh HD (/) (Local Disk) 134.4 GB of 697.5 GB free (19.3%)
 MobileBackups (Network Drive) 0 bytes of 697.5 GB free (0.0%)
 Time Machine Backups (Local Disk) 134.4 GB of 697.5 GB free (19.3%)
```

How is this different from ...
------------------------------

* [Sigar](http://sigar.hyperic.com): 
	* Sigar uses [JNI](http://docs.oracle.com/javase/8/docs/technotes/guides/jni/index.html) which requires a native DLL to be installed. Oshi uses [JNA](https://github.com/twall/jna) and doesn't require a native DLL to be installed. 
	* Sigar is licensed under Apache 2.0 license. Oshi is distributed under the EPL license.
	* Sigar appears to be no longer actively supported as-of 2010. Oshi is under active development as-of 2015.
* [OperatingSystemMXBean](http://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html)
	* The `com.sun.management` MXBean may not be availabile in non-Oracle JVMs.
	* The MXBean has very few methods that address system-wide statistics.
	* Oshi provides significantly more information than the OperatingSystemMXBean

License
-------
This project is licensed under the [Eclipse Public License 1.0](LICENSE_EPL).
