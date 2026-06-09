/**
 * JNA-based native implementation of the OSHI API for JDK 8+.
 * <p>
 * This module uses <a href="https://github.com/java-native-access/jna">JNA</a> for native access and supports all OSHI
 * platforms (Windows, macOS, Linux, Android, DragonFly BSD, FreeBSD, NetBSD, OpenBSD, Solaris, AIX).
 * <p>
 * Usage:
 *
 * <pre>{@code
 * SystemInfoProvider si = SystemInfoFactory.create();
 * HardwareAbstractionLayer hal = si.getHardware();
 * OperatingSystem os = si.getOperatingSystem();
 * }</pre>
 *
 * Re-using the same {@link oshi.SystemInfo} instance optimizes caching and CPU performance; creating a new instance
 * minimizes memory usage.
 *
 * @see oshi.SystemInfo
 */
module com.github.oshi {
    // API
    exports oshi;
    exports oshi.util.gpu;
    exports oshi.util.platform.mac;
    exports oshi.util.platform.unix.freebsd;
    exports oshi.util.platform.unix.dragonflybsd;
    exports oshi.util.platform.unix.openbsd;
    exports oshi.util.platform.unix.solaris;
    exports oshi.util.platform.windows;

    provides oshi.spi.SystemInfoProvider with oshi.SystemInfo;

    // JNA needs reflective access to Structure and ByReference subclasses
    opens oshi.jna to com.sun.jna;
    opens oshi.jna.common to com.sun.jna;
    opens oshi.jna.platform.linux to com.sun.jna;
    opens oshi.jna.platform.mac to com.sun.jna;
    opens oshi.jna.platform.windows to com.sun.jna;
    opens oshi.jna.platform.unix to com.sun.jna;

    // dependencies
    requires transitive com.github.oshi.common;
    requires transitive com.sun.jna;
    requires transitive com.sun.jna.platform;
    requires transitive java.desktop;
    requires org.slf4j;
}
