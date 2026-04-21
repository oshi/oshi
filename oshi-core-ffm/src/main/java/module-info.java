/**
 * This module provides access to the OSHI API and utility functions using the Foreign Function and Memory (FFM) API.
 */
module com.github.oshi.ffm {
    // API
    exports oshi;
    exports oshi.ffm;
    exports oshi.ffm.util;
    exports oshi.util.gpu;
    exports oshi.util.platform.mac;
    exports oshi.util.platform.windows;

    // dependencies
    requires transitive com.github.oshi.common;
    requires transitive java.desktop;
    requires org.slf4j;
}
