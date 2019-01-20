
# Guide to upgrading from OSHI 3.x to 4.x

OSHI 4.0 requires minimum Java 8 compatibility.

The `oshi-json` artifact has been completely removed. It is trivial to obtain
JSON output using [Jackson's ObjectMapper](http://www.mkyong.com/java/
jackson-2-convert-java-object-to-from-json/).

There is a new `oshi-demo` artifact which will contain many "how to" classes
to demonstrate OSHI's capabilities and integration with other libraries.

## API Changes

There is a new `VirtualMemory `class which is accessible with a getter from 
`GlobalMemory`.  Methods associated with swap file usage were moved to this
new class.

Several changes in the API highlight which attributes do not change and which
fetch dynamic information, as well as highlight operations with latency or
expensive computations.  In general the following rules are followed:
 - getX() (and isX() for boolean) are lazy getters for the initial data
query, and will store the value in an attribute, returning that same value on
subsequent calls.  When relevant, an updateAtrributes() method will be 
available to cause the getters to return updated values.
 - queryX() will get the latest value and typically identify more expensive
 (in cpu or time) methods.

The following getX() methods are now queryX():
 - TBD

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
