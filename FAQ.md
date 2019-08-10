What is the intended use of the API?
========
Users should create a new instance of [SystemInfo](http://oshi.github.io/oshi/apidocs/oshi/SystemInfo.html) and use the getters from this class to access the platform-specific hardware and software interfaces using the respective `get*()` methods. The interfaces in `oshi.hardware` and `oshi.software.os` provide cross-platform functionality. See the `main()` method of [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) for sample code.

Methods return a "snapshot" of current levels. To display values which change over time, it is intended that users poll for information no more frequently than approximately every second. Disk and file system calls may incur some latency and should be polled less frequently. 

Is the API backwards compatible between versions?
========
The interfaces and classes in `oshi.hardware` and `oshi.software.os` are considered the OSHI API and are guaranteed to be compatible with the same major version. Differences between major versions can be found in the [UPGRADING.md](UPGRADING.md) document.  

Most, if not all, of the platform-specific implementations of these APIs in lower level packages will remain the same, although it is not intended that users access platform-specific code, and some changes may occur between minor versions, most often in the number of arguments passed to constructors or platform-specific methods. Supporting code in the `oshi.util` package may, rarely, change between minor versions, usually associated with organizing package structure or changing parsing methods for efficiency/consistency/ease of use.

Code in the platform-specific `oshi.jna.*` packages is intended to be temporary and will be removed when that respective code is included in the JNA project.

Is OSHI Thread Safe?
========
Short answer: No, but multi-thread implementations can be constructed to avoid problems.

Longer answer: In the normal use case of OSHI (using getters to retrieve information) there will be no thread safety issues if each thread is attempting to get information from a different object.  There are generally two ways to do this: (1) Have each thread instantiate a new instance of `SystemInfo`, or (2) have each thread deal only with access from objects not used by other threads, e.g., one thread can fetch memory information while another thread fetches Disks, and another fetches file system information. Util classes accessed by multiple objects use synchronized access or factory patterns to prevent conflicts.  We would love to improve the thread safety, but the effort required exceeds the available bandwidth of the sole maintainer.

What minimum Java version is required?
========
OSHI 3.x is compatible with Java 7, but will not see any added features.  
OSHI 4.x requires minimum Java 8 compatibility, although backporting to Java 7 would not be difficult on a fork.
OSHI 5.x's requirements have not yet been finalized but will likely require at least Java 11.  

Which operating systems are supported?
========
OSHI has been implemented and tested on the following systems.  Some features may work on earlier versions.
* Windows 7 and higher.  (Nearly all features work on Vista and most work on Windows XP.)
* Mac OS X version 10.6 (Snow Leopard) and higher
* Linux (Most major distributions) Kernel 2.6 and higher
* Unix: Solaris 11 (SunOS 5.11) / FreeBSD 10

What API features are not implemented on some operating systems?
========
The following generally summarizes known exceptions. If you have missing data that is not on this list, please report it in an issue so we can investigate.
* Windows does not provide a load average, so the Processor's `getSystemLoadAverage()` returns -1.
* MacOS does not track time processors spend idle due to hard disk latency (iowait) or time spent processing hardware or software interrupts, and returns 0 for those associated tick values.
* Windows sensor (temperature, fans, voltage) readings are drawn from Microsoft's Windows Management Instrumentation (WMI) API; however, most hardware manufacturers do not publish these readings to WMI. If a value is not available through the Microsoft API, Oshi will attempt to retrieve values as published by the [Open Hardware Monitor](http://openhardwaremonitor.org/) if it is running.  Only temperature sensors are detected on FreeBSD using `coretemp`.
* Linux, Solaris, and FreeBSD may require either running as root/sudo or additional software installs for full capability, particularly HAL daemon (`hald`/`lshal`) and X (`xrandr`).

What is the history of OSHI and plans for future development?
========
OSHI was [started in 2010](https://code.dblock.org/2010/06/23/introducing-oshi-operating-system-and-hardware-information-java.html) 
by [@dblock](https://github.com/dblock) as a way to avoid the additional DLL installation requirements of SIGAR and have (at the time) a
license more friendly to commercial use. There was some initial work on basic CPU and Memory stats on Windows using native calls,
and ports to Mac and Linux mostly using command lines, but little in the way of added features. Still, the API skeleton was there,
well designed (still in use a decade later!) and was easy to build on.

In 2015, [@dbwiddis](https://github.com/dbwiddis) was working on a cloud-based distributed computing data mining problem utilizing 
[JPPF](https://jppf.org/). As part of optimizing the use of cloud resources (use all the CPU and memory that's being paid for) 
here was a need for Linux memory usage monitoring. JPPF's library used the OperatingSystemMXBean's getFreePhysicalMemory() method 
which was useless on Linux, as "free" always decreases to zero even though there's plenty "available". Web searches revealed only
two packaged alternatives: SIGAR (which had stopped development then) and OSHI (which had the same bug, but was open source and fixable). 
A bug report turned into an invitation to submit a PR, which turned into converting the OS X commandline calls to native methods and 
adding more, which turned into an invitation to take over the project and a very educational experience in learning git, maven, the 
software development lifecycle, unit testing, continuous integration, and more.

A few contributors have a grand plan for [OSHI 5](https://github.com/oshi/oshi5) and will be completely redesigning the API with
an eye toward more modular driver-based information access.  The current 4.x API will probably only see small incremental improvements, 
bug fixes, and attempts to increase poratibility to other operating systems and/or versions.  

Does OSHI work on Raspberry Pi hardware?
========
Yes, most of the Linux code works here and other Pi-specific code has been implemented but has seen limited testing.  As the 
developers do not have a Pi to test on, users reporting issues should be prepared to help test solutions.

Will you do an AIX port?
========
We'd love to. We need access to an AIX machine. If you can offer such access, create an issue to notify the team of the possibility.

Will you do an OpenBSD or NetBSD or kFreeBSD port?  How about Android?
========
Unlikely without bribes such as a lifetime supply of coffee.  But happy to advise someone else who wants to do so!

Will you do a Windows CE port?
========
No.

Will you implement ... ?
========
Maybe!  If you can contribute all the code to implement the feature, it will almost certainly be added.  Even if you can't code but can provide pointers to where the information can be found cross-platform, your feature has a good chance. Otherwise, you can always submit an issue to ask, but are at the mercy of the developers' time, enthusiasm level, and the availability of documentation for the feature.
