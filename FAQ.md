What is the intended use of the oshi-core API?
========
Users should create a new instance of [SystemInfo](http://dblock.github.io/oshi/apidocs/oshi/SystemInfo.html).  This provides access to the platform-specific hardware and software interfaces using the respective `get*()` methods.  The interfaces in `oshi.hardware` and `oshi.software.os` provide cross-platform functionality.

Methods return a "snapshot" of current levels.  To display values which change over time, it is intended that users poll for information no more frequently than one second. Disk and file system calls may incur some latency and should be polled less frequently.

What is the intended use of the oshi-json API?
========
Users should create a new instance of [SystemInfo](http://dblock.github.io/oshi/apidocs/oshi-json/SystemInfo.html) as in `oshi-core` and then use either the `toCompactJSON()` or `toPretyJSON()` methods as appropriate.  This provides access to the platform-specific JSON objects under the JSON object tree.

The `to*JSON()` methods take an optional `java.util.Properties` object as a parameter to filter or control the output.  Users may use the provided `loadProperties()` method in the `PropertiesUtil` class to load a properties file from the classpath, or generate their own properties programmatically.  The property values will correspond exactly to the JSON object tree, e.g., `hardware.processor` corresponds to the JSON tree's `hardware` attribute, an object with a `processor` attribute. Setting these attributes to `false` will suppress output of that attribute (and its children, if applicable).  A [sample configuration file](https://github.com/dblock/oshi/blob/master/oshi-json/src/test/resources/oshi.json.properties) is provided.

Objects retain the properties, so future calls to `to*JSON()` methods with no argument will use the same properties as previously provided. Passing a `new Properties()` object will reset the JSON output to non-filtered.

Is the API backwards compatible?
========
The interfaces and classes in `oshi.hardware` and `oshi.software.os` are considered the OSHI API and are guaranteed to be compatible with the same major version.  Differences between major versions can be found in the [UPGRADING.md](UPGRADING.md) document.  

Most, if not all, of the platform-specific implementations of these APIs in lower level packages will remain the same as well, although it is not intended that users access platform-specific code and some changes may occur between minor versions. Supporting code in the `oshi.util` package may, rarely, change between minor versions, usually associated with organizing package structure or changing parsing methods for efficiency/consistency/ease of use.

Code in the platform-specific `oshi.jna.*` packages is intended to be temporary and will be removed when that respective code is included in the JNA project.

What minimum Java version is required?
========
Beginning with version 2.5, OSHI uses Java 8 for the improved date/time classes.  The 2.6-m-java7 version (using a dependency to the threeten.org backport) is the last Java 7 version.  Users are encouraged to update to Java 8.

Which operating systems are supported?
========
OSHI has been implemented and tested on the following systems.  Some features may work on earlier versions.
* Windows 7 and higher. 
* macOS (OS X) version 10.6 (Snow Leopard) and higher
* Linux (Most major distributions) Kernel 2.6 and higher

What API features are not implemented on some operating systems?
========
The following generally summarizes known exceptions. If you have missing data that is not on this list, please report it in an issue so we can investigate.
* Windows does not provide a load average, so the Processor's `getSystemLoadAverage()` returns -1.
* MacOS does not track time processors spend idle due to hard disk latency (iowait) or time spent processing hardware or software interrupts, and returns 0 for those associated tick values.
* Windows sensor (temperature, fans, voltage) readings are drawn from Microsoft's Windows Management Instrumentation (WMI) API; however, most hardware manufacturers do not publish these readings to WMI. If a value is not available through the Microsoft API, Oshi will attempt to retrieve values as published by the [Open Hardware Monitor](http://openhardwaremonitor.org/) if it is running.

Will you implement feature X?
========
Maybe!  If you can contribute all the code to implement the feature, it will almost certainly be added.  Even if you can't code but can provide pointers to where the information can be found cross-platform, your feature has a good chance. Otherwise, you can always submit an issue to ask, but are at the mercy of the developers' time, enthusiasm level, and the availability of documentation for the feature.
