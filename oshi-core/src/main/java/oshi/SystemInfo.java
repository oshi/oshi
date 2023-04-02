/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Platform;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.unix.aix.AixHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.openbsd.OpenBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.unix.aix.AixOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.openbsd.OpenBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * System information. This is the main entry point to OSHI.
 * <p>
 * This object provides getters which instantiate the appropriate platform-specific implementations of
 * {@link oshi.software.os.OperatingSystem} (software) and {@link oshi.hardware.HardwareAbstractionLayer} (hardware).
 */
public class SystemInfo {

    // The platform isn't going to change, and making this static enables easy
    // access from outside this class
    private static final PlatformEnum CURRENT_PLATFORM = PlatformEnum.getValue(Platform.getOSType());

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
     */
    public static PlatformEnum getCurrentPlatform() {
        return CURRENT_PLATFORM;
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
        switch (CURRENT_PLATFORM) {
        case WINDOWS:
            return new WindowsOperatingSystem();
        case LINUX:
        case ANDROID:
            return new LinuxOperatingSystem();
        case MACOS:
            return new MacOperatingSystem();
        case SOLARIS:
            return new SolarisOperatingSystem();
        case FREEBSD:
            return new FreeBsdOperatingSystem();
        case AIX:
            return new AixOperatingSystem();
        case OPENBSD:
            return new OpenBsdOperatingSystem();
        default:
            throw new UnsupportedOperationException(NOT_SUPPORTED + CURRENT_PLATFORM.getName());
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
        switch (CURRENT_PLATFORM) {
        case WINDOWS:
            return new WindowsHardwareAbstractionLayer();
        case LINUX:
        case ANDROID:
            return new LinuxHardwareAbstractionLayer();
        case MACOS:
            return new MacHardwareAbstractionLayer();
        case SOLARIS:
            return new SolarisHardwareAbstractionLayer();
        case FREEBSD:
            return new FreeBsdHardwareAbstractionLayer();
        case AIX:
            return new AixHardwareAbstractionLayer();
        case OPENBSD:
            return new OpenBsdHardwareAbstractionLayer();
        default:
            throw new UnsupportedOperationException(NOT_SUPPORTED + CURRENT_PLATFORM.getName());
        }
    }
}
