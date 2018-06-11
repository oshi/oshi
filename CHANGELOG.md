3.6.0 (in progress)
================
* [#489](https://github.com/oshi/oshi/pull/489): Switch from WMI to native methods for most Windows Process data. - [@dbwiddis](https://github.com/dbwiddis).
* [#501](https://github.com/oshi/oshi/pull/501): Added HWDiskStore.updateDiskStats. - [@cjbrowne](https://github.com/cjbrowne).
* [#503](https://github.com/oshi/oshi/pull/503): Expose memory page size to API. - [@dbwiddis](https://github.com/dbwiddis).
* [#507](https://github.com/oshi/oshi/pull/507): Replace WMI with (faster) PDH queries for WindowsCentralProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#508](https://github.com/oshi/oshi/pull/508): Replace WMI with (faster) registry data for Windows Processes. - [@dbwiddis](https://github.com/dbwiddis).
* [#509](https://github.com/oshi/oshi/pull/509): Add pages swapped in/out to paging/swap file. - [@dbwiddis](https://github.com/dbwiddis).
* Your contribution here.

3.5.0 (4/15/18)
================
* [#446](https://github.com/oshi/oshi/pull/446): Add getChildProcesses to OperatingSystem. - [@jsimomaa](https://github.com/jsimomaa)
* [#447](https://github.com/oshi/oshi/pull/447), [#471](https://github.com/oshi/oshi/pull/471): Added context switches and interrupts - [@jpbempel](https://github.com/jpbempel), [@dbwiddis](https://github.com/dbwiddis).
* [#476](https://github.com/dblock/oshi/pull/476): Count CPU Packages - [@dbwiddis](https://github.com/dbwiddis).
* [#478](https://github.com/dblock/oshi/pull/478): Windows RSS now reports Private Working Set, matching Task Manager - [@dbwiddis](https://github.com/dbwiddis).
* Updated to JNA 4.5.1

3.4.5 (4/11/18)
================
* [#433](https://github.com/oshi/oshi/pull/433): Performance improvements for getProcesses() on Linux - [@bildechinger](https://github.com/bildechinger).
* [#455](https://github.com/oshi/oshi/pull/455): Open files/handles support - [@spyhunter99](https://github.com/spyhunter99).
* [#459](https://github.com/oshi/oshi/pull/459): New methods for querying for a list of specific pids - [@spyhunter99](https://github.com/spyhunter99).
* [#464](https://github.com/oshi/oshi/pull/464): OSGi fixes - [@lprimak](https://github.com/lprimak).
* [#465](https://github.com/oshi/oshi/pull/465): Include a shaded jar with all dependencies - [@lprimak](https://github.com/lprimak).

3.4.4 (10/15/17)
================
* [#392](https://github.com/dblock/oshi/pull/392): Fix NPE for processes terminating before iteration - [@dbwiddis](https://github.com/dbwiddis).
* [#396](https://github.com/oshi/oshi/pull/396): Fix issue on Mac OS X whereby the buffer size for the call to proc_listpids() was improperly calculated - [@brettwooldridge](https://github.com/brettwooldridge)
* Updated to JNA 4.5.0

3.4.3 (6/2/17)
================
* [#336](https://github.com/dblock/oshi/pull/336): Add Process Current Working Directory - [@dbwiddis](https://github.com/dbwiddis).
* [#357](https://github.com/dblock/oshi/pull/357): Prioritize OpenHardwareMonitor for Windows Sensors - [@dbwiddis](https://github.com/dbwiddis).
* [#362](https://github.com/oshi/oshi/pull/362): Add logical volume attribute to OSFileStore (Linux support only), providing a place for an alternate volume name. [@darinhoward](https://github.com/darinhoward)
* [#363](https://github.com/oshi/oshi/pull/363): Adding Steal Tick Type for Linux - [@darinhoward](https://github.com/darinhoward).
* [#375](https://github.com/oshi/oshi/pull/375): Added OSGi bundle support - [@swimmesberger](https://github.com/swimmesberger)
* Updated to JNA 4.4.0.

3.4.2 (3/2/2017)
================
* [#332](https://github.com/dblock/oshi/pull/332): Remove streamsupport dependency - [@dbwiddis](https://github.com/dbwiddis).

3.4.1 (3/1/2017)
================
* [#327](https://github.com/dblock/oshi/pull/327): Restore Java 7 compatibility. - [@dbwiddis](https://github.com/dbwiddis).
* [#328](https://github.com/dblock/oshi/pull/328): Updated to JNA 4.3.0. - [@dbwiddis](https://github.com/dbwiddis).

3.4.0 (2/26/2017)
================
* Switch groupId to com.github.oshi
* [#294](https://github.com/oshi/oshi/pull/294), [#305](https://github.com/oshi/oshi/pull/305): Add NetworkParams for network parameter of OS - [@chikei](https://github.com/chikei), [@dbwiddis](https://github.com/dbwiddis).
* [#295](https://github.com/oshi/oshi/pull/295): Make OSProcess (AbstractProcess.java) more easily extendible - [@michaeldesigaud](https://github.com/michaeldesigaud).
* [#307](https://github.com/dblock/oshi/pull/307): Deprecate CentralProcessor's getSystemSerialNumber method that duplicated ComputerSystem's getSerialNumber method. - [@dbwiddis](https://github.com/dbwiddis).
* [#308](https://github.com/dblock/oshi/pull/308): Add getProcessorID to CentralProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#309](https://github.com/dblock/oshi/pull/309): Reduce C library duplication. - [@dbwiddis](https://github.com/dbwiddis).
* [#317](https://github.com/dblock/oshi/pull/317): Add user/uid, group/gid, and command line to OSProcess. - [@dbwiddis](https://github.com/dbwiddis).

3.3 (12/31/2016)
================
* [#262](https://github.com/dblock/oshi/pull/262): Add bytesRead and bytesWritten to OSProcess - [@plamenko](https://github.com/plamenko).
* [#264](https://github.com/dblock/oshi/pull/264), [#289](https://github.com/dblock/oshi/pull/289): BIOS, manufacturer, and baseboard information - [@lundefugl](https://github.com/lundefugl), [@dbwiddis](https://github.com/dbwiddis).
* [#281](https://github.com/dblock/oshi/pull/281): Improve Linux battery AC device exclusion - [@dbwiddis](https://github.com/dbwiddis).
* [#282](https://github.com/dblock/oshi/pull/282): Get Windows version from WMI - [@dbwiddis](https://github.com/dbwiddis).
* [#283](https://github.com/dblock/oshi/pull/283): Fix Linux block device stats on some distributions - [@lu-ko](https://github.com/lu-ko).
* [#284](https://github.com/dblock/oshi/pull/284): Remove incorrect IOWait counter from WindowsCentralProcessor - [@dbwiddis](https://github.com/dbwiddis).
* [#285](https://github.com/dblock/oshi/pull/285): Rebrand Mac OS 10.12+ as macOS - [@dbwiddis](https://github.com/dbwiddis).
* [#286](https://github.com/dblock/oshi/pull/286): Reduce required calculations for LinuxProcess initialization - [@dbwiddis](https://github.com/dbwiddis).
* [#290](https://github.com/dblock/oshi/pull/290): Add input/output errors to Network IF - [@dbwiddis](https://github.com/dbwiddis).

3.2 (9/1/2016)
================
* [#243](https://github.com/dblock/oshi/pull/243): Make Windows network statistics 64-bit - [@dbwiddis](https://github.com/dbwiddis).
* [#244](https://github.com/dblock/oshi/pull/244): Add timestamps to Disk and Network IO Stats - [@dbwiddis](https://github.com/dbwiddis).
* [#253](https://github.com/dblock/oshi/pull/253): Properly handle CoreStorage Volumes on OSX - [@dbwiddis](https://github.com/dbwiddis).
* [#256](https://github.com/dblock/oshi/pull/256): Use DeviceID to link Windows Disks and Partitions - [@dbwiddis](https://github.com/dbwiddis).

3.1.1 (8/5/2016)
================
* [#239](https://github.com/dblock/oshi/pull/239): Fix exceptions on windows disks/partitions - [@dbwiddis](https://github.com/dbwiddis).
* [#240](https://github.com/dblock/oshi/pull/240): Check sysfs for Linux system serial number - [@dbwiddis](https://github.com/dbwiddis).

3.1 (8/1/2016)
================
* [#225](https://github.com/dblock/oshi/pull/225): Bugfixes from Coverity, FindBugs, and PMD - [@dbwiddis](https://github.com/dbwiddis).
* [#229](https://github.com/dblock/oshi/pull/229): Solaris port - [@dbwiddis](https://github.com/dbwiddis).
* [#232](https://github.com/dblock/oshi/pull/232): FreeBSD port - [@dbwiddis](https://github.com/dbwiddis).
* [#234](https://github.com/dblock/oshi/pull/234): Add read/write count and active disk time to HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).
* [#235](https://github.com/dblock/oshi/pull/235): Add partition information to HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).

3.0 (7/1/2016)
================
* [#205](https://github.com/dblock/oshi/pull/205): Separated oshi-core and oshi-json modules - [@dbwiddis](https://github.com/dbwiddis).
* [#209](https://github.com/dblock/oshi/pull/209): Move FileSystem and Processes to OperatingSystem - [@dbwiddis](https://github.com/dbwiddis).
* [#210](https://github.com/dblock/oshi/pull/210): Streamlined macOS FileSystem - [@dbwiddis](https://github.com/dbwiddis).
* [#211](https://github.com/dblock/oshi/pull/211): Combine IOwait and IRQ ticks into processor tick array - [@dbwiddis](https://github.com/dbwiddis).
* [#213](https://github.com/dblock/oshi/pull/213): Sort and Limit returned Processes - [@dbwiddis](https://github.com/dbwiddis).
* [#214](https://github.com/dblock/oshi/pull/214): Offer flat or tree USB listing - [@dbwiddis](https://github.com/dbwiddis).
* [#216](https://github.com/dblock/oshi/pull/216): Filter JSON output with properties - [@dbwiddis](https://github.com/dbwiddis).
* [#219](https://github.com/dblock/oshi/pull/219): NetworkIFs can now update stats - [@dbwiddis](https://github.com/dbwiddis).
* [#223](https://github.com/dblock/oshi/pull/223): JUnit Test overhaul, adds, refactoring - [@dbwiddis](https://github.com/dbwiddis).

2.6.2 (6/21/2016)
================
* [#199](https://github.com/dblock/oshi/pull/199): Use WMI queries for raw data instead of maintaining PDH threads - [@dbwiddis](https://github.com/dbwiddis).
* Multiple efficiency improvements

2.6-java7 / 2.6.1 (6/17/2016)
================
* [#190](https://github.com/dblock/oshi/pull/190): Add VendorID and ProductID to UsbDevice - [@dbwiddis](https://github.com/dbwiddis).
* [#193](https://github.com/dblock/oshi/pull/193): Add read/write to Windows and OS X HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).
* [#194](https://github.com/dblock/oshi/pull/194): Add volume name to OSFileStores - [@henryx](https://github.com/henryx).
* [#195](https://github.com/dblock/oshi/pull/195): Fixed reading multiple (in particular external) displays on Mac - [@dpagano](https://github.com/dpagano).
* [#197](https://github.com/dblock/oshi/pull/197): Add UUID to OSFileStores - [@dbwiddis](https://github.com/dbwiddis).
* [#198](https://github.com/dblock/oshi/pull/198): macOS 10.12 (Sierra) - [@dbwiddis](https://github.com/dbwiddis).
* Created a Java 7 backport using threeten.org dependency released as 2.6-m-java7

2.5.1-java7 / 2.5.2 (6/9/2016)
================
* [#186](https://github.com/dblock/oshi/pull/186), [#187](https://github.com/dblock/oshi/pull/187),  [#188](https://github.com/dblock/oshi/pull/188): Improve USB device trees - [@dbwiddis](https://github.com/dbwiddis).
* Created a Java 7 backport using threeten.org dependency released as 2.5.1-java7

2.5 (6/4/2016)
================
* Upgraded to Java 8 base support
* [#163](https://github.com/dblock/oshi/pull/163): Update getFileStores() method to include mount point [@henryx](https://github.com/henryx).
* [#165](https://github.com/dblock/oshi/pull/165): Added system-wide file descriptor counts - [@cholland1989](https://github.com/cholland1989).
* [#168](https://github.com/dblock/oshi/pull/168): Switched WMI queries to use COM - [@dbwiddis](https://github.com/dbwiddis).
* [#171](https://github.com/dblock/oshi/pull/171): Added detailed process statistics - [@dbwiddis](https://github.com/dbwiddis).
* [#176](https://github.com/dblock/oshi/pull/176): Eliminate Swing threads in WindowsFileSystem - [@henryx](https://github.com/henryx).
* [#180](https://github.com/dblock/oshi/pull/180): Made all system information classes serializable - [@dbwiddis](https://github.com/dbwiddis).
* [#181](https://github.com/dblock/oshi/pull/181): Added USB Device listing - [@dbwiddis](https://github.com/dbwiddis).
* [#184](https://github.com/dblock/oshi/pull/184): Improve Linux OS version parsing - [@dbwiddis](https://github.com/dbwiddis).

2.4 (5/2/2016)
================
* [#140](https://github.com/dblock/oshi/pull/140): Added process and thread counts - [@dbwiddis](https://github.com/dbwiddis).
* [#142](https://github.com/dblock/oshi/pull/142): Added methods for getting swap (total and used) in the system - [@henryx](https://github.com/henryx).
* [#145](https://github.com/dblock/oshi/pull/145): Refactored common code to abstract classes - [@dbwiddis](https://github.com/dbwiddis).
* [#147](https://github.com/dblock/oshi/pull/147), [#149](https://github.com/dblock/oshi/pull/149): Added disk information and statistics - [@henryx](https://github.com/henryx), [@dbwiddis](https://github.com/dbwiddis).
* [#150](https://github.com/dblock/oshi/pull/150): Added filesystem types - [@dbwiddis](https://github.com/dbwiddis).
* [#155](https://github.com/dblock/oshi/pull/155), [#157](https://github.com/dblock/oshi/pull/157): Added network interface statistics - [@henryx](https://github.com/henryx), [@dbwiddis](https://github.com/dbwiddis).

2.3 (4/14/2016)
================
* [#124](https://github.com/dblock/oshi/pull/124): Read Windows Sensor info from Open Hardware Monitor as fallback - [@dbwiddis](https://github.com/dbwiddis).
* [#129](https://github.com/dblock/oshi/pull/129): Improve Linux version and code name parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#132](https://github.com/dblock/oshi/pull/132), [#133](https://github.com/dblock/oshi/pull/133): Fix NPEs on Raspberry Pi - [@pcollaog](https://github.com/pcollaog).
* [#136](https://github.com/dblock/oshi/pull/136): Updated CPU load average to use system call - [@henryx](https://github.com/henryx).
* [#137](https://github.com/dblock/oshi/pull/137): Added iowait and irq ticks - [@dbwiddis](https://github.com/dbwiddis).

2.2 (3/1/2016)
================
* [#121](https://github.com/dblock/oshi/pull/121): Added CPU temperature, fan speeds, and voltage - [@dbwiddis](https://github.com/dbwiddis).
* [#123](https://github.com/dblock/oshi/pull/123): Handle JSON nulls - [@dbwiddis](https://github.com/dbwiddis).

2.1.2 (2/24/2016)
================
* [#118](https://github.com/dblock/oshi/pull/118): Port JSON to javax.json - [@dbwiddis](https://github.com/dbwiddis).
* [#120](https://github.com/dblock/oshi/pull/120): Support all windows processor methods - [@dbwiddis](https://github.com/dbwiddis).

2.1.1 (2/19/2016)
================
* [#114](https://github.com/dblock/oshi/pull/114): Memory information wasn't updating for Windows - [@dbwiddis](https://github.com/dbwiddis).
* [#116](https://github.com/dblock/oshi/pull/116): Added JSON output - [@dbwiddis](https://github.com/dbwiddis).

2.1 (1/1/2016)
================
* [#108](https://github.com/dblock/oshi/pull/108): Added Display info from EDID - [@dbwiddis](https://github.com/dbwiddis).
* [#111](https://github.com/dblock/oshi/pull/111): Catch exceptions when Linux c library missing - [@dbwiddis](https://github.com/dbwiddis).

2.0 (11/28/2015)
================
* [#101](https://github.com/dblock/oshi/pull/101): Refactored package structure for consistency - [@dbwiddis](https://github.com/dbwiddis).
* [#103](https://github.com/dblock/oshi/pull/103): Switched CentralProcessor to a single object for all processors - [@dbwiddis](https://github.com/dbwiddis).
* See [UPGRADING.md](UPGRADING.md) for more details.

1.5.2 (11/23/2015)
================
* [#98](https://github.com/dblock/oshi/pull/98): Upgraded JNA to 4.2.1 - [@dbwiddis](https://github.com/dbwiddis).
* [#100](https://github.com/dblock/oshi/pull/100): Add physical and logical CPU counts - [@dbwiddis](https://github.com/dbwiddis).

1.5.1 (10/15/2015)
================
* [#94](https://github.com/dblock/oshi/pull/94): Upgraded JNA to 4.2.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#96](https://github.com/dblock/oshi/pull/96): Read buffer immediately after Runtime.exec to prevent deadlock - [@dbwiddis](https://github.com/dbwiddis).
* [#97](https://github.com/dblock/oshi/pull/97): Add system serial number - [@dbwiddis](https://github.com/dbwiddis).

1.5 (9/21/2015)
================
* [#87](https://github.com/dblock/oshi/pull/87): Added SLF4J logging, changed exception throwing to log errors to be robust to lack of permissions - [@dbwiddis](https://github.com/dbwiddis).

1.4 (9/3/2015)
================
* [#71](https://github.com/dblock/oshi/pull/71), [#72](https://github.com/dblock/oshi/pull/72): Added support for Windows 10 & Windows Server 2016 - [@laurent-r](https://github.com/laurent-r).
* [#75](https://github.com/dblock/oshi/pull/75): Added uptime information - [@dbwiddis](https://github.com/dbwiddis).
* [#76](https://github.com/dblock/oshi/pull/76): Better linux CPU processor counting - [@dbwiddis](https://github.com/dbwiddis).
* [#78](https://github.com/dblock/oshi/pull/78): Execute FileSystemView on Swing's Event Dispatch Thread - [@dbwiddis](https://github.com/dbwiddis).

1.3 (6/27/2015)
================
* Upgraded to Java 7 base support
* Upgraded JNA to 4.1.0
* Brought over lessons learned from [waffle](https://github.com/dblock/waffle) for building project from source.
* [#50](https://github.com/dblock/oshi/pull/50): Added file store information - [@dbwiddis](https://github.com/dbwiddis).
* [#51](https://github.com/dblock/oshi/pull/51): Added CPU Ticks and switched to OperatingSystemMXBean for CPU load / load average - [@dbwiddis](https://github.com/dbwiddis).
* [#62](https://github.com/dblock/oshi/pull/62): Added Per-Processor CPU Load and Ticks - [@dbwiddis](https://github.com/dbwiddis).

1.2 (6/13/2014)
================

* Added TODO list and enhanced README documentation - [@ptitvert](https://github.com/ptitvert)
* Added Travis-CI - [@dblock](https://github.com/dblock).
* [#3](https://github.com/dblock/oshi/pull/3): Mavenized project - [@le-yams](https://github.com/le-yams).
* [#5](https://github.com/dblock/oshi/pull/5): Added Linux support - [@ptitvert](https://github.com/ptitvert).
* [#7](https://github.com/dblock/oshi/pull/7): Added Mac OS X Support - [@ptitvert](https://github.com/ptitvert).
* [#13](https://github.com/dblock/oshi/pull/13): Support for Windows 8.1 and Windows Server 2008 R2 - [@NagyGa1](https://github.com/NagyGa1).
* [#15](https://github.com/dblock/oshi/pull/15), [#18](https://github.com/dblock/oshi/pull/18): Added support for CPU load - [@kamenitxan](https://github.com/kamenitxan), [@Sorceror](https://github.com/Sorceror).
* [#25](https://github.com/dblock/oshi/pull/25), [#29](https://github.com/dblock/oshi/pull/29): Included inactive/reclaimable memory amount in GlobalMemory#getAvailable on Mac/Linux - [@dbwiddis](https://github.com/dbwiddis).
* [#27](https://github.com/dblock/oshi/pull/27): Replaced all Mac OS X command line parsing with JNA or System properties - [@dbwiddis](https://github.com/dbwiddis).
* [#30](https://github.com/dblock/oshi/pull/30): Added processor vendor frequency information - [@alessiofachechi](https://github.com/alessiofachechi).
* [#32](https://github.com/dblock/oshi/pull/32): Added battery state information - [@dbwiddis](https://github.com/dbwiddis).

1.1 (10/13/2013)
================

* Added support for Windows 8 to `oshi.software.os.OperatingSystemVersion`, `oshi.software.os.windows.nt.OSVersionInfoEx` - [@laurent-r](https://github.com/laurent-r).

1.0 (6/23/2010)
===============

* Initial public release - [@dblock](https://github.com/dblock).
