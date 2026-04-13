/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayerJNA;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayerJNA;
import oshi.hardware.platform.unix.aix.AixHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.openbsd.OpenBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystemJNA;
import oshi.software.os.mac.MacOperatingSystemJNA;
import oshi.software.os.unix.aix.AixOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * System information. This is the main entry point to OSHI, using JNA for native access.
 * <p>
 * This object provides getters which instantiate the appropriate platform-specific implementations of
 * {@link OperatingSystem} (software) and {@link HardwareAbstractionLayer} (hardware).
 * <p>
 * Quick start:
 *
 * <pre>{@code
 * SystemInfo si = new SystemInfo();
 * HardwareAbstractionLayer hal = si.getHardware();
 * OperatingSystem os = si.getOperatingSystem();
 *
 * // CPU usage (blocks for 1 second)
 * double cpuLoad = hal.getProcessor().getSystemCpuLoad(1000L);
 *
 * // Memory
 * GlobalMemory mem = hal.getMemory();
 * long availableBytes = mem.getAvailable();
 * }</pre>
 *
 * Platform-specific Hardware and Software objects are retrieved via memoized suppliers and cached on the SystemInfo
 * instance. To conserve memory at the cost of additional processing time, create a new SystemInfo for subsequent calls.
 * To conserve processing time at the cost of additional memory usage, re-use the same instance.
 * <p>
 * This implementation uses <a href="https://github.com/java-native-access/jna">JNA</a> for native access and supports
 * all OSHI platforms (Windows, macOS, Linux, Android, FreeBSD, OpenBSD, Solaris, AIX). Android is routed through the
 * Linux implementations. For JDK 25+, the {@code oshi-core-java25} module provides an alternative entry point
 * ({@code oshi.ffm.SystemInfo}) that uses the Foreign Function and Memory (FFM) API for potentially better performance
 * on supported platforms (currently Windows, macOS, and Linux).
 * <p>
 * Both this class and the FFM entry point require native access. Starting with <a href="https://openjdk.org/jeps/472">
 * JEP 472</a> (JDK 24), the JVM warns when native code is loaded, and a future JDK release will require
 * {@code --enable-native-access}. Applications that cannot enable native access can depend on the {@code oshi-common}
 * module alone and implement the OSHI interfaces without native calls. See the {@link oshi oshi package documentation}
 * for details.
 */
public class SystemInfo {

    private static final String NOT_SUPPORTED = "Operating system not supported: ";

    private final Supplier<OperatingSystem> os = memoize(SystemInfo::createOperatingSystem);

    private final Supplier<HardwareAbstractionLayer> hardware = memoize(SystemInfo::createHardware);

    /**
     * Create a new instance of {@link SystemInfo}. This is the main entry point to OSHI and provides access to
     * cross-platform code.
     * <p>
     * Platform-specific Hardware and Software objects are retrieved via memoized suppliers. To conserve memory at the
     * cost of additional processing time, create a new version of SystemInfo() for subsequent calls. To conserve
     * processing time at the cost of additional memory usage, re-use the same {@link SystemInfo} object for future
     * queries.
     */
    public SystemInfo() {
        // Intentionally empty, here to enable the constructor javadoc.
    }

    /**
     * Gets the {@link PlatformEnum} value representing this system.
     *
     * @return Returns the current platform
     * @deprecated Use {@link PlatformEnum#getCurrentPlatform()} instead.
     */
    @Deprecated
    public static PlatformEnum getCurrentPlatform() {
        return PlatformEnum.getCurrentPlatform();
    }

    /**
     * Creates a new instance of the appropriate platform-specific {@link oshi.software.os.OperatingSystem}.
     *
     * @return A new instance of {@link oshi.software.os.OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        return os.get();
    }

    private static OperatingSystem createOperatingSystem() {
        switch (PlatformEnum.getCurrentPlatform()) {
            case LINUX:
            case ANDROID:
                return new LinuxOperatingSystemJNA();
            case MACOS:
                return new MacOperatingSystemJNA();
            case WINDOWS:
                return new WindowsOperatingSystem();
            case SOLARIS:
                return new SolarisOperatingSystem();
            case FREEBSD:
                return new FreeBsdOperatingSystem();
            case AIX:
                return new AixOperatingSystem();
            case OPENBSD:
                return new OpenBsdOperatingSystem();
            default:
                throw new UnsupportedOperationException(NOT_SUPPORTED + PlatformEnum.getCurrentPlatform().getName());
        }
    }

    /**
     * Creates a new instance of the appropriate platform-specific {@link oshi.hardware.HardwareAbstractionLayer}.
     *
     * @return A new instance of {@link oshi.hardware.HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        return hardware.get();
    }

    private static HardwareAbstractionLayer createHardware() {
        switch (PlatformEnum.getCurrentPlatform()) {
            case LINUX:
            case ANDROID:
                return new LinuxHardwareAbstractionLayerJNA();
            case MACOS:
                return new MacHardwareAbstractionLayerJNA();
            case WINDOWS:
                return new WindowsHardwareAbstractionLayer();
            case SOLARIS:
                return new SolarisHardwareAbstractionLayer();
            case FREEBSD:
                return new FreeBsdHardwareAbstractionLayer();
            case AIX:
                return new AixHardwareAbstractionLayer();
            case OPENBSD:
                return new OpenBsdHardwareAbstractionLayer();
            default:
                throw new UnsupportedOperationException(NOT_SUPPORTED + PlatformEnum.getCurrentPlatform().getName());
        }
    }
}
