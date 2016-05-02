What is the intended use of the API?
========
Users should create a new instance of [SystemInfo](http://dblock.github.io/oshi/apidocs/oshi/SystemInfo.html).  This provides access to the platform-specific hardware and software interfaces using the respective `get*()` methods.  The interfaces in `oshi.hardware` and `oshi.software.os` provide cross-platform funcionality.

Most methods return a "snapshot" of current levels.  To display values which change over time, it is intended that users poll for information no more frequently than one second. Disk and file system calls may incur some latency and should be polled less frequently.

Is the API backwards compatible?
========
The interfaces and classes in `oshi.hardware` and `oshi.software.os` are considered the OSHI API and are guaranteed to be compatible with the same major version.  Differences between major versions can be found in the [UPGRADING.md](UPGRADING.md) document.  

Most, if not all, of the platform-specific implementations of these APIs in lower level packages will remain the same as well, although it is not intended that users access platform-specific code and some changes may occur between minor versions. Supporting code in the `oshi.util` package may, rarely, change between minor versions, usually associated with organizing package structure or changing parsing methods for efficiency/consistency/ease of use.

Code in the platform-specific `oshi.jna.*` packages is intended to be temporary and will be removed when that respective code is included in the JNA project.

Which operating systems are supported?
========
OSHI has been implemented and tested on the following systems.  Some features may work on earlier versions.
* Windows 7 and higher. 
* Mac OS X version 10.6 (Snow Leopard) and higher
* Linux (Most major distributions) Kernel 2.6 and higher

What API features are not implemented on some operating systems?
========
The following generally summarizes known exceptions. If you have missing data that is not on this list, please report it in an issue so we can investigate.
* Windows does not provide a load average, so the Processor's `getSystemLoadAverage()` returns -1.
* Mac OS X does not track time processors spend idle due to hard disk latency (iowait) or time spent processing hardware or software interrupts, and returns 0 for those associated tick values.
* Windows sensor (temperature, fans, voltage) readings are drawn from Microsoft's Windows Management Instrumentation (WMI) API; however, most hardware manufacturers do not publish these readings to WMI. If a value is not available through the Microsoft API, Oshi will attempt to retrieve values as published by the [Open Hardware Monitor](http://openhardwaremonitor.org/) if it is running.
* Total reads and writes to hard drives are only available on Linux.

Does OSHI always use native code calls?
========
When reasonably possible, OSHI uses the lowest level operating system functions required to obtain its information and avoids command line functions except when necessary or practical to avoid overly complex code.  General exceptions to native calls include:
* In Linux, the /proc pseudo-file system is intended by Linux kernel developers as a user-level API for most information, and files in this directory structure are parsed often.
* In Windows, WMI is called using the `wmic` command line interface.  While it is possible to programmatically access this information using COM system calls, that feature has not yet been implemented.

Will you implement feature X?
========
Maybe!  If you can contribute all the code to implement the feature, it will almost certainly be added.  Even if you can't code but can provide pointers to where the information can be found cross-platform, your feature has a good chance. Otherwise, you can always submit an issue to ask, but are at the mercy of the developers' time, enthusiasm level, and the availability of documentation for the feature.
