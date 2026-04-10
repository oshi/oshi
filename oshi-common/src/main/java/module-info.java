/**
 * This module provides the common OSHI API interfaces, abstract base classes, annotations, and utilities shared by all
 * OSHI implementations.
 */
module com.github.oshi.common {
    exports oshi.annotation;
    exports oshi.annotation.concurrent;
    exports oshi.hardware;
    exports oshi.hardware.common;
    exports oshi.software.common;
    exports oshi.software.os;
    exports oshi.util;
    exports oshi.util.driver.linux;
    exports oshi.util.driver.linux.proc;
    exports oshi.util.driver.unix;
    exports oshi.util.linux;
    exports oshi.util.tuples;

    requires transitive java.desktop;
    requires org.slf4j;
}
