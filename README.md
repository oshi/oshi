OSHI
====
[![Build Status](https://travis-ci.org/dblock/oshi.svg)](https://travis-ci.org/dblock/oshi)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.dblock/oshi-core)
[![Eclipse](http://img.shields.io/badge/license-Eclipse-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)

Oshi is a free JNA-based (native) operating system information library for Java. It doesn't require any additional native DLLs and aims to provide a cross-platform implementation to retrieve system information, such as version, memory, CPU, disk, battery, etc.

Essentials
----------
* [Download Oshi 1.2](http://search.maven.org/#artifactdetails|com.github.dblock|oshi-core|1.2|jar)
* [View the API](http://dblock.github.io/oshi/apidocs/)
* [View the Site](http://dblock.github.io/oshi/)

Where are we?
-------------
Oshi is a very young project. We'd like *you* to contribute a *nix port. Read the [project intro](http://code.dblock.org/introducing-oshi-operating-system-and-hardware-information-java).

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
* How much physical RAM
* How much available (free+reclaimable) RAM
* How many Logical CPUs (core * thread)
* CPU load %
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
CPU load: 4.2%
CPU load average: N/A
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
CPU ticks @ 0 sec:[952268, 13889, 187093, 106906198]
CPU ticks @ 1 sec:[952276, 13889, 187094, 106906389]
User: 4.0% Nice: 0.0% System: 0.5% Idle: 95.5%
CPU load: 5.4%
CPU load average: 0.43
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
CPU ticks @ 0 sec:[12056423, 0, 16937437, 225908843]
CPU ticks @ 1 sec:[12056442, 0, 16937461, 225909605]
User: 2.4% Nice: 0.0% System: 3.0% Idle: 94.7%
CPU load: 6.2%
CPU load average: 1.48
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
	* Oshi provides significantly more information than the OperatingSystemMXBean

License
-------
This project is licensed under the [Eclipse Public License 1.0](LICENSE_EPL).
