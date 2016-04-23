2.4 (in-progress)
================
* [#140](https://github.com/dblock/oshi/pull/140): Added process and thread counts - [@dbwiddis](https://github.com/dbwiddis).
* [#142](https://github.com/dblock/oshi/pull/142): Added methods for getting swap (total and used) in the system - [@henryx](https://github.com/henryx).
* [#145](https://github.com/dblock/oshi/pull/145): Refactored common code to abstract classes - [@dbwiddis](https://github.com/dbwiddis).
* [#147](https://github.com/dblock/oshi/pull/147): Added disk information for Linux - [@henryx](https://github.com/henryx).
* Your contribution here.

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
* See site page for individual commits going forwards as only major changes will be mentioned manually in change log.
* Upgraded to java 7 base support
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
