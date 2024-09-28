# 6.6.6 (in progress)

* Your contribution here!

# 6.6.0 (2024-04-13), 6.6.1 (2024-05-26), 6.6.2 (2024-07-21), 6.6.3 (2024-08-20), 6.6.4 (2024-09-15), 6.6.5 (2024-09-16)

##### New Features
* [#2603](https://github.com/oshi/oshi/pull/2603),
  [#2625](https://github.com/oshi/oshi/pull/2625): Add part number to Physical Memory - [@BartekDziurowicz](https://github.com/BartekDziurowicz), [@dbwiddis](https://github.com/dbwiddis).
* [#2644](https://github.com/oshi/oshi/pull/2644): Add serial number to Physical Memory - [@Tegrason](https://github.com/Tegrason).

##### Bug fixes / Improvements
* [#2605](https://github.com/oshi/oshi/pull/2605): Reduce CpuStat.getSystemCpuLoadticks memory allocation pressure - [@chrisribble](https://github.com/chrisribble).
* [#2612](https://github.com/oshi/oshi/pull/2612): Use 1k buffer in FileUtils.readLines to reduce heap allocation pressure - [@chrisribble](https://github.com/chrisribble).
* [#2621](https://github.com/oshi/oshi/pull/2621): Cache thread counters when updating OS Process with suspended state - [@dbwiddis](https://github.com/dbwiddis).
* [#2626](https://github.com/oshi/oshi/pull/2626): Make sys and dev paths on Linux configurable - [@dbwiddis](https://github.com/dbwiddis).
* [#2627](https://github.com/oshi/oshi/pull/2627): Add more SMBIOSMemoryType values - [@dbwiddis](https://github.com/dbwiddis).
* [#2645](https://github.com/oshi/oshi/pull/2645): fix getOwningProcessId sometimes return -1 on 64x linux - [@yourancc](https://github.com/yourancc).
* [#2660](https://github.com/oshi/oshi/pull/2660): Add macOS 15 (Sequoia) to version properties - [@dbwiddis](https://github.com/dbwiddis).
* [#2662](https://github.com/oshi/oshi/pull/2662): Only warn on duplicate properties files if they differ - [@dbwiddis](https://github.com/dbwiddis).
* [#2692](https://github.com/oshi/oshi/pull/2692): Do not log errors for reading process arguments on Linux - [@wolfs](https://github.com/wolfs).
* [#2704](https://github.com/oshi/oshi/pull/2704): Properly parse CPU vendor when lscpu not available - [@dbwiddis](https://github.com/dbwiddis).
* [#2705](https://github.com/oshi/oshi/pull/2705): Restore optional legacy method of calculating Windows System CPU - [@dbwiddis](https://github.com/dbwiddis).
* [#2711](https://github.com/oshi/oshi/pull/2711): Do not log error on macOS for hw.nperflevels - [@Puppy4C](https://github.com/Puppy4C).
* [#2722](https://github.com/oshi/oshi/pull/2722): Fix speed value for LinuxNetworkIF - [@Puppy4C](https://github.com/Puppy4C).
* [#2724](https://github.com/oshi/oshi/pull/2724): Clarify IO bytes documentation on OSProcess - [@dbwiddis](https://github.com/dbwiddis).
* [#2725](https://github.com/oshi/oshi/pull/2725): Reduce redundant logging on perf counter failures - [@dbwiddis](https://github.com/dbwiddis).
* [#2726](https://github.com/oshi/oshi/pull/2726): JNA 5.15.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#2740](https://github.com/oshi/oshi/pull/2740): Fix Linux CPU thermal zone path string - [@1056227556](https://github.com/1056227556).

# 6.5.0 (2024-03-10)

##### New Features
* [#2592](https://github.com/oshi/oshi/pull/2592): Add getFeatureFlags method to CentralProcessor API - [@dbwiddis](https://github.com/dbwiddis).

# 6.4.0 (2022-12-02), 6.4.1 (2023-03-18), 6.4.2 (2023-05-02), 6.4.3 (2023-06-06), 6.4.4 (2023-07-01), 6.4.5 (2023-08-20), 6.4.6 (2023-09-24), 6.4.7 (2023-11-01), 6.4.8 (2023-11-24), 6.4.9 (2023-12-10), 6.4.10 (2023-12-23), 6.4.11 (2024-01-11), 6.4.12 (2024-02-10), 6.4.13 (2024-02-25)

##### New Features
* [#2261](https://github.com/oshi/oshi/pull/2261): Add getThreadId, getCurrentThread and getCurrentProcess to OperatingSystem API - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#2259](https://github.com/oshi/oshi/pull/2259): Cache AIX partition list to reduce disk reads from lspv - [@dbwiddis](https://github.com/dbwiddis).
* [#2260](https://github.com/oshi/oshi/pull/2260): Use regex to pre-filter to parseable CPU numbers for ARM Macs - [@dbwiddis](https://github.com/dbwiddis).
* [#2262](https://github.com/oshi/oshi/pull/2262): Consistent treatment of AIX tick lengths - [@dbwiddis](https://github.com/dbwiddis).
* [#2264](https://github.com/oshi/oshi/pull/2264): Don't assume ticks match logical processor count - [@dbwiddis](https://github.com/dbwiddis).
* [#2292](https://github.com/oshi/oshi/pull/2292): Update to JNA 5.13.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#2315](https://github.com/oshi/oshi/pull/2315),
  [#2318](https://github.com/oshi/oshi/pull/2318): Fix parsing generally and for FreeBSD cpu detection - [@decketron](https://github.com/decketron).
* [#2327](https://github.com/oshi/oshi/pull/2327): Improve Udev exception handling - [@dbwiddis](https://github.com/dbwiddis).
* [#2329](https://github.com/oshi/oshi/pull/2329): Allow using SLF4J 1 in OSGi containers - [@mshabarov](https://github.com/mshabarov).
* [#2361](https://github.com/oshi/oshi/pull/2361): Convert per-process CPU ticks on Apple Silicon to milliseconds - [@dbwiddis](https://github.com/dbwiddis).
* [#2362](https://github.com/oshi/oshi/pull/2362): Make use of Kstat2 configurable - [@dbwiddis](https://github.com/dbwiddis).
* [#2377](https://github.com/oshi/oshi/pull/2377): Graceful fallback for macOS Process user or group name - [@dbwiddis](https://github.com/dbwiddis).
* [#2393](https://github.com/oshi/oshi/pull/2393): Get threadId by syscall when gettid not available - [@silencewood](https://github.com/silencewood).
* [#2394](https://github.com/oshi/oshi/pull/2394): Fix bit shifting in CPUID calculation - [@dbwiddis](https://github.com/dbwiddis).
* [#2396](https://github.com/oshi/oshi/pull/2396): Add command-line fallbacks for udev and sysfs processor info - [@dbwiddis](https://github.com/dbwiddis).
* [#2407](https://github.com/oshi/oshi/pull/2407): Improve performance of Linux User and Group name queries - [@dbwiddis](https://github.com/dbwiddis).
* [#2421](https://github.com/oshi/oshi/pull/2421): Handle non-unique UUIDs in demo ComputerID class - [@dbwiddis](https://github.com/dbwiddis).
* [#2427](https://github.com/oshi/oshi/pull/2427): Lookup hardware implementer if lscpu fails to do so - [@dbwiddis](https://github.com/dbwiddis).
* [#2434](https://github.com/oshi/oshi/pull/2434): Fix Windows OS Process logic to use registry values - [@tzfun](https://github.com/tzfun).
* [#2443](https://github.com/oshi/oshi/pull/2443): Include IPConnections on macOS that listen on both IPv4 and IPv6 protocols - [@rieck0](https://github.com/rieck0).
* [#2446](https://github.com/oshi/oshi/pull/2446): Fix parsing Loongson CPU names - [@Glavo](https://github.com/Glavo).
* [#2460](https://github.com/oshi/oshi/pull/2460): Fix AIX tests for virtual/unused drives - [@dbwiddis](https://github.com/dbwiddis).
* [#2480](https://github.com/oshi/oshi/pull/2480): Use sysfs as a backup for Linux power supply without udev - [@dbwiddis](https://github.com/dbwiddis).
* [#2487](https://github.com/oshi/oshi/pull/2487): Improve performance of thread details query for a single process - [@dbwiddis](https://github.com/dbwiddis).
* [#2514](https://github.com/oshi/oshi/pull/2514): Fix NPE in ProcessorIdentifier edge case - [@dbwiddis](https://github.com/dbwiddis).
* [#2527](https://github.com/oshi/oshi/pull/2527): Remove unicode degree sign from output to improve portability - [@dbwiddis](https://github.com/dbwiddis).
* [#2533](https://github.com/oshi/oshi/pull/2533): Changed GPU info gathering mechanism on Windows - [@komelgman](https://github.com/komelgman).
* [#2436](https://github.com/oshi/oshi/pull/2436),
  [#2535](https://github.com/oshi/oshi/pull/2535): Fall back to vendor frequency on failed max on Windows - [@dbwiddis](https://github.com/dbwiddis).
* [#2538](https://github.com/oshi/oshi/pull/2538): JNA 5.14.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#2545](https://github.com/oshi/oshi/pull/2545): Improve calculations for Linux CPU max frequency - [@dbwiddis](https://github.com/dbwiddis).
* [#2548](https://github.com/oshi/oshi/pull/2548): Support Apple M3 chip - [@dbwiddis](https://github.com/dbwiddis).
* [#2549](https://github.com/oshi/oshi/pull/2549): Add newer Intel and AMD architectures - [@dbwiddis](https://github.com/dbwiddis).
* [#2559](https://github.com/oshi/oshi/pull/2559): Improve handling of missing JNA classes in LinuxOperatingSystem init - [@dbwiddis](https://github.com/dbwiddis).
* [#2562](https://github.com/oshi/oshi/pull/2562): Allow opting out of use of udev via LinuxOperatingSystem- [@chadlwilson](https://github.com/chadlwilson).
* [#2278](https://github.com/oshi/oshi/pull/2278): Use lscpu Model Name as backup for cpuName - [@13276965576](https://github.com/13276965576).
* [#2588](https://github.com/oshi/oshi/pull/2588): Fix parsing of strings to long arrays - [@cl728](https://github.com/cl728).

# 6.3.0 (2022-10-16), 6.3.1 (2022-10-30), 6.3.2 (2022-11-16)

##### New Features
* [#2129](https://github.com/oshi/oshi/pull/2129): Added JMX demo project - [@SalvadorRomo](https://github.com/SalvadorRomo).
* [#2197](https://github.com/oshi/oshi/pull/2197): Added support for Android OS - [@milan-fabian](https://github.com/milan-fabian).
* [#2198](https://github.com/oshi/oshi/pull/2198): Added Processor Cache Information - [@dbwiddis](https://github.com/dbwiddis).
* [#2218](https://github.com/oshi/oshi/pull/2218): Added system-wide per-process open file descriptor limits - [@gitseti](https://github.com/gitseti)
* [#2225](https://github.com/oshi/oshi/pull/2225): Added process specific open file descriptor limits - [@gitseti](https://github.com/gitseti)

##### Bug fixes / Improvements
* [#2179](https://github.com/oshi/oshi/pull/2179): Update JUnit EnabledOnOS for OpenBSD and FreeBSD - [@dbwiddis](https://github.com/dbwiddis).
* [#2180](https://github.com/oshi/oshi/pull/2180): Suppress log warnings for common non-root procfs failures - [@dbwiddis](https://github.com/dbwiddis).
* [#2181](https://github.com/oshi/oshi/pull/2181): Better handling of ARM CPU Names - [@dbwiddis](https://github.com/dbwiddis).
* [#2204](https://github.com/oshi/oshi/pull/2204): Improve performance using parallel streams for processes and threads - [@adrian-kong](https://github.com/adrian-kong).
* [#2212](https://github.com/oshi/oshi/pull/2212): Suppress log warnings for common non-root macOS sysctl failures - [@pavangole](https://github.com/pavangole).
* [#2224](https://github.com/oshi/oshi/pull/2224): Detect Windows Server 2022 in older JDKs - [@dbwiddis](https://github.com/dbwiddis).
* [#2229](https://github.com/oshi/oshi/pull/2229): Fix division by zero on AIX with fewer logical processors than physical processors - [@dbwiddis](https://github.com/dbwiddis).
* [#2243](https://github.com/oshi/oshi/pull/224e): Actually return Windows IP Connections - [@dbwiddis](https://github.com/dbwiddis).

# 6.2.0 (2022-06-26), 6.2.1 (2022-06-29), 6.2.2 (2022-07-20)

##### Performance improvement
This release leverages improvements in JNA 5.12.1 which should significantly improve performance. Finalizers in JNA were replaced by Cleaners, reducing the impact of `Memory` objects in tenured heap space by promptly releasing native memory allocations.

In addition, JNA's `Memory` class now implements `Closeable`. All direct and most indirect allocations of `Memory` in OSHI now have their underlying native allocation freed proactively.
* [#2075](https://github.com/oshi/oshi/pull/2075): Reduce heap thrash with HKEY_PERFORMANCE_DATA buffer - [@dbwiddis](https://github.com/dbwiddis).
* [#2080](https://github.com/oshi/oshi/pull/2080): JNA 5.12.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#2081](https://github.com/oshi/oshi/pull/2081): Proactively free direct native Memory allocations - [@dbwiddis](https://github.com/dbwiddis).
* [#2082](https://github.com/oshi/oshi/pull/2082),
  [#2083](https://github.com/oshi/oshi/pull/2083),
  [#2085](https://github.com/oshi/oshi/pull/2085),
  [#2090](https://github.com/oshi/oshi/pull/2090),
  [#2091](https://github.com/oshi/oshi/pull/2091): Proactively free indirect native Memory allocations - [@dbwiddis](https://github.com/dbwiddis).
* [#2094](https://github.com/oshi/oshi/pull/2094): JNA 5.12.1 - [@dbwiddis](https://github.com/dbwiddis).

##### New Features
* [#2046](https://github.com/oshi/oshi/pull/2046): Added getSystemCpuLoad/getProcessorCpuLoad convenience methods - [@Osiris-Team](https://github.com/Osiris-Team).
* [#2050](https://github.com/oshi/oshi/pull/2050): Implement optional Windows Load Average - [@dbwiddis](https://github.com/dbwiddis).
* [#2118](https://github.com/oshi/oshi/pull/2118): Support Apple M2 chip - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#2016](https://github.com/oshi/oshi/pull/2016): Make disabled counter check robust to invalid registry types - [@dbwiddis](https://github.com/dbwiddis).
* [#2033](https://github.com/oshi/oshi/pull/2033): Graceful fallback for CPU Topology without udev - [@dbwiddis](https://github.com/dbwiddis).
* [#2034](https://github.com/oshi/oshi/pull/2034): Fallback or log warning to avoid exception with no udev - [@dbwiddis](https://github.com/dbwiddis).
* [#2039](https://github.com/oshi/oshi/pull/2039): Include PID 0 on macOS - [@dbwiddis](https://github.com/dbwiddis).
* [#2054](https://github.com/oshi/oshi/pull/2054): Prevent NPE when network interface has no statistics - [@dbwiddis](https://github.com/dbwiddis).
* [#2055](https://github.com/oshi/oshi/pull/2055): Fix incomplete collection of child processes - [@marcelkliemannel](https://github.com/marcelkliemannel).
* [#2077](https://github.com/oshi/oshi/pull/2077): Fix processor numbering with Windows Processor Groups - [@dbwiddis](https://github.com/dbwiddis).
* [#2078](https://github.com/oshi/oshi/pull/2078): Support macOS 13 (Ventura) - [@dbwiddis](https://github.com/dbwiddis).
* [#2089](https://github.com/oshi/oshi/pull/2089): PDH wild card counters need English objects but localized instances - [@dbwiddis](https://github.com/dbwiddis).
* [#2097](https://github.com/oshi/oshi/pull/2108): Prefer character classes to alternators in regex - [@varun83388](https://github.com/varun83388).
* [#2095](https://github.com/oshi/oshi/pull/2095): Avoid using reserved identifiers as variable names - [@muhammetgumus](https://github.com/muhammetgumus)
* [#2099](https://github.com/oshi/oshi/pull/2099): Remove useless public constructor for abstract class - [@victorjbassey](https://github.com/victorjbassey).
* [#2124](https://github.com/oshi/oshi/pull/2124): Properly determine Apple Silicon frequency - [@dbwiddis](https://github.com/dbwiddis).
* [#2133](https://github.com/oshi/oshi/pull/2133): Fix NPE for null canonical host name - [@dbwiddis](https://github.com/dbwiddis).

# 6.1.0 (2022-01-20), 6.1.1 (2022-02-13), 6.1.2 (2022-02-14), 6.1.3 (2022-02-22), 6.1.4 (2022-03-01), 6.1.5 (2022-03-15), 6.1.6 (2022-04-10)

##### New Features
* [#1851](https://github.com/oshi/oshi/pull/1851),
  [#1858](https://github.com/oshi/oshi/pull/1858): Add PhysicalProcessor class to expose hybrid processor topology - [@dbwiddis](https://github.com/dbwiddis).
* [#1886](https://github.com/oshi/oshi/pull/1886): Implement Processor Utility to optionally match Windows Task Manager CPU usage - [@dbwiddis](https://github.com/dbwiddis).
* [#1974](https://github.com/oshi/oshi/pull/1974): Enable suppression of disabled perfmon counter warnings - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1831](https://github.com/oshi/oshi/pull/1831): Improve Solaris and AIX process listing using procfs - [@dbwiddis](https://github.com/dbwiddis).
* [#1836](https://github.com/oshi/oshi/pull/1836): Remove all lsof calls and replace with procfs equivalent - [@dbwiddis](https://github.com/dbwiddis).
* [#1837](https://github.com/oshi/oshi/pull/1837): Implement Kstat2 for Solaris 11.4+ - [@dbwiddis](https://github.com/dbwiddis).
* [#1844](https://github.com/oshi/oshi/pull/1844): Update Microarchitecture table - [@dbwiddis](https://github.com/dbwiddis).
* [#1849](https://github.com/oshi/oshi/pull/1849): Use udev for Linux cpu enumeration and frequency - [@dbwiddis](https://github.com/dbwiddis).
* [#1859](https://github.com/oshi/oshi/pull/1859): Fix battery power usage rate on Fedora/RHEL - [@dbwiddis](https://github.com/dbwiddis).
* [#1869](https://github.com/oshi/oshi/pull/1869): Ignore mount headers on AIX filesystem - [@dbwiddis](https://github.com/dbwiddis).
* [#1889](https://github.com/oshi/oshi/pull/1889): Improved calculation of AIX disk reads and writes - [@siddhantdixit](https://github.com/siddhantdixit).
* [#1898](https://github.com/oshi/oshi/pull/1898): Fix Solaris Utmpx structure mapping - [@dbwiddis](https://github.com/dbwiddis).
* [#1909](https://github.com/oshi/oshi/pull/1909): Move configuration string constants to GlobalConfig - [@dbwiddis](https://github.com/dbwiddis).
* [#1933](https://github.com/oshi/oshi/pull/1933): Remove malformed DOCTYPE tags in 6.1.1 release POM files - [@dbwiddis](https://github.com/dbwiddis).
* [#1937](https://github.com/oshi/oshi/pull/1937),
  [#1939](https://github.com/oshi/oshi/pull/1939): Make Processor Utility calculations robust to edge cases - [@dbwiddis](https://github.com/dbwiddis).
* [#1944](https://github.com/oshi/oshi/pull/1944): Get page size, hz, and feature bits from Linux aux vector - [@dbwiddis](https://github.com/dbwiddis).
* [#1945](https://github.com/oshi/oshi/pull/1945): Refactor all binary file reading to use ByteBuffers - [@dbwiddis](https://github.com/dbwiddis).
* [#1949](https://github.com/oshi/oshi/pull/1949): Refine Processor Utility calculations for more precision - [@dbwiddis](https://github.com/dbwiddis).
* [#1950](https://github.com/oshi/oshi/pull/1950): Handle Processor Utility 32-bit counter rollover - [@dbwiddis](https://github.com/dbwiddis).
* [#1960](https://github.com/oshi/oshi/pull/1960),
  [#1962](https://github.com/oshi/oshi/pull/1962): Improve kstat chain locking - [@dbwiddis](https://github.com/dbwiddis).
* [#1966](https://github.com/oshi/oshi/pull/1966): Determine kstat2 branch by attempting to load library - [@dbwiddis](https://github.com/dbwiddis).
* [#1971](https://github.com/oshi/oshi/pull/1971): Show performance and efficiency core total on CPU toString - [@dbwiddis](https://github.com/dbwiddis).
* [#1988](https://github.com/oshi/oshi/pull/1988): Update to JNA 5.11.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#2007](https://github.com/oshi/oshi/pull/2007): Pick one property file to load even with duplicates - [@dbwiddis](https://github.com/dbwiddis).

##### Artifact Removal
* [#1868](https://github.com/oshi/oshi/pull/1868): Remove shaded jar artifact - [@dbwiddis](https://github.com/dbwiddis).

# 6.0.0 (2021-12-31)

##### Breaking Changes
* [#1724](https://github.com/oshi/oshi/pull/1724): Removed deprecated MACOSX value from PlatformEnum and SystemInfo and removed the getCurrentPlatformEnum() method - [@Novaenn](https://github.com/Novaenn).
* [#1725](https://github.com/oshi/oshi/pull/1725): Removed deprecated process sorting methods from the OperatingSystem class - [@varnaa](https://github.com/varnaa).
* [#1729](https://github.com/oshi/oshi/pull/1729): Changed the return value of LinuxOSPRocess and MacOSProcess method getCommandLine() from null-delimited string to space-delimited string - [@prathamgandhi](https://github.com/prathamgandhi).
* [#1730](https://github.com/oshi/oshi/pull/1730): Changed the return value of getServices() from array to list in OperatingSystem - [@adrian-kong](https://github.com/adrian-kong).
* [#1736](https://github.com/oshi/oshi/pull/1736): Changed the return type of the NetworkInterface method getMTU() from int to long in all its OS implementations.  - [@Simba-97](https://github.com/Simba-97).

# 5.8.0 (2021-07-18), 5.8.1 (2021-08-22), 5.8.2 (2021-09-05), 5.8.3 (2021-10-21), 5.8.5 (2021-11-24), 5.8.6 (2021-12-14), 5.8.7 (2021-12-31)

##### New Features
* [#1654](https://github.com/oshi/oshi/pull/1654): API for process arguments and environment - [@basil](https://github.com/basil) and [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1673](https://github.com/oshi/oshi/pull/1673): Fix FreeBSD ps command arguments for context switches - [@basil](https://github.com/basil).
* [#1675](https://github.com/oshi/oshi/pull/1675): Replace ps argument lists with enum - [@dbwiddis](https://github.com/dbwiddis).
* [#1678](https://github.com/oshi/oshi/pull/1678): Refactor to fix leaking udev reference in LinuxUsbDevice - [@mattmacleod](https://github.com/mattmacleod).
* [#1680](https://github.com/oshi/oshi/pull/1680): Move supported operating system check out of SystemInfo constructor - [@KyongSik-Yoon](https://github.com/KyongSik-Yoon).
* [#1701](https://github.com/oshi/oshi/pull/1701): Update to JNA 5.9.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#1707](https://github.com/oshi/oshi/pull/1707): Display Windows 11 version for insider builds - [@dbwiddis](https://github.com/dbwiddis).
* [#1711](https://github.com/oshi/oshi/pull/1711),
  [#1749](https://github.com/oshi/oshi/pull/1749): Fix WMI backup table source for process counters - [@dbwiddis](https://github.com/dbwiddis).
* [#1712](https://github.com/oshi/oshi/pull/1712): Align PlatformEnum to JNA Platform type - [@dbwiddis](https://github.com/dbwiddis).
* [#1768](https://github.com/oshi/oshi/pull/1768): Fixed incorrect use of reference equality - [@mythili-rajaraman](https://github.com/mythili-rajaraman).
* [#1792](https://github.com/oshi/oshi/pull/1792): Fix fd leaks in Solaris after Runtime.exec calls - [@shvo123](https://github.com/shvo123).
* [#1796](https://github.com/oshi/oshi/pull/1796): Ban the use of Junit 4 and associated Hamcrest Core - [@mprins](https://github.com/mprins)
* [#1803](https://github.com/oshi/oshi/pull/1803): Configure checkstyle, remove code-assert - [@dbwiddis](https://github.com/dbwiddis).
* [#1808](https://github.com/oshi/oshi/pull/1808): Restrict imports with maven enforcer - [@dbwiddis](https://github.com/dbwiddis).
* [#1812](https://github.com/oshi/oshi/pull/1812): Add tests for all WMI drivers and fix failures - [@dbwiddis](https://github.com/dbwiddis).
* [#1822](https://github.com/oshi/oshi/pull/1822): Fix handle leaks in Windows after Runtime.exec calls - [@shvo123](https://github.com/shvo123).

# 5.7.0 (2021-04-01), 5.7.1 (2021-04-15), 5.7.2 (2021-05-01), 5.7.3 (2021-05-16), 5.7.4 (2021-05-30), 5.7.5 (2021-06-12)

##### New Features
* [#1584](https://github.com/oshi/oshi/pull/1584): Add logical volume group information - [@tausiflife](https://github.com/tausiflife).
* [#1587](https://github.com/oshi/oshi/pull/1587): Add context switches to OSProcess - [@dbwiddis](https://github.com/dbwiddis).
* [#1592](https://github.com/oshi/oshi/pull/1592): Add suspended process and thread state for Windows - [@dbwiddis](https://github.com/dbwiddis).
* [#1626](https://github.com/oshi/oshi/pull/1626): Add getIndex() in NetworkIF and demo of Interfaces - [@angelyouyou](https://github.com/angelyouyou).

##### Bug fixes / Improvements
* [#1585](https://github.com/oshi/oshi/pull/1585): macOS doesn't provide system level context switches or interrupts - [@dbwiddis](https://github.com/dbwiddis).
* [#1596](https://github.com/oshi/oshi/pull/1596): Single COM initialization for groups of queries - [@dbwiddis](https://github.com/dbwiddis).
* [#1603](https://github.com/oshi/oshi/pull/1603): Improve performance of Windows USB device tree parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#1605](https://github.com/oshi/oshi/pull/1605): Cache localized perf counter object strings - [@dbwiddis](https://github.com/dbwiddis).
* [#1608](https://github.com/oshi/oshi/pull/1608): LinuxOSProcess#getOpenFiles returns one more than expected - [@slaha](https://github.com/slaha).
* [#1610](https://github.com/oshi/oshi/pull/1610): Remove redundant check for isLocalInterface - [@barddoo](https://github.com/barddoo).
* [#1614](https://github.com/oshi/oshi/pull/1614): Simplify Windows version parsing using JDK properties - [@dbwiddis](https://github.com/dbwiddis).
* [#1620](https://github.com/oshi/oshi/pull/1620): Reduced log level to debug for fallback class loading - [@UnusualFrog](https://github.com/UnusualFrog).
* [#1628](https://github.com/oshi/oshi/pull/1628): Null check volume name when iterating Mac File Stores - [@dbwiddis](https://github.com/dbwiddis).
* [#1631](https://github.com/oshi/oshi/pull/1631): Null check all CFStrings to prevent exceptions - [@dbwiddis](https://github.com/dbwiddis).
* [#1649](https://github.com/oshi/oshi/pull/1649): Fix macOS and unix sysctl mappings for size_t - [@dbwiddis](https://github.com/dbwiddis).
* [#1657](https://github.com/oshi/oshi/pull/1657): macOS 12 Monterey - [@dbwiddis](https://github.com/dbwiddis).
* [#1662](https://github.com/oshi/oshi/pull/1662): PDH queries shouldn't be localized on Vista+ - [@dbwiddis](https://github.com/dbwiddis).
* [#1664](https://github.com/oshi/oshi/pull/1664): Fix PDH failed query thread safety - [@dbwiddis](https://github.com/dbwiddis).
* [#1665](https://github.com/oshi/oshi/pull/1665): (Java11 branch) JNA needs reflective access to Windows structure mappings - [@vatbub](https://github.com/vatbub).

# 5.6.0 (2021-03-01), 5.6.1 (2021-03-22)

##### New Features
* [#1541](https://github.com/oshi/oshi/pull/1541): Expose the alias of a network interface (Windows and Linux) - [@dornand](https://github.com/dornand).
* [#1546](https://github.com/oshi/oshi/pull/1546): Expose network interface operational status (Windows and Linux) - [@dornand](https://github.com/dornand).
* [#1548](https://github.com/oshi/oshi/pull/1548): Add getter for descendants of a process - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1552](https://github.com/oshi/oshi/pull/1552): Handle exceptions querying Windows user info - [@dbwiddis](https://github.com/dbwiddis).
* [#1562](https://github.com/oshi/oshi/pull/1562): Fix missing space in WMI process query - [@dbwiddis](https://github.com/dbwiddis).
* [#1566](https://github.com/oshi/oshi/pull/1566): Handle new WinAPI Logical Processor Information types - [@dbwiddis](https://github.com/dbwiddis).
* [#1567](https://github.com/oshi/oshi/pull/1567): Handle empty process performance registry query - [@dbwiddis](https://github.com/dbwiddis).
* [#1569](https://github.com/oshi/oshi/pull/1569): Fix udev reference leak in LinuxNetworkIF - [@dbwiddis](https://github.com/dbwiddis).
* [#1576](https://github.com/oshi/oshi/pull/1576): JNA 5.8.0 - [@dbwiddis](https://github.com/dbwiddis).

# 5.5.0 (2021-02-08), 5.5.1 (2021-02-21)

##### New Features
* New `oshi-core-java11` artifact (in beta) intended for modular projects.
* [#1526](https://github.com/oshi/oshi/pull/1526): Make process filtering and sorting more flexible - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1509](https://github.com/oshi/oshi/pull/1509): Directly read M1 CPU IOPlatformDevice registry entries - [@dbwiddis](https://github.com/dbwiddis).
* [#1523](https://github.com/oshi/oshi/pull/1523): Fix Windows partition GUID retrieval - [@dbwiddis](https://github.com/dbwiddis).
* [#1524](https://github.com/oshi/oshi/pull/1524): Fix Windows USB serial number retrieval - [@ymortier](https://github.com/ymortier).
* [#1529](https://github.com/oshi/oshi/pull/1529): JNA 5.7.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#1535](https://github.com/oshi/oshi/pull/1535): Fix NetworkIF.isConnectorPresent() on Windows - [@dornand](https://github.com/dornand).
* [#1536](https://github.com/oshi/oshi/pull/1536): Fix Windows version parsing regression - [@dbwiddis](https://github.com/dbwiddis).

# 5.4.0 (2021-01-18), 5.4.1 (2021-01-24)

##### New Features
* [#1461](https://github.com/oshi/oshi/pull/1461): List TCP and UDP connections - [@dbwiddis](https://github.com/dbwiddis).
* [#1466](https://github.com/oshi/oshi/pull/1466): OpenBSD port - [@mprins](https://github.com/mprins), [@dbwiddis](https://github.com/dbwiddis).
* [#1473](https://github.com/oshi/oshi/pull/1473): List Desktop Windows - [@dbwiddis](https://github.com/dbwiddis).
* [#1476](https://github.com/oshi/oshi/pull/1476): Include ComputerSystem Hardware UUID - [@dbwiddis](https://github.com/dbwiddis).
* [#1478](https://github.com/oshi/oshi/pull/1478): Added configuration properties to exclude/include filestores - [@majster-kat](https://github.com/majster-kat).

##### Bug fixes / Improvements
* [#1464](https://github.com/oshi/oshi/pull/1464): Also get disk statistics from AppleAPFSContainerScheme - [@mpfz0r](https://github.com/mpfz0r).
* [#1485](https://github.com/oshi/oshi/pull/1485),
  [#1493](https://github.com/oshi/oshi/pull/1493): Parse Apple M1 Processor ID info - [@dbwiddis](https://github.com/dbwiddis).
* [#1488](https://github.com/oshi/oshi/pull/1488),
  [#1489](https://github.com/oshi/oshi/pull/1489): Use IOUSB plane to iterate/recurse Mac USB tree - [@dbwiddis](https://github.com/dbwiddis).
* [#1490](https://github.com/oshi/oshi/pull/1490): Apple M1 Baseboard and Firmware backups - [@dbwiddis](https://github.com/dbwiddis).
* [#1494](https://github.com/oshi/oshi/pull/1494): Deprecate MACOSX platform enum - [@dbwiddis](https://github.com/dbwiddis).
* [#1495](https://github.com/oshi/oshi/pull/1495): Report Linux filesystem label - [@dbwiddis](https://github.com/dbwiddis).
* [#1497](https://github.com/oshi/oshi/pull/1497): Don't unnecessarily make lists unmodifiable - [@dbwiddis](https://github.com/dbwiddis).
* [#1498](https://github.com/oshi/oshi/pull/1498): Identify Rosetta as virtual architecture - [@dbwiddis](https://github.com/dbwiddis).
* [#1501](https://github.com/oshi/oshi/pull/1501): Get bare metal Apple M1 CPU info from IODeviceTree - [@dbwiddis](https://github.com/dbwiddis).
* [#1502](https://github.com/oshi/oshi/pull/1502): Fix bitness mismatch on 32-bit OpenBSD - [@dbwiddis](https://github.com/dbwiddis).
* [#1505](https://github.com/oshi/oshi/pull/1505): Fix windows disk transfer time (yet again) - [@dbwiddis](https://github.com/dbwiddis).


# 5.3.0 (2020-10-11), 5.3.1 (2020-10-18), 5.3.2 (2020-10-25), 5.3.3 (2020-10-28), 5.3.4 (2020-11-01), 5.3.5 (2020-11-11), 5.3.6 (2020-11-15), 5.3.7 (2020-12-20)

##### New Features
* [#1350](https://github.com/oshi/oshi/pull/1350): Optionally list loopback and virtual network interfaces - [@zalintyre](https://github.com/zalintyre).
* [#1359](https://github.com/oshi/oshi/pull/1359), [#1379](https://github.com/oshi/oshi/pull/1379): Set suppressed network filesystem types and pseudo filesystem types via config - [@J-Jimmy](https://github.com/J-Jimmy), [@mprins](https://github.com/mprins).
* [#1387](https://github.com/oshi/oshi/pull/1387): Switch tests to JUnit5 and Hamcrest matchers - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1353](https://github.com/oshi/oshi/pull/1353): runNative locale issues on linux - [@dmitraver](https://github.com/dmitraver)
* [#1355](https://github.com/oshi/oshi/pull/1355): Allow variable size structs for macOS IP stat sysctl calls - [@dbwiddis](https://github.com/dbwiddis).
* [#1362](https://github.com/oshi/oshi/pull/1362): Correct invalid Windows processor bitmask logic on 64th core - [@J-Jimmy](https://github.com/J-Jimmy).
* [#1363](https://github.com/oshi/oshi/pull/1363), [#1374](https://github.com/oshi/oshi/pull/1374): Index logical processors by NUMA node - [@dbwiddis](https://github.com/dbwiddis).
* [#1368](https://github.com/oshi/oshi/pull/1368): Backup for getting process name and state - [@J-Jimmy](https://github.com/J-Jimmy).
* [#1375](https://github.com/oshi/oshi/pull/1375): Reduce log level for expected Windows permission failures - [@dbwiddis](https://github.com/dbwiddis).
* [#1380](https://github.com/oshi/oshi/pull/1389): Fix exception sorting child process list on Windows - [@agaponik](https://github.com/agaponik).
* [#1382](https://github.com/oshi/oshi/pull/1382): Fix exception on 32-bit Windows thread stats - [@dbwiddis](https://github.com/dbwiddis).
* [#1388](https://github.com/oshi/oshi/pull/1388): Fix service listing with systemd 245 and newer - [@Szwendacz99](https://github.com/Szwendacz99).
* [#1389](https://github.com/oshi/oshi/pull/1389): Vendor frequency from non-Intel chips - [@dbwiddis](https://github.com/dbwiddis).
* [#1399](https://github.com/oshi/oshi/pull/1399): Fix redundant multiplication in Linux max Freq - [@dbwiddis](https://github.com/dbwiddis).
* [#1400](https://github.com/oshi/oshi/pull/1400): Make Windows System CPU usage Process-group aware - [@dbwiddis](https://github.com/dbwiddis).
* [#1402](https://github.com/oshi/oshi/pull/1402): Don't use localized environment for xrandr - [@dbwiddis](https://github.com/dbwiddis).
* [#1409](https://github.com/oshi/oshi/pull/1409): Parse processor name on Orange Pi - [@dbwiddis](https://github.com/dbwiddis).
* [#1410](https://github.com/oshi/oshi/pull/1410): Adapt to macOS 11 version reverse compatibility - [@dbwiddis](https://github.com/dbwiddis).
* [#1411](https://github.com/oshi/oshi/pull/1411): Add mapping of device and volume to be used for getting uuid - [@tausiflife](https://github.com/tausiflife).
* [#1413](https://github.com/oshi/oshi/pull/1413): Handle macOS unsigned byte ifTypes - [@dbwiddis](https://github.com/dbwiddis).
* [#1419](https://github.com/oshi/oshi/pull/1419): WMI performance improvement demo classes - [@dbwiddis](https://github.com/dbwiddis).
* [#1432](https://github.com/oshi/oshi/pull/1432): Properly round Linux current frequency - [@dbwiddis](https://github.com/dbwiddis).
* [#1434](https://github.com/oshi/oshi/pull/1434): Document unsigned int MTU and update toString - [@dbwiddis](https://github.com/dbwiddis).
* [#1440](https://github.com/oshi/oshi/pull/1440): Add ps backup for command line for macOS Big Sur compatibility - [@dbwiddis](https://github.com/dbwiddis).
* [#1442](https://github.com/oshi/oshi/pull/1442), [#1443](https://github.com/oshi/oshi/pull/1443): FreeBSD CI; fix FreeBSD Test Failures - [@dbwiddis](https://github.com/dbwiddis).
* [#1455](https://github.com/oshi/oshi/pull/1455): Fix hanging prstat call on Solaris thread details - [@dbwiddis](https://github.com/dbwiddis).
* [#1457](https://github.com/oshi/oshi/pull/1457): Fix macOS codename with new versioning scheme - [@dbwiddis](https://github.com/dbwiddis).
* [#1460](https://github.com/oshi/oshi/pull/1460): Fetch thread names on Linux - [@dbwiddis](https://github.com/dbwiddis).

# 4.9.1 / 5.2.1 (2020-07-14), 4.9.2 / 5.2.2 (2020-07-20), 4.9.3 / 5.2.3 (2020-08-09), 4.9.4 / 5.2.4 (2020-08-16), 4.9.5 / 5.2.5 (2020-08-30)

##### New Features
* [#1282](https://github.com/oshi/oshi/pull/1282): (5.x) AIX Port - [@tausiflife](https://github.com/tausiflife), [@dbwiddis](https://github.com/dbwiddis).
* [#1290](https://github.com/oshi/oshi/pull/1290): Demo class matching filestore to partition - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1285](https://github.com/oshi/oshi/pull/1285): Fallback to read properties file from classloader of the class - [@ngyukman](https://github.com/ngyukman).
* [#1298](https://github.com/oshi/oshi/pull/1298): Use lshw as backup for max CPU frequency - [@dbwiddis](https://github.com/dbwiddis), [@Szwendacz99](https://github.com/Szwendacz99)
* [#1299](https://github.com/oshi/oshi/pull/1299): JNA 5.6.0 / macOS 11 Compatibility - [@dbwiddis](https://github.com/dbwiddis)
* [#1302](https://github.com/oshi/oshi/pull/1302): More accurate process start times - [@dbwiddis](https://github.com/dbwiddis).
* [#1307](https://github.com/oshi/oshi/pull/1307): Correctly fetch logical partitions on Windows - [@AnakinHou](https://github.com/AnakinHou).
* [#1310](https://github.com/oshi/oshi/pull/1310): Use fragment size in Linux FileStore size calculation - [@dbwiddis](https://github.com/dbwiddis).
* [#1316](https://github.com/oshi/oshi/pull/1316): Fix ARM architecture parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#1320](https://github.com/oshi/oshi/pull/1320): Avoid NPE for failed Windows registry counter size - [@dbwiddis](https://github.com/dbwiddis).
* [#1327](https://github.com/oshi/oshi/pull/1327): Fix Raspberry Pi sensor parsing - [@dbwiddis](https://github.com/dbwiddis).

# 4.9.0 / 5.2.0 (2020-06-25)

##### New Features
* [#1247](https://github.com/oshi/oshi/pull/1247): Add Automatic-Module-Name to support JPMS - [@dbwiddis](https://github.com/dbwiddis).
* [#1258](https://github.com/oshi/oshi/pull/1258): (5.x) Add threads details of a process - [@tausiflife](https://github.com/tausiflife).
* [#1262](https://github.com/oshi/oshi/pull/1262): Read macOS versions from properties file - [@hkbiet](https://github.com/hkbiet).
* [#1270](https://github.com/oshi/oshi/pull/1270): (5.x) Add page fault info to OSProcess - [@tausiflife](https://github.com/tausiflife).

##### Bug fixes / Improvements
* [#1266](https://github.com/oshi/oshi/pull/1266): Suppress repeated attempts to query failed PDH - [@dbwiddis](https://github.com/dbwiddis).
* [#1267](https://github.com/oshi/oshi/pull/1267): Check proc_pidinfo return value before incrementing numberOfThreads - [@markkulube](https://github.com/markkulube).

# 4.8.0 / 5.1.0 (2020-05-20), 4.8.1 / 5.1.1 (2020-05-30), 4.8.2 / 5.1.2 (2020-06-07)

##### New Features
* [#1240](https://github.com/oshi/oshi/pull/1240): Add a driver for proc/pid/statm - [@dbwiddis](https://github.com/dbwiddis).
* [#1241](https://github.com/oshi/oshi/pull/1241): (5.x) Add code-assert - [@dbwiddis](https://github.com/dbwiddis).
* [#1231](https://github.com/oshi/oshi/pull/1231): Add OSSessions. - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1246](https://github.com/oshi/oshi/pull/1246): Configure data source for OperatingSystem#getSessions - [@dbwiddis](https://github.com/dbwiddis).
* [#1252](https://github.com/oshi/oshi/pull/1252): Fallback to command line if getSessions sanity check fails - [@dbwiddis](https://github.com/dbwiddis).
* [#1256](https://github.com/oshi/oshi/pull/1256): Fix calculation of Linux process start time - [@dbwiddis](https://github.com/dbwiddis).
* [#1237](https://github.com/oshi/oshi/pull/1237): Update Udev to object oriented style - [@dbwiddis](https://github.com/dbwiddis).
* [#1245](https://github.com/oshi/oshi/pull/1245): Refactor PerfCounterQuery classes and fix memory leak - [@dbwiddis](https://github.com/dbwiddis).
* [#1229](https://github.com/oshi/oshi/pull/1229): Changed the linux and solaris virtual memory swapins/outs to count just swaps - [@roeezz](https://github.com/roeezz)

# 5.0.0 (2020-05-05), 5.0.1 (2020-05-06), 5.0.2 (2020-05-14)

##### New Features
* [#1177](https://github.com/oshi/oshi/pull/1177): Remove deprecated code. - [@dbwiddis](https://github.com/dbwiddis).
* [#1178](https://github.com/oshi/oshi/pull/1178): Make NetworkIF an interface. - [@dbwiddis](https://github.com/dbwiddis).
* [#1181](https://github.com/oshi/oshi/pull/1181): Make HWPartition immutable. - [@dbwiddis](https://github.com/dbwiddis).
* [#1185](https://github.com/oshi/oshi/pull/1185): Make HWDiskStore an interface. - [@dbwiddis](https://github.com/dbwiddis).
* [#1186](https://github.com/oshi/oshi/pull/1186): List returns for Display, PowerSource, SoundCard, GraphicsCard. - [@dbwiddis](https://github.com/dbwiddis).
* [#1187](https://github.com/oshi/oshi/pull/1187): List returns for UsbDevice. - [@dbwiddis](https://github.com/dbwiddis).
* [#1189](https://github.com/oshi/oshi/pull/1189): List returns for CentralProcessor, GlobalMemory. - [@dbwiddis](https://github.com/dbwiddis).
* [#1190](https://github.com/oshi/oshi/pull/1190): Make OSFileStore an interface. - [@dbwiddis](https://github.com/dbwiddis).
* [#1191](https://github.com/oshi/oshi/pull/1191): Make OSProcess an interface. - [@dbwiddis](https://github.com/dbwiddis).
* [#1194](https://github.com/oshi/oshi/pull/1194): Optionally batch WMI Command Line queries. - [@dbwiddis](https://github.com/dbwiddis).

# 4.7.0 (2020-04-25), 4.7.1 (2020-05-02), 4.7.2 (2020-05-06), 4.7.3 (2020-05-14)

##### New Features
* [#1174](https://github.com/oshi/oshi/pull/1174): Add TCP and UDP statistics. - [@dbwiddis](https://github.com/dbwiddis).
* [#1183](https://github.com/oshi/oshi/pull/1183): Add more VirtualMemory information. - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1219](https://github.com/oshi/oshi/pull/1219): Only get primary group on WindowsOSProcess. - [@dbwiddis](https://github.com/dbwiddis).

# 4.6.0 (2020-04-02), 4.6.1 (2020-04-08)

##### New Features
* [#894](https://github.com/oshi/oshi/pull/894): Look up microarchitecture from processor identifier. - [@tbradellis](https://github.com/tbradellis).
* [#1150](https://github.com/oshi/oshi/pull/1150): Add fields to NetworkIF to help determine physical interfaces. - [@dbwiddis](https://github.com/dbwiddis).
* [#1151](https://github.com/oshi/oshi/pull/1151): Add Graphics Card information. - [@dbwiddis](https://github.com/dbwiddis).
* [#1157](https://github.com/oshi/oshi/pull/1157): Audit and annotate ThreadSafe classes. - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1155](https://github.com/oshi/oshi/pull/1155): Linux proc symlinks may show as (deleted). - [@jlangst6](https://github.com/jlangst6).

# 4.5.0 (2020-03-12), 4.5.2 (2020-03-20)

##### New Features
* [#1123](https://github.com/oshi/oshi/pull/1123): Add driver to parse Linux proc/diskstats. - [@dbwiddis](https://github.com/dbwiddis).
* [#1124](https://github.com/oshi/oshi/pull/1124): Add driver to parse Linux proc/pid/stat. - [@dbwiddis](https://github.com/dbwiddis).
* [#1125](https://github.com/oshi/oshi/pull/1125): Add driver to parse Linux proc/stat and proc/uptime. - [@dbwiddis](https://github.com/dbwiddis).
* [#1127](https://github.com/oshi/oshi/pull/1127): Add Volume Label to OSFileStore. - [@dbwiddis](https://github.com/dbwiddis).
* [#1140](https://github.com/oshi/oshi/pull/1140): Demo Swing GUI. - [@dbwiddis](https://github.com/dbwiddis).
* [#1143](https://github.com/oshi/oshi/pull/1140): Add process CPU usage between ticks calculation. - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1139](https://github.com/oshi/oshi/pull/1139): Fix Windows FileStore updating. - [@dbwiddis](https://github.com/dbwiddis).

# 4.4.0 (2020-02-12), 4.4.1 (2020-02-17), 4.4.2 (2020-02-20)

##### New Features
* [#1098](https://github.com/oshi/oshi/pull/1098): Option to limit FileStore list to local file systems. - [@Space2Man](https://github.com/Space2Man).
* [#1100](https://github.com/oshi/oshi/pull/1100): Get FileStore options. - [@dbwiddis](https://github.com/dbwiddis).
* [#1101](https://github.com/oshi/oshi/pull/1101): Add network interface dropped packets and collisions. - [@dbwiddis](https://github.com/dbwiddis).
* [#1105](https://github.com/oshi/oshi/pull/1105): Added additional pseudo filesystems. - [@Space2Man](https://github.com/Space2Man).

# 4.3.0 (2020-01-02), 4.3.1 (2020-02-05)

##### New Features
* [#1057](https://github.com/oshi/oshi/pull/1057): Added Subnet Mask & Prefix Length to NetworkIF. - [@vesyrak](https://github.com/Vesyrak).
* [#1095](https://github.com/oshi/oshi/pull/1095): Vend JSON via HTTP Server (oshi-demo). - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1060](https://github.com/oshi/oshi/pull/1060): Fixed Linux page size calculation. - [@dbwiddis](https://github.com/dbwiddis).
* [#1063](https://github.com/oshi/oshi/pull/1063),
  [#1065](https://github.com/oshi/oshi/pull/1065): Fixed Windows disk transfer time. - [@Space2Man](https://github.com/Space2Man).
* [#1070](https://github.com/oshi/oshi/pull/1070): Improve PDH counter robustness. - [@dbwiddis](https://github.com/dbwiddis).
* [#1073](https://github.com/oshi/oshi/pull/1073): Fix Linux Process stats in OpenVZ. - [@dbwiddis](https://github.com/dbwiddis).
* [#1075](https://github.com/oshi/oshi/pull/1075): Use systemctl for stopped Linux Services. - [@dbwiddis](https://github.com/dbwiddis).
* [#1093](https://github.com/oshi/oshi/pull/1093): Fix Windows firmware field ordering. - [@dbwiddis](https://github.com/dbwiddis).

# 4.2.0 (2019-11-09), 4.2.1 (2019-11-14)

##### New Features
* [#1038](https://github.com/oshi/oshi/pull/1038): More Battery Statistics. - [@dbwiddis](https://github.com/dbwiddis).
* [#1041](https://github.com/oshi/oshi/pull/1041): Process Affinity. - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#1039](https://github.com/oshi/oshi/pull/1039): JNA 5.5.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#1045](https://github.com/oshi/oshi/pull/1045): Better event log exception handling. - [@dbwiddis](https://github.com/dbwiddis).

# 4.1.0 (2019-10-16), 4.1.1 (2019-10-24)

##### New Features
API CHANGE: This version implements thread-safe getters for OSHI attributes.
As part of this change, support for serialization has been removed.
In addition, some setters have been removed from the API, as they were never intended for end user use.
Additional setter removal may occur in future versions.

The default configuration file has been renamed to `oshi.properties` to prevent classpath conflicts.
* [#943](https://github.com/oshi/oshi/pull/943),
  [#944](https://github.com/oshi/oshi/pull/944),
  [#948](https://github.com/oshi/oshi/pull/948),
  [#949](https://github.com/oshi/oshi/pull/949),
  [#950](https://github.com/oshi/oshi/pull/950),
  [#953](https://github.com/oshi/oshi/pull/953),
  [#968](https://github.com/oshi/oshi/pull/968),
  [#972](https://github.com/oshi/oshi/pull/972): Add toString methods to API interface implementations -
  [@agithyogendra](https://github.com/agithyogendra),
  [@rohitkukreja1508](https://github.com/rohitkukreja1508),
  [@colinbobolin](https://github.com/colinbobolin),
  [@phillips0616](https://github.com/phillips0616),
  [@BooSandy1994](https://github.com/BooSandy1994),
  [@shivangi14](https://github.com/shivangi14),
  [@fdmcneill2019](https://github.com/fdmcneill2019),
  [@dbwiddis](https://github.com/dbwiddis).
* [#959](https://github.com/oshi/oshi/pull/959): Implement thread safety -- Hardware API overhaul. - [@dbwiddis](https://github.com/dbwiddis).
* [#960](https://github.com/oshi/oshi/pull/960): OSProcess constructor with PID. - [@Potat0x](https://github.com/Potat0x).
* [#981](https://github.com/oshi/oshi/pull/981): List Services - [@agithyogendra](https://github.com/agithyogendra).
* [#1005](https://github.com/oshi/oshi/pull/1005): PhysicalMemory class - [@rohitkukreja1508](https://github.com/rohitkukreja1508).

##### Bug fixes / Improvements
* [#962](https://github.com/oshi/oshi/pull/962): Properly handle null WMI DateTime results. - [@dbwiddis](https://github.com/dbwiddis).
* [#963](https://github.com/oshi/oshi/pull/964): Move the ProcessorIdentifier inner class to the CentralProcessor class - [@Praveen101997](https://github.com/Praveen101997).
* [#971](https://github.com/oshi/oshi/pull/971): Fix handle leak in WindowsDisplay.java - [@r10a](https://github.com/r10a).
* [#977](https://github.com/oshi/oshi/pull/977): Rename default configuration - [@cilki](https://github.com/cilki).
* [#989](https://github.com/oshi/oshi/pull/989): Improve Windows current frequency stats. - [@dbwiddis](https://github.com/dbwiddis).
* [#995](https://github.com/oshi/oshi/pull/995): CoreFoundation, IOKit, DiskArbitration API overhaul. - [@dbwiddis](https://github.com/dbwiddis).
* [#1008](https://github.com/oshi/oshi/pull/1008): Specialize getHostName() - [@2kindsofcs](https://github.com/2kindsofcs).

# 4.0.0 (2019-08-10)

##### New Features
* [#756](https://github.com/oshi/oshi/pull/756): Require Java 8. - [@dbwiddis](https://github.com/dbwiddis).
* [#773](https://github.com/oshi/oshi/pull/773): Remove oshi-json artifact. - [@dbwiddis](https://github.com/dbwiddis).
* [#774](https://github.com/oshi/oshi/pull/774): API overhaul - ComputerSystem, Baseboard, and Firmware. - [@dbwiddis](https://github.com/dbwiddis).
* [#775](https://github.com/oshi/oshi/pull/775): API overhaul - GlobalMemory, new VirtualMemory. - [@dbwiddis](https://github.com/dbwiddis).
* [#776](https://github.com/oshi/oshi/pull/776): oshi-demo artifact. - [@dbwiddis](https://github.com/dbwiddis).
* [#779](https://github.com/oshi/oshi/pull/779): API overhaul - CentralProcessor, new LogicalProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#794](https://github.com/oshi/oshi/pull/794): Add NUMA nodes to Logical Processors. - [@dbwiddis](https://github.com/dbwiddis).
* [#838](https://github.com/oshi/oshi/pull/838),
  [#845](https://github.com/oshi/oshi/pull/845),
  [#911](https://github.com/oshi/oshi/pull/911): JNA version updates. - [@dbwiddis](https://github.com/dbwiddis).
* [#914](https://github.com/oshi/oshi/pull/914): Add System Boot Time. - [@shannondavid](https://github.com/shannondavid).
* [#916](https://github.com/oshi/oshi/pull/916): Move Uptime and Boot Time to OperatingSystem class. - [@dbwiddis](https://github.com/dbwiddis).
* [#917](https://github.com/oshi/oshi/pull/917): API overhaul - Sensors. - [@dbwiddis](https://github.com/dbwiddis).
* [#929](https://github.com/oshi/oshi/pull/929): Add isElevated check to OperatingSystem. - [@dbwiddis](https://github.com/dbwiddis).

##### Bug fixes / Improvements
* [#857](https://github.com/oshi/oshi/pull/857): Fix CPU temperature - [@rlouwerens](https://github.com/rlouwerens).
* [#901](https://github.com/oshi/oshi/pull/901): Fix incorrect physical processor count on Linux. - [@ellesummer](https://github.com/ellesummer).
* [#918](https://github.com/oshi/oshi/pull/918): Removed time interval based caching. - [@dbwiddis](https://github.com/dbwiddis).
* [#921](https://github.com/oshi/oshi/pull/921): Removed static map based caching. - [@dbwiddis](https://github.com/dbwiddis).
* [#922](https://github.com/oshi/oshi/pull/922): Show OSProcess Bitness. - [@dbwiddis](https://github.com/dbwiddis).
* [#926](https://github.com/oshi/oshi/pull/926): Fix SMC datatype reading. - [@dbwiddis](https://github.com/dbwiddis).
* [#928](https://github.com/oshi/oshi/pull/928): Raspberry Pi compatibility fixes. - [@dbwiddis](https://github.com/dbwiddis).
* [#931](https://github.com/oshi/oshi/pull/931): Standardize attribute updating. - [@dbwiddis](https://github.com/dbwiddis).

3.14.0 (2021-11-14)
================
(`oshi-core` artifact only)
* [#1764](https://github.com/oshi/oshi/pull/1764): Restore Java 6 compatibility. - [@dbwiddis](https://github.com/dbwiddis).

3.13.0 (2019-01-18), 3.13.1 (2019-04-21), 3.13.2 (2019-04-28), 3.13.3 (2019-06-05), 3.13.4 (2019-09-06), 3.13.5 (2020-01-02), 3.13.6 (2020-07-14)
================
* [#763](https://github.com/oshi/oshi/pull/763): Refactor PDH/WMI Fallback. - [@dbwiddis](https://github.com/dbwiddis).
* [#766](https://github.com/oshi/oshi/pull/766): Use query key to update counters in groups. - [@dbwiddis](https://github.com/dbwiddis).
* [#767](https://github.com/oshi/oshi/pull/767): Allow subclassing WmiQueryHandler with reflection. - [@dbwiddis](https://github.com/dbwiddis).
* [#769](https://github.com/oshi/oshi/pull/769): Close PDH handles after each query. - [@dbwiddis](https://github.com/dbwiddis).
* [#839](https://github.com/oshi/oshi/pull/838): JNA 5.3.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#845](https://github.com/oshi/oshi/pull/845): JNA 5.3.1. - [@dbwiddis](https://github.com/dbwiddis).
* [#1299](https://github.com/oshi/oshi/pull/1299): JNA 5.6.0 / macOS 11 Compatibility - [@dbwiddis](https://github.com/dbwiddis)

3.12.1 (2018-12-31), 3.12.2 (2019-01-10)
================
* [#728](https://github.com/oshi/oshi/pull/728): Separate WMI Query Handling from Util. - [@retomerz](https://github.com/retomerz).
* [#730](https://github.com/oshi/oshi/pull/730): Fix Windows process token handle leak. - [@dbwiddis](https://github.com/dbwiddis).
* [#731](https://github.com/oshi/oshi/pull/731): Switch to MIT License, JNA 5.2.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#735](https://github.com/oshi/oshi/pull/735): Windows XP Compatibility fixes. - [@dbwiddis](https://github.com/dbwiddis).
* [#737](https://github.com/oshi/oshi/pull/737): Properly handle redundant COM initialization. - [@dbwiddis](https://github.com/dbwiddis).

3.12.0 (2018-12-16)
================
* [#694](https://github.com/oshi/oshi/pull/694): ComputerIdentifier Util Method - [@Aashishthakur10](https://github.com/Aashishthakur10).
* [#699](https://github.com/oshi/oshi/pull/699): Fix PerfData error handling - [@dbwiddis](https://github.com/dbwiddis).
* [#703](https://github.com/oshi/oshi/pull/703): Remove deprecated CentralProcessor serialNumber method - [@dbwiddis](https://github.com/dbwiddis).
* [#704](https://github.com/oshi/oshi/pull/704): Check for Virtual Machine - [@haidong](https://github.com/haidong).
* [#724](https://github.com/oshi/oshi/pull/724): Refactor unsigned long bitmasking - [@LiborB] (https://github.com/LiborB).

3.11.0 (2018-11-21)
================
* [#685](https://github.com/oshi/oshi/pull/685): Get Linux HZ from system config - [@dbwiddis](https://github.com/dbwiddis).
* [#686](https://github.com/oshi/oshi/pull/686): JNA 5.1.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#688](https://github.com/oshi/oshi/pull/688): Fix Linux proc stat and pagesize parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#689](https://github.com/oshi/oshi/pull/689): Optionally skip slow OSProcess fields - [@dbwiddis](https://github.com/dbwiddis).
* [#690](https://github.com/oshi/oshi/pull/690): Prioritize system-release for Fedora and CentOS version - [@dbwiddis](https://github.com/dbwiddis).
* [#691](https://github.com/oshi/oshi/pull/691): Cache OSProcesses on Linux - [@dbwiddis](https://github.com/dbwiddis).

3.10.0 (2018-11-03)
================
* [#656](https://github.com/oshi/oshi/pull/656): JNA 5.0.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#659](https://github.com/oshi/oshi/pull/659): Add free/total inode counts. - [@Space2Man](https://github.com/Space2Man).
* [#666](https://github.com/oshi/oshi/pull/666): Recreate counter handles when invalid - [@dbwiddis](https://github.com/dbwiddis).
* [#675](https://github.com/oshi/oshi/pull/675): Solaris 10 network stats compatibility fix - [@dbwiddis](https://github.com/dbwiddis).

3.9.1 (2018-10-14)
================
* [#647](https://github.com/oshi/oshi/pull/647): Fix Windows idle counter calculation. - [@dbwiddis](https://github.com/dbwiddis).
* [#653](https://github.com/oshi/oshi/pull/653): Fix transferTime in WindowsDisks by using 1-%Idle - [@Space2Man](https://github.com/Space2Man).

3.9.0 (2018-10-07)
================
* [#630](https://github.com/oshi/oshi/pull/630),
  [#640](https://github.com/oshi/oshi/pull/640),
  [#645](https://github.com/oshi/oshi/pull/645),
  [#652](https://github.com/oshi/oshi/pull/652),
  [#655](https://github.com/oshi/oshi/pull/655): Add Sound Card list. - [@bilalAM](https://github.com/bilalAM).
* [#636](https://github.com/oshi/oshi/pull/636): Catch exception when english counters missing. - [@dbwiddis](https://github.com/dbwiddis).
* [#639](https://github.com/oshi/oshi/pull/639): Implement QueueLength metric in HWDiskStore. - [@Space2Man](https://github.com/Space2Man).

3.8.1 (2018-09-01), 3.8.2 (2018-09-07), 3.8.3 (2018-09-14), 3.8.4 (2018-09-04)
================
* [#592](https://github.com/oshi/oshi/pull/592): Test for non-numeric doubles in JSON parsing. - [@dbwiddis](https://github.com/dbwiddis).
* [#597](https://github.com/oshi/oshi/pull/597): Fix Windows serialnumber fallback bug. - [@dbwiddis](https://github.com/dbwiddis).
* [#603](https://github.com/oshi/oshi/pull/603): Fix Process Uptime bug. - [@anitakosman](https://github.com/anitakosman).
* [#604](https://github.com/oshi/oshi/pull/604): Fix Windows interrupt count bug. - [@dbwiddis](https://github.com/dbwiddis).
* [#605](https://github.com/oshi/oshi/pull/605): Update OSGi imports. - [@dbwiddis](https://github.com/dbwiddis).
* [#608](https://github.com/oshi/oshi/pull/608): Fix Windows context swith count bug. - [@dbwiddis](https://github.com/dbwiddis).
* [#611](https://github.com/oshi/oshi/pull/611): Fix proc stat parsing on older Linux distros. - [@dbwiddis](https://github.com/dbwiddis).
* [#612](https://github.com/oshi/oshi/pull/612): OSProcess toString. - [@dbwiddis](https://github.com/dbwiddis).
* [#614](https://github.com/oshi/oshi/pull/614): Remove unneeded debug query and fix a WMI cast error - [@dbwiddis](https://github.com/dbwiddis).
* [#626](https://github.com/oshi/oshi/pull/626): Fix calculation of Hz on Linux - [@dbwiddis](https://github.com/dbwiddis).

3.8.0 (2018-08-20)
================
* [#580](https://github.com/oshi/oshi/pull/580): Windows process uptime wasn't updating. - [@dbwiddis](https://github.com/dbwiddis).
* [#585](https://github.com/oshi/oshi/pull/585): Fix WMI type mapping and BSTR allocation. - [@dbwiddis](https://github.com/dbwiddis).
* [#586](https://github.com/oshi/oshi/pull/586): Add PerfDataUtil.removeAllCounters. - [@dbwiddis](https://github.com/dbwiddis).
* [#587](https://github.com/oshi/oshi/pull/587): Localize PDH instance enumeration. - [@dbwiddis](https://github.com/dbwiddis).
* [#588](https://github.com/oshi/oshi/pull/588): WMI backup for all PDH Counters. - [@dbwiddis](https://github.com/dbwiddis).

3.7.1 (2018-07-28), 3.7.2 (2018-08-01)
================
* [#571](https://github.com/oshi/oshi/pull/571): CIM date broken for timezones east of GMT. - [@dbwiddis](https://github.com/dbwiddis).
* [#573](https://github.com/oshi/oshi/pull/573): Don't get PDH swap stats if no swap. - [@dbwiddis](https://github.com/dbwiddis).
* [#574](https://github.com/oshi/oshi/pull/574): Suppress repeat updates of failed PDH counters. - [@dbwiddis](https://github.com/dbwiddis).
* [#575](https://github.com/oshi/oshi/pull/575): Replace WMI value types with Variant types. - [@dbwiddis](https://github.com/dbwiddis).
* [#577](https://github.com/oshi/oshi/pull/577): Get Windows temperature from PDH counters. - [@dbwiddis](https://github.com/dbwiddis).

3.7.0 (2018-07-28)
================
* [#551](https://github.com/oshi/oshi/pull/551): Check for zero-length PDH counter lists to avoid exceptions - [@dbwiddis](https://github.com/dbwiddis).
* [#556](https://github.com/oshi/oshi/pull/556): WMI timeouts, standardization, and simplification. - [@dbwiddis](https://github.com/dbwiddis).
* [#557](https://github.com/oshi/oshi/pull/557): Localize PDH Counter paths. - [@dbwiddis](https://github.com/dbwiddis).
* [#561](https://github.com/oshi/oshi/pull/561): Optimize Process CPU sort. - [@dbwiddis](https://github.com/dbwiddis).
* [#564](https://github.com/oshi/oshi/pull/564): Cache WMI connections. - [@dbwiddis](https://github.com/dbwiddis).
* [#567](https://github.com/oshi/oshi/pull/567): Cache USB devices. - [@dbwiddis](https://github.com/dbwiddis).
* [#569](https://github.com/oshi/oshi/pull/569): Remove threetenbp dependency. - [@dbwiddis](https://github.com/dbwiddis).

3.6.1 (2018-06-28), 3.6.2 (2018-07-10)
================
* [#527](https://github.com/oshi/oshi/pull/527): Correct process information caching and command line retrieval under Windows - [@dustin-johnson](https://github.com/dustin-johnson).
* [#533](https://github.com/oshi/oshi/pull/533): Filter to CPU zone if multiple Windows Thermal sensors. - [@dbwiddis](https://github.com/dbwiddis).
* [#542](https://github.com/oshi/oshi/pull/542): Disabled Windows performance collection leads to empty Process cache - [@MarcMil](https://github.com/MarcMil).
* [#547](https://github.com/oshi/oshi/pull/547): Remove DataTypeConverter dependency so OSHI builds on Java 9+ - [@dbwiddis](https://github.com/dbwiddis).

3.6.0 (2018-06-20)
================
* [#489](https://github.com/oshi/oshi/pull/489): Switch from WMI to native methods for most Windows Process data. - [@dbwiddis](https://github.com/dbwiddis).
* [#501](https://github.com/oshi/oshi/pull/501): Added HWDiskStore.updateDiskStats. - [@cjbrowne](https://github.com/cjbrowne).
* [#503](https://github.com/oshi/oshi/pull/503): Expose memory page size to API. - [@dbwiddis](https://github.com/dbwiddis).
* [#507](https://github.com/oshi/oshi/pull/507): Replace WMI with (faster) PDH queries for WindowsCentralProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#508](https://github.com/oshi/oshi/pull/508): Replace WMI with (faster) registry data for Windows Processes. - [@dbwiddis](https://github.com/dbwiddis).
* [#509](https://github.com/oshi/oshi/pull/509): Add pages swapped in/out to paging/swap file. - [@dbwiddis](https://github.com/dbwiddis).
* [#518](https://github.com/oshi/oshi/pull/518): Add OS bitness. - [@dbwiddis](https://github.com/dbwiddis).

3.5.0 (2018-04-15)
================
* [#446](https://github.com/oshi/oshi/pull/446): Add getChildProcesses to OperatingSystem. - [@jsimomaa](https://github.com/jsimomaa)
* [#447](https://github.com/oshi/oshi/pull/447),
  [#471](https://github.com/oshi/oshi/pull/471): Added context switches and interrupts -
  [@jpbempel](https://github.com/jpbempel),
  [@dbwiddis](https://github.com/dbwiddis).
* [#476](https://github.com/oshi/oshi/pull/476): Count CPU Packages - [@dbwiddis](https://github.com/dbwiddis).
* [#478](https://github.com/oshi/oshi/pull/478): Windows RSS now reports Private Working Set, matching Task Manager - [@dbwiddis](https://github.com/dbwiddis).
* Updated to JNA 4.5.1

3.4.5 (2018-04-11)
================
* [#433](https://github.com/oshi/oshi/pull/433): Performance improvements for getProcesses() on Linux - [@bildechinger](https://github.com/bildechinger).
* [#455](https://github.com/oshi/oshi/pull/455): Open files/handles support - [@spyhunter99](https://github.com/spyhunter99).
* [#459](https://github.com/oshi/oshi/pull/459): New methods for querying for a list of specific pids - [@spyhunter99](https://github.com/spyhunter99).
* [#464](https://github.com/oshi/oshi/pull/464): OSGi fixes - [@lprimak](https://github.com/lprimak).
* [#465](https://github.com/oshi/oshi/pull/465): Include a shaded jar with all dependencies - [@lprimak](https://github.com/lprimak).

3.4.4 (2017-10-15)
================
* [#392](https://github.com/oshi/oshi/pull/392): Fix NPE for processes terminating before iteration - [@dbwiddis](https://github.com/dbwiddis).
* [#396](https://github.com/oshi/oshi/pull/396): Fix issue on macOS whereby the buffer size for the call to proc_listpids() was improperly calculated - [@brettwooldridge](https://github.com/brettwooldridge)
* Updated to JNA 4.5.0

3.4.3 (2017-06-02)
================
* [#336](https://github.com/oshi/oshi/pull/336): Add Process Current Working Directory - [@dbwiddis](https://github.com/dbwiddis).
* [#357](https://github.com/oshi/oshi/pull/357): Prioritize OpenHardwareMonitor for Windows Sensors - [@dbwiddis](https://github.com/dbwiddis).
* [#362](https://github.com/oshi/oshi/pull/362): Add logical volume attribute to OSFileStore (Linux support only), providing a place for an alternate volume name. [@darinhoward](https://github.com/darinhoward)
* [#363](https://github.com/oshi/oshi/pull/363): Adding Steal Tick Type for Linux - [@darinhoward](https://github.com/darinhoward).
* [#375](https://github.com/oshi/oshi/pull/375): Added OSGi bundle support - [@swimmesberger](https://github.com/swimmesberger)
* Updated to JNA 4.4.0.

3.4.2 (2017-03-02)
================
* [#332](https://github.com/oshi/oshi/pull/332): Remove streamsupport dependency - [@dbwiddis](https://github.com/dbwiddis).

3.4.1 (2017-03-01)
================
* [#327](https://github.com/oshi/oshi/pull/327): Restore Java 7 compatibility. - [@dbwiddis](https://github.com/dbwiddis).
* [#328](https://github.com/oshi/oshi/pull/328): Updated to JNA 4.3.0. - [@dbwiddis](https://github.com/dbwiddis).

3.4.0 (2017-02-26)
================
* Switch groupId to com.github.oshi
* [#294](https://github.com/oshi/oshi/pull/294), [#305](https://github.com/oshi/oshi/pull/305): Add NetworkParams for network parameter of OS - [@chikei](https://github.com/chikei), [@dbwiddis](https://github.com/dbwiddis).
* [#295](https://github.com/oshi/oshi/pull/295): Make OSProcess (AbstractProcess.java) more easily extendible - [@michaeldesigaud](https://github.com/michaeldesigaud).
* [#307](https://github.com/oshi/oshi/pull/307): Deprecate CentralProcessor's getSystemSerialNumber method that duplicated ComputerSystem's getSerialNumber method. - [@dbwiddis](https://github.com/dbwiddis).
* [#308](https://github.com/oshi/oshi/pull/308): Add getProcessorID to CentralProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#309](https://github.com/oshi/oshi/pull/309): Reduce C library duplication. - [@dbwiddis](https://github.com/dbwiddis).
* [#317](https://github.com/oshi/oshi/pull/317): Add user/uid, group/gid, and command line to OSProcess. - [@dbwiddis](https://github.com/dbwiddis).

3.3 (2016-12-31)
================
* [#262](https://github.com/oshi/oshi/pull/262): Add bytesRead and bytesWritten to OSProcess - [@plamenko](https://github.com/plamenko).
* [#264](https://github.com/oshi/oshi/pull/264), [#289](https://github.com/oshi/oshi/pull/289): BIOS, manufacturer, and baseboard information - [@lundefugl](https://github.com/lundefugl), [@dbwiddis](https://github.com/dbwiddis).
* [#281](https://github.com/oshi/oshi/pull/281): Improve Linux battery AC device exclusion - [@dbwiddis](https://github.com/dbwiddis).
* [#282](https://github.com/oshi/oshi/pull/282): Get Windows version from WMI - [@dbwiddis](https://github.com/dbwiddis).
* [#283](https://github.com/oshi/oshi/pull/283): Fix Linux block device stats on some distributions - [@lu-ko](https://github.com/lu-ko).
* [#284](https://github.com/oshi/oshi/pull/284): Remove incorrect IOWait counter from WindowsCentralProcessor - [@dbwiddis](https://github.com/dbwiddis).
* [#285](https://github.com/oshi/oshi/pull/285): Rebrand Mac OS 10.12+ as macOS - [@dbwiddis](https://github.com/dbwiddis).
* [#286](https://github.com/oshi/oshi/pull/286): Reduce required calculations for LinuxProcess initialization - [@dbwiddis](https://github.com/dbwiddis).
* [#290](https://github.com/oshi/oshi/pull/290): Add input/output errors to Network IF - [@dbwiddis](https://github.com/dbwiddis).

3.2 (2016-09-01)
================
* [#243](https://github.com/oshi/oshi/pull/243): Make Windows network statistics 64-bit - [@dbwiddis](https://github.com/dbwiddis).
* [#244](https://github.com/oshi/oshi/pull/244): Add timestamps to Disk and Network IO Stats - [@dbwiddis](https://github.com/dbwiddis).
* [#253](https://github.com/oshi/oshi/pull/253): Properly handle CoreStorage Volumes on OSX - [@dbwiddis](https://github.com/dbwiddis).
* [#256](https://github.com/oshi/oshi/pull/256): Use DeviceID to link Windows Disks and Partitions - [@dbwiddis](https://github.com/dbwiddis).

3.1.1 (2016-08-05)
================
* [#239](https://github.com/oshi/oshi/pull/239): Fix exceptions on windows disks/partitions - [@dbwiddis](https://github.com/dbwiddis).
* [#240](https://github.com/oshi/oshi/pull/240): Check sysfs for Linux system serial number - [@dbwiddis](https://github.com/dbwiddis).

3.1 (2016-08-01)
================
* [#225](https://github.com/oshi/oshi/pull/225): Bugfixes from Coverity, FindBugs, and PMD - [@dbwiddis](https://github.com/dbwiddis).
* [#229](https://github.com/oshi/oshi/pull/229): Solaris port - [@dbwiddis](https://github.com/dbwiddis).
* [#232](https://github.com/oshi/oshi/pull/232): FreeBSD port - [@dbwiddis](https://github.com/dbwiddis).
* [#234](https://github.com/oshi/oshi/pull/234): Add read/write count and active disk time to HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).
* [#235](https://github.com/oshi/oshi/pull/235): Add partition information to HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).

3.0 (2016-07-01)
================
* [#205](https://github.com/oshi/oshi/pull/205): Separated oshi-core and oshi-json modules - [@dbwiddis](https://github.com/dbwiddis).
* [#209](https://github.com/oshi/oshi/pull/209): Move FileSystem and Processes to OperatingSystem - [@dbwiddis](https://github.com/dbwiddis).
* [#210](https://github.com/oshi/oshi/pull/210): Streamlined macOS FileSystem - [@dbwiddis](https://github.com/dbwiddis).
* [#211](https://github.com/oshi/oshi/pull/211): Combine IOwait and IRQ ticks into processor tick array - [@dbwiddis](https://github.com/dbwiddis).
* [#213](https://github.com/oshi/oshi/pull/213): Sort and Limit returned Processes - [@dbwiddis](https://github.com/dbwiddis).
* [#214](https://github.com/oshi/oshi/pull/214): Offer flat or tree USB listing - [@dbwiddis](https://github.com/dbwiddis).
* [#216](https://github.com/oshi/oshi/pull/216): Filter JSON output with properties - [@dbwiddis](https://github.com/dbwiddis).
* [#219](https://github.com/oshi/oshi/pull/219): NetworkIFs can now update stats - [@dbwiddis](https://github.com/dbwiddis).
* [#223](https://github.com/oshi/oshi/pull/223): JUnit Test overhaul, adds, refactoring - [@dbwiddis](https://github.com/dbwiddis).

2.6.2 (2016-06-21)
================
* [#199](https://github.com/oshi/oshi/pull/199): Use WMI queries for raw data instead of maintaining PDH threads - [@dbwiddis](https://github.com/dbwiddis).
* Multiple efficiency improvements

2.6-java7 / 2.6.1 (2016-06-17)
================
* [#190](https://github.com/oshi/oshi/pull/190): Add VendorID and ProductID to UsbDevice - [@dbwiddis](https://github.com/dbwiddis).
* [#193](https://github.com/oshi/oshi/pull/193): Add read/write to Windows and macOS HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).
* [#194](https://github.com/oshi/oshi/pull/194): Add volume name to OSFileStores - [@henryx](https://github.com/henryx).
* [#195](https://github.com/oshi/oshi/pull/195): Fixed reading multiple (in particular external) displays on Mac - [@dpagano](https://github.com/dpagano).
* [#197](https://github.com/oshi/oshi/pull/197): Add UUID to OSFileStores - [@dbwiddis](https://github.com/dbwiddis).
* [#198](https://github.com/oshi/oshi/pull/198): macOS 10.12 (Sierra) - [@dbwiddis](https://github.com/dbwiddis).
* Created a Java 7 backport using threeten.org dependency released as 2.6-m-java7

2.5.1-java7 / 2.5.2 (2016-06-09)
================
* [#186](https://github.com/oshi/oshi/pull/186), [#187](https://github.com/oshi/oshi/pull/187),  [#188](https://github.com/oshi/oshi/pull/188): Improve USB device trees - [@dbwiddis](https://github.com/dbwiddis).
* Created a Java 7 backport using threeten.org dependency released as 2.5.1-java7

2.5 (2016-04-06)
================
* Upgraded to Java 8 base support
* [#163](https://github.com/oshi/oshi/pull/163): Update getFileStores() method to include mount point [@henryx](https://github.com/henryx).
* [#165](https://github.com/oshi/oshi/pull/165): Added system-wide file descriptor counts - [@cholland1989](https://github.com/cholland1989).
* [#168](https://github.com/oshi/oshi/pull/168): Switched WMI queries to use COM - [@dbwiddis](https://github.com/dbwiddis).
* [#171](https://github.com/oshi/oshi/pull/171): Added detailed process statistics - [@dbwiddis](https://github.com/dbwiddis).
* [#176](https://github.com/oshi/oshi/pull/176): Eliminate Swing threads in WindowsFileSystem - [@henryx](https://github.com/henryx).
* [#180](https://github.com/oshi/oshi/pull/180): Made all system information classes serializable - [@dbwiddis](https://github.com/dbwiddis).
* [#181](https://github.com/oshi/oshi/pull/181): Added USB Device listing - [@dbwiddis](https://github.com/dbwiddis).
* [#184](https://github.com/oshi/oshi/pull/184): Improve Linux OS version parsing - [@dbwiddis](https://github.com/dbwiddis).

2.4 (2016-05-02)
================
* [#140](https://github.com/oshi/oshi/pull/140): Added process and thread counts - [@dbwiddis](https://github.com/dbwiddis).
* [#142](https://github.com/oshi/oshi/pull/142): Added methods for getting swap (total and used) in the system - [@henryx](https://github.com/henryx).
* [#145](https://github.com/oshi/oshi/pull/145): Refactored common code to abstract classes - [@dbwiddis](https://github.com/dbwiddis).
* [#147](https://github.com/oshi/oshi/pull/147),
  [#149](https://github.com/oshi/oshi/pull/149): Added disk information and statistics -
  [@henryx](https://github.com/henryx),
  [@dbwiddis](https://github.com/dbwiddis).
* [#150](https://github.com/oshi/oshi/pull/150): Added filesystem types - [@dbwiddis](https://github.com/dbwiddis).
* [#155](https://github.com/oshi/oshi/pull/155), [#157](https://github.com/oshi/oshi/pull/157): Added network interface statistics - [@henryx](https://github.com/henryx), [@dbwiddis](https://github.com/dbwiddis).

2.3 (2016-04-14)
================
* [#124](https://github.com/oshi/oshi/pull/124): Read Windows Sensor info from Open Hardware Monitor as fallback - [@dbwiddis](https://github.com/dbwiddis).
* [#129](https://github.com/oshi/oshi/pull/129): Improve Linux version and code name parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#132](https://github.com/oshi/oshi/pull/132), [#133](https://github.com/oshi/oshi/pull/133): Fix NPEs on Raspberry Pi - [@pcollaog](https://github.com/pcollaog).
* [#136](https://github.com/oshi/oshi/pull/136): Updated CPU load average to use system call - [@henryx](https://github.com/henryx).
* [#137](https://github.com/oshi/oshi/pull/137): Added iowait and irq ticks - [@dbwiddis](https://github.com/dbwiddis).

2.2 (2016-03-01)
================
* [#121](https://github.com/oshi/oshi/pull/121): Added CPU temperature, fan speeds, and voltage - [@dbwiddis](https://github.com/dbwiddis).
* [#123](https://github.com/oshi/oshi/pull/123): Handle JSON nulls - [@dbwiddis](https://github.com/dbwiddis).

2.1.2 (2016-02-24)
================
* [#118](https://github.com/oshi/oshi/pull/118): Port JSON to javax.json - [@dbwiddis](https://github.com/dbwiddis).
* [#120](https://github.com/oshi/oshi/pull/120): Support all windows processor methods - [@dbwiddis](https://github.com/dbwiddis).

2.1.1 (2016-02-19)
================
* [#114](https://github.com/oshi/oshi/pull/114): Memory information wasn't updating for Windows - [@dbwiddis](https://github.com/dbwiddis).
* [#116](https://github.com/oshi/oshi/pull/116): Added JSON output - [@dbwiddis](https://github.com/dbwiddis).

2.1 (2016-01-01)
================
* [#108](https://github.com/oshi/oshi/pull/108): Added Display info from EDID - [@dbwiddis](https://github.com/dbwiddis).
* [#111](https://github.com/oshi/oshi/pull/111): Catch exceptions when Linux c library missing - [@dbwiddis](https://github.com/dbwiddis).

2.0 (2015-11-28)
================
* [#101](https://github.com/oshi/oshi/pull/101): Refactored package structure for consistency - [@dbwiddis](https://github.com/dbwiddis).
* [#103](https://github.com/oshi/oshi/pull/103): Switched CentralProcessor to a single object for all processors - [@dbwiddis](https://github.com/dbwiddis).
* See [UPGRADING.md](UPGRADING.md) for more details.

1.5.2 2015-11-23)
================
* [#98](https://github.com/oshi/oshi/pull/98): Upgraded JNA to 4.2.1 - [@dbwiddis](https://github.com/dbwiddis).
* [#100](https://github.com/oshi/oshi/pull/100): Add physical and logical CPU counts - [@dbwiddis](https://github.com/dbwiddis).

1.5.1 (2015-10-15)
================
* [#94](https://github.com/oshi/oshi/pull/94): Upgraded JNA to 4.2.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#96](https://github.com/oshi/oshi/pull/96): Read buffer immediately after Runtime.exec to prevent deadlock - [@dbwiddis](https://github.com/dbwiddis).
* [#97](https://github.com/oshi/oshi/pull/97): Add system serial number - [@dbwiddis](https://github.com/dbwiddis).

1.5 (2015-09-21)
================
* [#87](https://github.com/oshi/oshi/pull/87): Added SLF4J logging, changed exception throwing to log errors to be robust to lack of permissions - [@dbwiddis](https://github.com/dbwiddis).

1.4 (2015-09-03)
================
* [#71](https://github.com/oshi/oshi/pull/71),
  [#72](https://github.com/oshi/oshi/pull/72): Added support for Windows 10 & Windows Server 2016 - [@laurent-r](https://github.com/laurent-r).
* [#75](https://github.com/oshi/oshi/pull/75): Added uptime information - [@dbwiddis](https://github.com/dbwiddis).
* [#76](https://github.com/oshi/oshi/pull/76): Better linux CPU processor counting - [@dbwiddis](https://github.com/dbwiddis).
* [#78](https://github.com/oshi/oshi/pull/78): Execute FileSystemView on Swing's Event Dispatch Thread - [@dbwiddis](https://github.com/dbwiddis).

1.3 (2015-06-27)
================
* Upgraded to Java 7 base support
* Upgraded JNA to 4.1.0
* Brought over lessons learned from [waffle](https://github.com/Waffle/waffle) for building project from source.
* [#50](https://github.com/oshi/oshi/pull/50): Added file store information - [@dbwiddis](https://github.com/dbwiddis).
* [#51](https://github.com/oshi/oshi/pull/51): Added CPU Ticks and switched to OperatingSystemMXBean for CPU load / load average - [@dbwiddis](https://github.com/dbwiddis).
* [#62](https://github.com/oshi/oshi/pull/62): Added Per-Processor CPU Load and Ticks - [@dbwiddis](https://github.com/dbwiddis).

1.2 (2015-06-13)
================

* Added TODO list and enhanced README documentation - [@ptitvert](https://github.com/ptitvert)
* Added Travis-CI - [@dblock](https://github.com/dblock).
* [#3](https://github.com/oshi/oshi/pull/3): Mavenized project - [@le-yams](https://github.com/le-yams).
* [#5](https://github.com/oshi/oshi/pull/5): Added Linux support - [@ptitvert](https://github.com/ptitvert).
* [#7](https://github.com/oshi/oshi/pull/7): Added macOS Support - [@ptitvert](https://github.com/ptitvert).
* [#13](https://github.com/oshi/oshi/pull/13): Support for Windows 8.1 and Windows Server 2008 R2 - [@NagyGa1](https://github.com/NagyGa1).
* [#15](https://github.com/oshi/oshi/pull/15), [#18](https://github.com/oshi/oshi/pull/18): Added support for CPU load - [@kamenitxan](https://github.com/kamenitxan), [@Sorceror](https://github.com/Sorceror).
* [#25](https://github.com/oshi/oshi/pull/25), [#29](https://github.com/oshi/oshi/pull/29): Included inactive/reclaimable memory amount in GlobalMemory#getAvailable on Mac/Linux - [@dbwiddis](https://github.com/dbwiddis).
* [#27](https://github.com/oshi/oshi/pull/27): Replaced all macOS command line parsing with JNA or System properties - [@dbwiddis](https://github.com/dbwiddis).
* [#30](https://github.com/oshi/oshi/pull/30): Added processor vendor frequency information - [@alessiofachechi](https://github.com/alessiofachechi).
* [#32](https://github.com/oshi/oshi/pull/32): Added battery state information - [@dbwiddis](https://github.com/dbwiddis).

1.1 (2013-10-13)
================
* Added support for Windows 8 to `oshi.software.os.OperatingSystemVersion`, `oshi.software.os.windows.nt.OSVersionInfoEx` - [@laurent-r](https://github.com/laurent-r).

1.0 (2010-06-23)
===============
* Initial public release - [@dblock](https://github.com/dblock).
