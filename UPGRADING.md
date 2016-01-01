# Guide to upgrading from Oshi 1.x to 2.x

Even though it's a major release, Oshi 2.0 functionality is identical to
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
