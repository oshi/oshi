module com.github.oshi {
    // API
    exports oshi;
    exports oshi.hardware;
    exports oshi.software.os;
    exports oshi.util;

    // JNA needs reflective access to Structure subclasses
    opens oshi.jna.platform.linux to com.sun.jna;
    opens oshi.jna.platform.mac to com.sun.jna;
    opens oshi.jna.platform.windows to com.sun.jna;
    opens oshi.jna.platform.unix to com.sun.jna;

    // dependencies
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires transitive java.desktop;
    requires org.slf4j;
}
