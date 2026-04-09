/**
 * This module provides access to the OSHI API and utility functions.
 */
module com.github.oshi {
    // API
    exports oshi;
    exports oshi.util.gpu;
    exports oshi.util.platform.linux;
    exports oshi.util.platform.mac;
    exports oshi.util.platform.unix.freebsd;
    exports oshi.util.platform.unix.openbsd;
    exports oshi.util.platform.unix.solaris;
    exports oshi.util.platform.windows;

    // JNA needs reflective access to Structure and ByReference subclasses
    opens oshi.jna to com.sun.jna;
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
