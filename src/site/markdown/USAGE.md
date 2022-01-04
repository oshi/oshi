Installation
-------------------

Include OSHI and its dependencies on your classpath. We strongly recommend using [Maven/Gradle](https://search.maven.org/artifact/com.github.oshi/oshi-core/6.0.0/jar)

*Note: OSHI uses the latest version of [JNA](https://github.com/java-native-access/jna).

If you experience a `NoClassDefFoundError` or `NoSuchMethodError` issues with JNA artifacts, you likely have
an older version of either `jna` or `jna-platform` in your classpath from a transitive dependency on another project.
Consider one or more of the following steps to resolve the conflict:
- Listing OSHI earlier (or first) in your dependency list
- Specifying the most recent version of JNA (both `jna` and `jna-platform` artifacts) in your `pom.xml` as dependencies.
- If you are using the Spring Boot Starter Parent version 2.2 and earlier that includes JNA as a dependency:
    - Upgrade to version 2.3 which does not have a JNA dependency (preferred)
    - If you must use version 2.2 or earlier, override the `jna.version` property to the latest JNA version.


Usage
-------------------

[See basic use of OSHI](https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java)

Using OSHI is as simple as creating an instance of [SystemInfo](https://oshi.github.io/oshi/oshi-core/apidocs/oshi/SystemInfo.html)

```java
SystemInfo systemInfo = new SystemInfo();
```

Operating System
-------------------

Using previous instance of system info to retrieve the [operating system](https://oshi.github.io/oshi/oshi-core/apidocs/oshi/software/os/OperatingSystem.html)

Provides cross-platform implementation of OS, FileSystem, Process information
```java
OperatingSystem os = si.getOperatingSystem();
```

Hardware
-------------------

Using previous instance of system info to retrieve the [hardware abstraction](https://oshi.github.io/oshi/oshi-core/apidocs/oshi/hardware/HardwareAbstractionLayer.html)

Provides access to hardware items (processors/memory/battery/disks)
```java
HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
```

Further Read
-------------------

See the [PERFORMANCE](PERFORMANCE.md) document for general CPU/Memory tradeoffs and specific Windows (WMI) recommendations depending upon your application.

See the [FAQ](FAQ.md) document for common implementation and calculation questions.

Some settings are configurable in the [`oshi.properties`](https://github.com/oshi/oshi/blob/master/oshi-core/src/main/resources/oshi.properties) file, which may also be manipulated using the [`GlobalConfig`](https://oshi.github.io/oshi/apidocs/oshi/util/GlobalConfig.html) class. This should be done at startup, as configuration is not thread-safe and OSHI does not guarantee re-reading the configuration during operation.

The `oshi-demo` artifact includes [several proof-of-concept examples](https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/) of using OSHI to obtain information, including a basic Swing GUI.
