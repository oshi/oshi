5.2.0 (in progress)
================
* [#1247](https://github.com/oshi/oshi/pull/1247): Add Automatic-Module-Name to support JPMS - [@dbwiddis](https://github.com/dbwiddis).
* [#1262](https://github.com/oshi/oshi/pull/1262): Read macOS versions from properties file - [@hkbiet](https://github.com/hkbiet). 
* [#1266](https://github.com/oshi/oshi/pull/1266): Suppress repeated attempts to query failed PDH - [@dbwiddis](https://github.com/dbwiddis).
* [#1267](https://github.com/oshi/oshi/pull/1267): Check proc_pidinfo return value before incrementing numberOfThreads - [@markkulube](https://github.com/markkulube).
* Your contribution here 

4.8.2 / 5.1.2 (6/7/2020)
================
* [#1246](https://github.com/oshi/oshi/pull/1246): Configure data source for OperatingSystem#getSessions - [@dbwiddis](https://github.com/dbwiddis).
* [#1252](https://github.com/oshi/oshi/pull/1252): Fallback to command line if getSessions sanity check fails - [@dbwiddis](https://github.com/dbwiddis).
* [#1256](https://github.com/oshi/oshi/pull/1256): Fix calculation of Linux process start time - [@dbwiddis](https://github.com/dbwiddis).

4.8.1 / 5.1.1 (5/30/2020)
================
* [#1237](https://github.com/oshi/oshi/pull/1237): Update Udev to object oriented style - [@dbwiddis](https://github.com/dbwiddis).
* [#1240](https://github.com/oshi/oshi/pull/1240): Add a driver for proc/pid/statm - [@dbwiddis](https://github.com/dbwiddis).
* [#1241](https://github.com/oshi/oshi/pull/1241): (5.x) Add code-assert - [@dbwiddis](https://github.com/dbwiddis).
* [#1245](https://github.com/oshi/oshi/pull/1245): Refactor PerfCounterQuery classes and fix memory leak - [@dbwiddis](https://github.com/dbwiddis).

4.8.0 / 5.1.0 (5/20/2020)
================
* [#1229](https://github.com/oshi/oshi/pull/1229): Changed the linux and solaris virtual memory swapins/outs to count just swaps - [@roeezz](https://github.com/roeezz)
* [#1231](https://github.com/oshi/oshi/pull/1231): Add OSSessions. - [@dbwiddis](https://github.com/dbwiddis).
* [#1233](https://github.com/oshi/oshi/pull/1233), [#1234](https://github.com/oshi/oshi/pull/1234): Added more unit tests in CentralProcessorTest and ParseUtilTest - [@zachsez](https://github.com/zachsez).
* [#1195](https://github.com/oshi/oshi/pull/1195), 
  [#1222](https://github.com/oshi/oshi/pull/1222),
  [#1224](https://github.com/oshi/oshi/pull/1224),
  [#1225](https://github.com/oshi/oshi/pull/1225),
  [#1226](https://github.com/oshi/oshi/pull/1226),
  [#1228](https://github.com/oshi/oshi/pull/1228),
  [#1232](https://github.com/oshi/oshi/pull/1232),
  [#1235](https://github.com/oshi/oshi/pull/1235):
  Added messages to unit test assertions in multiple classes. - 
  [@tomokos2](https://github.com/tomokos2),
  [@david145noone](https://github.com/david145noone),
  [@tausiflife](https://github.com/tausiflife),
  [@tschens95](https://github.com/tschens95),
  [@dPramod](https://github.com/dPramod),
  [@roeezz](https://github.com/roeezz),
  [@zachsez](https://github.com/zachsez),
  [@RaymondLZhou](https://github.com/RaymondLZhou).

5.0.0 (5/5/2020), 5.0.1 (5/6/2020), 5.0.2 (5/14/2020)
================
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

4.7.0 (4/25/2020), 4.7.1 (5/2/2020), 4.7.2 (5/6/2020), 4.7.3 (5/14/2020)
================
* [#1174](https://github.com/oshi/oshi/pull/1174): Add TCP and UDP statistics. - [@dbwiddis](https://github.com/dbwiddis).
* [#1183](https://github.com/oshi/oshi/pull/1183): Add more VirtualMemory information. - [@dbwiddis](https://github.com/dbwiddis).
* [#1219](https://github.com/oshi/oshi/pull/1219): Only get primary group on WindowsOSProcess. - [@dbwiddis](https://github.com/dbwiddis).

4.6.0 (4/2/2020), 4.6.1 (4/8/2020)
================
* [#894](https://github.com/oshi/oshi/pull/894): Look up microarchitecture from processor identifier. - [@tbradellis](https://github.com/tbradellis).
* [#1150](https://github.com/oshi/oshi/pull/1150): Add fields to NetworkIF to help determine physical interfaces. - [@dbwiddis](https://github.com/dbwiddis).
* [#1151](https://github.com/oshi/oshi/pull/1151): Add Graphics Card information. - [@dbwiddis](https://github.com/dbwiddis).
* [#1155](https://github.com/oshi/oshi/pull/1155): Linux proc symlinks may show as (deleted). - [@jlangst6](https://github.com/jlangst6).
* [#1157](https://github.com/oshi/oshi/pull/1157): Audit and annotate ThreadSafe classes. - [@dbwiddis](https://github.com/dbwiddis).

4.5.0 (3/12/2020), 4.5.2 (3/20/2020)
================
* [#1123](https://github.com/oshi/oshi/pull/1123): Add driver to parse Linux proc/diskstats. - [@dbwiddis](https://github.com/dbwiddis).
* [#1124](https://github.com/oshi/oshi/pull/1124): Add driver to parse Linux proc/pid/stat. - [@dbwiddis](https://github.com/dbwiddis).
* [#1125](https://github.com/oshi/oshi/pull/1125): Add driver to parse Linux proc/stat and proc/uptime. - [@dbwiddis](https://github.com/dbwiddis).
* [#1127](https://github.com/oshi/oshi/pull/1127): Add Volume Label to OSFileStore. - [@dbwiddis](https://github.com/dbwiddis).
* [#1139](https://github.com/oshi/oshi/pull/1139): Fix Windows FileStore updating. - [@dbwiddis](https://github.com/dbwiddis).
* [#1140](https://github.com/oshi/oshi/pull/1140): Demo Swing GUI. - [@dbwiddis](https://github.com/dbwiddis).
* [#1143](https://github.com/oshi/oshi/pull/1140): Add process CPU usage between ticks calculation. - [@dbwiddis](https://github.com/dbwiddis).

4.4.0 (2/12/2020), 4.4.1 (2/17/2020), 4.4.2 (2/20/2020)
================
* [#1098](https://github.com/oshi/oshi/pull/1098): Option to limit FileStore list to local file systems. - [@Space2Man](https://github.com/Space2Man).
* [#1100](https://github.com/oshi/oshi/pull/1100): Get FileStore options. - [@dbwiddis](https://github.com/dbwiddis).
* [#1101](https://github.com/oshi/oshi/pull/1101): Add network interface dropped packets and collisions. - [@dbwiddis](https://github.com/dbwiddis).
* [#1105](https://github.com/oshi/oshi/pull/1105): Added additional pseudo filesystems. - [@Space2Man](https://github.com/Space2Man).

4.3.0 (1/2/2020), 4.3.1 (2/5/2020)
================
* [#1057](https://github.com/oshi/oshi/pull/1057): Added Subnet Mask & Prefix Length to NetworkIF. - [@vesyrak](https://github.com/Vesyrak).
* [#1060](https://github.com/oshi/oshi/pull/1060): Fixed Linux page size calculation. - [@dbwiddis](https://github.com/dbwiddis).
* [#1063](https://github.com/oshi/oshi/pull/1063),
  [#1065](https://github.com/oshi/oshi/pull/1065): Fixed Windows disk transfer time. - [@Space2Man](https://github.com/Space2Man).
* [#1070](https://github.com/oshi/oshi/pull/1070): Improve PDH counter robustness. - [@dbwiddis](https://github.com/dbwiddis).
* [#1073](https://github.com/oshi/oshi/pull/1073): Fix Linux Process stats in OpenVZ. - [@dbwiddis](https://github.com/dbwiddis).
* [#1075](https://github.com/oshi/oshi/pull/1075): Use systemctl for stopped Linux Services. - [@dbwiddis](https://github.com/dbwiddis).
* [#1093](https://github.com/oshi/oshi/pull/1093): Fix Windows firmware field ordering. - [@dbwiddis](https://github.com/dbwiddis).
* [#1095](https://github.com/oshi/oshi/pull/1095): Vend JSON via HTTP Server (oshi-demo). - [@dbwiddis](https://github.com/dbwiddis).

4.2.0 (11/9/2019), 4.2.1 (11/14/2019)
================
* [#1038](https://github.com/oshi/oshi/pull/1038): More Battery Statistics. - [@dbwiddis](https://github.com/dbwiddis).
* [#1039](https://github.com/oshi/oshi/pull/1039): JNA 5.5.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#1041](https://github.com/oshi/oshi/pull/1041): Process Affinity. - [@dbwiddis](https://github.com/dbwiddis).
* [#1045](https://github.com/oshi/oshi/pull/1045): Better event log exception handling. - [@dbwiddis](https://github.com/dbwiddis).

4.1.0 (10/16/2019), 4.1.1 (10/24/2019)
================
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
* [#962](https://github.com/oshi/oshi/pull/962): Properly handle null WMI DateTime results. - [@dbwiddis](https://github.com/dbwiddis).
* [#963](https://github.com/oshi/oshi/pull/964): Move the ProcessorIdentifier inner class to the CentralProcessor class - [@Praveen101997](https://github.com/Praveen101997).  
* [#971](https://github.com/oshi/oshi/pull/971): Fix handle leak in WindowsDisplay.java - [@r10a](https://github.com/r10a).
* [#977](https://github.com/oshi/oshi/pull/977): Rename default configuration - [@cilki](https://github.com/cilki).
* [#981](https://github.com/oshi/oshi/pull/981): List Services - [@agithyogendra](https://github.com/agithyogendra).
* [#989](https://github.com/oshi/oshi/pull/989): Improve Windows current frequency stats. - [@dbwiddis](https://github.com/dbwiddis).
* [#995](https://github.com/oshi/oshi/pull/995): CoreFoundation, IOKit, DiskArbitration API overhaul. - [@dbwiddis](https://github.com/dbwiddis).
* [#1005](https://github.com/oshi/oshi/pull/1005): PhysicalMemory class - [@rohitkukreja1508](https://github.com/rohitkukreja1508).
* [#1008](https://github.com/oshi/oshi/pull/1008): Specialize getHostName() - [@2kindsofcs](https://github.com/2kindsofcs).
* Your contribution here.

4.0.0 (8/10/2019)
================
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
* [#857](https://github.com/oshi/oshi/pull/857): Fix CPU temperature - [@rlouwerens](https://github.com/rlouwerens).
* [#901](https://github.com/oshi/oshi/pull/901): Fix incorrect physical processor count on Linux. - [@ellesummer](https://github.com/ellesummer).
* [#914](https://github.com/oshi/oshi/pull/914): Add System Boot Time. - [@shannondavid](https://github.com/shannondavid).
* [#916](https://github.com/oshi/oshi/pull/916): Move Uptime and Boot Time to OperatingSystem class. - [@dbwiddis](https://github.com/dbwiddis).
* [#917](https://github.com/oshi/oshi/pull/917): API overhaul - Sensors. - [@dbwiddis](https://github.com/dbwiddis).
* [#918](https://github.com/oshi/oshi/pull/918): Removed time interval based caching. - [@dbwiddis](https://github.com/dbwiddis).
* [#921](https://github.com/oshi/oshi/pull/921): Removed static map based caching. - [@dbwiddis](https://github.com/dbwiddis).
* [#922](https://github.com/oshi/oshi/pull/922): Show OSProcess Bitness. - [@dbwiddis](https://github.com/dbwiddis).
* [#926](https://github.com/oshi/oshi/pull/926): Fix SMC datatype reading. - [@dbwiddis](https://github.com/dbwiddis).
* [#928](https://github.com/oshi/oshi/pull/928): Raspberry Pi compatibility fixes. - [@dbwiddis](https://github.com/dbwiddis).
* [#929](https://github.com/oshi/oshi/pull/929): Add isElevated check to OperatingSystem. - [@dbwiddis](https://github.com/dbwiddis).
* [#931](https://github.com/oshi/oshi/pull/931): Standardize attribute updating. - [@dbwiddis](https://github.com/dbwiddis).

3.13.0 (1/18/2019), 3.13.1 (4/21/2019), 3.13.2 (4/28/2019), 3.13.3 (6/5/2019), 3.13.4 (9/6/2019), 3.13.5 (1/2/2020)
================
* [#763](https://github.com/oshi/oshi/pull/763): Refactor PDH/WMI Fallback. - [@dbwiddis](https://github.com/dbwiddis).
* [#766](https://github.com/oshi/oshi/pull/766): Use query key to update counters in groups. - [@dbwiddis](https://github.com/dbwiddis).
* [#767](https://github.com/oshi/oshi/pull/767): Allow subclassing WmiQueryHandler with reflection. - [@dbwiddis](https://github.com/dbwiddis).
* [#769](https://github.com/oshi/oshi/pull/769): Close PDH handles after each query. - [@dbwiddis](https://github.com/dbwiddis).
* [#839](https://github.com/oshi/oshi/pull/838): JNA 5.3.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#845](https://github.com/oshi/oshi/pull/845): JNA 5.3.1. - [@dbwiddis](https://github.com/dbwiddis).

3.12.1 (12/31/2018), 3.12.2 (1/10/2019)
================
* [#728](https://github.com/oshi/oshi/pull/728): Separate WMI Query Handling from Util. - [@retomerz](https://github.com/retomerz).
* [#730](https://github.com/oshi/oshi/pull/730): Fix Windows process token handle leak. - [@dbwiddis](https://github.com/dbwiddis).
* [#731](https://github.com/oshi/oshi/pull/731): Switch to MIT License, JNA 5.2.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#735](https://github.com/oshi/oshi/pull/735): Windows XP Compatibility fixes. - [@dbwiddis](https://github.com/dbwiddis).
* [#737](https://github.com/oshi/oshi/pull/737): Properly handle redundant COM initialization. - [@dbwiddis](https://github.com/dbwiddis).

3.12.0 (12/16/2018)
================
* [#694](https://github.com/oshi/oshi/pull/694): ComputerIdentifier Util Method - [@Aashishthakur10](https://github.com/Aashishthakur10).
* [#699](https://github.com/oshi/oshi/pull/699): Fix PerfData error handling - [@dbwiddis](https://github.com/dbwiddis).
* [#703](https://github.com/oshi/oshi/pull/703): Remove deprecated CentralProcessor serialNumber method - [@dbwiddis](https://github.com/dbwiddis).
* [#704](https://github.com/oshi/oshi/pull/704): Check for Virtual Machine - [@haidong](https://github.com/haidong).
* [#724](https://github.com/oshi/oshi/pull/724): Refactor unsigned long bitmasking - [@LiborB] (https://github.com/LiborB).

3.11.0 (11/21/2018)
================
* [#685](https://github.com/oshi/oshi/pull/685): Get Linux HZ from system config - [@dbwiddis](https://github.com/dbwiddis).
* [#686](https://github.com/oshi/oshi/pull/686): JNA 5.1.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#688](https://github.com/oshi/oshi/pull/688): Fix Linux proc stat and pagesize parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#689](https://github.com/oshi/oshi/pull/689): Optionally skip slow OSProcess fields - [@dbwiddis](https://github.com/dbwiddis).
* [#690](https://github.com/oshi/oshi/pull/690): Prioritize system-release for Fedora and CentOS version - [@dbwiddis](https://github.com/dbwiddis).
* [#691](https://github.com/oshi/oshi/pull/691): Cache OSProcesses on Linux - [@dbwiddis](https://github.com/dbwiddis).

3.10.0 (11/03/2018)
================
* [#656](https://github.com/oshi/oshi/pull/656): JNA 5.0.0. - [@dbwiddis](https://github.com/dbwiddis).
* [#659](https://github.com/oshi/oshi/pull/659): Add free/total inode counts. - [@Space2Man](https://github.com/Space2Man).
* [#666](https://github.com/oshi/oshi/pull/666): Recreate counter handles when invalid - [@dbwiddis](https://github.com/dbwiddis).
* [#675](https://github.com/oshi/oshi/pull/675): Solaris 10 network stats compatibility fix - [@dbwiddis](https://github.com/dbwiddis).

3.9.1 (10/14/2018)
================
* [#647](https://github.com/oshi/oshi/pull/647): Fix Windows idle counter calculation. - [@dbwiddis](https://github.com/dbwiddis).
* [#653](https://github.com/oshi/oshi/pull/653): Fix transferTime in WindowsDisks by using 1-%Idle - [@Space2Man](https://github.com/Space2Man).

3.9.0 (10/7/2018)
================
* [#630](https://github.com/oshi/oshi/pull/630), 
  [#640](https://github.com/oshi/oshi/pull/640), 
  [#645](https://github.com/oshi/oshi/pull/645), 
  [#652](https://github.com/oshi/oshi/pull/652), 
  [#655](https://github.com/oshi/oshi/pull/655): Add Sound Card list. - [@bilalAM](https://github.com/bilalAM).
* [#636](https://github.com/oshi/oshi/pull/636): Catch exception when english counters missing. - [@dbwiddis](https://github.com/dbwiddis).
* [#639](https://github.com/oshi/oshi/pull/639): Implement QueueLength metric in HWDiskStore. - [@Space2Man](https://github.com/Space2Man).

3.8.1 (09/01/2018), 3.8.2 (09/07/2018), 3.8.3 (09/14/2018), 3.8.4 (09/24/2018)
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

3.8.0 (08/20/2018)
================
* [#580](https://github.com/oshi/oshi/pull/580): Windows process uptime wasn't updating. - [@dbwiddis](https://github.com/dbwiddis).
* [#585](https://github.com/oshi/oshi/pull/585): Fix WMI type mapping and BSTR allocation. - [@dbwiddis](https://github.com/dbwiddis).
* [#586](https://github.com/oshi/oshi/pull/586): Add PerfDataUtil.removeAllCounters. - [@dbwiddis](https://github.com/dbwiddis).
* [#587](https://github.com/oshi/oshi/pull/587): Localize PDH instance enumeration. - [@dbwiddis](https://github.com/dbwiddis).
* [#588](https://github.com/oshi/oshi/pull/588): WMI backup for all PDH Counters. - [@dbwiddis](https://github.com/dbwiddis).

3.7.1 (07/28/2018), 3.7.2 (08/01/2018)
================
* [#571](https://github.com/oshi/oshi/pull/571): CIM date broken for timezones east of GMT. - [@dbwiddis](https://github.com/dbwiddis).
* [#573](https://github.com/oshi/oshi/pull/573): Don't get PDH swap stats if no swap. - [@dbwiddis](https://github.com/dbwiddis).
* [#574](https://github.com/oshi/oshi/pull/574): Suppress repeat updates of failed PDH counters. - [@dbwiddis](https://github.com/dbwiddis).
* [#575](https://github.com/oshi/oshi/pull/575): Replace WMI value types with Variant types. - [@dbwiddis](https://github.com/dbwiddis).
* [#577](https://github.com/oshi/oshi/pull/577): Get Windows temperature from PDH counters. - [@dbwiddis](https://github.com/dbwiddis).

3.7.0 (07/28/2018)
================
* [#551](https://github.com/oshi/oshi/pull/551): Check for zero-length PDH counter lists to avoid exceptions - [@dbwiddis](https://github.com/dbwiddis).
* [#556](https://github.com/oshi/oshi/pull/556): WMI timeouts, standardization, and simplification. - [@dbwiddis](https://github.com/dbwiddis).
* [#557](https://github.com/oshi/oshi/pull/557): Localize PDH Counter paths. - [@dbwiddis](https://github.com/dbwiddis).
* [#561](https://github.com/oshi/oshi/pull/561): Optimize Process CPU sort. - [@dbwiddis](https://github.com/dbwiddis).
* [#564](https://github.com/oshi/oshi/pull/564): Cache WMI connections. - [@dbwiddis](https://github.com/dbwiddis).
* [#567](https://github.com/oshi/oshi/pull/567): Cache USB devices. - [@dbwiddis](https://github.com/dbwiddis).
* [#569](https://github.com/oshi/oshi/pull/569): Remove threetenbp dependency. - [@dbwiddis](https://github.com/dbwiddis).

3.6.1 (6/28/2018), 3.6.2 (7/10/2018)
================
* [#527](https://github.com/oshi/oshi/pull/527): Correct process information caching and command line retrieval under Windows - [@dustin-johnson](https://github.com/dustin-johnson).
* [#533](https://github.com/oshi/oshi/pull/533): Filter to CPU zone if multiple Windows Thermal sensors. - [@dbwiddis](https://github.com/dbwiddis).
* [#542](https://github.com/oshi/oshi/pull/542): Disabled Windows performance collection leads to empty Process cache - [@MarcMil](https://github.com/MarcMil).
* [#547](https://github.com/oshi/oshi/pull/547): Remove DataTypeConverter dependency so OSHI builds on Java 9+ - [@dbwiddis](https://github.com/dbwiddis).

3.6.0 (6/20/2018)
================
* [#489](https://github.com/oshi/oshi/pull/489): Switch from WMI to native methods for most Windows Process data. - [@dbwiddis](https://github.com/dbwiddis).
* [#501](https://github.com/oshi/oshi/pull/501): Added HWDiskStore.updateDiskStats. - [@cjbrowne](https://github.com/cjbrowne).
* [#503](https://github.com/oshi/oshi/pull/503): Expose memory page size to API. - [@dbwiddis](https://github.com/dbwiddis).
* [#507](https://github.com/oshi/oshi/pull/507): Replace WMI with (faster) PDH queries for WindowsCentralProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#508](https://github.com/oshi/oshi/pull/508): Replace WMI with (faster) registry data for Windows Processes. - [@dbwiddis](https://github.com/dbwiddis).
* [#509](https://github.com/oshi/oshi/pull/509): Add pages swapped in/out to paging/swap file. - [@dbwiddis](https://github.com/dbwiddis).
* [#518](https://github.com/oshi/oshi/pull/518): Add OS bitness. - [@dbwiddis](https://github.com/dbwiddis).

3.5.0 (4/15/2018)
================
* [#446](https://github.com/oshi/oshi/pull/446): Add getChildProcesses to OperatingSystem. - [@jsimomaa](https://github.com/jsimomaa)
* [#447](https://github.com/oshi/oshi/pull/447), 
  [#471](https://github.com/oshi/oshi/pull/471): Added context switches and interrupts - 
  [@jpbempel](https://github.com/jpbempel), 
  [@dbwiddis](https://github.com/dbwiddis).
* [#476](https://github.com/oshi/oshi/pull/476): Count CPU Packages - [@dbwiddis](https://github.com/dbwiddis).
* [#478](https://github.com/oshi/oshi/pull/478): Windows RSS now reports Private Working Set, matching Task Manager - [@dbwiddis](https://github.com/dbwiddis).
* Updated to JNA 4.5.1

3.4.5 (4/11/2018)
================
* [#433](https://github.com/oshi/oshi/pull/433): Performance improvements for getProcesses() on Linux - [@bildechinger](https://github.com/bildechinger).
* [#455](https://github.com/oshi/oshi/pull/455): Open files/handles support - [@spyhunter99](https://github.com/spyhunter99).
* [#459](https://github.com/oshi/oshi/pull/459): New methods for querying for a list of specific pids - [@spyhunter99](https://github.com/spyhunter99).
* [#464](https://github.com/oshi/oshi/pull/464): OSGi fixes - [@lprimak](https://github.com/lprimak).
* [#465](https://github.com/oshi/oshi/pull/465): Include a shaded jar with all dependencies - [@lprimak](https://github.com/lprimak).

3.4.4 (10/15/17)
================
* [#392](https://github.com/oshi/oshi/pull/392): Fix NPE for processes terminating before iteration - [@dbwiddis](https://github.com/dbwiddis).
* [#396](https://github.com/oshi/oshi/pull/396): Fix issue on Mac OS X whereby the buffer size for the call to proc_listpids() was improperly calculated - [@brettwooldridge](https://github.com/brettwooldridge)
* Updated to JNA 4.5.0

3.4.3 (6/2/17)
================
* [#336](https://github.com/oshi/oshi/pull/336): Add Process Current Working Directory - [@dbwiddis](https://github.com/dbwiddis).
* [#357](https://github.com/oshi/oshi/pull/357): Prioritize OpenHardwareMonitor for Windows Sensors - [@dbwiddis](https://github.com/dbwiddis).
* [#362](https://github.com/oshi/oshi/pull/362): Add logical volume attribute to OSFileStore (Linux support only), providing a place for an alternate volume name. [@darinhoward](https://github.com/darinhoward)
* [#363](https://github.com/oshi/oshi/pull/363): Adding Steal Tick Type for Linux - [@darinhoward](https://github.com/darinhoward).
* [#375](https://github.com/oshi/oshi/pull/375): Added OSGi bundle support - [@swimmesberger](https://github.com/swimmesberger)
* Updated to JNA 4.4.0.

3.4.2 (3/2/2017)
================
* [#332](https://github.com/oshi/oshi/pull/332): Remove streamsupport dependency - [@dbwiddis](https://github.com/dbwiddis).

3.4.1 (3/1/2017)
================
* [#327](https://github.com/oshi/oshi/pull/327): Restore Java 7 compatibility. - [@dbwiddis](https://github.com/dbwiddis).
* [#328](https://github.com/oshi/oshi/pull/328): Updated to JNA 4.3.0. - [@dbwiddis](https://github.com/dbwiddis).

3.4.0 (2/26/2017)
================
* Switch groupId to com.github.oshi
* [#294](https://github.com/oshi/oshi/pull/294), [#305](https://github.com/oshi/oshi/pull/305): Add NetworkParams for network parameter of OS - [@chikei](https://github.com/chikei), [@dbwiddis](https://github.com/dbwiddis).
* [#295](https://github.com/oshi/oshi/pull/295): Make OSProcess (AbstractProcess.java) more easily extendible - [@michaeldesigaud](https://github.com/michaeldesigaud).
* [#307](https://github.com/oshi/oshi/pull/307): Deprecate CentralProcessor's getSystemSerialNumber method that duplicated ComputerSystem's getSerialNumber method. - [@dbwiddis](https://github.com/dbwiddis).
* [#308](https://github.com/oshi/oshi/pull/308): Add getProcessorID to CentralProcessor. - [@dbwiddis](https://github.com/dbwiddis).
* [#309](https://github.com/oshi/oshi/pull/309): Reduce C library duplication. - [@dbwiddis](https://github.com/dbwiddis).
* [#317](https://github.com/oshi/oshi/pull/317): Add user/uid, group/gid, and command line to OSProcess. - [@dbwiddis](https://github.com/dbwiddis).

3.3 (12/31/2016)
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

3.2 (9/1/2016)
================
* [#243](https://github.com/oshi/oshi/pull/243): Make Windows network statistics 64-bit - [@dbwiddis](https://github.com/dbwiddis).
* [#244](https://github.com/oshi/oshi/pull/244): Add timestamps to Disk and Network IO Stats - [@dbwiddis](https://github.com/dbwiddis).
* [#253](https://github.com/oshi/oshi/pull/253): Properly handle CoreStorage Volumes on OSX - [@dbwiddis](https://github.com/dbwiddis).
* [#256](https://github.com/oshi/oshi/pull/256): Use DeviceID to link Windows Disks and Partitions - [@dbwiddis](https://github.com/dbwiddis).

3.1.1 (8/5/2016)
================
* [#239](https://github.com/oshi/oshi/pull/239): Fix exceptions on windows disks/partitions - [@dbwiddis](https://github.com/dbwiddis).
* [#240](https://github.com/oshi/oshi/pull/240): Check sysfs for Linux system serial number - [@dbwiddis](https://github.com/dbwiddis).

3.1 (8/1/2016)
================
* [#225](https://github.com/oshi/oshi/pull/225): Bugfixes from Coverity, FindBugs, and PMD - [@dbwiddis](https://github.com/dbwiddis).
* [#229](https://github.com/oshi/oshi/pull/229): Solaris port - [@dbwiddis](https://github.com/dbwiddis).
* [#232](https://github.com/oshi/oshi/pull/232): FreeBSD port - [@dbwiddis](https://github.com/dbwiddis).
* [#234](https://github.com/oshi/oshi/pull/234): Add read/write count and active disk time to HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).
* [#235](https://github.com/oshi/oshi/pull/235): Add partition information to HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).

3.0 (7/1/2016)
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

2.6.2 (6/21/2016)
================
* [#199](https://github.com/oshi/oshi/pull/199): Use WMI queries for raw data instead of maintaining PDH threads - [@dbwiddis](https://github.com/dbwiddis).
* Multiple efficiency improvements

2.6-java7 / 2.6.1 (6/17/2016)
================
* [#190](https://github.com/oshi/oshi/pull/190): Add VendorID and ProductID to UsbDevice - [@dbwiddis](https://github.com/dbwiddis).
* [#193](https://github.com/oshi/oshi/pull/193): Add read/write to Windows and OS X HWDiskStores - [@dbwiddis](https://github.com/dbwiddis).
* [#194](https://github.com/oshi/oshi/pull/194): Add volume name to OSFileStores - [@henryx](https://github.com/henryx).
* [#195](https://github.com/oshi/oshi/pull/195): Fixed reading multiple (in particular external) displays on Mac - [@dpagano](https://github.com/dpagano).
* [#197](https://github.com/oshi/oshi/pull/197): Add UUID to OSFileStores - [@dbwiddis](https://github.com/dbwiddis).
* [#198](https://github.com/oshi/oshi/pull/198): macOS 10.12 (Sierra) - [@dbwiddis](https://github.com/dbwiddis).
* Created a Java 7 backport using threeten.org dependency released as 2.6-m-java7

2.5.1-java7 / 2.5.2 (6/9/2016)
================
* [#186](https://github.com/oshi/oshi/pull/186), [#187](https://github.com/oshi/oshi/pull/187),  [#188](https://github.com/oshi/oshi/pull/188): Improve USB device trees - [@dbwiddis](https://github.com/dbwiddis).
* Created a Java 7 backport using threeten.org dependency released as 2.5.1-java7

2.5 (6/4/2016)
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

2.4 (5/2/2016)
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

2.3 (4/14/2016)
================
* [#124](https://github.com/oshi/oshi/pull/124): Read Windows Sensor info from Open Hardware Monitor as fallback - [@dbwiddis](https://github.com/dbwiddis).
* [#129](https://github.com/oshi/oshi/pull/129): Improve Linux version and code name parsing - [@dbwiddis](https://github.com/dbwiddis).
* [#132](https://github.com/oshi/oshi/pull/132), [#133](https://github.com/oshi/oshi/pull/133): Fix NPEs on Raspberry Pi - [@pcollaog](https://github.com/pcollaog).
* [#136](https://github.com/oshi/oshi/pull/136): Updated CPU load average to use system call - [@henryx](https://github.com/henryx).
* [#137](https://github.com/oshi/oshi/pull/137): Added iowait and irq ticks - [@dbwiddis](https://github.com/dbwiddis).

2.2 (3/1/2016)
================
* [#121](https://github.com/oshi/oshi/pull/121): Added CPU temperature, fan speeds, and voltage - [@dbwiddis](https://github.com/dbwiddis).
* [#123](https://github.com/oshi/oshi/pull/123): Handle JSON nulls - [@dbwiddis](https://github.com/dbwiddis).

2.1.2 (2/24/2016)
================
* [#118](https://github.com/oshi/oshi/pull/118): Port JSON to javax.json - [@dbwiddis](https://github.com/dbwiddis).
* [#120](https://github.com/oshi/oshi/pull/120): Support all windows processor methods - [@dbwiddis](https://github.com/dbwiddis).

2.1.1 (2/19/2016)
================
* [#114](https://github.com/oshi/oshi/pull/114): Memory information wasn't updating for Windows - [@dbwiddis](https://github.com/dbwiddis).
* [#116](https://github.com/oshi/oshi/pull/116): Added JSON output - [@dbwiddis](https://github.com/dbwiddis).

2.1 (1/1/2016)
================
* [#108](https://github.com/oshi/oshi/pull/108): Added Display info from EDID - [@dbwiddis](https://github.com/dbwiddis).
* [#111](https://github.com/oshi/oshi/pull/111): Catch exceptions when Linux c library missing - [@dbwiddis](https://github.com/dbwiddis).

2.0 (11/28/2015)
================
* [#101](https://github.com/oshi/oshi/pull/101): Refactored package structure for consistency - [@dbwiddis](https://github.com/dbwiddis).
* [#103](https://github.com/oshi/oshi/pull/103): Switched CentralProcessor to a single object for all processors - [@dbwiddis](https://github.com/dbwiddis).
* See [UPGRADING.md](UPGRADING.md) for more details.

1.5.2 (11/23/2015)
================
* [#98](https://github.com/oshi/oshi/pull/98): Upgraded JNA to 4.2.1 - [@dbwiddis](https://github.com/dbwiddis).
* [#100](https://github.com/oshi/oshi/pull/100): Add physical and logical CPU counts - [@dbwiddis](https://github.com/dbwiddis).

1.5.1 (10/15/2015)
================
* [#94](https://github.com/oshi/oshi/pull/94): Upgraded JNA to 4.2.0 - [@dbwiddis](https://github.com/dbwiddis).
* [#96](https://github.com/oshi/oshi/pull/96): Read buffer immediately after Runtime.exec to prevent deadlock - [@dbwiddis](https://github.com/dbwiddis).
* [#97](https://github.com/oshi/oshi/pull/97): Add system serial number - [@dbwiddis](https://github.com/dbwiddis).

1.5 (9/21/2015)
================
* [#87](https://github.com/oshi/oshi/pull/87): Added SLF4J logging, changed exception throwing to log errors to be robust to lack of permissions - [@dbwiddis](https://github.com/dbwiddis).

1.4 (9/3/2015)
================
* [#71](https://github.com/oshi/oshi/pull/71), 
  [#72](https://github.com/oshi/oshi/pull/72): Added support for Windows 10 & Windows Server 2016 - [@laurent-r](https://github.com/laurent-r).
* [#75](https://github.com/oshi/oshi/pull/75): Added uptime information - [@dbwiddis](https://github.com/dbwiddis).
* [#76](https://github.com/oshi/oshi/pull/76): Better linux CPU processor counting - [@dbwiddis](https://github.com/dbwiddis).
* [#78](https://github.com/oshi/oshi/pull/78): Execute FileSystemView on Swing's Event Dispatch Thread - [@dbwiddis](https://github.com/dbwiddis).

1.3 (6/27/2015)
================
* Upgraded to Java 7 base support
* Upgraded JNA to 4.1.0
* Brought over lessons learned from [waffle](https://github.com/Waffle/waffle) for building project from source.
* [#50](https://github.com/oshi/oshi/pull/50): Added file store information - [@dbwiddis](https://github.com/dbwiddis).
* [#51](https://github.com/oshi/oshi/pull/51): Added CPU Ticks and switched to OperatingSystemMXBean for CPU load / load average - [@dbwiddis](https://github.com/dbwiddis).
* [#62](https://github.com/oshi/oshi/pull/62): Added Per-Processor CPU Load and Ticks - [@dbwiddis](https://github.com/dbwiddis).

1.2 (6/13/2014)
================

* Added TODO list and enhanced README documentation - [@ptitvert](https://github.com/ptitvert)
* Added Travis-CI - [@dblock](https://github.com/dblock).
* [#3](https://github.com/oshi/oshi/pull/3): Mavenized project - [@le-yams](https://github.com/le-yams).
* [#5](https://github.com/oshi/oshi/pull/5): Added Linux support - [@ptitvert](https://github.com/ptitvert).
* [#7](https://github.com/oshi/oshi/pull/7): Added Mac OS X Support - [@ptitvert](https://github.com/ptitvert).
* [#13](https://github.com/oshi/oshi/pull/13): Support for Windows 8.1 and Windows Server 2008 R2 - [@NagyGa1](https://github.com/NagyGa1).
* [#15](https://github.com/oshi/oshi/pull/15), [#18](https://github.com/oshi/oshi/pull/18): Added support for CPU load - [@kamenitxan](https://github.com/kamenitxan), [@Sorceror](https://github.com/Sorceror).
* [#25](https://github.com/oshi/oshi/pull/25), [#29](https://github.com/oshi/oshi/pull/29): Included inactive/reclaimable memory amount in GlobalMemory#getAvailable on Mac/Linux - [@dbwiddis](https://github.com/dbwiddis).
* [#27](https://github.com/oshi/oshi/pull/27): Replaced all Mac OS X command line parsing with JNA or System properties - [@dbwiddis](https://github.com/dbwiddis).
* [#30](https://github.com/oshi/oshi/pull/30): Added processor vendor frequency information - [@alessiofachechi](https://github.com/alessiofachechi).
* [#32](https://github.com/oshi/oshi/pull/32): Added battery state information - [@dbwiddis](https://github.com/dbwiddis).

1.1 (10/13/2013)
================

* Added support for Windows 8 to `oshi.software.os.OperatingSystemVersion`, `oshi.software.os.windows.nt.OSVersionInfoEx` - [@laurent-r](https://github.com/laurent-r).

1.0 (6/23/2010)
===============

* Initial public release - [@dblock](https://github.com/dblock).
