/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

import java.util.function.Supplier;

import static oshi.util.Memoizer.memoize;

public class SystemInfo {

    private static final PlatformEnum CURRENT_PLATFORM;

    static {
        String osName = System.getProperty("os.name");
        CURRENT_PLATFORM = switch (osName) {
        case String name when name.startsWith("Linux") -> PlatformEnum.LINUX;
        case String name when name.startsWith("Mac") || name.startsWith("Darwin") -> PlatformEnum.MACOS;
        case String name when name.startsWith("Windows") -> PlatformEnum.WINDOWS;
        default -> PlatformEnum.UNSUPPORTED;
        };
    }

    private static final String NOT_SUPPORTED = "Unsupported platform: " + CURRENT_PLATFORM;

    private final Supplier<OperatingSystem> os = memoize(SystemInfo::createOperatingSystem);
    private final Supplier<HardwareAbstractionLayer> hardware = memoize(SystemInfo::createHardware);

    /**
     * Constructs a new SystemInfo instance.
     */
    public SystemInfo() {
        // Empty constructor for API consistency
    }

    private static OperatingSystem createOperatingSystem() {
        return switch (CURRENT_PLATFORM) {
        case LINUX -> new LinuxOperatingSystem();
        case MACOS -> new MacOperatingSystem();
        case WINDOWS -> new WindowsOperatingSystem();
        default -> throw new UnsupportedOperationException(NOT_SUPPORTED);
        };
    }

    private static HardwareAbstractionLayer createHardware() {
        return switch (CURRENT_PLATFORM) {
        case LINUX -> new LinuxHardwareAbstractionLayer();
        case MACOS -> new MacHardwareAbstractionLayer();
        case WINDOWS -> new WindowsHardwareAbstractionLayer();
        default -> throw new UnsupportedOperationException(NOT_SUPPORTED);
        };
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
     * Creates a new instance of the appropriate platform-specific {@link OperatingSystem}.
     *
     * @return A new platform-specific instance implementing {@link OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        return os.get();
    }

    /**
     * Creates a new instance of the appropriate platform-specific {@link HardwareAbstractionLayer}.
     *
     * @return A new platform-specific instance implementing {@link HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        return hardware.get();
    }
}
