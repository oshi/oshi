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
CPU load: 70.59%
Power: 2:42 remaining
 System Battery @ 97.0%
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
CPU load: 70.59%
Power: 2:42 remaining
 BAT0 @ 97.0%
```

For Mac OS X:

```
Apple Mac OS X 10.9.5 (Mavericks) build 13F34
4 CPU(s):
 Intel(R) Core(TM) i7-2677M CPU @ 1.80GHz
 Intel(R) Core(TM) i7-2677M CPU @ 1.80GHz
 Intel(R) Core(TM) i7-2677M CPU @ 1.80GHz
 Intel(R) Core(TM) i7-2677M CPU @ 1.80GHz
Identifier: Intel64 Family 6 Model 42 Stepping 7
Memory: 17.3 MB/4 GB
CPU load: 70.59%
Power: 2:42 remaining
 InternalBattery-0 @ 96.0%
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
