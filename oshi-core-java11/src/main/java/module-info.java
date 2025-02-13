/**
 * This module provides access to the OSHI API and utility functions.
 */
module com.github.oshi {
    // API
    exports oshi;
    exports oshi.hardware;
    exports oshi.software.os;
    exports oshi.util;

    // JNA needs reflective access to Structure and ByReference subclasses
    opens oshi.jna to com.sun.jna;
    opens oshi.jna.platform.linux to com.sun.jna;
    opens oshi.jna.platform.mac to com.sun.jna;
    opens oshi.jna.platform.windows to com.sun.jna;
    opens oshi.jna.platform.unix to com.sun.jna;

    // dependencies
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires transitive java.desktop;
    requires io.github.pandalxb.jlibrehardwaremonitor.config;
    requires io.github.pandalxb.jlibrehardwaremonitor.manager;
    requires io.github.pandalxb.jlibrehardwaremonitor.model;
    requires org.apache.commons.collections4;
    requires org.slf4j;
}
