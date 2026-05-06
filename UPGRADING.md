# Guide to upgrading from OSHI 6.x to 7.x

## Configuration property key changes (7.1.0)

Three property keys have been renamed to use consistent all-lowercase naming, enabling environment variable configuration:

| Old key | New key |
|---------|---------|
| `oshi.os.unix.whoCommand` | `oshi.os.unix.whocommand` |
| `oshi.os.solaris.allowKstat2` | `oshi.os.solaris.allowkstat2` |
| `oshi.os.linux.sensors.cpuTemperature.types` | `oshi.os.linux.sensors.cputemperature.types` |

If you reference these keys in your `oshi.properties` file, update them to the new lowercase names. Old camelCase keys (`whoCommand`, `allowKstat2`, `cpuTemperature.types`) will be silently ignored if present. The Java constant names (`OSHI_OS_UNIX_WHOCOMMAND`, `OSHI_OS_SOLARIS_ALLOWKSTAT2`) are unchanged.

## Deprecated method and class removal

### Process memory: `getResidentSetSize()` replaced by two methods

The deprecated `OSProcess.getResidentSetSize()` method has been removed. It has been replaced by two distinct methods:

- `getResidentMemory()` — the true RSS value including shared libraries, matching `ps` and `top`.
- `getPrivateResidentMemory()` — the private working set / footprint value displayed by graphical system monitors (Windows Task Manager, macOS Activity Monitor, GNOME System Monitor).

On Windows, the old `getResidentSetSize()` returned the Private Working Set. To preserve that behavior, use `getPrivateResidentMemory()`. On all other platforms, use `getResidentMemory()` for the equivalent value.

### `PlatformEnum` moved to `oshi.util` package

The `oshi.PlatformEnum` class in `oshi-core` and the `oshi.PlatformEnumFFM` class in `oshi-core-ffm` have been removed. Use `oshi.util.PlatformEnum` (in the `oshi-common` module) instead. This class is available to all modules without requiring JNA or FFM.

The deprecated `SystemInfo.getCurrentPlatform()` (JNA) and `SystemInfoFFM.getCurrentPlatform()` (FFM) methods have also been removed. Use `oshi.util.PlatformEnum.getCurrentPlatform()` directly.

### `SystemInfoFFM` removed

The deprecated `oshi.SystemInfoFFM` class has been removed. Use `oshi.ffm.SystemInfo` as the FFM entry point.

### Misspelled `GlobalConfig` constants removed

The following misspelled constants in `GlobalConfig` have been removed:

| Removed | Replacement |
|---|---|
| `OSHI_OS_WINDOWS_PERFDISK_DIABLED` | `OSHI_OS_WINDOWS_PERFDISK_DISABLED` |
| `OSHI_OS_WINDOWS_PERFOS_DIABLED` | `OSHI_OS_WINDOWS_PERFOS_DISABLED` |
| `OSHI_OS_WINDOWS_PERFPROC_DIABLED` | `OSHI_OS_WINDOWS_PERFPROC_DISABLED` |

## Merge of oshi-core-java11 into oshi-core

The separate `oshi-core-java11` artifact has been merged into `oshi-core`, which now includes a `module-info.class` directly (compiled at Java 9, with all other classes at Java 8). Users of `oshi-core-java11` should switch to `oshi-core`.

### Module path behavior change

In OSHI 6.x, `oshi-core` declared an `Automatic-Module-Name` of `com.github.oshi` in its manifest but had no module descriptor. In 7.x, `oshi-core` contains a full `module-info.class`. This means:

- On the module path, `oshi-core` is now a proper named module with explicit exports and `opens` directives for JNA reflective access.
- If you previously relied on classpath behavior (e.g., accessing non-exported internal packages), you may need to add `--add-opens` flags or switch to the classpath.

To force classpath behavior (e.g., in Maven Surefire):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <useModulePath>false</useModulePath>
    </configuration>
</plugin>
```

## FFM Module Artifact and JPMS Rename

The FFM module artifact has been renamed from `oshi-core-java25` to `oshi-core-ffm`.

The JPMS module name for the FFM module has changed from `com.github.oshi` to `com.github.oshi.ffm`. Update your `module-info.java`:

```java
requires com.github.oshi.ffm;
```

# Guide to upgrading from OSHI 5.x to 6.x

OSHI 6.0.0 is functionally equivalent to 5.8.7, with minor API updates as noted below.

## API Changes

### Deprecated method removal

The deprecated methods `getProcesses(int limit, ProcessSort sort)`, `getChildProcesses(int parentPid, int limit, ProcessSort sort)`, and the enum `ProcessSort` were removed, replaced by methods leveraging constants in the `ProcessSorting` class.

The deprecated method `getCurrentPlatformEnum()` was removed from `SystemInfo` and the deprecated `MACOSX` value has been removed from `PlatformEnum`.

### Changed return values/types

The value returned from `MacOSProcess` and `LinuxOSProcess` method `getCommandLine()` has been changed from null-delimited string to space-delimited string. Due to potential truncation on some operating systems, parsing this method should not be relied upon. The `getArguments()` method is the preferred alternative to obtain a list of the command line arguments.

The method `getServices()` now returns a `List<OSService>` rather than an array.

The value returned from `NetworkIF` method `getMTU()` has been changed from an `int` to a `long`. This impacts Windows users who had special treatment of local interfaces which previously matched the JDK's `NetworkInterface` return of a 32-bit -1 value. This value is now a long
representing the max unsigned integer value.

# Guide to upgrading from OSHI 4.x to 5.x

OSHI 5.0.0-5.1.2 releases are functionally equivalent to 4.7.0-4.8.2 releases,
with three categories of changes supporting full thread safety:
* Remove setters from API.
* Change getters which return arrays of objects to return unmodifiable lists.
* Remove deprecated code.

For users of JPMS, OSHI 5.2.0 and 4.9.0 were released with an Automatic Module Name of `com.github.oshi`,
which may differ from the `oshi.core` module name previously resolved automatically.

## API Changes

### Setter removal

The `NetworkIF`, `HWDiskStore`, `OSFileStore`, and `OSProcess` classes are now interfaces, with setters removed.

The `HWPartition` class is now immutable, with setters removed.

### UnmodifiableList return types

The `HardwareAbstractionLayer` methods `getNetworkIFs()`, `getDisks()`, `getPowerSources()`,
`getDisplays()`, `getSoundCards()`, `getGraphicsCards()`, `getUsbDevices()`,
the `HWDiskstore` method `getPartitions()`, the `FileSystem` method `getFileStores()`,
the `GlobalMemory` method `getPhysicalMemory()`, the `OperatingSystem` methods for `getProcesses()`,
the `CentralProcessor` method `getLogicalProcessors()`, and the `UsbDevice` method `getConnectedDevices()`
now return an `UnmodifiableList` instead of an array.

### Method changes

The `OperatingSystem` methods fetching `OSProcess` information using a `slowFields` boolean have been removed,
as the behavior they enabled is now done by default. Windows users should note the addition of a configurable
parameter optionally allowing WMI caching for improved performance of `OsProcess#getCommandLine`.

The `OperatingSystem` method `getProcessAffinityMask()` is now on the `OSProcess` object as `getAffinityMask()`.

The `OSFileStore` method `updateAtrributes()` is now spelled correctly as `updateAttributes()`.

### Deprecated method removal

The deprecated `OperatingSystemVersion` interface and its getter `OperatingSystem`.`getVersion` were removed.
Its `getVersion()`, `getCodeName()`, and `getBuildNumber()` methods are available on the object returned
from the method `getVersionInfo()`.

The deprecated `CentralProcessor` methods `getVendor()`, `getName()`, `getFamily()`, `getModel()`,
`getStepping()`, `getProcessor()`, `getIdentifier()`, `getVendorFreq()`, and `isCpu64bit` were removed.
The equivalent methods are available on the object returned from `getProcessorIdentifier()`.

The deprecated `PowerSource` methods `getRemainingCapacity()` and `getTimeRemaining()` were removed,
replaced by `getRemainingCapacityPercent()` and `getTimeRemainingEstimated()`.

The deprecated `OSProcess` method `calculateCpuPercent()` was removed, replaced by `getProcessCpuLoadCumulative()`.

# Guide to upgrading from OSHI 3.x to 4.x

OSHI 4.0 requires minimum Java 8 compatibility.

The `oshi-json` artifact has been completely removed. It is trivial to obtain JSON output using the
[Jackson ObjectMapper](https://www.mkyong.com/java/jackson-2-convert-java-object-to-from-json/).

There is a new `oshi-demo` artifact which will contain many "how to" classes
to demonstrate OSHI's capabilities and integration with other libraries. These classes are intended
as proof-of-concept only, and are not intended for production use.

## API Changes

`NetworkIF#getNetworkInterface()` is now `queryNetworkInterface()` to prevent
Jackson's ObjectMapper from attempting to serialize the returned object.

There is a new `VirtualMemory `class which is accessible with a getter from
`GlobalMemory`.  Methods associated with swap file usage were moved to this
new class.

The `CentralProcessor` setters were removed from the API. The methods
`getSystemCpuLoadBetweenTicks()` and `getProcessorCpuLoadBetweenTicks()` now take
an argument with the previous set of ticks, rather than internally saving the
previous call. This enables users to measure over a longer period or multiple
different periods.  The `getSystemCpuLoad()` method has been removed; users
running the Oracle JVM should use the  `OperatingSystemMXBean` method if
they desire this value.  The no-argument `getSystemLoadAverage()` has been
removed; users can call with an argument of 1 to obtain the same value.

The `getSystemUptime()` method was moved from the `CentralProcessor` class to
the `OperatingSystem` class.

The `NetworkIF#updateNetworkStats()` and `HWDiskStore#updateDiskStats()` methods
were renamed to `updateAttributes()` to conform to other similarly named methods
to permit update of individual elements of arrays.

# Guide to upgrading from OSHI 2.x to 3.x

The most significant change in OSHI 3.0 is the separation of JSON output to a
separate artifact, filtering output using configurable properties. Users of
`oshi-core` who do not require JSON will find most of the API the same except
as noted below.  Those who use JSON will find improved functionality in the
`oshi-json` module.

## API Changes - oshi-core

The `CentralProcessor`'s `getSystemIOWaitTicks()` and `getSystemIrqTicks()`
methods were removed. The `getSystemCpuLoadTicks()` now include the IOWait and
IRQ tick information previously reported by those methods, although Idle time
no longer includes IOWait, and System Time no longer includes IRQ ticks. The
`getProcessorCpuLoadTicks()` now includes per-processor IOWait and IRQ tick
information.

The `getFileSystem()` method was moved from the `HardwareAbstractionLayer` to
the `OperatingSystem`.  The `getFileStores()` method on the
`HardwareAbstractionLayer` was redundant with the same method on the
`FileSystem` class and was removed.

The `OSProcess` methods (`getProcesses()`, `getProcess()`, `getProcessId()`,
`getProcessCount()`, and `getThreadCount()`) were moved from the
`CentralProcessor` to the `OperatingSystem`.

The (`OperatingSystem`'s) `getProcesses()` method now takes two arguments, to
limit the number of results returned, and to sort the results.

The `HardwareAbstractionLayer`'s `getUsbDevices()` method now takes a boolean
argument which offers both the existing tree-based or a flat list format.

The `Networks` interface had an `updateNetworkStats()` method that was not
reachable from cross-platform code. That method is now on the `NetworkIF`
class.

## API Changes - oshi-json

Decorator classes for the OSHI API which enable JSON functionality are now in
the `oshi.json.*` packages with the same API changes in the previous section.

JSON objects associated with the above method changes were updated:
 - `systemCpuLoadTicks` and `processorCpuLoadTicks` now have 7-element arrays
 instead of 4, and the `systemIOWaitTicks` and `systemIrqTicks` elements have
 been removed from the `processor` object.
 - `fileSystem` is now an element of `operatingSystem` rather than `processor`.
 - `fileStores` is now an element of `fileSystem` rather than `processor`.
 - `processID`, `processCount`, `threadCount`, and `processes` are now
 elements of `operatingSystem` rather than `processor`.

While the existing `toJSON()` method remains and is backwards compatible, the
new API permits using a `java.util.Properties` object as an optional parameter
which will be persistent to future (no argument) calls to that method until
replaced.  See the [FAQ](FAQ.md) for more information.

# Guide to upgrading from OSHI 1.x to 2.x

Even though it's a major release, OSHI 2.0 functionality is identical to
1.5.2.  For the most part, the highest level APIs demonstrated in the test
classes have remained the same, except as documented below.  Several lower
level packages and classes have been moved and/or renamed.

## Package Changes

New packages `oshi.jna.platform.*` were created and code which extends
`com.sun.jna.Library` was moved into its respective platform library.  These
classes should be considered non-API as they may be removed if/when their
code is incorporated into the JNA project.

New packages `oshi.hardware.platform.*` were created and contain the
platform-specific implementations of interfaces in `oshi.hardware`, with
implementing classes renamed to prepend the platform name to the interface
name.  Similar renaming was done for implementations of `oshi.software.os`
in the respective `oshi.software.os.*` packages.

## API Changes

The `Memory` interface was renamed `GlobalMemory` to avoid name conflict with
JNA's `Memory` class.

The `Processor` interface, which represented one of an array of logical
processor objects, was renamed `CentralProcessor` and represents the entire
System CPU which may contain multiple logical processors.  Methods applicable
to an individual logical processor were modified to return arrays.

The `HardwareAbstractionLayer`'s `getProcessors()` method was renamed to
`getProcessor()` and now returns a singular `CentralProcessor` object.

Specific changes to `CentralProcessor` methods:
* The constructor no longer takes a processor number argument and the
`getProcessorNumber()` method was removed
* The deprecated `getLoad()` method was removed. Use
`getSystemCpuLoadBetweenTicks()`.
* The `getProcessorCpuLoadBetweenTicks()` method now returns an array of
load values, one value for each logical processor.
* The `getProcessorCpuLoadTicks()` method now returns a two dimensional
array; one array per logical processor, each containing the tick values
previously returned for a single processor.
