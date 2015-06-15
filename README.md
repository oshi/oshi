OSHI
====

[![Build Status](https://travis-ci.org/dblock/oshi.svg)](https://travis-ci.org/dblock/oshi)

Oshi is a free JNA-based (native) operating system information library for Java. It doesn't require any additional native DLLs and aims to provide a cross-platform implementation to retrieve system information, such as version, memory, CPU, disk, battery, etc.

Download
--------

* [Oshi 1.2](https://github.com/dblock/oshi/releases/download/v1.2/oshi-1.2.zip)

Maven 
--------

* To include Oshi in your Maven project, add the following dependency:
```xml
        <dependency>
            <groupId>com.github.dblock</groupId>
            <artifactId>oshi-core</artifactId>
            <version>1.2</version>
        </dependency>
```

Where are we?
-------------

Oshi is a very young project. We'd like *you* to contribute a *nix port. Read the [project intro](http://code.dblock.org/introducing-oshi-operating-system-and-hardware-information-java).

Current supported platforms
---------------------------

- Windows
- Linux
- Mac OS-X

Current supported features
--------------------------

### Operating Systems ###

* Manufacturer
  - GNU/Linux
  - Microsoft
  - Apple

* Family
  - Mac OS X
  - Windows
  - Linux Distribution (Fedora, Ubuntu, ...)

* Version
  - Version number
  - Codename
  - Build

### Hardware ###

* How much physical RAM
* How much available RAM
* How many CPUs (core * thread)
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

This project is licensed under the [Eclipse Public License 1.0](LICENSE.txt).
