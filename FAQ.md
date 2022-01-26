What is the intended use of the API?
========
Users should create a new instance of [SystemInfo](https://oshi.github.io/oshi/apidocs/oshi/SystemInfo.html) and use the getters from this class to access the platform-specific hardware and software interfaces using the respective `get*()` methods. The interfaces in `oshi.hardware` and `oshi.software.os` provide cross-platform functionality. See the `main()` method of [SystemInfoTest](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java) for sample code.

Methods return a "snapshot" of current levels. To display values which change over time, it is intended that users poll for information no more frequently than approximately every second. Disk and file system calls may incur some latency and should be polled less frequently.
CPU usage calculation precision depends on the relation of the polling interval to both system clock tick granularity and the number of logical processors.

Is the API backwards compatible between versions?
========
The interfaces and classes in `oshi.hardware` and `oshi.software.os` are considered the OSHI API and are guaranteed to be compatible with the same major version. Differences between major versions can be found in the [UPGRADING.md](UPGRADING.md) document.  

Most, if not all, of the platform-specific implementations of these APIs in lower level packages will remain the same, although it is not intended that users access platform-specific code, and some changes may occur between minor versions, most often in the number of arguments passed to constructors or platform-specific methods. Supporting code in the `oshi.driver` and `oshi.util` packages may,
rarely, change between minor versions, usually associated with organizing package structure or changing parsing methods for efficiency/consistency/ease of use.

Code in the platform-specific `oshi.jna.*` packages is intended to be temporary and will be removed when that respective code is included in the JNA project.

Does OSHI support Open Service Gateway initiative (OSGi) modules?
========
OSHI adds OSGi manifest entries using `maven-source-plugin` and `mvn-bnd-plugin`. Submit an issue if the configuration of these plugins needs to be adjusted to support your project.

Does OSHI support Java Module System (JPMS) modules?
========
OSHI publishes an `oshi-core-java11` artifact with a full module descriptor (and only modular dependencies), which will allow the existing API to be placed on the module path. This artifact shares the same API as `oshi-core`.

The `oshi-core` artifact includes `Automatic-Module-Name` of `com.github.oshi` in its manifest.  However, Java Module System users are encouraged to use the `oshi-core-java11` artifact to take full advantage of modularization.

More fine grained modularization is being considered in a possible future major API rewrite targeting JDK 21 compatibility and leveraging features from Project Panama (JEP-370, JEP-383, and JEP-389). If you have a specific use case that would benefit from modularization, submit an issue to discuss it.

Is OSHI Thread Safe?
========
OSHI 5.X and above is thread safe with the exceptions noted below. `@Immutable`, `@ThreadSafe`, and `@NotThreadSafe` document
each class. The following classes are not thread-safe:
 - `GlobalConfig` does not protect against multiple threads manipulating the configuration programmatically.
 However, these methods are intended to be used by a single thread at startup in lieu of reading a configuration file.
 OSHI gives no guarantees on re-reading changed configurations.
 - On non-Windows platforms, the `getSessions()` method on the `OperatingSystem` interface uses native code which is not thread safe. While OSHI's methods employ synchronization to coordinate access from its own threads, users are cautioned that other operating system code may access the same underlying data structures and produce unexpected results, particularly on servers with frequent new logins.
The `oshi.os.unix.whoCommand` property may be set to parse the Posix-standard `who` command in preference to the native implementation,
which may use reentrant code on some platforms.
 - The `PerfCounterQueryHandler` class is not thread-safe but is only internally used in single-thread contexts,
and is not intended for user use.

Earlier versions do not guarantee thread safety, and it should not be assumed.

What minimum Java version is required?
========
OSHI 4.x and later require minimum Java 8 compatibility. This minimum level will be retained through at least OpenJDK 8 EOL.

OSHI 3.x is compatible with Java 7 up to 3.13.x versions.  OSHI 3.14.0 restored Java 6 compatibility for the `oshi-core` artifact only. While no new features are envisioned for this branch, bug fixes will be considered if requested on a case basis, particularly if fixed in a later version.

Which operating systems are supported?
========
OSHI has been implemented and tested on the following systems.  Some features may work on earlier versions.
* Windows 7 and higher.  (Nearly all features work on Vista and most work on Windows XP.)
* macOS version 10.6 (Snow Leopard) and higher.
* Linux (Most major distributions) Kernel 2.6 and higher
* FreeBSD 10 
* OpenBSD 6.8
* Solaris 11 (SunOS 5.11) 
* AIX 7.1 (POWER4)

How do I resolve JNA `NoClassDefFoundError` or `NoSuchMethodError` issues?
========
OSHI uses the latest version of JNA, which may conflict with other dependencies your project (or its parent) includes.
If you experience a `NoClassDefFoundError` or `NoSuchMethodError` issues with JNA artifacts, you likely have
an older version of either `jna` or `jna-platform` in your classpath from a transitive dependency on another project.
Consider one or more of the following steps to resolve the conflict:
 - Listing OSHI earlier (or first) in your dependency list 
 - Specifying the most recent version of JNA (both `jna` and `jna-platform` artifacts) in your `pom.xml` as dependencies.
 - If you are using the Spring Boot Starter Parent version 2.2 and earlier that includes JNA as a dependency:
   - Upgrade to version 2.3 which does not have a JNA dependency (preferred)
   - If you must use version 2.2 or earlier, override the `jna.version` property to the latest JNA version.

Why does OSHI's System CPU usage differ from the Windows Task Manager?
========
CPU usage is generally calculated as (active time / active+idle time). From a pure chronological standpoint of "time spent processing tasks" the values calculated in OSHI represent a consistent measure of overall CPU utilization.  It is impossible to exceed 100% with this metric, and the values presented for Windows are consistent with other operating systems.

The Windows Task Manager displays performance using "Processor Utility". Modern CPUs can change frequencies to boost performance or save energy and the Task Manager accounts for these differences.  Processor Utility is documented as "the amount of work a processor is completing, as a percentage of the amount of work the processor could complete if it were running at its nominal performance and never idle. On some processors, Processor Utility may exceed 100%."  (In reality, the Task Manger caps the value at 100%.)

In general, under heavy load, the Task Manager will show a higher percentage than OSHI, but under mostly idle load (when individual cores may reduce frequencies) the Task Manager will show a lower percentage than OSHI.

Why does OSHI's Process CPU usage differ from the Windows Task Manager?
========
CPU usage is generally calculated as (active time / active+idle time). On a multi-processor system, the "idle" time can be accrued on each/any of the logical processors.

For System and per-Processor CPU ticks, the total number of "idle" ticks is available for this calculation, and CPU usage will never exceed 100%.

For per-Process CPU ticks, there is no "idle" counter available, so the calculation ends up being (active time / up time). It is possible for a multi-threaded process to accrue more active clock time than elapsed clock time, and result in CPU usage over 100%
(e.g., on a 4-processor system it could in theory reach 400%). This interpretation matches the value displayed in `ps` or `top` on
Unix-based operating systems. However, Windows scales process CPU usage to the system, so that the sum of all Process CPU percentages can never exceed 100% (ignoring roundoff errors). On a 4-processor system, a single-threaded process maximizing usage of one logical processor will show (on Windows) as 25% usage. OSHI's calculation for Process CPU load will report the Unix-based calculation in this class, which would be closer to 100%.

If you want per-Process CPU load to match the Windows Task Manager display, you should divide OSHI's calculation by the number of logical processors.  This is an entirely cosmetic preference.

How is OSHI different from SIGAR?
========
Both OSHI and Hyperic's [SIGAR](https://github.com/hyperic/sigar) (System Information Gatherer and Reporter)
provide cross-platform operating system and hardware information, and are both used to support distributed
system monitoring and reporting, among other use cases. The OSHI project was started, and development
continues, to overcome specific shortcomings in SIGAR for some use cases.  OSHI does have feature parity
with nearly all SIGAR functions. Key differences include:
 - **Additional DLL** SIGAR's implementation is primarily in native C, compiled separately for its supported
operating systems. It therefore requires users to download an additional DLL specific to their operating
system. This does have a few advantages for specific, targeted use cases, including faster native code routines,
and availability of some native compiler intrinsics. In contrast, OSHI accesses native APIs using JNA, which
does not require user installation of any additional platform-specific DLLs.
 - **Corporate Development / Abandonment** SIGAR was developed commercially at Hyperic to support monitoring of
their HQ product. Hyperic's products were later acquired by VMWare, which has transitioned away from Hyperic
products and have completely abandoned SIGAR. The [last release](https://github.com/hyperic/sigar/releases/tag/sigar-1.6.4)
was in 2010 and the [last source commit](https://github.com/hyperic/sigar/commit/7a6aefc7fb315fc92445edcb902a787a6f0ddbd9)
was in 2015. [Multiple independent forks](https://github.com/hyperic/sigar/issues/95) by existing users attempt
to fix specific bugs/incompatibilities but none has emerged as a maintained/released fork.  In contrast, OSHI's 
development has been entirely done by open source volunteers, and it is under active development as of 2021.
 - **Support** SIGAR is completely unsupported by its authors, and there is no organized community support.
OSHI is supported actively to fix bugs, respond to questions, and implement new features.

Does OSHI work on ARM hardware?
========
Yes, CI is actively conducted on Linux ARM hardware and other platforms will be added when hardware is
available for such testing. Note that many features (e.g., CPUID, and processor identification such as
family, model, stepping, and vendor frequency) are based on Intel chips and may have different corresponding
meanings.

Does OSHI work on Apple M1 hardware?
========
OSHI works with native `AArch64` support when JNA is version 5.7.0 or later.

OSHI works using virtual x86 hardware under Rosetta if you are executing an x86-based JVM. 

Does OSHI work on Raspberry Pi hardware?
========
Yes, most of the Linux code works here and other Pi-specific code has been implemented but has seen 
limited testing.  As the developers do not have a Pi to test on, users reporting issues should be 
prepared to help test solutions.

Will you implement ... ?
========
Maybe!  If you can contribute all the code to implement the feature, it will almost certainly be added.  Even if you can't code but can provide pointers to where the information can be found cross-platform, your feature has a good chance. Otherwise, you can always submit an issue to ask, but are at the mercy of the developers' time, enthusiasm level, and the availability of documentation for the feature.
