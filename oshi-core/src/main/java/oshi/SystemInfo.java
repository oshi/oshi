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
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.openbsd.OpenBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystemJNA;
import oshi.software.os.mac.MacOperatingSystemJNA;
import oshi.software.os.unix.aix.AixOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;

/**
 * System information. This is the main entry point to OSHI.
 * <p>
 * This object provides getters which instantiate the appropriate platform-specific implementations of
 * {@link oshi.software.os.OperatingSystem} (software) and {@link oshi.hardware.HardwareAbstractionLayer} (hardware).
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
