/**
 * FFM-based native implementation of the OSHI API for JDK 25+.
 * <p>
 * This module uses the Foreign Function and Memory (FFM) API for native access and currently supports Windows, macOS,
 * Linux, FreeBSD, OpenBSD, and Solaris (illumos). For broader platform support (DragonFly BSD, NetBSD, AIX), use the
 * JNA-based {@code com.github.oshi} module.
 * <p>
 * Usage:
 *
 * <pre>{@code
 * SystemInfoProvider si = SystemInfoFactory.create();
 * HardwareAbstractionLayer hal = si.getHardware();
 * OperatingSystem os = si.getOperatingSystem();
 * }</pre>
 *
 * Re-using the same {@link oshi.ffm.SystemInfo} instance optimizes caching and CPU performance; creating a new instance
 * minimizes memory usage.
 *
 * @see oshi.ffm.SystemInfo
 */
module com.github.oshi.ffm {
    // API
    exports oshi.ffm;
    exports oshi.ffm.util;
    exports oshi.ffm.util.gpu;
    exports oshi.ffm.util.platform.mac;
    exports oshi.ffm.util.platform.unix.freebsd;
    exports oshi.ffm.util.platform.unix.openbsd;
    exports oshi.ffm.util.platform.unix.solaris;
    exports oshi.ffm.util.platform.windows;

    provides oshi.spi.SystemInfoProvider with oshi.ffm.SystemInfo;

    // dependencies
    requires transitive com.github.oshi.common;
    requires transitive java.desktop;
    requires org.slf4j;
}
