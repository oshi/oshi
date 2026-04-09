/**
 * This module provides the common OSHI API interfaces, abstract base classes, annotations, and utilities shared by all
 * OSHI implementations.
 */
module com.github.oshi.common {
    exports oshi.annotation;
    exports oshi.annotation.concurrent;
    exports oshi.hardware;
    exports oshi.software.os;
    exports oshi.util;
    exports oshi.util.tuples;

    requires transitive java.desktop;
    requires org.slf4j;
}
