# OSHI Performance Considerations


## CPU/memory trade-offs

OSHI avoids aching large amount of information, leaving caching to the user.  Limited use of caching is employed in many classes using Memoized suppliers in instance fields, thus avoiding repeated operating system calls.

Users with memory constraints can ensure the existing cached information is disposed of by using a new instance of `SystemInfo` and the subordinate classes when collecting data.

## Updating statistics on objects in a list

Many of the individual objects returned by lists, such as `OSProcess`, `NetworkIF`, `OSFileStore`, and others, have an `updateAttributes()` method that operates only on that object. These are intended for use primarily if that individual process is the only one being monitored/updated.  In many cases, the entire list must be queried to provide the information, so users updating multiple objects in a list should simply re-query the entire list once and then correlate the new set of results to the old ones in their own application.

## Windows process command lines

On Windows, process command lines are only available in WMI and require a significant WMI overhead unless OSHI is running with elevated
permissions or the process is owned by the same user.

By default, command lines are not pre-fetched in `OSProcess` objects, and are populated by a single query for each process, assuming command line queries will be done ad-hoc by a user.

If your application requires updating more than a few command lines owned by other users, CPU performance can be significantly improved by fetching and caching the entire list of command line results. This must be enabled in the configuration file, or by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_COMMANDLINE_BATCH, true);` shortly after startup (at least before the first command line query).

## Windows suspended processes

On Windows, determining if a process is suspended requires querying the state of its threads.

By default, all Windows processes are presented as "Running". If your application requires knowing which processes are suspended, collecting the thread details works best for ad hoc requests. To query all processes, it is more efficient (but still slow) to query thread performance counters. This must be enabled in the configuration file, or by calling `GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, true);` shortly after startup (at least before the first instantiation of the Operating System class).

## Windows WMI query COM initialization

Because OSHI does not know whether COM has been initialized externally, each WMI query requires initializing and uninitializing COM.  Each WMI query's response time can be improved by about 3 to 5 milliseconds by having your application do the necessary COM initialization and extending OSHI's `WmiQueryHandler` with a version that omits that overhead.  The `UserComInit` and `WmiNoComInitQueryHandler` classes in the `oshi-demo` artifact demonstrate this capability, and can be further customized.

## Physical device latency

Updates of statistics on disks, filestores, USB devices, and some network information may incur physical device or network latency and respond more slowly than other native calls.  Periodic polling for updates should generally be less frequent than OS-kernel based statistics such as CPU and memory.
