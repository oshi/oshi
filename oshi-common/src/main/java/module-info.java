/**
 * Common OSHI API interfaces, abstract base classes, annotations, and utilities shared by all OSHI implementations.
 * <p>
 * This module contains <i>no native code</i> and is unaffected by <a href="https://openjdk.org/jeps/472">JEP 472</a>
 * restrictions on native access. It can be used standalone by applications that cannot or prefer not to enable native
 * access (e.g., in restricted JVM environments).
 * <p>
 * To build a native-free OSHI implementation:
 * <ol>
 * <li>Depend on this module only (Maven: {@code com.github.oshi:oshi-common}).</li>
 * <li>Extend the abstract base classes in {@code oshi.hardware.common} and {@code oshi.software.common}. For hardware,
 * subclass {@code AbstractHardwareAbstractionLayer} and override its {@code protected abstract create*()} factory
 * methods to return your platform-specific implementations. For software and individual components (e.g.,
 * {@code AbstractOperatingSystem}, {@code AbstractCentralProcessor}, {@code AbstractSensors}), override the
 * {@code protected abstract query*()} methods with platform-specific logic using command-line tools
 * ({@code oshi.util.ExecutingCommand}), {@code /proc} file parsing ({@code oshi.util.FileUtil},
 * {@code oshi.util.ProcUtil}), or other non-native techniques.</li>
 * </ol>
 */
module com.github.oshi.common {
    exports oshi.annotation;
    exports oshi.annotation.concurrent;
    exports oshi.driver.common.mac;
    exports oshi.driver.common.windows.perfmon;
    exports oshi.driver.common.windows.registry;
    exports oshi.hardware;
    exports oshi.hardware.common;
    exports oshi.hardware.common.platform.linux;
    exports oshi.hardware.common.platform.mac;
    exports oshi.hardware.common.platform.unix;
    exports oshi.hardware.common.platform.windows;
    exports oshi.software.common;
    exports oshi.software.common.os.linux;
    exports oshi.software.common.os.mac;
    exports oshi.software.common.os.windows;
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
